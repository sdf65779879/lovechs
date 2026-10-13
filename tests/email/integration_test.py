"""Real HTTP -> Spring Security -> Service -> MySQL -> SMTP tests. No application mocks."""
import json
import time
import unittest
import urllib.error
import urllib.request
import uuid

from run_tests import OUTPUT, compose, ensure_isolated, sql, wait_ready

BASE = "http://127.0.0.1:19080/api/v1"
INBOX = "http://127.0.0.1:19026"


def http(url, method="GET", body=None, token=None, extra_headers=None):
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if token:
        headers["Authorization"] = "Bearer " + token
    headers.update(extra_headers or {})
    request = urllib.request.Request(url, data=None if body is None else json.dumps(body, ensure_ascii=False).encode(), headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


class EmailIntegrationTest(unittest.TestCase):
    results = []

    @classmethod
    def setUpClass(cls):
        ensure_isolated()
        if http(INBOX + "/health")[1].get("fixture") != "couple-email-tests":
            raise RuntimeError("Refusing to test against an unknown mail server")
        cls.admin = http(BASE + "/auth/login", "POST", {"username": "admin", "password": "admin123"})[1]["data"]["token"]
        cls.user = http(BASE + "/auth/login", "POST", {"username": "love", "password": "love123"})[1]["data"]["token"]
        if cls.call("/admin/email/settings")["data"]["provider"] != "SMTP":
            raise RuntimeError("Integration tests require the real SMTP provider")

    @classmethod
    def tearDownClass(cls):
        OUTPUT.mkdir(parents=True, exist_ok=True)
        (OUTPUT / "integration-results.json").write_text(json.dumps(cls.results, ensure_ascii=False, indent=2), encoding="utf-8")

    @classmethod
    def call(cls, path, method="GET", body=None, user=False, extra_headers=None):
        return http(BASE + path, method, body, cls.user if user else cls.admin, extra_headers)[1]

    def setUp(self):
        self.config = {
            "smtp": {"host": "mail", "port": 2525, "encryption": "NONE", "fromAddress": "sender@example.test",
                     "username": "sender@example.test", "password": "fixture-app-password", "clearPassword": False},
            "channel": {"enabled": True, "recipients": "Chef@example.test;chef@EXAMPLE.test;second@example.test", "event": "NEW_ORDER"},
        }
        self.save()
        self.call("/cart", "DELETE", user=True)
        http(INBOX + "/control", "POST", {"clear": True})

    def tearDown(self):
        successful = not any(test is self for test, _ in self._outcome.result.failures + self._outcome.result.errors)
        self.results.append({"test": self._testMethodName, "passed": successful})

    def save(self, config=None):
        response = self.call("/admin/email/settings", "PUT", config or self.config)
        self.assertEqual(response["code"], 0, response.get("message"))

    def queue_test(self):
        before = self.call("/admin/email/deliveries")["data"]
        ids = {item["id"] for item in before}
        self.assertEqual(self.call("/admin/email/test", "POST")["code"], 0)
        return next(item["id"] for item in self.call("/admin/email/deliveries")["data"] if item["id"] not in ids)

    def delivery(self, ident):
        return next(item for item in self.call("/admin/email/deliveries")["data"] if item["id"] == ident)

    def wait_status(self, ident, status, timeout=12):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            item = self.delivery(ident)
            if item["status"] == status:
                return item
            time.sleep(0.2)
        self.fail(f"Delivery {ident} expected {status}, got {item['status']}: {item['errorMessage']}")

    def inbox(self):
        return http(INBOX + "/messages")[1]["messages"]

    def add_cart(self):
        self.assertEqual(self.call("/cart", "POST", {"dishId": 1, "quantity": 2, "taste": "微辣", "remark": "少盐"}, user=True)["code"], 0)

    def order(self, key=None):
        return self.call("/orders", "POST", {"remark": "不要葱 <script>纯文本</script>", "expectTime": "2026-10-13 19:00"},
                         user=True, extra_headers={"Idempotency-Key": key or str(uuid.uuid4())})

    def test_01_smtp_authentication_mime_and_recipient_deduplication(self):
        ident = self.queue_test()
        item = self.wait_status(ident, "SENT")
        self.assertEqual(item["attempts"], 1)
        self.assertEqual(item["recipientCount"], 2)
        mails = self.inbox()
        self.assertEqual(len(mails), 1)
        self.assertEqual(mails[0]["from"], "sender@example.test")
        self.assertEqual(mails[0]["recipients"], ["Chef@example.test", "second@example.test"])
        self.assertIn("大厨新订单通知测试", mails[0]["subject"])
        self.assertIn("这是一封测试邮件", mails[0]["body"])
        self.assertIsNone(mails[0]["bccHeader"])

    def test_02_customer_order_sends_snapshot_and_idempotent_replay_sends_once(self):
        self.add_cart()
        key = str(uuid.uuid4())
        response = self.order(key)
        self.assertEqual(response["code"], 0)
        order = response["data"]
        replay = self.order(key)
        self.assertEqual(replay["data"]["id"], order["id"])
        delivery = [item for item in self.call("/admin/email/deliveries")["data"] if item.get("orderId") == order["id"]]
        self.assertEqual(len(delivery), 1)
        self.wait_status(delivery[0]["id"], "SENT")
        self.assertEqual(len(self.inbox()), 1)
        mail = self.inbox()[0]
        self.assertIn(order["orderNo"], mail["subject"])
        for text in ["宝贝", "番茄牛腩拌面 × 2", "微辣", "少盐", "不要葱 <script>纯文本</script>", "2026-10-13 19:00"]:
            self.assertIn(text, mail["body"])
        self.assertEqual(self.call("/cart", user=True)["data"], [])

    def test_03_disabled_channel_has_no_order_notification_but_allows_explicit_test(self):
        self.config["channel"]["enabled"] = False
        self.save()
        self.add_cart()
        order = self.order()["data"]
        self.assertFalse(any(item.get("orderId") == order["id"] for item in self.call("/admin/email/deliveries")["data"]))
        self.assertEqual(self.inbox(), [])
        self.wait_status(self.queue_test(), "SENT")

    def test_04_invalid_recipients_reject_entire_update(self):
        original = self.call("/admin/email/settings")["data"]
        for recipients in ["invalid", "a@example.test\r\nBcc: victim@example.test", "Chef <chef@example.test>",
                           ",".join(f"chef{i}@example.test" for i in range(6))]:
            self.config["channel"]["recipients"] = recipients
            response = self.call("/admin/email/settings", "PUT", self.config)
            self.assertNotEqual(response["code"], 0)
            self.assertEqual(self.call("/admin/email/settings")["data"], original)

    def test_05_password_is_encrypted_hidden_retained_and_explicitly_cleared(self):
        raw = sql("SELECT config_value FROM sys_config WHERE config_key='email_system';")
        self.assertNotIn("fixture-app-password", raw)
        self.assertIn("v1:", raw)
        view = self.call("/admin/email/settings")["data"]
        self.assertTrue(view["smtp"]["passwordConfigured"])
        self.assertNotIn("encryptedPassword", json.dumps(view))
        self.config["smtp"]["password"] = ""
        self.save()
        self.wait_status(self.queue_test(), "SENT")
        self.config["channel"]["enabled"] = False
        self.config["smtp"]["clearPassword"] = True
        self.save()
        self.assertFalse(self.call("/admin/email/settings")["data"]["smtp"]["passwordConfigured"])

    def test_06_authentication_failure_is_permanent_safe_and_does_not_rollback_order(self):
        self.config["smtp"]["password"] = "wrong-fixture-password"
        self.save()
        self.add_cart()
        order = self.order()["data"]
        ident = next(item["id"] for item in self.call("/admin/email/deliveries")["data"] if item.get("orderId") == order["id"])
        item = self.wait_status(ident, "FAILED")
        self.assertEqual(item["attempts"], 1)
        self.assertIn("认证失败", item["errorMessage"])
        self.assertNotIn("fixture-private-response", json.dumps(item))
        self.assertEqual(self.inbox(), [])
        self.assertEqual(self.call(f"/orders/{order['id']}", user=True)["data"]["status"], "PENDING")
        self.assertEqual(self.call(f"/admin/orders/{order['id']}/status", "PUT", {"status": "ACCEPTED"})["code"], 0)

    def test_07_smtp_5xx_is_not_retried_and_vendor_response_is_hidden(self):
        http(INBOX + "/control", "POST", {"replyCode": 550, "remaining": 1})
        item = self.wait_status(self.queue_test(), "FAILED")
        self.assertEqual(item["attempts"], 1)
        self.assertIn("拒收", item["errorMessage"])
        self.assertNotIn("fixture-private-response", item["errorMessage"])
        self.assertEqual(self.inbox(), [])

    def test_08_transient_retry_survives_restart_and_uses_latest_recipients(self):
        http(INBOX + "/control", "POST", {"replyCode": 451, "remaining": 1})
        ident = self.queue_test()
        self.wait_status(ident, "RETRY")
        delay = int(sql(f"SELECT TIMESTAMPDIFF(SECOND,updated_at,next_attempt_at) FROM email_delivery WHERE id={ident};"))
        self.assertGreaterEqual(delay, 29)
        self.assertLessEqual(delay, 31)
        self.config["channel"]["recipients"] = "updated-chef@example.test"
        self.save()
        compose("restart", "backend")
        wait_ready()
        self.assertEqual(self.delivery(ident)["status"], "RETRY")
        # Only the isolated test database is advanced; production retry intervals are asserted above.
        sql(f"UPDATE email_delivery SET next_attempt_at=CURRENT_TIMESTAMP WHERE id={ident};")
        item = self.wait_status(ident, "SENT")
        self.assertEqual(item["attempts"], 2)
        self.assertEqual(self.inbox()[0]["recipients"], ["updated-chef@example.test"])

    def test_09_retry_stops_after_three_actual_smtp_attempts(self):
        http(INBOX + "/control", "POST", {"replyCode": 451, "remaining": 3})
        ident = self.queue_test()
        self.wait_status(ident, "RETRY")
        sql(f"UPDATE email_delivery SET next_attempt_at=CURRENT_TIMESTAMP WHERE id={ident};")
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline and self.delivery(ident)["attempts"] < 2:
            time.sleep(0.2)
        item = self.wait_status(ident, "RETRY")
        self.assertEqual(item["attempts"], 2)
        delay = int(sql(f"SELECT TIMESTAMPDIFF(SECOND,updated_at,next_attempt_at) FROM email_delivery WHERE id={ident};"))
        self.assertGreaterEqual(delay, 119)
        self.assertLessEqual(delay, 121)
        sql(f"UPDATE email_delivery SET next_attempt_at=CURRENT_TIMESTAMP WHERE id={ident};")
        item = self.wait_status(ident, "FAILED")
        self.assertEqual(item["attempts"], 3)
        self.assertEqual(http(INBOX + "/messages")[1]["authenticatedConnections"], 3)
        self.assertEqual(self.inbox(), [])

    def test_10_admin_security_public_config_and_generic_config_protection(self):
        for path, method in [("/admin/email/settings", "GET"), ("/admin/email/settings", "PUT"),
                             ("/admin/email/deliveries", "GET"), ("/admin/email/test", "POST")]:
            self.assertEqual(http(BASE + path, method, self.config if method == "PUT" else None)[0], 401)
            self.assertEqual(http(BASE + path, method, self.config if method == "PUT" else None, self.user)[0], 403)
        self.assertNotIn("email_", json.dumps(self.call("/admin/configs")["data"]))
        public = self.call("/configs/public", user=True)["data"]
        self.assertEqual(set(public), {"businessOpen", "notice"})
        for key in ["email_system", "EMAIL_SYSTEM", " email_order_channel "]:
            self.assertNotEqual(self.call("/admin/configs", "PUT", {"configs": {key: "unsafe"}})["code"], 0)

    def test_11_real_transaction_rolls_back_order_items_and_cart_on_queue_failure(self):
        self.add_cart()
        before = sql("SELECT COUNT(*) FROM orders;")
        before_items = sql("SELECT COUNT(*) FROM order_item;")
        sql("CREATE TRIGGER qa_reject_email BEFORE INSERT ON email_delivery FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture queue failure';")
        try:
            response = self.order()
            self.assertNotEqual(response["code"], 0)
            self.assertEqual(sql("SELECT COUNT(*) FROM orders;"), before)
            self.assertEqual(sql("SELECT COUNT(*) FROM order_item;"), before_items)
            self.assertEqual(len(self.call("/cart", user=True)["data"]), 1)
            self.assertEqual(self.inbox(), [])
        finally:
            sql("DROP TRIGGER IF EXISTS qa_reject_email;")

    def test_12_disable_channel_before_retry_skips_pending_delivery(self):
        http(INBOX + "/control", "POST", {"replyCode": 451, "remaining": 1})
        self.add_cart()
        order = self.order()["data"]
        ident = next(item["id"] for item in self.call("/admin/email/deliveries")["data"] if item.get("orderId") == order["id"])
        self.wait_status(ident, "RETRY")
        self.config["channel"]["enabled"] = False
        self.save()
        sql(f"UPDATE email_delivery SET next_attempt_at=CURRENT_TIMESTAMP WHERE id={ident};")
        self.wait_status(ident, "SKIPPED")
        self.assertEqual(self.inbox(), [])


if __name__ == "__main__":
    unittest.main(verbosity=2)
