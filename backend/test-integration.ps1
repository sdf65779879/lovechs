# 情侣点餐系统 - 后端全量接口联调脚本
# 前置：MySQL 容器(3307) + 后端(8091) 已启动
$ErrorActionPreference = "Continue"
$base = "http://localhost:8091/api"
$out = "S:\Desktop\Csh1\couple-ordering\backend\integration-result.txt"
$lines = New-Object System.Collections.ArrayList
function N($s) { [void]$lines.Add($s) }

function Api {
    param($method, $path, $bodyObj = $null, $token = $null, $headersIn = $null)
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    if ($headersIn) { foreach ($k in $headersIn.Keys) { $headers[$k] = $headersIn[$k] } }
    try {
        $req = @{ Method = $method; Uri = "$base$path"; Headers = $headers; TimeoutSec = 15 }
        if ($null -ne $bodyObj) {
            $json = $bodyObj | ConvertTo-Json -Depth 10 -Compress
            $req.ContentType = "application/json; charset=utf-8"
            $req.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
        }
        $resp = Invoke-RestMethod @req
        return @{ ok = $true; r = $resp }
    } catch {
        $er = $_.Exception.Response
        $text = ""
        if ($er) { try { $sr = New-Object System.IO.StreamReader($er.GetResponseStream()); $text = $sr.ReadToEnd() } catch {} }
        return @{ ok = $false; status = if ($er) { [int]$er.StatusCode } else { -1 }; text = $text }
    }
}

function New-Idem($prefix) { return @{ "Idempotency-Key" = ("$prefix-" + [guid]::NewGuid().ToString()) } }

# ============ 1. 登录 ============
N "==== 1. 认证 ===="
$a = Api POST "/v1/auth/login" @{ username = "admin"; password = "admin123" }
N "admin login: ok=$($a.ok) code=$($a.r.code) nickname=$($a.r.data.user.nickname) role=$($a.r.data.user.role)"
$adminToken = $a.r.data.token

$l = Api POST "/v1/auth/login" @{ username = "love"; password = "love123" }
N "love  login: ok=$($l.ok) code=$($l.r.code) nickname=$($l.r.data.user.nickname) role=$($l.r.data.user.role)"
$loveToken = $l.r.data.token

$bad = Api POST "/v1/auth/login" @{ username = "love"; password = "wrong-pwd" }
N "wrong password rejected: $($bad.text)"
$noauth = Api GET "/v1/auth/me"
N "no token /me: http=$($noauth.status) body=$($noauth.text)"
$me = Api GET "/v1/auth/me" $null $loveToken
N "love /me: username=$($me.r.data.username) nickname=$($me.r.data.nickname)"

# ============ 2. 管理端：分类 / 菜品 / 上传 ============
N "`n==== 2. 分类/菜品/上传（管理端） ===="
$c = Api POST "/v1/admin/categories" @{ name = "隐藏小灶"; sort = 99; status = 1 } $adminToken
N "create category: code=$($c.r.code) id=$($c.r.data)"
$newCat = $c.r.data

$d = Api POST "/v1/admin/dishes" @{
    categoryId = $newCat; name = "爱心煎蛋"; description = "大厨的拿手早餐，溏心的那种"
    tags = "拿手菜,清淡"; loveValue = 5; sort = 1; status = 1; isRecommend = 1
    image = "/images/placeholder/1.svg"
} $adminToken
N "create dish: code=$($d.r.code) id=$($d.r.data)"
$newDish = $d.r.data

# 合法 PNG 上传（1x1 像素）
$pngB64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
$pngPath = Join-Path $env:TEMP "couple-test.png"
[IO.File]::WriteAllBytes($pngPath, [Convert]::FromBase64String($pngB64))
$up = & curl.exe -s -X POST -H "Authorization: Bearer $adminToken" -F "file=@$pngPath;type=image/png" "$base/v1/files/image"
N "upload png: $up"
# 非法类型上传
$txtPath = Join-Path $env:TEMP "couple-test.txt"
[IO.File]::WriteAllText($txtPath, "hello")
$upBad = & curl.exe -s -X POST -H "Authorization: Bearer $adminToken" -F "file=@$txtPath;type=text/plain" "$base/v1/files/image"
N "upload txt rejected: $upBad"

# ============ 3. 用户端：浏览 / 搜索 ============
N "`n==== 3. 浏览/搜索（用户端） ===="
$cats = Api GET "/v1/categories" $null $loveToken
N "categories count=$($cats.r.data.Count)（初始 6 + 新增 1 = 7）"
$dishes = Api GET "/v1/dishes?pageNum=1&pageSize=50" $null $loveToken
N "dishes total=$($dishes.r.data.total)（初始 15 + 新增 1 = 16）"
$rec = Api GET "/v1/dishes/recommend?limit=20" $null $loveToken
$recHit = ($rec.r.data | Where-Object { $_.id -eq $newDish }).Count
N "recommend count=$($rec.r.data.Count), contains new dish=$recHit"
$detail = Api GET "/v1/dishes/$newDish" $null $loveToken
N "dish detail: name=$($detail.r.data.name), tags=$($detail.r.data.tagList -join '/')"
$kw = [uri]::EscapeDataString("煎蛋")
$search = Api GET "/v1/dishes?keyword=$kw&pageSize=20" $null $loveToken
N "search '煎蛋': total=$($search.r.data.total), first=$($search.r.data.list[0].name)"

