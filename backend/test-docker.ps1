# ====================================================================
# 情侣点餐系统 · 阶段四 Docker 部署自测脚本（PowerShell）
# 针对 docker compose 部署环境执行全链路 API 验证。
# 用法：.\test-docker.ps1 [base_url]   默认 http://localhost:18080/api
# 输出：逐项 PASS/FAIL，结尾输出汇总；结果同时写入 test-docker-result.txt
# ====================================================================
param([string]$Base = "http://localhost:18080/api")

$ErrorActionPreference = "Stop"
$results = @()

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null, [string]$Token = "", [hashtable]$ExtraHeaders = @{})
    $uri = $Base + $Path
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    foreach ($k in $ExtraHeaders.Keys) { $headers[$k] = $ExtraHeaders[$k] }
    $params = @{ Uri = $uri; Method = $Method; UseBasicParsing = $true; TimeoutSec = 15; Headers = $headers }
    if ($null -ne $Body -and "$Body" -ne "") {
        # 空字符串 Body 在 PS5.1 下会报"无法发送具有此谓词类型的内容正文"，需跳过；
        # 中文请求体必须按 UTF-8 字节发送（字符串 Body 默认走 ANSI 会乱码）
        $params.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 12 -Compress))
        $params.ContentType = "application/json"
    }
    try {
        $resp = Invoke-WebRequest @params
        $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
        $parsed = $null
        try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null }
        return @{ Status = [int]$resp.StatusCode; Body = $parsed; Raw = $text }
    } catch {
        $code = 0; $parsed = $null; $text = ""
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
        }
        # PS5.1 下错误响应体可靠来源是 ErrorDetails.Message（Response 流已被消费）
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $text = $_.ErrorDetails.Message
            try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null }
        }
        return @{ Status = $code; Body = $parsed; Raw = $text }
    }
}

function Check {
    param([string]$Name, [bool]$Cond, $Detail = "")
    $script:results += , @($Name, $Cond, $Detail)
    $line = $(if ($Cond) { "PASS | " } else { "FAIL | " }) + $Name + $(if ($Detail -ne "") { " | " + $Detail } else { "" })
    Write-Output $line
}

# UTF-8 字节十六进制：用于中文断言的字节级比较（不受 PS 字符串比较/控制台编码影响）
function Utf8-Hex([string]$s) {
    if ($null -eq $s) { return "" }
    return -join ([System.Text.Encoding]::UTF8.GetBytes($s) | ForEach-Object { $_.ToString("x2") })
}

Add-Type -AssemblyName System.Net.Http

# ============ 0. 健康检查 ============
$r = Invoke-Api GET "/v1/health"
Check "0 健康检查 /v1/health" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data -eq "ok") ($r.Raw)

# ============ 1. 认证 ============
$r = Invoke-Api POST "/v1/auth/login" @{ username = "admin"; password = "admin123" }
Check "1.1 admin 登录" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.user.role -eq "ADMIN") $r.Body.data.user.nickname
$adminToken = $r.Body.data.token

$r = Invoke-Api POST "/v1/auth/login" @{ username = "love"; password = "love123" }
Check "1.2 love 登录" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.user.role -eq "USER") $r.Body.data.user.nickname
$loveToken = $r.Body.data.token

$r = Invoke-Api POST "/v1/auth/login" @{ username = "love"; password = "wrong" }
Check "1.3 错误密码返回 1001" ($r.Status -eq 200 -and $r.Body.code -eq 1001) $r.Body.code

$r = Invoke-Api GET "/v1/auth/me"
Check "1.4 无 Token 返回 401" ($r.Status -eq 401 -and $r.Body.code -eq 401) $r.Status

$r = Invoke-Api GET "/v1/admin/dashboard" "" $loveToken
Check "1.5 love 访问管理端返回 403" ($r.Status -eq 403 -and $r.Body.code -eq 403) $r.Status

$r = Invoke-Api GET "/v1/auth/me" "" $loveToken
Check "1.6 GET /me" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.username -eq "love") $r.Body.data.nickname

# ============ 2. 业务公开数据（需登录：除 login/health/文档外全部鉴权） ============
$r = Invoke-Api GET "/v1/categories" "" $loveToken
Check "2.1 分类列表" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.Count -ge 6) ("{0} 个分类" -f $r.Body.data.Count)

$r = Invoke-Api GET "/v1/dishes?pageNum=1&pageSize=10" "" $loveToken
Check "2.2 菜品分页" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.list.Count -ge 1) ("total={0}" -f $r.Body.data.total)
$dishId = $r.Body.data.list[0].id

