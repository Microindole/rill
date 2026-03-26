@echo off
setlocal

if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

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
echo Run scripts\build.cmd first.
exit /b 1
)

if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java" -jar "%JAR_PATH%" %*
) else (
    java -jar "%JAR_PATH%" %*
)
