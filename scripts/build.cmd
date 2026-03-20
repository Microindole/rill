@echo off
setlocal

call "%~dp0..\mvnw.cmd" -DskipTests package %*
