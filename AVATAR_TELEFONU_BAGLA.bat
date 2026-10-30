@echo off
chcp 65001 >nul
title AVATAR - Telefon Baglanti Araci
color 0A
echo ================================================================
echo    AVATAR PLATFORMU - TELEFON PORT BAGLANTI ARACI
echo ================================================================
echo.

set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if not exist %ADB% (
    color 0C
    echo [HATA] ADB bulunamadi: %ADB%
    echo Android Studio SDK kurulumunu kontrol ediniz.
    echo.
    pause
    exit /b 1
)

echo [*] Bagli telefonlar araniyor...
%ADB% devices

echo.
set FOUND=0
for /f "tokens=1,2" %%A in ('%ADB% devices') do (
    if "%%B"=="device" (
        set FOUND=1
        echo [*] Cihaz bulundu: %%A
        echo [*] Port 3000 yonlendiriliyor (adb reverse tcp:3000 tcp:3000)...
        %ADB% -s %%A reverse tcp:3000 tcp:3000
    )
)

echo.
if "%FOUND%"=="1" (
    color 0A
    echo ================================================================
    echo  [BASARILI] Telefonunuz basariyla bilgisayara baglandi!
    echo.
    echo  Mobil uygulamada Sunucu Adresini su sekilde kaydedebilirsiniz:
    echo      http://127.0.0.1:3000/api/
    echo ================================================================
) else (
    color 0E
    echo ================================================================
    echo  [UYARI] Bagli bir telefon bulunamadi!
    echo  1. USB kablosunu cikartip tekrar takin.
    echo  2. Telefonda "USB Hata Ayiklamaya izin ver" uyarisi ciktiysa ONAYLAYIN.
    echo ================================================================
)

echo.
pause
