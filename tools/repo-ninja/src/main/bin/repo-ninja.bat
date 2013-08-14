rem Change the following line!
set MIDPOINT_HOME="c:\midpoint-home"

rem Sets the basedir to the directory where libraries are located
set BASEDIR=%~dp0..

java -classpath "%BASEDIR%\lib\*" -Dmidpoint.home=%MIDPOINT_HOME% com.evolveum.midpoint.tools.ninja.Main %*
