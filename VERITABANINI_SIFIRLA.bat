@echo off
chcp 65001 >nul
title AVATAR - Veritabani Sifirlama
color 0C
echo ================================================================
echo    AVATAR PLATFORMU - TEST VERILERINI SIFIRLAMA ARACI
echo ================================================================
echo.
echo [DIKKAT] Bu islem veritabanindaki tum:
echo   - Parcalama kayitlarini
echo   - Hammadde stoklarini
echo   - Lotlari ve lot hareketlerini
echo   - Faturalari, siparisleri ve bildirimleri
echo SIFIRLAYACAKTIR (Tablo yapilari ve kullanici hesaplari korunur).
echo.
echo Devam etmek icin herhangi bir tusa basin... (Vazgecmek icin pencereyi kapatin)
pause >nul
echo.
echo [*] Veritabanindaki veriler temizleniyor...
docker exec -i avatar_mysql mysql -uavatar_user -pavatar_pass_2026 avatar_db < "%~dp0database\reset_data.sql"

if %ERRORLEVEL% EQU 0 (
    color 0A
    echo.
    echo ================================================================
    echo  [BASARILI] Veritabani verileri basariyla sifirlandi!
    echo  ID sayaclari 1'e dondu, tablolara dokunulmadi.
    echo.
    echo  Mobil uygulamadaki eski onbellek kayitlarini da sifirlamak icin:
    echo  Telefonda: Ayarlar ^> Uygulamalar ^> Avatar ^> Depolama ^> Verileri Temizle
    echo ================================================================
) else (
    echo.
    echo ================================================================
    echo  [HATA] Docker uzerinden MySQL'e baglanilamadi.
    echo  Lutfen Docker Desktop'in calistigindan emin olun.
    echo ================================================================
)

echo.
pause
