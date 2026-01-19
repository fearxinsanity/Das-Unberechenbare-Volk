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
echo jpackage arbeitet jetzt... das kann bis zu 1 Minute dauern...

:: Wir erstellen einen sauberen Input-Ordner nur fuer die EXE
if exist "target\input" rd /s /q "target\input"
mkdir "target\input"
copy "target\!JAR_FILE!" "target\input\"

:: Hier startet jpackage
jpackage --type app-image ^
  --dest "target\dist" ^
  --name "%EXE_NAME%" ^
  --input "target\input" ^
  --main-jar "!JAR_FILE!" ^
  --main-class %MAIN_CLASS% ^
  --icon "%ICON_PATH%" ^
  --vendor "Nico Hoffmann" ^
  --verbose

if %ERRORLEVEL% NEQ 0 (
    echo [FEHLER] jpackage ist fehlgeschlagen.
    echo Versuche: jpackage --version in der CMD einzugeben.
    pause
    exit /b 1
)

echo ======================================================
echo SCHRITT 4: Ordnerstruktur aufbauen...
echo ======================================================
if exist "%ABGABE_NAME%" rd /s /q "%ABGABE_NAME%"
mkdir "%ABGABE_NAME%\Anwendung"
mkdir "%ABGABE_NAME%\Quellcode"
mkdir "%ABGABE_NAME%\Dokumentation"

:: Kopiere die fertige Anwendung
xcopy /E /I /Y "target\dist\%EXE_NAME%" "%ABGABE_NAME%\Anwendung"

:: Kopiere Quellcode (ohne target)
xcopy /E /I /Y "src" "%ABGABE_NAME%\Quellcode\src"
copy "pom.xml" "%ABGABE_NAME%\Quellcode\"

:: Dokumentation kopieren
if exist "Documentation" xcopy /E /I /Y "Documentation" "%ABGABE_NAME%\Dokumentation"

echo ======================================================
echo SCHRITT 5: Kopieren auf Laufwerk D:
echo ======================================================
if not exist "%ZIEL_PFAD%" mkdir "%ZIEL_PFAD%"
xcopy /E /I /Y "%ABGABE_NAME%" "%ZIEL_PFAD%\%ABGABE_NAME%\"

echo.
echo ======================================================
echo ERFOLGREICH! Alles fertig unter:
echo %ZIEL_PFAD%\%ABGABE_NAME%
echo ======================================================
pause