@echo off

setlocal

set NINJA_JAR=ninja-3.7.2-SNAPSHOT.jar

set BIN_DIR=%~dp0
set ROOT_DIR=%BIN_DIR%..
set VAR_DIR=%ROOT_DIR%\var
set NINJA_JAR_PATH=%ROOT_DIR%\lib\%NINJA_JAR%

set PARAMETERS=%*

set LOADER_PATH=
:argloop
IF NOT "%1"=="" (
    IF "%1"=="-j" (
        SET LOADER_PATH=-Dloader.path=%2
        SHIFT
    )
    IF "%1"=="--jdbc" (
        SET LOADER_PATH=-Dloader.path=%2
        SHIFT
    )
    SHIFT
    GOTO :argloop
)

if "%MIDPOINT_HOME%" == "" (
    if not exist "%VAR_DIR%" (
        echo Error: Default midpoint.home directory "%VAR_DIR%" does not exist.
        goto end
    )
    set MIDPOINT_HOME=%VAR_DIR%
)

if not "%MIDPOINT_HOME%" == "%MIDPOINT_HOME:;=%" (
    echo Error: Unable to start as MIDPOINT_HOME contains a semicolon ";" character
    goto end
)

echo Using MIDPOINT_HOME:   "%MIDPOINT_HOME%"

if not exist "%NINJA_JAR_PATH%" (
    echo Error: %NINJA_JAR% is not in the lib directory.
    echo Cannot start ninja
    goto end
)

rem ----- Execute The Requested Command ---------------------------------------

set RUN_JAVA=java
if not "%JAVA_HOME%" == "" set RUN_JAVA=%JAVA_HOME%\bin\java

echo Using LOADER_PATH:     "%LOADER_PATH%"
echo Using RUN_JAVA:        "%RUN_JAVA%"

"%RUN_JAVA%" "%LOADER_PATH%" -jar "%NINJA_JAR_PATH%" -m "%MIDPOINT_HOME%" %PARAMETERS%

:end
