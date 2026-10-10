# 补充联调：个人资料 / 修改密码 / 用户管理 + 清理残留订单
$base = "http://localhost:8091/api"
$dir = "S:\Desktop\Csh1\couple-ordering\backend"

# 与 test-errors.ps1 相同的 JSON 发送方式：ordered + ConvertTo-Json + UTF-8 字节
function Send-Json {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][System.Collections.IDictionary]$Body,
        [hashtable]$Headers = @{},
        [string]$AppendWriteOut
    )
    $tmp = [IO.Path]::GetTempFileName()
    try {
        $json = ConvertTo-Json -InputObject $Body -Depth 20 -Compress
        [IO.File]::WriteAllBytes($tmp, [System.Text.Encoding]::UTF8.GetBytes($json))
        $curlArgs = New-Object System.Collections.Generic.List[string]
        $curlArgs.Add("-s"); $curlArgs.Add("-X"); $curlArgs.Add($Method)
        foreach ($name in $Headers.Keys) {
            $curlArgs.Add("-H"); $curlArgs.Add("$name`: $($Headers[$name])")
        }
        $curlArgs.Add("--data-binary"); $curlArgs.Add("@$tmp")
        if ($AppendWriteOut) { $curlArgs.Add("-w"); $curlArgs.Add($AppendWriteOut) }
        $curlArgs.Add($Url)
        return & curl.exe @curlArgs
    }
    finally {
        Remove-Item -LiteralPath $tmp -ErrorAction SilentlyContinue
    }
}

$R = New-Object System.Collections.ArrayList
function Note($s) { [void]$R.Add($s) }

# 登录
$admin = (Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "admin"; password = "admin123" }) `
    @{ "Content-Type" = "application/json" } | ConvertFrom-Json).data.token
$love = (Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "love"; password = "love123" }) `
    @{ "Content-Type" = "application/json" } | ConvertFrom-Json).data.token
$hAuth = @{ "Authorization" = "Bearer $love"; "Content-Type" = "application/json" }
$aAuth = @{ "Authorization" = "Bearer $admin"; "Content-Type" = "application/json" }

Note "==== A. 个人资料 ===="
$p1 = Send-Json "PUT" "$base/v1/auth/profile" `
    ([ordered]@{ nickname = "宝贝"; avatar = "/uploads/202610/avatar-demo.png" }) $hAuth
Note "update profile: $p1"
$me = (curl.exe -s "$base/v1/auth/me" -H "Authorization: Bearer $love" | ConvertFrom-Json).data
Note "me after: nickname=$($me.nickname) avatar=$($me.avatar)"

Note "`n==== B. 修改密码 ===="
$pw1 = Send-Json "PUT" "$base/v1/auth/password" `
    ([ordered]@{ oldPassword = "love123"; newPassword = "love1234" }) $hAuth
Note "change password: $pw1"
$loginNew = Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "love"; password = "love1234" }) `
    @{ "Content-Type" = "application/json" }
Note "login with new pwd: code=$(($loginNew | ConvertFrom-Json).code)"
$pwBad = Send-Json "PUT" "$base/v1/auth/password" `
    ([ordered]@{ oldPassword = "wrong-old"; newPassword = "xxxxxx" }) $hAuth
Note "wrong old password: $pwBad"
# 改回原密码
Send-Json "PUT" "$base/v1/auth/password" `
    ([ordered]@{ oldPassword = "love1234"; newPassword = "love123" }) $hAuth | Out-Null
$loginBack = Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "love"; password = "love123" }) `
    @{ "Content-Type" = "application/json" }
Note "login after restore: code=$(($loginBack | ConvertFrom-Json).code)"

Note "`n==== C. 用户管理 ===="
$u1 = Send-Json "POST" "$base/v1/admin/users" `
    ([ordered]@{ username = "guest2"; password = "guest123"; nickname = "客串小二"; role = "USER" }) $aAuth
Note "create user: $u1"
$guestId = ($u1 | ConvertFrom-Json).data.id
$uDup = Send-Json "POST" "$base/v1/admin/users" `
    ([ordered]@{ username = "guest2"; password = "guest123"; nickname = "重复" }) $aAuth
Note "duplicate username: $uDup"
$gLogin = Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "guest2"; password = "guest123" }) `
    @{ "Content-Type" = "application/json" }
Note "guest login: code=$(($gLogin | ConvertFrom-Json).code)"
# 禁用
Send-Json "PUT" "$base/v1/admin/users/$guestId/status" `
    ([ordered]@{ status = 0 }) $aAuth | Out-Null
$gDisabled = Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "guest2"; password = "guest123" }) `
    @{ "Content-Type" = "application/json" }
Note "guest login while disabled: $gDisabled"
# 重置密码后启用
Send-Json "PUT" "$base/v1/admin/users/$guestId/password/reset" `
    ([ordered]@{ password = "newpass1" }) $aAuth | Out-Null
Send-Json "PUT" "$base/v1/admin/users/$guestId/status" `
    ([ordered]@{ status = 1 }) $aAuth | Out-Null
$gReset = Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "guest2"; password = "newpass1" }) `
    @{ "Content-Type" = "application/json" }
Note "guest login after reset: code=$(($gReset | ConvertFrom-Json).code)"
# 再次禁用，保持系统干净
Send-Json "PUT" "$base/v1/admin/users/$guestId/status" `
    ([ordered]@{ status = 0 }) $aAuth | Out-Null
$userPage = curl.exe -s "$base/v1/admin/users?keyword=guest" -H "Authorization: Bearer $admin"
Note "user page keyword=guest: total=$(($userPage | ConvertFrom-Json).data.total)"

Note "`n==== D. 清理残留订单（id=4，测试遗留 PENDING） ===="
$clean = Send-Json "PUT" "$base/v1/admin/orders/4/status" `
    ([ordered]@{ status = "REJECTED"; rejectReason = "测试残留单，自动清理" }) $aAuth
Note "reject leftover: $clean"

$R | Out-File "$dir\integration-supplement.txt" -Encoding utf8
Write-Output "done"
