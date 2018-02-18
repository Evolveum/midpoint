@echo off

setlocal

set "BIN_DIR=%~dp0"
set "COMMAND=%~1"

:parse

if "%~1"=="" goto endparse
if "%~1"=="-c" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="--charset" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-h" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="--help" (
	set "OPTIONS=%OPTIONS% %~1"
)	
if "%~1"=="-m" (
	echo INFO: midpoint.home set by default
)
if "%~1"=="--midpoint-home" (
	echo INFO: midpoint.home set by default
)
if "%~1"=="-p" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="--password" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-P" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="--password-ask" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-s" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="--silent" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="-U" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="--url" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-u" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="--username" (
	set "OPTIONS=%OPTIONS% %~1 %~2"
)
if "%~1"=="-v" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="--verbose" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="-V" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="--version" (
	set "OPTIONS=%OPTIONS% %~1"
)
if "%~1"=="-f" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1 %~2"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
	)
)
if "%~1"=="--filter" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1 %~2"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
	)
)
if "%~1"=="-o" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1 %~2"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
	)
)
if "%~1"=="--oid" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1 %~2"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
	)
)
if "%~1"=="-O" (
	if "%COMMAND%"=="export" (
	echo INFO: Output file already set by default
	) 
	if "%COMMAND%"=="import" (
	echo INFO: Overwrite option already set by default
	)
)
if "%~1"=="--output" (
	if "%COMMAND%"=="export" (
	echo INFO: Output file already set by default
	) 
	if "%COMMAND%"=="import" (
	echo INFO: Overwrite option already set by default
	)
)
if "%~1"=="--overwrite" (
	if "%COMMAND%"=="export" (
	echo INFO: Output file already set by default
	) 
	if "%COMMAND%"=="import" (
	echo INFO: Overwrite option already set by default
	)
)
if "%~1"=="-r" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1"
	)
)
if "%~1"=="--raw" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1"
	)
)
if "%~1"=="-t" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1 %~2"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
	)
)
if "%~1"=="--type" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1 %~2"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
	)
)
if "%~1"=="-z" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1"
	)
)
if "%~1"=="--zip" (
	if "%COMMAND%"=="export" (
	set "EXP_OPTIONS=%EXP_OPTIONS% %~1"
	) 
	if "%COMMAND%"=="import" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1"
	)
)
if "%~1"=="-e" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1"
)
if "%~1"=="--allowUnencryptedValues" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1"
)
if "%~1"=="-i" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
)
if "%~1"=="--input" (
	set "IMP_OPTIONS=%IMP_OPTIONS% %~1 %~2"
)
if "%~1"=="-k" (
	set "KEY_OPTIONS=%KEY_OPTIONS% %~1 %~2"
)
if "%~1"=="--key-password" (
	set "KEY_OPTIONS=%KEY_OPTIONS% %~1 %~2"
)
if "%~1"=="-K" (
	set "KEY_OPTIONS=%KEY_OPTIONS% %~1"
)
if "%~1"=="--key-password-ask" (
	set "KEY_OPTIONS=%KEY_OPTIONS% %~1"
)

shift
goto parse

:endparse

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

For /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c-%%a-%%b)
For /f "tokens=1-2 delims=/:" %%a in ("%TIME%") do (set mytime=%%a-%%b)

rem ----- Execute The Requested Command ---------------------------------------

echo Using MIDPOINT_HOME:   "%MIDPOINT_HOME%"

set _EXECJAVA=%_RUNJAVA%

if ""%COMMAND%"" == ""export"" goto doExport
if ""%COMMAND%"" == ""import"" goto doImport
if ""%COMMAND%"" == ""keys""   goto doKeys

:doExport
shift
goto execExport

:doImport
shift
goto execImport

:doKeys
shift
goto execKeys

:execExport
start /b java -jar "%cd%\lib\ninja.jar" %OPTIONS% -m "%MIDPOINT_HOME%" export %EXP_OPTIONS% -O "export.%mydate%_%mytime%.xml"
goto end

:execImport
start /b java -jar "%cd%\lib\ninja.jar" %OPTIONS% -m "%MIDPOINT_HOME%" import %IMP_OPTIONS% -O
goto end

:execKeys
start /b java -jar "%cd%\lib\ninja.jar" %OPTIONS% -m "%MIDPOINT_HOME%" keys %KEY_OPTIONS%
goto end

:end

