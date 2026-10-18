@echo off
chcp 65001 >nul
title employee11 - Spring Boot
set "JAVA_HOME=D:\Code\jdk-8.0.504.1-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "D:\ruanjian\DeepSeek Harness\code\employee11"

echo ============================================
echo  employee11 starting on http://localhost:8080
echo  admin login: admin / admin123
echo  close this window or press Ctrl+C to stop
echo ============================================
echo.

java -jar target\employee-1.0-SNAPSHOT.jar

echo.
echo [process exited] code=%ERRORLEVEL%
pause
