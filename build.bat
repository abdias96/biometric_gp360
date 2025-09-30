@echo off
echo ========================================
echo GP360 Biometric Service Builder
echo ========================================
echo.

REM Check if lib directory exists
if not exist "lib" (
    echo ERROR: lib directory not found.
    echo Please run setup.bat first to copy required libraries.
    echo.
    echo To setup: .\setup.bat [PowerShell] or setup.bat [CMD]
    pause
    exit /b 1
)

REM Check for required libraries - Try U.are.U SDK first, then legacy
set LIBS_FOUND=0
set SDK_TYPE=none

REM Check for U.are.U SDK libraries
if exist "lib\dpuareu.jar" (
    if exist "lib\dpjavapos.jar" (
        set LIBS_FOUND=1
        set SDK_TYPE=uareu
        echo Found U.are.U SDK libraries
    )
)

REM If U.are.U not found, check for legacy OneTouch SDK
if %LIBS_FOUND%==0 (
    if exist "lib\dpfpenrollment.jar" (
        if exist "lib\dpfpverification.jar" (
            set LIBS_FOUND=1
            set SDK_TYPE=onetouch
            echo Found OneTouch SDK libraries [legacy]
        )
    )
)

if %LIBS_FOUND%==0 (
    echo ERROR: No DigitalPersona SDK libraries found in lib directory.
    echo Please run setup.bat first to copy the U.are.U SDK libraries.
    echo.
    echo Expected libraries in lib directory:
    echo   For U.are.U SDK: dpuareu.jar, dpjavapos.jar, jpos113.jar
    echo   For OneTouch SDK: dpfpenrollment.jar, dpfpverification.jar
    pause
    exit /b 1
)

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo Maven is not installed or not in PATH
    echo Attempting to build with javac...
    goto :javac_build
) else (
    goto :maven_build
)

:maven_build
echo Building with Maven...
call mvn clean package
if %errorlevel% equ 0 (
    echo.
    echo Build successful!
    echo JAR file created at: target\biometric-service-1.0.0-jar-with-dependencies.jar
    echo.
    echo To run the application:
    echo java -jar target\biometric-service-1.0.0-jar-with-dependencies.jar
) else (
    echo Build failed!
)
goto :end

:javac_build
echo Building with javac...
echo.

REM Create directories
if not exist "build\classes" mkdir build\classes
if not exist "dist" mkdir dist


REM Set classpath based on available SDK
if "%SDK_TYPE%"=="uareu" (
    set CLASSPATH=lib\dpuareu.jar;lib\dpjavapos.jar;lib\jpos113.jar;lib\json-20230618.jar;lib\mysql-connector-j-9.4.0.jar;lib\xercesImpl-2.6.2.jar;lib\xmlParserAPIs.jar
) else (
    set CLASSPATH=lib\dpfpenrollment.jar;lib\dpfpverification.jar;lib\dpotapi.jar;lib\dpotjni.jar;lib\json-20230618.jar;lib\mysql-connector-j-9.4.0.jar
)

REM Find all Java files and compile
echo Compiling Java sources...
dir /s /B src\main\java\*.java > sources.txt
javac -cp "%CLASSPATH%" -d build\classes @sources.txt
del sources.txt

if %errorlevel% equ 0 (
    echo Compilation successful!

    REM Create manifest for Enrollment
    echo Creating enrollment manifest...
    echo Manifest-Version: 1.0> build\MANIFEST.MF
    echo Main-Class: com.gp360.biometric.BiometricApplication>> build\MANIFEST.MF
    echo Class-Path: lib/dpuareu.jar lib/dpjavapos.jar lib/jpos113.jar lib/json-20230618.jar lib/mysql-connector-j-9.4.0.jar lib/xercesImpl-2.6.2.jar lib/xmlParserAPIs.jar>> build\MANIFEST.MF
    echo.>> build\MANIFEST.MF

    REM Create JAR for enrollment
    echo Creating JAR file...
    cd build\classes
    jar cfm ..\..\dist\BiometricService.jar ..\MANIFEST.MF com\*
    cd ..\..

    REM Create manifest for Verification
    echo Creating verification manifest...
    echo Manifest-Version: 1.0> build\MANIFEST_VERIFICATION.MF
    echo Main-Class: com.gp360.biometric.verification.VerificationApp>> build\MANIFEST_VERIFICATION.MF
    echo Class-Path: lib/dpuareu.jar lib/dpjavapos.jar lib/jpos113.jar lib/json-20230618.jar lib/mysql-connector-j-9.4.0.jar lib/xercesImpl-2.6.2.jar lib/xmlParserAPIs.jar>> build\MANIFEST_VERIFICATION.MF
    echo.>> build\MANIFEST_VERIFICATION.MF

    REM Create JAR for verification
    echo Creating verification JAR file...
    cd build\classes
    jar cfm ..\..\dist\BiometricVerification.jar ..\MANIFEST_VERIFICATION.MF com\*
    cd ..\..

    echo.
    echo Build successful!
    echo JAR files created:
    echo   - dist\BiometricService.jar [Enrollment]
    echo   - dist\BiometricVerification.jar [Verification]
    echo.
    echo To run:
    echo   Enrollment: run.bat [inmateId]
    echo   Verification: verificar.bat
) else (
    echo Compilation failed!
)

:end
pause