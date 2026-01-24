@echo off
setlocal enabledelayedexpansion

:: --- KONFIGURATION ---
set "ABGABE_NAME=hoffmann_nico"
set "EXE_NAME=DUV"
set "ZIEL_PFAD=D:\4_Projekte\Das unberechenbare Volk"
set "MAIN_CLASS=de.schulprojekt.duv.view.MainLauncher"
set "ICON_PATH=src\main\resources\de\schulprojekt\duv\Pictures\DUV_Logo.ico"
:: ---------------------

echo ======================================================
echo SCHRITT 1 & 2: Maven Build läuft...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo [FEHLER] Maven Build fehlgeschlagen.
    pause
    exit /b 1
)

echo ======================================================
echo SCHRITT 3: DUV.exe erstellen (BITTE WARTEN...)
echo ======================================================

:: Wir suchen die erstellte JAR
for %%f in (target\*-fat-executable.jar) do set "JAR_FILE=%%~nxf"

if "!JAR_FILE!"=="" (
    echo [FEHLER] Keine Fat-JAR gefunden!
    pause
    exit /b 1
)

echo Gefundene Datei: !JAR_FILE!
echo jpackage arbeitet jetzt...

:: Wir erstellen einen sauberen Input-Ordner nur für die EXE
if exist "target\input" rd /s /q "target\input"
mkdir "target\input"
:: Korrigierter Kopierbefehl für die JAR
copy "target\!JAR_FILE!" "target\input\"

:: Sicherstellen, dass der Zielordner für jpackage leer ist
if exist "target\dist" rd /s /q "target\dist"

:: Hier startet jpackage
jpackage --type app-image ^
  --dest "target\dist" ^
  --name "%EXE_NAME%" ^
  --input "target\input" ^
  --main-jar "!JAR_FILE!" ^
  --main-class %MAIN_CLASS% ^
  --icon "%ICON_PATH%" ^
  --vendor "Nico Hoffmann"

if %ERRORLEVEL% NEQ 0 (
    echo [FEHLER] jpackage ist fehlgeschlagen.
    pause
    exit /b 1
)

echo ======================================================
echo SCHRITT 4: Ordnerstruktur aufbauen...
echo ======================================================
:: Erstellt nur den Hauptordner und den Anwendungs-Unterordner
if exist "%ABGABE_NAME%" rd /s /q "%ABGABE_NAME%"
mkdir "%ABGABE_NAME%\Anwendung"

:: Kopiere NUR die fertige Anwendung (Inhalt des jpackage-Images)
xcopy /E /I /Y "target\dist\%EXE_NAME%" "%ABGABE_NAME%\Anwendung"

echo ======================================================
echo SCHRITT 5: Kopieren auf Zielpfad
echo ======================================================
if not exist "%ZIEL_PFAD%" mkdir "%ZIEL_PFAD%"
xcopy /E /I /Y "%ABGABE_NAME%" "%ZIEL_PFAD%\%ABGABE_NAME%\"

echo.
echo ======================================================
echo ERFOLGREICH! NUR die Anwendung befindet sich unter:
echo %ZIEL_PFAD%\%ABGABE_NAME%
echo ======================================================
pause