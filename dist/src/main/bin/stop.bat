@echo off

setlocal

set "BIN_DIR=%~dp0"
set "EXECUTABLE=%BIN_DIR%midpoint.bat"

if not exist "%EXECUTABLE%" (
    echo Error: Cannot find "%EXECUTABLE%"
    echo This file is needed to run this program
    goto end
)

call "%EXECUTABLE%" stop %*

:end