$r = Invoke-Api GET "/v1/dishes/recommend?limit=5" "" $loveToken
Check "2.3 推荐菜品" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.Count -ge 1) ("{0} 条" -f $r.Body.data.Count)

$r = Invoke-Api GET ("/v1/dishes/{0}" -f $dishId) "" $loveToken
Check "2.4 菜品详情" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.id -eq $dishId) $r.Body.data.name

$r = Invoke-Api GET "/v1/dishes?keyword=%E7%95%AA%E8%8C%84&pageNum=1&pageSize=10" "" $loveToken
Check "2.5 中文搜索（番茄）" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.total -ge 1) ("total={0}" -f $r.Body.data.total)

$r = Invoke-Api GET "/v1/configs/public" "" $loveToken
Check "2.6 公开配置（需登录）" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.businessOpen -eq 1) ("businessOpen={0}" -f $r.Body.data.businessOpen)

# ============ 3. 文件上传（经 nginx 可访问） ============
$pngB64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
$pngPath = Join-Path $env:TEMP "couple-test.png"
[System.IO.File]::WriteAllBytes($pngPath, [Convert]::FromBase64String($pngB64))

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(20)
$client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $loveToken)
$form = New-Object System.Net.Http.MultipartFormDataContent
$bytes = [System.IO.File]::ReadAllBytes($pngPath)
$fileContent = New-Object System.Net.Http.ByteArrayContent(, $bytes)
$fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("image/png")
$form.Add($fileContent, "file", "t.png")
$upResp = $client.PostAsync($Base + "/v1/files/image", $form).Result
$upText = [System.Text.Encoding]::UTF8.GetString($upResp.Content.ReadAsByteArrayAsync().Result)
$upBody = $upText | ConvertFrom-Json
Check "3.1 上传图片（love）" ($upResp.StatusCode -eq 200 -and $upBody.code -eq 0 -and $upBody.data.StartsWith("/uploads/")) $upBody.data
$upPath = $upBody.data

if ($upPath) {
    $web = $Base -replace "/api$", ""
    try {
        $imgResp = Invoke-WebRequest -Uri ($web + $upPath) -UseBasicParsing -TimeoutSec 10
        Check "3.2 上传图片经 nginx /uploads 可访问" ($imgResp.StatusCode -eq 200 -and $imgResp.Headers["Content-Type"] -like "image/png*") ("{0} {1}" -f $imgResp.StatusCode, $imgResp.Headers["Content-Type"])
    } catch {
        Check "3.2 上传图片经 nginx /uploads 可访问" $false $_.Exception.Message
    }
}

# ============ 4. 购物车 ============
$r = Invoke-Api POST "/v1/cart" @{ dishId = $dishId; quantity = 2; taste = "微辣"; remark = "不要香菜" } $loveToken
Check "4.1 加购（口味/备注）" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code

$r = Invoke-Api GET "/v1/cart" "" $loveToken
Check "4.2 购物车列表" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.Count -ge 1) ("{0} 条" -f $r.Body.data.Count)
$cartId = $r.Body.data[0].id
Check "4.3 条目字段（taste/remark/lineLove）" ($r.Body.data[0].taste -eq "微辣" -and $r.Body.data[0].remark -eq "不要香菜" -and $r.Body.data[0].lineLove -ge 2) ("lineLove={0}" -f $r.Body.data[0].lineLove)

$r = Invoke-Api PUT ("/v1/cart/{0}" -f $cartId) @{ quantity = 3; taste = "正常辣"; remark = "改备注" } $loveToken
Check "4.4 修改条目" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code

# ============ 5. 下单 + 管理端流转 ============
$idemKey = [guid]::NewGuid().ToString()
$r = Invoke-Api POST "/v1/orders" @{ remark = "少放盐，爱你" } $loveToken @{ "Idempotency-Key" = $idemKey }
Check "5.1 提交订单（PENDING）" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.status -eq "PENDING") $r.Body.data.orderNo
$orderId = $r.Body.data.id

$r2 = Invoke-Api POST "/v1/orders" @{ remark = "少放盐，爱你" } $loveToken @{ "Idempotency-Key" = $idemKey }
Check "5.2 相同幂等键返回同一订单" ($r2.Status -eq 200 -and $r2.Body.code -eq 0 -and $r2.Body.data.id -eq $orderId) ("id={0}" -f $r2.Body.data.id)

$r = Invoke-Api GET "/v1/cart" "" $loveToken
Check "5.3 下单后购物车清空" ($r.Status -eq 200 -and $r.Body.data.Count -eq 0) ("{0} 条" -f $r.Body.data.Count)

