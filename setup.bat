@echo off
echo ========================================
echo GP360 Biometric Service - Setup
echo ========================================
echo.

REM Create lib directory if it doesn't exist
if not exist "lib" (
    echo Creating lib directory...
    mkdir lib
)

REM Check for DigitalPersona U.are.U SDK libraries
echo Checking DigitalPersona U.are.U SDK libraries...
set SDK_FOUND=0

REM Check for U.are.U SDK (preferred)
if exist "lib\dpuareu.jar" (
    if exist "lib\dpjavapos.jar" (
        set SDK_FOUND=1
        echo U.are.U SDK libraries found
    )
)

REM If U.are.U not found, check for legacy OneTouch SDK
if %SDK_FOUND%==0 (
    if exist "lib\dpfpenrollment.jar" (
        if exist "lib\dpfpverification.jar" (
            set SDK_FOUND=1
            echo OneTouch SDK libraries found [legacy]
        )
    )
)

if %SDK_FOUND%==0 (
    echo WARNING: DigitalPersona SDK libraries not found
    echo Please ensure one of the following SDK sets is in the lib directory:
    echo.
    echo Option 1 - U.are.U SDK [recommended]:
    echo - dpuareu.jar
    echo - dpjavapos.jar
    echo - jpos113.jar
    echo.
    echo Option 2 - OneTouch SDK [legacy]:
    echo - dpfpenrollment.jar
    echo - dpfpverification.jar
    echo - dpotapi.jar
    echo - dpotjni.jar
)

REM Download additional dependencies if needed
echo.
echo Checking for additional dependencies...

REM Check for JSON library
if exist "lib\json-*.jar" (
    echo JSON library found
) else (
    echo Downloading JSON library...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/json/json/20230618/json-20230618.jar' -OutFile 'lib\json-20230618.jar'"
    if exist "lib\json-20230618.jar" (
        echo JSON library downloaded successfully
    ) else (
        echo Failed to download JSON library. Please download manually from:
        echo https://mvnrepository.com/artifact/org.json/json
    )
)

REM Check for MySQL Connector - supports both old and new naming
set MYSQL_FOUND=0
if exist "lib\mysql-connector-java-*.jar" set MYSQL_FOUND=1
if exist "lib\mysql-connector-j-*.jar" set MYSQL_FOUND=1

if %MYSQL_FOUND%==0 (
    echo.
    echo MySQL Connector not found. Downloading...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar' -OutFile 'lib\mysql-connector-j-9.4.0.jar'"
    if exist "lib\mysql-connector-j-9.4.0.jar" (
        echo MySQL Connector downloaded successfully
    ) else (
        echo Failed to download MySQL Connector. Please download manually from:
        echo https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    )
) else (
    echo MySQL Connector found
)

echo.
echo ========================================
echo Setup Complete
echo ========================================
echo.
echo Next steps:
echo 1. Run: build.bat
echo 2. Run: run.bat [inmateId]
echo.
pause