@echo off

rem This script upgrades specified target 3.7.x midPoint directory to midPoint 3.8
rem by replacing appropriate files

setlocal

set BIN_DIR=%~dp0
set DIST_DIR=%BIN_DIR%..

echo MidPoint 3.8 distribution (i.e. source) is in %DIST_DIR%

if not exist "%DIST_DIR%\bin" goto :wrongDistribution
if not exist "%DIST_DIR%\lib" goto :wrongDistribution

set MIDPOINT_EXISTING_FOLDER=%1

if x%MIDPOINT_EXISTING_FOLDER% == x goto noParameter

echo Existing midPoint installation (i.e. target) is in %MIDPOINT_EXISTING_FOLDER%

if not exist %MIDPOINT_EXISTING_FOLDER% goto :noExistingFolder
if not exist %MIDPOINT_EXISTING_FOLDER%\bin goto :wrongExistingFolder
if not exist %MIDPOINT_EXISTING_FOLDER%\lib goto :wrongExistingFolder
if exist %MIDPOINT_EXISTING_FOLDER%\bin.backup goto :backupAlreadyExists
if exist %MIDPOINT_EXISTING_FOLDER%\lib.backup goto :backupAlreadyExists

echo Renaming %MIDPOINT_EXISTING_FOLDER%\bin to %MIDPOINT_EXISTING_FOLDER%\bin.backup ...
rename %MIDPOINT_EXISTING_FOLDER%\bin bin.backup

echo Renaming %MIDPOINT_EXISTING_FOLDER%\lib to %MIDPOINT_EXISTING_FOLDER%\lib.backup ...
rename %MIDPOINT_EXISTING_FOLDER%\lib lib.backup

echo Deleting %MIDPOINT_EXISTING_FOLDER%\doc
del /F /S /Q %MIDPOINT_EXISTING_FOLDER%\doc\* 1>nul

echo Copying from %DIST_DIR% into %MIDPOINT_EXISTING_FOLDER%
xcopy /S /E /Y /Q "%DIST_DIR%\*" %MIDPOINT_EXISTING_FOLDER%

echo MidPoint installation upgrade finished.

goto :end

:noParameter
echo ---
echo No arguments supplied. Please specify the location of existing midPoint installation that is to be updated.
goto end

:wrongDistribution
echo ---
echo Error: Wrong distribution: Directory %DIST_DIR%\bin or %DIST_DIR%\lib does not exist.
goto end

:noExistingFolder
echo ---
echo Error: Target folder %MIDPOINT_EXISTING_FOLDER% does not exist.
goto end

:wrongExistingFolder
echo ---
echo Error: Wrong target folder with existing midPoint installation: Directory %MIDPOINT_EXISTING_FOLDER%\bin or %MIDPOINT_EXISTING_FOLDER%\lib does not exist.
goto end

:backupAlreadyExists
echo ---
echo Error: Backup directory (bin.backup or lib.backup) already exists in %MIDPOINT_EXISTING_FOLDER%. This means that the upgrade has been already run. If you are sure that you want to run this upgrade, please remove the backup directories and start over.
goto end

:end
