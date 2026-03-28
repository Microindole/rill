@echo off
setlocal
set "APP_HOME=%~dp0.."
set "JAVA_EXE=%APP_HOME%\runtime\bin\java.exe"
set "LAUNCHER_JAR=%APP_HOME%\rill-launcher.jar"

if "%~1"=="" goto usage

set "MODE=%~1"
shift

if /I "%MODE%"=="server" goto server
if /I "%MODE%"=="mysql-server" goto mysql
if /I "%MODE%"=="sql" goto sql
if /I "%MODE%"=="client" goto sql
if /I "%MODE%"=="gui" goto gui
if /I "%MODE%"=="log" goto log
if /I "%MODE%"=="log-reader" goto log
if /I "%MODE%"=="data" goto data
if /I "%MODE%"=="data-reader" goto data
if /I "%MODE%"=="help" goto usage
if /I "%MODE%"=="-h" goto usage
if /I "%MODE%"=="--help" goto usage

echo Unsupported mode: %MODE%
goto usage_error

:server
"%APP_HOME%\bin\rill-server.cmd" %*
goto end

:mysql
"%APP_HOME%\bin\rill-mysql.cmd" %*
goto end

:sql
"%APP_HOME%\bin\rill-cli.cmd" %*
goto end

:gui
"%APP_HOME%\bin\rill-gui.cmd" %*
goto end

:log
if not exist "%JAVA_EXE%" (
  echo Missing bundled runtime: %JAVA_EXE%
  exit /b 1
)
if not exist "%LAUNCHER_JAR%" (
  echo Log tool is not included in this Rill distribution.
  exit /b 2
)
"%JAVA_EXE%" -jar "%LAUNCHER_JAR%" log %*
goto end

:data
if not exist "%JAVA_EXE%" (
  echo Missing bundled runtime: %JAVA_EXE%
  exit /b 1
)
if not exist "%LAUNCHER_JAR%" (
  echo Data tool is not included in this Rill distribution.
  exit /b 2
)
"%JAVA_EXE%" -jar "%LAUNCHER_JAR%" data %*
goto end

:usage
echo Usage:
echo   rill ^<mode^> [args]
echo.
echo Modes:
echo   server        Start the native rill TCP server
echo   mysql-server  Start the MySQL protocol server
echo   sql           Start the terminal SQL client
echo   gui           Start the Swing GUI client
echo   log           Inspect log files
echo   data          Inspect or export database files
echo.
echo Compatibility aliases:
echo   client        Alias for sql
echo   log-reader    Alias for log
echo   data-reader   Alias for data
goto end

:usage_error
exit /b 1

:end