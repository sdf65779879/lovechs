# 错误响应体验证：错误密码 / 非法状态流转 / 歇业下单
$base = "http://localhost:8091/api"
$dir = "S:\Desktop\Csh1\couple-ordering\backend"

<#
    统一发送 JSON：
    1. [ordered]@{} 由调用方构建，保持字段顺序；
    2. ConvertTo-Json -Depth 20 -Compress 生成紧凑 JSON；
    3. UTF8.GetBytes 转为 UTF-8 无 BOM 字节写入临时文件；
    4. curl --data-binary "@file" 按原始字节发送，避免中文乱码。
#>
function Send-Json {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][System.Collections.IDictionary]$Body,
        [hashtable]$Headers = @{},
        [string]$OutFile,
        [string]$AppendWriteOut
    )
    $tmp = [IO.Path]::GetTempFileName()
    try {
        $json = ConvertTo-Json -InputObject $Body -Depth 20 -Compress
        [IO.File]::WriteAllBytes($tmp, [System.Text.Encoding]::UTF8.GetBytes($json))

        $curlArgs = New-Object System.Collections.Generic.List[string]
        $curlArgs.Add("-s")
        $curlArgs.Add("-X"); $curlArgs.Add($Method)
        if ($OutFile) { $curlArgs.Add("-o"); $curlArgs.Add($OutFile) }
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

# 登录获取两个角色的 Token
$admin = (Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "admin"; password = "admin123" }) `
    @{ "Content-Type" = "application/json" } | ConvertFrom-Json).data.token
$love = (Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "love"; password = "love123" }) `
    @{ "Content-Type" = "application/json" } | ConvertFrom-Json).data.token

# e1 错误密码
$e1 = Send-Json "POST" "$base/v1/auth/login" `
    ([ordered]@{ username = "love"; password = "bad" }) `
    @{ "Content-Type" = "application/json" } `
    -AppendWriteOut "`nHTTP:%{http_code}"

# e2 非法状态流转：取一个 PENDING 订单，直接跳到 COMPLETED
$pending = (curl.exe -s "$base/v1/admin/orders?status=PENDING" `
    -H "Authorization: Bearer $admin" | ConvertFrom-Json).data.list
$pid0 = $pending[0].id
$e2 = Send-Json "PUT" "$base/v1/admin/orders/$pid0/status" `
    ([ordered]@{ status = "COMPLETED" }) `
    @{ "Authorization" = "Bearer $admin"; "Content-Type" = "application/json" } `
    -AppendWriteOut "`nHTTP:%{http_code}"

# e3 歇业下单：先关闭营业，再加购、下单
Send-Json "PUT" "$base/v1/admin/configs" `
    ([ordered]@{ configs = [ordered]@{ business_open = "0" } }) `
    @{ "Authorization" = "Bearer $admin"; "Content-Type" = "application/json" } | Out-Null
$dish = (curl.exe -s "$base/v1/dishes?pageSize=1" `
    -H "Authorization: Bearer $love" | ConvertFrom-Json).data.list[0].id
Send-Json "POST" "$base/v1/cart" `
    ([ordered]@{ dishId = $dish; quantity = 1 }) `
    @{ "Authorization" = "Bearer $love"; "Content-Type" = "application/json" } | Out-Null
$e3 = Send-Json "POST" "$base/v1/orders" `
    ([ordered]@{ remark = "" }) `
    @{ "Authorization" = "Bearer $love"; "Content-Type" = "application/json" } `
    -AppendWriteOut "`nHTTP:%{http_code}"

# 恢复营业并清空购物车
Send-Json "PUT" "$base/v1/admin/configs" `
    ([ordered]@{ configs = [ordered]@{ business_open = "1" } }) `
    @{ "Authorization" = "Bearer $admin"; "Content-Type" = "application/json" } | Out-Null
curl.exe -s -X DELETE "$base/v1/cart" -H "Authorization: Bearer $love" | Out-Null

"e1 wrong-password:`r`n$e1`r`n`r`ne2 illegal-transition(order $pid0):`r`n$e2`r`n`r`ne3 order-while-closed:`r`n$e3" |
    Out-File "$dir\diag-errors.txt" -Encoding utf8
Write-Output "done"
