@echo off
set SERVICE_NAME=MidPoint
set "BIN_DIR=%~dp0%~2"

if not "%MIDPOINT_HOME%" == "" goto gotHome
cd "%BIN_DIR%.."
mkdir var
cd var
mkdir log
cd "%BIN_DIR%.."
set "MIDPOINT_HOME=%cd%\var"
:gotHome

REM if script for service exists
if exist "%BIN_DIR%\service.bat" goto okBoot
echo The service.bat file is not in \bin directory or is no accessible
goto end
:okBoot

if exist "%BIN_DIR%\midpoint.exe" goto okHome
echo The midpoint.exe was not found…
goto end
:okHome
rem Make sure prerequisite environment variables are set
if not "%JAVA_HOME%" == "" goto gotJdkHome
if not "%JRE_HOME%" == "" goto gotJreHome
echo Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
echo Service will try to guess them from the registry.
goto okJavaHome
:gotJreHome

if not exist "%JRE_HOME%\bin\java.exe" goto noJavaHome
if not exist "%JRE_HOME%\bin\javaw.exe" goto noJavaHome
goto okJavaHome
:gotJdkHome

if not exist "%JAVA_HOME%\bin\javac.exe" goto noJavaHome
rem Java 9 has a different directory structure
if exist "%JAVA_HOME%\jre\bin\java.exe" goto preJava9Layout
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if not exist "%JAVA_HOME%\bin\javaw.exe" goto noJavaHome
if not "%JRE_HOME%" == "" goto okJavaHome
set "JRE_HOME=%JAVA_HOME%"
goto okJavaHome
:preJava9Layout

if not exist "%JAVA_HOME%\jre\bin\javaw.exe" goto noJavaHome
if not "%JRE_HOME%" == "" goto okJavaHome
set "JRE_HOME=%JAVA_HOME%\jre"
goto okJavaHome
:noJavaHome

echo The JAVA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
echo NB: JAVA_HOME should point to a JDK not a JRE
goto end
:okJavaHome

REM MIDPOINT_WAR if not defined
if exist "%cd%\lib\midpoint.war" goto gotWar
echo The midpoint.war is not in \lib directory
echo Can not start midPoint
goto end
:gotWar

if "%MIDPOINT_HOME%" == "%MIDPOINT_HOME:;=%" goto homeNoSemicolon
echo Using MIDPOINT_HOME: "%MIDPOINT_HOME%"
echo Unable to start as MIDPOINT_HOME contains a semicolon (;) character
goto end
:homeNoSemicolon

REM ----- Execute The Requested Command ---------------------------------------

set EXECUTABLE=%BIN_DIR%\midpoint.exe
set PR_INSTALL=%EXECUTABLE%
set MIDPOINT_START_CLASS=com.evolveum.midpoint.tools.layout.MidPointWarLauncher

REM Service log configuration
set PR_LOGPREFIX=%SERVICE_NAME%
set PR_LOGPATH=%MIDPOINT_HOME%\log
set PR_STDOUTPUT=auto
set PR_STDERROR=auto
set PR_LOGLEVEL=Error

REM Path to java installation
REM Try to use the server jvm
set "PR_JVM=%JRE_HOME%\bin\server\jvm.dll"
if exist "%PR_JVM%" goto foundJvm
REM Try to use the client jvm
set "PR_JVM=%JRE_HOME%\bin\client\jvm.dll"
if exist "%PR_JVM%" goto foundJvm
echo Warning: Neither 'server' nor 'client' jvm.dll was found at JRE_HOME.
set PR_JVM=auto
:foundJvm

set PR_CLASSPATH=%cd%\lib\midpoint.war

REM Statup configuration
set PR_STARTUP=auto
set PR_STARTMODE=jvm
set PR_STARTMETHOD=main
set PR_STARTPARAMS=start
set PR_STARTCLASS=%MIDPOINT_START_CLASS%

REM Shutdown configuration
set PR_STOPMODE=jvm
set PR_STOPMETHOD=%PR_STARTMETHOD%
set PR_STOPPARAMS=stop
set PR_STOPCLASS=%MIDPOINT_START_CLASS%

REM JVM configuration
set PR_JVMMS=1024
set PR_JVMMX=1024

if %1 == install goto doInstall
if %1 == remove goto doRemove
if %1 == uninstall goto doRemove
echo Unknown parameter "%1"

:doRemove
rem Remove the service
"%EXECUTABLE%" //DS//%SERVICE_NAME%
echo The service '%SERVICE_NAME%' has been removed
goto end

:doInstall
REM Install the service
echo Installing the service '%SERVICE_NAME%' ...
echo Using MIDPOINT_HOME:    "%MIDPOINT_HOME%"
echo Using JAVA_HOME:        "%JAVA_HOME%"
echo Using JRE_HOME:         "%JRE_HOME%"

REM Install service
"%PR_INSTALL%" //IS//%SERVICE_NAME% ^
--StartMode="%PR_STARTMODE%" ^
--StartClass="%PR_STARTCLASS%" ^
--StartMethod="%PR_STARTMETHOD%" ^
--StopMode="%PR_STOPMODE%" ^
--StopClass="%PR_STOPCLASS%"  ^
--StopMethod="%PR_STOPMETHOD%" ^
--StartParams="%PR_STARTPARAMS%" ^
--StopParams="%PR_STOPPARAMS%" ^
--Jvm="%PR_JVM%" ^
--JvmMs="%PR_JVMMS%" ^
--JvmMx="%PR_JVMMX%" ^
--Startup="%PR_STARTUP%" ^
--LogPath="%PR_LOGPATH%" ^
--LogPrefix="%SERVICE_NAME%" ^
--LogLevel="%PR_LOGLEVEL%" ^
--StdOutput="%PR_STDOUTPUT%" ^
--StdError="%PR_STDERROR%" ^
--JvmOptions -Dmidpoint.home="%MIDPOINT_HOME%";-Dpython.cachedir="%MIDPOINT_HOME%\tmp";-Djavax.net.ssl.trustStore="%MIDPOINT_HOME%\keystore.jceks";-Djavax.net.ssl.trustStoreType=jceks ^
--Classpath="%PR_CLASSPATH%"  

if not errorlevel 1 goto installed
echo Failed installing '%SERVICE_NAME%' service
goto end
:installed

:end
cd %CURRENT_DIR%