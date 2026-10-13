# 邮件集成与端到端测试

从项目根目录运行。需要 Docker Compose、Python 3.10+、Node.js/npm；项目 `.env` 按根目录 README 配好即可，不需要真实邮箱账号。

```powershell
# 构建测试所用镜像 → 12 项真实集成测试 → 浏览器端到端测试 → 清理隔离环境
python tests/email/run_tests.py all

# 当前镜像已构建时跳过构建
python tests/email/run_tests.py all --skip-build

# 分阶段运行：集成通过后保留环境，继续 E2E；E2E 结束自动清理
python tests/email/run_tests.py integration --skip-build --keep-environment
python tests/email/run_tests.py e2e --skip-build

# 失败后需要检查环境时保留；检查结束手动清理
python tests/email/run_tests.py start --skip-build
python tests/email/run_tests.py stop
```

测试环境固定使用 Compose 项目 `couple-email-tests`，拥有独立 MySQL 卷、JWT 密钥和邮件加密密钥。应用使用真实 SMTP Provider；本地 SMTP 收件服务只收信、不会转发到外部邮箱。测试前校验环境身份，SQL 故障注入和重试时间调整仅操作这个测试项目的数据库。

测试网站为 `http://127.0.0.1:19080`，邮件收件检查接口为 `http://127.0.0.1:19026/messages`。端口需空闲。项目现有 `18080` 服务和数据卷不参与测试。用于本地 SMTP 的账号和授权码均是固定的虚构测试数据。

`integration_test.py` 通过实际 HTTP、JWT 权限、Service、MyBatis/JDBC、MySQL 和 SMTP 验证正常发送、正文、幂等、开关、校验、密码安全、认证失败、5xx 拒收、4xx 重试、重启恢复、权限及真实事务回滚。没有 Mock 应用组件。为了缩短等待，先断言 30 / 120 秒重试间隔已正确入库，再将隔离数据库中的待重试时间提前；实际发送次数仍由应用完成。

`e2e.js` 由 Playwright CLI 的 `run-code --filename` 执行。使用独立管理员和食客浏览器会话，实际点击界面完成配置、测试发送、手机选菜、两份数量、微辣、单品备注、整单备注、指定时间、提交订单，以及大厨接单 → 制作 → 完成，等待食客端自动轮询更新。另验证错误授权码的持续脱敏提示、关闭通知后下单无邮件。脚本不通过 API 创建订单或修改订单状态；只读取 API 和本地收件箱以核对结果。

结果、测试密钥及日志位于 Git 忽略的 `backend/target/email-integration/`：

- `integration-results.json`：12 项集成测试结果。
- `e2e-result.json` / `e2e-result.txt`：端到端断言及原始 CLI 结果。
- `backend.log`：集成期间后端日志，并检查测试授权码没有出现在日志中。

浏览器截图和食客回放位于 Git 忽略的 `output/playwright/email-e2e-*`。失败时保留这些产物便于定位；默认删除临时测试容器和数据库卷。`--keep-environment` 只保留隔离测试环境。

覆盖的是本地 SMTP 协议投递。TLS/STARTTLS 设置分支已有后端单元测试；外部邮件服务的真实证书、账号认证和收件箱投递仍须配置邮箱后验证。
