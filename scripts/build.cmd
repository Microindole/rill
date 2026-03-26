@echo off
setlocal

if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

call "%~dp0..\mvnw.cmd" -DskipTests package %*
