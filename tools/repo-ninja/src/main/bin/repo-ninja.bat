set MIDPOINT_HOME="c:\midpoint-home"

rem sets the basedir to the directory where this batch file is locaed
set BASEDIR=%~dp0

java -classpath "%BASEDIR%\lib\*" -Dmidpoint.home=%MIDPOINT_HOME% com.evolveum.midpoint.tools.ninja.Main %*
