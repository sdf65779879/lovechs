"""An isolated SMTP receiver, with controllable failures and a JSON inbox.

Never forwards mail. The Compose network is private and only the inbox HTTP
port is published, on the host loopback interface. Credentials are synthetic.
"""
import argparse
import base64
import json
import socketserver
import threading
from email import policy
from email.parser import BytesParser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class Inbox:
    lock = threading.Lock()
    messages = []
    reply_code = 250
    remaining = 0
    authenticated_connections = 0


class SmtpHandler(socketserver.StreamRequestHandler):
    def reply(self, text):
        self.wfile.write((text + "\r\n").encode())
        self.wfile.flush()

    def line(self):
        return self.rfile.readline(65536).decode("utf-8", errors="replace").rstrip("\r\n")

    def authenticate(self, username, password):
        if username == "sender@example.test" and password == "fixture-app-password":
            with Inbox.lock:
                Inbox.authenticated_connections += 1
            self.reply("235 2.7.0 Authentication successful")
            return True
        self.reply("535 5.7.8 fixture-private-response credential denied")
        return False

    def handle(self):
        self.connection.settimeout(10)
        self.reply("220 isolated-fixture ESMTP")
        authenticated = False
        sender = ""
        recipients = []
        try:
            while True:
                line = self.line()
                if not line:
                    return
                command = line.split(" ", 1)[0].upper()
                if command in ("EHLO", "HELO"):
                    self.reply("250-isolated-fixture")
                    self.reply("250-AUTH PLAIN LOGIN")
                    self.reply("250 SIZE 1048576")
                elif command == "AUTH":
                    parts = line.split()
                    if parts[1].upper() == "PLAIN":
                        if len(parts) < 3:
                            self.reply("334 ")
                        payload = base64.b64decode(parts[2] if len(parts) > 2 else self.line()).decode().split("\x00")
                        authenticated = self.authenticate(payload[-2], payload[-1])
                    elif parts[1].upper() == "LOGIN":
                        if len(parts) > 2:
                            username = base64.b64decode(parts[2]).decode()
                        else:
                            self.reply("334 VXNlcm5hbWU6")
                            username = base64.b64decode(self.line()).decode()
                        self.reply("334 UGFzc3dvcmQ6")
                        authenticated = self.authenticate(username, base64.b64decode(self.line()).decode())
                    else:
                        self.reply("504 Unsupported authentication")
                elif command == "MAIL":
                    if not authenticated:
                        self.reply("530 Authentication required")
                        continue
                    sender = line.split(":", 1)[1].strip().strip("<>")
                    recipients = []
                    self.reply("250 Sender accepted")
                elif command == "RCPT":
                    recipients.append(line.split(":", 1)[1].strip().strip("<>"))
                    self.reply("250 Recipient accepted")
                elif command == "DATA":
                    self.reply("354 End with dot")
                    raw = bytearray()
                    while True:
                        item = self.rfile.readline(65536)
                        if item == b".\r\n":
                            break
                        if not item or len(raw) + len(item) > 1048576:
                            return
                        raw.extend(item[1:] if item.startswith(b"..") else item)
                    with Inbox.lock:
                        code = Inbox.reply_code if Inbox.remaining else 250
                        if Inbox.remaining:
                            Inbox.remaining -= 1
                        if code == 250:
                            message = BytesParser(policy=policy.default).parsebytes(bytes(raw))
                            body = message.get_body(preferencelist=("plain",))
                            Inbox.messages.append({
                                "id": len(Inbox.messages) + 1,
                                "from": sender,
                                "recipients": list(recipients),
                                "subject": str(message["Subject"]),
                                "body": body.get_content() if body else "",
                                "bccHeader": message.get("Bcc"),
                            })
                    self.reply(f"{code} fixture-private-response {'accepted' if code == 250 else 'delivery denied'}")
                elif command == "QUIT":
                    self.reply("221 Bye")
                    return
                elif command in ("RSET", "NOOP"):
                    self.reply("250 OK")
                else:
                    self.reply("502 Unsupported command")
        except (TimeoutError, ConnectionError, ValueError):
            return


class SmtpServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


class InboxHandler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # Do not log bodies or authentication data.

    def respond(self, data, status=200):
        raw = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path == "/health":
            self.respond({"fixture": "couple-email-tests", "status": "ok"})
        elif self.path == "/messages":
            with Inbox.lock:
                self.respond({"messages": Inbox.messages, "authenticatedConnections": Inbox.authenticated_connections})
        else:
            self.respond({"error": "not found"}, 404)

    def do_POST(self):
        if self.path != "/control":
            self.respond({"error": "not found"}, 404)
            return
        data = json.loads(self.rfile.read(int(self.headers.get("Content-Length", "0"))))
        with Inbox.lock:
            if data.get("clear"):
                Inbox.messages = []
                Inbox.authenticated_connections = 0
            Inbox.reply_code = int(data.get("replyCode", 250))
            Inbox.remaining = int(data.get("remaining", 0))
        self.respond({"ok": True})


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    args = parser.parse_args()
    smtp = SmtpServer((args.host, 2525), SmtpHandler)
    threading.Thread(target=smtp.serve_forever, daemon=True).start()
    print("Isolated SMTP fixture ready", flush=True)
    ThreadingHTTPServer((args.host, 8026), InboxHandler).serve_forever()
