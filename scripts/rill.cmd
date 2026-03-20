@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "TARGET_DIR=%SCRIPT_DIR%..\target"
set "JAR_PATH="

for /f "delims=" %%I in ('dir /b /o-d "%TARGET_DIR%\rill-*.jar" 2^>nul') do (
    set "JAR_PATH=%TARGET_DIR%\%%I"
    goto found
)

:found
if not defined JAR_PATH (
    echo No packaged jar found under target\.
    echo Run mvnw.cmd -DskipTests package first.
    exit /b 1
)

java -jar "%JAR_PATH%" %*
