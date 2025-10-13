@echo off

rem
rem Copyright (C) 2010-2025 Evolveum and contributors
rem
rem Licensed under the EUPL-1.2 or later.
rem

setlocal

set "BIN_DIR=%~dp0"
set "EXECUTABLE=%BIN_DIR%midpoint.bat"

if not exist "%EXECUTABLE%" (
    echo Error: Cannot find "%EXECUTABLE%"
    echo This file is needed to run this program
    goto end
)

call "%EXECUTABLE%" start %*

:end
