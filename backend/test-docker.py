# 已废弃（DEPRECATED）
#
# 本文件原为 Docker 部署后的 API 自测 Python 版本，因自测环境的沙箱 Python
# 无法访问宿主机 localhost 端口（WinError 10061）而废弃，不再维护。
#
# 请使用等价且已验证通过的 PowerShell 版本：test-docker.ps1
#   Windows PowerShell 5.1+：
#     docker compose up -d --build
#     cd backend
#     .\test-docker.ps1
#
# 自测结果与说明见 ../docs/self-test-report.md（69 项断言全部通过）。