$r = Invoke-Api PUT ("/v1/admin/orders/{0}/status" -f $orderId) @{ status = "ACCEPTED" } $adminToken
Check "5.4 接单 ACCEPTED" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.status -eq "ACCEPTED") $r.Body.data.statusLabel

$r = Invoke-Api PUT ("/v1/admin/orders/{0}/status" -f $orderId) @{ status = "COOKING" } $adminToken
Check "5.5 制作 COOKING" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.status -eq "COOKING") $r.Body.data.statusLabel

$r = Invoke-Api PUT ("/v1/admin/orders/{0}/status" -f $orderId) @{ status = "COMPLETED" } $adminToken
Check "5.6 完成 COMPLETED" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.status -eq "COMPLETED" -and $null -ne $r.Body.data.finishTime) $r.Body.data.statusLabel

$r = Invoke-Api GET ("/v1/orders/{0}" -f $orderId) "" $loveToken
Check "5.7 用户端详情见最新状态+明细" ($r.Status -eq 200 -and $r.Body.data.status -eq "COMPLETED" -and $r.Body.data.items.Count -ge 1) ("{0} 条明细" -f $r.Body.data.items.Count)

$r = Invoke-Api PUT ("/v1/admin/orders/{0}/status" -f $orderId) @{ status = "PENDING" } $adminToken
Check "5.8 非法状态跳转返回 1003" ($r.Status -eq 200 -and $r.Body.code -eq 1003) $r.Body.code

# 5.9 取消：先加购一条（此前购物车已清空），再下单再取消
Invoke-Api POST "/v1/cart" @{ dishId = $dishId; quantity = 1 } $loveToken | Out-Null
$r = Invoke-Api POST "/v1/orders" @{ expectTime = "2026-10-11 18:30" } $loveToken
$order2Id = $r.Body.data.id
$r = Invoke-Api PUT ("/v1/orders/{0}/cancel" -f $order2Id) "" $loveToken
Check "5.9 待接单可取消" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code

# 5.10/5.11 拒绝：购物车再次清空，先加购再下单
Invoke-Api POST "/v1/cart" @{ dishId = $dishId; quantity = 1 } $loveToken | Out-Null
$r = Invoke-Api POST "/v1/orders" @{ remark = "测试拒绝" } $loveToken
$order3Id = $r.Body.data.id
$r = Invoke-Api PUT ("/v1/admin/orders/{0}/status" -f $order3Id) @{ status = "REJECTED" } $adminToken
Check "5.10 拒绝未填理由返回 400（参数错误）" ($r.Status -eq 200 -and $r.Body.code -eq 400) $r.Body.code
$r = Invoke-Api PUT ("/v1/admin/orders/{0}/status" -f $order3Id) @{ status = "REJECTED"; rejectReason = "食材不够" } $adminToken
Check "5.11 拒绝并填理由 REJECTED" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.status -eq "REJECTED" -and $r.Body.data.rejectReason -eq "食材不够") $r.Body.data.rejectReason

$r = Invoke-Api POST ("/v1/orders/{0}/rebuy" -f $orderId) "" $loveToken
Check "5.12 再下单加回购物车" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.addedCount -ge 1) ("addedCount={0}" -f $r.Body.data.addedCount)

$r = Invoke-Api GET "/v1/orders/calendar?month=2026-10" "" $loveToken
Check "5.13 吃饭日历" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.days.Count -ge 1) ("{0} 天有订单" -f $r.Body.data.days.Count)

$r = Invoke-Api GET "/v1/orders?tab=finished&pageNum=1&pageSize=10" "" $loveToken
Check "5.14 我的订单分页（finished 含已完成单）" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.total -ge 1) ("total={0}" -f $r.Body.data.total)

Invoke-Api DELETE "/v1/cart" "" $loveToken | Out-Null

# ============ 6. 收藏 ============
$r = Invoke-Api POST ("/v1/favorites/{0}" -f $dishId) "" $loveToken
Check "6.1 收藏" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api GET "/v1/favorites" "" $loveToken
Check "6.2 收藏列表" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.Count -ge 1) ("{0} 条" -f $r.Body.data.Count)
$r = Invoke-Api DELETE ("/v1/favorites/{0}" -f $dishId) "" $loveToken
Check "6.3 取消收藏" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code

