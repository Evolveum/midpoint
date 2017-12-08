@echo off

setlocal

set "BIN_DIR=%~dp0%~1"
if not "%MIDPOINT_HOME%" == "" goto gotHome
cd "%BIN_DIR%.."
mkdir var
cd var
mkdir log 
cd "%BIN_DIR%.."
set "MIDPOINT_HOME=%cd%\var"
echo %MIDPOINT_HOME%
:gotHome

if exist "%BIN_DIR%\midpoint.bat" goto okHome
echo The MIDPOINT_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

set "EXECUTABLE=%BIN_DIR%\midpoint.bat"

rem Check that target executable exists
if exist "%EXECUTABLE%" goto okExec
echo Cannot find "%EXECUTABLE%"
echo This file is needed to run this program
goto end
:okExec

rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

call "%EXECUTABLE%" start %CMD_LINE_ARGS%

:end