# ============ 4. 收藏 ============
N "`n==== 4. 收藏 ===="
$fav = Api POST "/v1/favorites/$newDish" $null $loveToken
N "favorite add: code=$($fav.r.code)"
$favs = Api GET "/v1/favorites" $null $loveToken
N "favorites count=$($favs.r.data.Count)"
$det2 = Api GET "/v1/dishes/$newDish" $null $loveToken
N "detail favorite flag=$($det2.r.data.favorite)"
Api DELETE "/v1/favorites/$newDish" $null $loveToken | Out-Null
$favs2 = Api GET "/v1/favorites" $null $loveToken
N "after unfavorite count=$($favs2.r.data.Count)"
Api POST "/v1/favorites/$newDish" $null $loveToken | Out-Null

# ============ 5. 购物车 ============
N "`n==== 5. 购物车 ===="
Api POST "/v1/cart" @{ dishId = $newDish; quantity = 2; taste = "微辣"; remark = "不要香菜" } $loveToken | Out-Null
$seedDish = ($dishes.r.data.list | Where-Object { $_.id -ne $newDish } | Select-Object -First 1)
Api POST "/v1/cart" @{ dishId = $seedDish.id; quantity = 1; taste = "正常"; remark = "" } $loveToken | Out-Null
$cartList = Api GET "/v1/cart" $null $loveToken
$firstLine = $cartList.r.data | Where-Object { $_.dishId -eq $newDish }
N "cart lines=$($cartList.r.data.Count); new dish line: qty=$($firstLine.quantity) taste=$($firstLine.taste) remark=$($firstLine.remark) lineLove=$($firstLine.lineLove)"
$cartId = $firstLine.id
Api PUT "/v1/cart/$cartId" @{ quantity = 3; taste = "中辣"; remark = "不要葱" } $loveToken | Out-Null
$cartList2 = Api GET "/v1/cart" $null $loveToken
$line2 = $cartList2.r.data | Where-Object { $_.id -eq $cartId }
N "after update: qty=$($line2.quantity) taste=$($line2.taste) remark=$($line2.remark)"

# ============ 6. 下单 + 状态流转 ============
N "`n==== 6. 下单与状态流转 ===="
$idem = New-Idem "t1"
$o = Api POST "/v1/orders" @{ remark = "多放一点爱呀"; expectTime = $null } $loveToken $idem
N "submit order: code=$($o.r.code) no=$($o.r.data.orderNo) total=$($o.r.data.totalLove) status=$($o.r.data.status)"
$orderId = $o.r.data.id
$o2 = Api POST "/v1/orders" @{ remark = "多放一点爱呀"; expectTime = $null } $loveToken $idem
N "repeat with same Idempotency-Key: same order=$($o2.r.data.id -eq $orderId)"
$cartAfter = Api GET "/v1/cart" $null $loveToken
N "cart cleared after order: lines=$($cartAfter.r.data.Count)"
$ol = Api GET "/v1/orders?tab=active&pageSize=10" $null $loveToken
N "user active orders=$($ol.r.data.total)"
$ao = Api GET "/v1/admin/orders?status=PENDING" $null $adminToken
N "admin pending orders=$($ao.r.data.total)"
$s1 = Api PUT "/v1/admin/orders/$orderId/status" @{ status = "ACCEPTED" } $adminToken
N "ACCEPTED: code=$($s1.r.code) status=$($s1.r.data.status) acceptTime=$($s1.r.data.acceptTime)"
$sBad = Api PUT "/v1/admin/orders/$orderId/status" @{ status = "COMPLETED" } $adminToken
N "illegal ACCEPTED->COMPLETED rejected: $($sBad.text)"
$s2 = Api PUT "/v1/admin/orders/$orderId/status" @{ status = "COOKING" } $adminToken
N "COOKING: status=$($s2.r.data.status)"
$od = Api GET "/v1/orders/$orderId" $null $loveToken
N "user poll: sees=$($od.r.data.status) label=$($od.r.data.statusLabel) items=$($od.r.data.items.Count)"
$s3 = Api PUT "/v1/admin/orders/$orderId/status" @{ status = "COMPLETED" } $adminToken
N "COMPLETED: status=$($s3.r.data.status) finishTime=$($s3.r.data.finishTime)"
$olf = Api GET "/v1/orders?tab=finished&pageSize=10" $null $loveToken
N "user finished orders=$($olf.r.data.total)"

