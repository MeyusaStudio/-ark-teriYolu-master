# AVATAR Telefon Otomatik Port Köprüsü (Port 3000)
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

while ($true) {
    try {
        if (Test-Path $adb) {
            $devices = & $adb devices 2>$null | Where-Object { $_ -match "(\S+)\s+device$" } | ForEach-Object { $matches[1] }
            foreach ($dev in $devices) {
                $list = & $adb -s $dev reverse --list 2>$null
                if ($list -notmatch "tcp:3000") {
                    & $adb -s $dev reverse tcp:3000 tcp:3000 2>$null
                }
            }
        }
    } catch {
        # Yoksay ve döngüye devam et
    }
    Start-Sleep -Seconds 5
}
