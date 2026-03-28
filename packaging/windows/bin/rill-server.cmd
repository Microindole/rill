@echo off
setlocal
set "APP_HOME=%~dp0.."
set "JAVA_EXE=%APP_HOME%\runtime\bin\java.exe"
set "TARGET_JAR=%APP_HOME%\server\rill-server.jar"

if not exist "%JAVA_EXE%" (
  echo Missing bundled runtime: %JAVA_EXE%
  exit /b 1
)

if not exist "%TARGET_JAR%" (
  echo Core server is not included in this Rill distribution.
  exit /b 2
)

"%JAVA_EXE%" -jar "%TARGET_JAR%" %*