@echo off

setlocal

set "BIN_DIR=%~dp0"

:parse
if "%~1"=="" goto endparse
if "%~1"=="-c" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-h" (
	set "OPTIONS=%OPTIONS% %~1"
)	
if "%~1"=="-m" (
	echo midpoint.home set by default
)
if "%~1"=="-p" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-P" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-s" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="-U" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-u" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-v" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="-V" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="-k" (
	set "KEY_OPTIONS=%KEY_OPTIONS% %~1 %~2"
)
if "%~1"=="-K" (
	set "KEY_OPTIONS=%KEY_OPTIONS% %~1"
)
shift
goto parse
:endparse

echo "BIN DIR %BIN_DIR%"
rem if script for export is in bin directory
if exist "%BIN_DIR%\keys.bat" goto okBoot
echo %BIN_DIR%
echo The keys.bat file is not in \bin
goto end
:okBoot

rem set midpoint.home
if not "%MIDPOINT_HOME%" == "" goto gotHome
cd "%BIN_DIR%.."
if exist "%BIN_DIR%..\var" goto setHome
echo %BIN_DIR%
echo ERROR: midpoint.home directory desn't exist
goto end
:setHome

set "MIDPOINT_HOME=%cd%\var"
echo %MIDPOINT_HOME%
echo %BIN_DIR%
:gotHome

rem NINJA_JAR if not defined
if exist "%cd%\lib\ninja.jar" goto gotJar
echo The ninja.jar is not in \lib directory
echo Can not start ninja
goto end

:gotJar
if "%MIDPOINT_HOME%" == "%MIDPOINT_HOME:;=%" goto homeNoSemicolon
echo Using MIDPOINT_HOME:   "%MIDPOINT_HOME%"
echo Unable to start as MIDPOINT_HOME contains a semicolon (;) character
goto end

:homeNoSemicolon

rem ----- Execute The Requested Command ---------------------------------------

echo Using MIDPOINT_HOME:   "%MIDPOINT_HOME%"

start /b java -jar "%cd%\lib\ninja.jar" %OPTIONS% -m "%MIDPOINT_HOME%" keys %KEY_OPTIONS%


:end