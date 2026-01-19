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
echo SCHRITT 1: System-Check
echo ======================================================

:: Pruefe Maven
call mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FEHLER] Maven wurde im Skript nicht gefunden, obwohl es in der CMD klappt.
    echo Versuche folgendes: Schliesse IntelliJ/alle Terminals und starte die .bat neu.
    pause
    exit /b 1
)

:: Pruefe jpackage (Teil von Java 21)
call jpackage --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FEHLER] jpackage wurde nicht gefunden.
    echo Dein Java 21 Pfad ist evtl. nicht korrekt im PATH.
    pause
    exit /b 1
)

echo [OK] Maven und jpackage sind bereit.

echo.
echo ======================================================
echo SCHRITT 2: Bereinigen und Fat-JAR bauen...
echo ======================================================
:: Wir nutzen einfach 'mvn', da es global im System bekannt ist
call mvn clean package -DskipTests

echo.
echo ======================================================
echo SCHRITT 3: DUV.exe erstellen...
echo ======================================================
:: Suche die JAR im target Ordner
for %%f in (target\*-fat-executable.jar) do set "JAR_FILE=%%~nxf"

if "!JAR_FILE!"=="" (
    echo [FEHLER] Keine Fat-JAR gefunden! Pruefe deine pom.xml.
    pause
    exit /b 1
)

jpackage --type app-image ^
  --dest "target\dist" ^
  --name "%EXE_NAME%" ^
  --input "target" ^
  --main-jar "!JAR_FILE!" ^
  --main-class %MAIN_CLASS% ^
  --icon "%ICON_PATH%" ^
  --vendor "Nico Hoffmann"

echo.
echo ======================================================
echo SCHRITT 4: Abgabe-Struktur aufbauen...
echo ======================================================
if exist "%ABGABE_NAME%" rd /s /q "%ABGABE_NAME%"
mkdir "%ABGABE_NAME%\Anwendung"
mkdir "%ABGABE_NAME%\Quellcode"
mkdir "%ABGABE_NAME%\Dokumentation"

:: Kopiere die Anwendung (EXE + Runtime)
xcopy /E /I /Y "target\dist\%EXE_NAME%" "%ABGABE_NAME%\Anwendung"
:: Kopiere Quellcode
xcopy /E /I /Y "src" "%ABGABE_NAME%\Quellcode\src"
copy "pom.xml" "%ABGABE_NAME%\Quellcode\"

echo.
echo ======================================================
echo SCHRITT 5: Finale Kopie nach D:
echo ======================================================
if not exist "%ZIEL_PFAD%" mkdir "%ZIEL_PFAD%"
xcopy /E /I /Y "%ABGABE_NAME%" "%ZIEL_PFAD%\%ABGABE_NAME%\"

echo.
echo ======================================================
echo ERFOLGREICH! Projekt liegt unter:
echo %ZIEL_PFAD%\%ABGABE_NAME%
echo ======================================================
pause