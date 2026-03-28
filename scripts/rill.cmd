@echo off
setlocal

if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if "%~1"=="" goto usage

for %%I in ("%~dp0..") do set "ROOT_DIR=%%~fI"
set "MODE=%~1"
shift
set "JAR_PATH="
set "FORWARDED_MODE="

if /I "%MODE%"=="help" goto usage
if /I "%MODE%"=="-h" goto usage
if /I "%MODE%"=="--help" goto usage

if /I "%MODE%"=="server" set "TARGET_DIR=%ROOT_DIR%\rill-server\target" & set "ARTIFACT_PATTERN=rill-server-*-server.jar"
if /I "%MODE%"=="mysql-server" set "TARGET_DIR=%ROOT_DIR%\rill-server\target" & set "ARTIFACT_PATTERN=rill-server-*-mysql-server.jar"
if /I "%MODE%"=="sql" set "TARGET_DIR=%ROOT_DIR%\rill-client\target" & set "ARTIFACT_PATTERN=rill-client-*-cli.jar"
if /I "%MODE%"=="client" set "TARGET_DIR=%ROOT_DIR%\rill-client\target" & set "ARTIFACT_PATTERN=rill-client-*-cli.jar"
if /I "%MODE%"=="gui" set "TARGET_DIR=%ROOT_DIR%\rill-client\target" & set "ARTIFACT_PATTERN=rill-client-*-gui.jar"
if /I "%MODE%"=="web" set "TARGET_DIR=%ROOT_DIR%\rill-app-web\target" & set "ARTIFACT_PATTERN=rill-app-web-*.jar"
if /I "%MODE%"=="spring" set "TARGET_DIR=%ROOT_DIR%\rill-app-web\target" & set "ARTIFACT_PATTERN=rill-app-web-*.jar"
if /I "%MODE%"=="log" set "TARGET_DIR=%ROOT_DIR%\rill-launcher\target" & set "ARTIFACT_PATTERN=rill-launcher-*.jar" & set "FORWARDED_MODE=log"
if /I "%MODE%"=="log-reader" set "TARGET_DIR=%ROOT_DIR%\rill-launcher\target" & set "ARTIFACT_PATTERN=rill-launcher-*.jar" & set "FORWARDED_MODE=log"
if /I "%MODE%"=="data" set "TARGET_DIR=%ROOT_DIR%\rill-launcher\target" & set "ARTIFACT_PATTERN=rill-launcher-*.jar" & set "FORWARDED_MODE=data"
if /I "%MODE%"=="data-reader" set "TARGET_DIR=%ROOT_DIR%\rill-launcher\target" & set "ARTIFACT_PATTERN=rill-launcher-*.jar" & set "FORWARDED_MODE=data"

if not defined TARGET_DIR (
    echo Unsupported mode: %MODE%
    echo Supported modes: server, mysql-server, sql, gui, web, log, data
    exit /b 1
)

for /f "usebackq delims=" %%I in (`powershell -NoProfile -Command "$item = Get-ChildItem -Path '%TARGET_DIR%' -Filter '%ARTIFACT_PATTERN%' | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName; if ($item) { $item }"`) do (
    set "JAR_PATH=%%I"
    goto found
)

:found
if not defined JAR_PATH (
    echo No packaged %MODE% jar found.
    echo Run scripts\build.cmd first.
    exit /b 1
)

if defined JAVA_HOME (
    if defined FORWARDED_MODE (
        "%JAVA_HOME%\bin\java" -jar "%JAR_PATH%" %FORWARDED_MODE% %*
    ) else (
        "%JAVA_HOME%\bin\java" -jar "%JAR_PATH%" %*
    )
) else (
    if defined FORWARDED_MODE (
        java -jar "%JAR_PATH%" %FORWARDED_MODE% %*
    ) else (
        java -jar "%JAR_PATH%" %*
    )
)

exit /b %ERRORLEVEL%

:usage
echo Usage:
echo   scripts\rill.cmd ^<mode^> [args]
echo.
echo Modes:
echo   server        Start the native rill TCP server
echo   mysql-server  Start the MySQL protocol server
echo   sql           Start the terminal SQL client
echo   gui           Start the Swing GUI client
echo   web           Start the Spring Boot Web application
echo   data          Inspect or export database files
echo   log           Inspect log files
echo.
echo Compatibility aliases:
echo   client        Alias for sql
echo   spring        Alias for web
echo   data-reader   Alias for data
echo   log-reader    Alias for log
echo.
echo Examples:
echo   scripts\rill.cmd server --port=8848
echo   scripts\rill.cmd sql --host=127.0.0.1 --port=8848 --user=root
echo   scripts\rill.cmd log
echo   scripts\rill.cmd web
exit /b 0