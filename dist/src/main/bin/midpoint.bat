@echo off

setlocal

set "BIN_DIR=%~dp0%~2"
if not "%MIDPOINT_HOME%" == "" goto gotHome
cd "%BIN_DIR%.."
mkdir var
cd var
mkdir log 
cd "%BIN_DIR%.."
set "MIDPOINT_HOME=%cd%\var"
echo %MIDPOINT_HOME%
echo %BIN_DIR%
:gotHome

rem if script for start and stop exists
if exist "%BIN_DIR%\midpoint.bat" goto okBoot
echo %BIN_DIR%
echo The midpoint.bat file is not in \bin directory or is no accessible
goto end
:okBoot

rem if start/stop out file exists
if not "%BOOT_OUT%" == "" goto okOut
set "BOOT_OUT=%MIDPOINT_HOME%\log\midpoint.out"
echo %BOOT_OUT%
:okOut

rem MIDPOINT_WAR if not defined
if exist "%cd%\lib\midpoint.war" goto gotWar
echo The midpoint.war is not in \lib directory
echo Can not start midPoint
goto end
:gotWar

if "%MIDPOINT_HOME%" == "%MIDPOINT_HOME:;=%" goto homeNoSemicolon
echo Using MIDPOINT_HOME:   "%MIDPOINT_HOME%"
echo Unable to start as MIDPOINT_HOME contains a semicolon (;) character
goto end
:homeNoSemicolon


rem ----- Execute The Requested Command ---------------------------------------

echo Using MIDPOINT_HOME:   "%MIDPOINT_HOME%"

set _EXECJAVA=%_RUNJAVA%
set _NOHUP=nohup
set ACTION=start

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop

:doStart
shift
goto execStart

:doStop
shift
goto execStop

:execStart
echo "%cd%\lib\midpoint.war"
start /b javaw -jar -Xms2048M -Xmx2048M -Djavax.net.ssl.trustStore=%MIDPOINT_HOME%\keystore.jceks -Djavax.net.ssl.trustStoreType=jceks -Dmidpoint.home=%MIDPOINT_HOME% %cd%\lib\midpoint.war > %BOOT_OUT% 2>&1 &

:execStop
echo "%cd%\lib\midpoint.war"
FOR /F "usebackq tokens=5" %%i IN (`netstat -aon ^| findstr "0.0.0.0:8080"`) DO taskkill /F /PID %%i