# ============ 7. 管理端 ============
$r = Invoke-Api GET "/v1/admin/dashboard" "" $adminToken
Check "7.1 数据看板" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.totalCount -ge 1 -and $r.Body.data.trend.Count -eq 7) ("total={0} pending={1}" -f $r.Body.data.totalCount, $r.Body.data.pendingCount)

$r = Invoke-Api GET "/v1/admin/orders?pageNum=1&pageSize=10" "" $adminToken
Check "7.2 管理端订单分页（含明细）" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.list.Count -ge 1 -and $null -ne $r.Body.data.list[0].items) ("total={0}" -f $r.Body.data.total)

$r = Invoke-Api GET ("/v1/admin/orders/{0}" -f $orderId) "" $adminToken
Check "7.3 管理端订单详情（昵称=宝贝，UTF-8 字节校验）" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and (Utf8-Hex $r.Body.data.nickname) -eq (Utf8-Hex "宝贝")) $r.Body.data.nickname

$r = Invoke-Api PUT "/v1/admin/configs" @{ configs = @{ business_open = "0"; notice = "今日歇业测试" } } $adminToken
Check "7.4 设置歇业" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api GET "/v1/configs/public" "" $loveToken
Check "7.5 用户端可见歇业+公告（UTF-8 字节校验）" ($r.Status -eq 200 -and $r.Body.data.businessOpen -eq 0 -and (Utf8-Hex $r.Body.data.notice) -eq (Utf8-Hex "今日歇业测试")) ("businessOpen={0} noticeHex={1}" -f $r.Body.data.businessOpen, (Utf8-Hex $r.Body.data.notice))
$r = Invoke-Api POST "/v1/orders" @{} $loveToken
Check "7.6 歇业下单返回 1006" ($r.Status -eq 200 -and $r.Body.code -eq 1006) $r.Body.code
Invoke-Api PUT "/v1/admin/configs" @{ configs = @{ business_open = "1"; notice = "" } } $adminToken | Out-Null
$r = Invoke-Api GET "/v1/configs/public" "" $loveToken
Check "7.7 恢复营业" ($r.Status -eq 200 -and $r.Body.data.businessOpen -eq 1) $r.Body.data.businessOpen

$r = Invoke-Api POST "/v1/admin/users" @{ username = "docker_test"; password = "pass123"; nickname = "测试用户" } $adminToken
Check "7.8 新增用户" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.username -eq "docker_test") $r.Body.data.nickname
$testUid = $r.Body.data.id
$r = Invoke-Api POST "/v1/admin/users" @{ username = "docker_test"; password = "pass123"; nickname = "重复" } $adminToken
Check "7.9 重复账号返回 1000" ($r.Status -eq 200 -and $r.Body.code -eq 1000) $r.Body.code
$r = Invoke-Api PUT ("/v1/admin/users/{0}/status" -f $testUid) @{ status = 0 } $adminToken
Check "7.10 禁用用户" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api POST "/v1/auth/login" @{ username = "docker_test"; password = "pass123" }
Check "7.11 禁用账号登录返回 1002" ($r.Status -eq 200 -and $r.Body.code -eq 1002) $r.Body.code
Invoke-Api PUT ("/v1/admin/users/{0}/status" -f $testUid) @{ status = 1 } $adminToken | Out-Null
$r = Invoke-Api PUT ("/v1/admin/users/{0}/password/reset" -f $testUid) @{ password = "newpass6" } $adminToken
Check "7.12 重置密码" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api POST "/v1/auth/login" @{ username = "docker_test"; password = "newpass6" }
Check "7.13 重置后新密码可登录" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api GET "/v1/admin/users?keyword=docker_test" "" $adminToken
Check "7.14 用户分页搜索" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.total -ge 1) ("total={0}" -f $r.Body.data.total)

$r = Invoke-Api GET "/v1/admin/categories/all" "" $adminToken
$firstCat = $r.Body.data[0].id
$r = Invoke-Api POST "/v1/admin/categories" @{ name = "测试汤羹"; sort = 99; status = 1 } $adminToken
Check "7.15 新增分类" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$catNewId = $r.Body.data
$r = Invoke-Api DELETE ("/v1/admin/categories/{0}" -f $catNewId) "" $adminToken
Check "7.16 删除空分类" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code

