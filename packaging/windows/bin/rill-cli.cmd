@echo off
setlocal
set "APP_HOME=%~dp0.."
"%APP_HOME%\runtime\bin\java.exe" -jar "%APP_HOME%\client\rill-cli.jar" %*
