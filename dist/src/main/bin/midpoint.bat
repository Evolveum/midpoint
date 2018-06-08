@echo off

setlocal

if "%1" == "start" goto doStart
if "%1" == "stop" goto doStop

echo Error: No command given. Specify either start or stop.
goto end

:doStart
set BIN_DIR=%~dp0
set LIB_DIR=%BIN_DIR%..\lib

if "%MIDPOINT_HOME%" == "" (
    cd "%BIN_DIR%.."
    if not exist var mkdir var
    if not exist var\log mkdir var\log
    set "MIDPOINT_HOME=%BIN_DIR%..\var"
)
echo Using MIDPOINT_HOME:   "%MIDPOINT_HOME%"

if not exist "%BIN_DIR%midpoint.bat" (
    echo Error: The midpoint.bat file is not in bin directory or is not accessible.
    goto end
)

if not exist "%LIB_DIR%\midpoint.war" (
    echo Error: The midpoint.war is not in the lib directory
    goto end
)

if not "%MIDPOINT_HOME%" == "%MIDPOINT_HOME:;=%" (
    echo Error: MIDPOINT_HOME contains a semicolon ";" character.
    goto end
)

if "%BOOT_OUT%" == "" set BOOT_OUT=%MIDPOINT_HOME%\log\midpoint.out
echo Using BOOT_OUT:        "%BOOT_OUT%"

rem ----- Execute The Requested Start Command ---------------------------------------

shift
set RUN_JAVA=javaw
if not "%JAVA_HOME%" == "" set RUN_JAVA=%JAVA_HOME%\bin\javaw

echo Using RUN_JAVA:        "%RUN_JAVA%"
echo Using JAVA_OPTS:       "%JAVA_OPTS%"
echo Using parameters:      "%*"
echo.
echo Starting midPoint.
start /b %RUN_JAVA% -jar %JAVA_OPTS% -Xms2048M -Xmx2048M -Dpython.cachedir="%MIDPOINT_HOME%\tmp" -Djavax.net.ssl.trustStore="%MIDPOINT_HOME%\keystore.jceks" -Djavax.net.ssl.trustStoreType=jceks -Dmidpoint.home="%MIDPOINT_HOME%" "%LIB_DIR%\midpoint.war" %* > "%BOOT_OUT%" 2>&1
goto end

:doStop

set MIDPOINT_PORT=8080

shift
echo Trying to find and stop a process listening on port %MIDPOINT_PORT%...
set MIDPOINT_FOUND=
FOR /F "usebackq tokens=5" %%i IN (`netstat -aon ^| findstr "0.0.0.0:%MIDPOINT_PORT% "`) DO (
    taskkill /F /PID %%i
    set MIDPOINT_FOUND=true
)
if not "%MIDPOINT_FOUND%" == "true" echo No process listening on %MIDPOINT_PORT% was found.
goto end

:end