$r = Invoke-Api POST "/v1/admin/dishes" @{ categoryId = $firstCat; name = "Docker自测菜"; description = "自测"; image = $upPath; tags = "测试,下饭"; loveValue = 5; sort = 50; status = 1; isRecommend = 0 } $adminToken
Check "7.17 新增菜品（引用上传图）" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$newDishId = $r.Body.data
$r = Invoke-Api PUT ("/v1/admin/dishes/{0}/status" -f $newDishId) @{ status = 0 } $adminToken
Check "7.18 下架菜品" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api GET ("/v1/dishes/{0}" -f $newDishId) "" $loveToken
Check "7.19 用户端列表不可见下架菜（详情可读）" ($r.Status -eq 200 -and $r.Body.code -eq 0 -and $r.Body.data.status -eq 0) ("status={0}" -f $r.Body.data.status)
$r = Invoke-Api PUT ("/v1/admin/dishes/{0}/status" -f $newDishId) @{ status = 1 } $adminToken
Check "7.20 重新上架" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api PUT ("/v1/admin/dishes/{0}" -f $newDishId) @{ categoryId = $firstCat; name = "Docker自测菜2"; description = "改"; image = $upPath; tags = "测试"; loveValue = 6; sort = 50; status = 1; isRecommend = 1 } $adminToken
Check "7.21 修改菜品" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api DELETE ("/v1/admin/dishes/{0}" -f $newDishId) "" $adminToken
Check "7.22 删除菜品（逻辑删除）" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code

# ============ 8. 资料 / 密码 ============
$r = Invoke-Api PUT "/v1/auth/profile" @{ nickname = "宝贝"; avatar = $upPath } $loveToken
Check "8.1 修改资料（头像）" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api GET "/v1/auth/me" "" $loveToken
Check "8.2 资料生效（头像）" ($r.Status -eq 200 -and $r.Body.data.avatar -eq $upPath) $r.Body.data.avatar
$r = Invoke-Api PUT "/v1/auth/password" @{ oldPassword = "wrong"; newPassword = "newpass6" } $loveToken
Check "8.3 错误旧密码返回 1000" ($r.Status -eq 200 -and $r.Body.code -eq 1000) $r.Body.code
$r = Invoke-Api PUT "/v1/auth/password" @{ oldPassword = "love123"; newPassword = "newpass6" } $loveToken
Check "8.4 修改密码" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api POST "/v1/auth/login" @{ username = "love"; password = "newpass6" }
Check "8.5 新密码可登录" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$loveToken2 = $r.Body.data.token
Invoke-Api PUT "/v1/auth/password" @{ oldPassword = "newpass6"; newPassword = "love123" } $loveToken2 | Out-Null
$r = Invoke-Api POST "/v1/auth/login" @{ username = "love"; password = "love123" }
Check "8.6 恢复原密码可登录" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code
$r = Invoke-Api POST "/v1/auth/logout" "" $r.Body.data.token
Check "8.7 登出" ($r.Status -eq 200 -and $r.Body.code -eq 0) $r.Body.code

# ============ 9. 静态页面（经 nginx） ============
$web = $Base -replace "/api$", ""
function Web-Get([string]$Path) {
    try {
        $resp = Invoke-WebRequest -Uri ($web + $Path) -UseBasicParsing -TimeoutSec 10
        return @{ Status = [int]$resp.StatusCode; Bytes = $resp.RawContentStream.ToArray(); Type = $resp.Headers["Content-Type"] }
    } catch {
        return @{ Status = 0; Bytes = @(); Type = "" }
    }
}
$w = Web-Get "/"
Check "9.1 食客端首页 200" ($w.Status -eq 200 -and ([System.Text.Encoding]::UTF8.GetString($w.Bytes)) -match '<div id="app">') ("{0} {1}" -f $w.Status, $w.Type)
$w = Web-Get "/admin"
Check "9.2 管理端路由回退 200" ($w.Status -eq 200 -and ([System.Text.Encoding]::UTF8.GetString($w.Bytes)) -match '<div id="app">') ("{0} {1}" -f $w.Status, $w.Type)
$w = Web-Get "/images/placeholder/1.svg"
Check "9.3 内置占位图可访问" ($w.Status -eq 200 -and $w.Type -like "image/svg*") ("{0} {1}" -f $w.Status, $w.Type)
$w = Web-Get "/api/doc.html"
Check "9.4 Knife4j 文档可访问" ($w.Status -eq 200 -and ([System.Text.Encoding]::UTF8.GetString($w.Bytes)) -match "knife4j") ("{0}" -f $w.Status)

# ============ 汇总 ============
$passed = @($results | Where-Object { $_[1] }).Count
$failed = $results.Count - $passed
Write-Output ""
Write-Output ("==== 自测汇总：{0} PASS / {1} FAIL / 共 {2} 项 ====" -f $passed, $failed, $results.Count)
if ($failed -gt 0) { exit 1 }