# ============ 7. 取消 / 拒绝 ============
N "`n==== 7. 取消 / 拒绝 ===="
Api POST "/v1/cart" @{ dishId = $seedDish.id; quantity = 1 } $loveToken | Out-Null
$oc = Api POST "/v1/orders" @{ remark = "想取消" } $loveToken (New-Idem "c")
$cancelId = $oc.r.data.id
$cx = Api PUT "/v1/orders/$cancelId/cancel" $null $loveToken
N "cancel pending: code=$($cx.r.code)"
$cxd = Api GET "/v1/orders/$cancelId" $null $loveToken
N "cancelled status=$($cxd.r.data.status)"
$cxBad = Api PUT "/v1/orders/$orderId/cancel" $null $loveToken
N "cancel completed rejected: $($cxBad.text)"
Api POST "/v1/cart" @{ dishId = $seedDish.id; quantity = 1 } $loveToken | Out-Null
$orj = Api POST "/v1/orders" @{ remark = "能做吗" } $loveToken (New-Idem "rj")
$rejectId = $orj.r.data.id
$rj = Api PUT "/v1/admin/orders/$rejectId/status" @{ status = "REJECTED"; rejectReason = "今天没有食材啦" } $adminToken
N "reject: code=$($rj.r.code) status=$($rj.r.data.status) reason=$($rj.r.data.rejectReason)"
Api POST "/v1/cart" @{ dishId = $seedDish.id; quantity = 1 } $loveToken | Out-Null
$orj2 = Api POST "/v1/orders" @{ remark = "?" } $loveToken (New-Idem "rj2")
$rjBad = Api PUT "/v1/admin/orders/$($orj2.r.data.id)/status" @{ status = "REJECTED" } $adminToken
N "reject without reason rejected: $($rjBad.text)"

# ============ 8. 再下单 / 日历 / 看板 / 配置 / 越权 ============
N "`n==== 8. 再下单/日历/看板/配置/越权 ===="
$rb = Api POST "/v1/orders/$orderId/rebuy" $null $loveToken
N "rebuy: code=$($rb.r.code) addedCount=$($rb.r.data.addedCount) skipped=$($rb.r.data.skippedNames -join ',')"
$rbCart = Api GET "/v1/cart" $null $loveToken
N "cart after rebuy lines=$($rbCart.r.data.Count)"
Api DELETE "/v1/cart" $null $loveToken | Out-Null

$cal = Api GET "/v1/orders/calendar?month=2026-10" $null $loveToken
$orderDays = ($cal.r.data.days | Where-Object { $_.orders.Count -gt 0 }).Count
N "calendar: month=$($cal.r.data.month) days=$($cal.r.data.days.Count) daysWithOrders=$orderDays"

$dash = Api GET "/v1/admin/dashboard" $null $adminToken
N "dashboard: today=$($dash.r.data.todayOrderCount) pending=$($dash.r.data.pendingCount) total=$($dash.r.data.totalCount) top5=$($dash.r.data.topDishes.Count) trend=$($dash.r.data.trend.Count)"
N "top dishes: " + (($dash.r.data.topDishes | ForEach-Object { "$($_.dishName)x$($_.count)" }) -join ", ")
N "trend: " + (($dash.r.data.trend | ForEach-Object { "$($_.date)=$($_.count)" }) -join ", ")

$pub = Api GET "/v1/configs/public" $null $loveToken
N "public config: open=$($pub.r.data.businessOpen) notice=[$($pub.r.data.notice)]"
$cfg = Api PUT "/v1/admin/configs" @{ configs = @{ business_open = "0"; notice = "今晚出去吃~" } } $adminToken
N "update config: code=$($cfg.r.code)"
$pub2 = Api GET "/v1/configs/public" $null $loveToken
N "after update: open=$($pub2.r.data.businessOpen) notice=$($pub2.r.data.notice)"
Api POST "/v1/cart" @{ dishId = $seedDish.id; quantity = 1 } $loveToken | Out-Null
$closed = Api POST "/v1/orders" @{ remark = "?" } $loveToken (New-Idem "closed")
N "order while closed rejected: $($closed.text)"
Api PUT "/v1/admin/configs" @{ configs = @{ business_open = "1"; notice = "" } } $adminToken | Out-Null
Api DELETE "/v1/cart" $null $loveToken | Out-Null

$forbid = Api GET "/v1/admin/dashboard" $null $loveToken
N "love accesses admin API: http=$($forbid.status) body=$($forbid.text)"
$adminDish = Api GET "/v1/dishes?pageSize=5" $null $adminToken
N "admin browses user dishes: code=$($adminDish.r.code)"

$lines | Out-File -FilePath $out -Encoding utf8
Write-Output "DONE: $($lines.Count) lines -> $out"
