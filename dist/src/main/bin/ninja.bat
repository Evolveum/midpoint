@echo off

setlocal

set "BIN_DIR=%~dp0"

rem if script for execution is in bin directory
if exist "%BIN_DIR%\ninja.bat" goto okBoot
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

java -jar "%cd%\lib\ninja.jar" -m "%MIDPOINT_HOME%" %*
goto end


:end

