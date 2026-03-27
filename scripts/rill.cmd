@echo off
setlocal

if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if "%~1"=="" goto usage

set "MODE=%~1"
shift

set "SCRIPT_DIR=%~dp0"
set "JAR_PATH="

if /I "%MODE%"=="server" set "TARGET_DIR=%SCRIPT_DIR%..\rill-server\target" & set "ARTIFACT_PATTERN=rill-server-*-server.jar"
if /I "%MODE%"=="mysql-server" set "TARGET_DIR=%SCRIPT_DIR%..\rill-server\target" & set "ARTIFACT_PATTERN=rill-server-*-mysql-server.jar"
if /I "%MODE%"=="client" set "TARGET_DIR=%SCRIPT_DIR%..\rill-client\target" & set "ARTIFACT_PATTERN=rill-client-*-cli.jar"
if /I "%MODE%"=="gui" set "TARGET_DIR=%SCRIPT_DIR%..\rill-client\target" & set "ARTIFACT_PATTERN=rill-client-*-gui.jar"
if /I "%MODE%"=="spring" set "TARGET_DIR=%SCRIPT_DIR%..\rill-app-web\target" & set "ARTIFACT_PATTERN=rill-app-web-*.jar"

if /I "%MODE%"=="help" goto usage
if /I "%MODE%"=="-h" goto usage
if /I "%MODE%"=="--help" goto usage

if not defined TARGET_DIR (
echo Unsupported mode: %MODE%
echo Supported modes: server, mysql-server, client, gui, spring
exit /b 1
)

for /f "delims=" %%I in ('dir /b /o-d "%TARGET_DIR%\%ARTIFACT_PATTERN%" 2^>nul') do (
    set "JAR_PATH=%TARGET_DIR%\%%I"
    goto found
)

:found
if not defined JAR_PATH (
echo No packaged %MODE% jar found.
echo Run scripts\build.cmd first.
exit /b 1
)

if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java" -jar "%JAR_PATH%" %*
) else (
    java -jar "%JAR_PATH%" %*
)

exit /b %ERRORLEVEL%

:usage
echo Usage:
echo   scripts\rill.cmd ^<mode^> [args]
echo.
echo Modes:
echo   server        Start the native rill TCP server
echo   mysql-server  Start the MySQL protocol server
echo   client        Start the terminal client
echo   gui           Start the Swing GUI client
echo   spring        Start the Spring Boot Web application
echo.
echo Examples:
echo   scripts\rill.cmd server --port=8848
echo   scripts\rill.cmd client --host=127.0.0.1 --port=8848 --user=root
echo   scripts\rill.cmd spring
exit /b 0
