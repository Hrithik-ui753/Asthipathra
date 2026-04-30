@echo off
setlocal

set SRC_DIR=src
set OUT_DIR=out
set LIB_DIR=lib
set MODE=%1

if not exist %OUT_DIR% mkdir %OUT_DIR%

set CP=%LIB_DIR%\sqlite-jdbc.jar;%LIB_DIR%\jbcrypt.jar;%LIB_DIR%\pdfbox-app.jar;%OUT_DIR%

if not exist %LIB_DIR%\sqlite-jdbc.jar (
  echo Missing dependency: %LIB_DIR%\sqlite-jdbc.jar
  exit /b 1
)
if not exist %LIB_DIR%\jbcrypt.jar (
  echo Missing dependency: %LIB_DIR%\jbcrypt.jar
  exit /b 1
)
if not exist %LIB_DIR%\pdfbox-app.jar (
  echo Missing dependency: %LIB_DIR%\pdfbox-app.jar
  exit /b 1
)

echo Compiling...
javac -cp "%CP%" -d %OUT_DIR% %SRC_DIR%\Main.java %SRC_DIR%\db\*.java %SRC_DIR%\dao\*.java %SRC_DIR%\model\*.java %SRC_DIR%\service\*.java %SRC_DIR%\util\*.java %SRC_DIR%\ui\*.java %SRC_DIR%\ui\pages\*.java
if %errorlevel% neq 0 (
  echo Compilation failed.
  exit /b 1
)

if /I "%MODE%"=="health" (
  echo Running backend health check...
  java -cp "%CP%" Main --health-check
) else (
  echo Running Asthipathra...
  java -cp "%CP%" Main
)
