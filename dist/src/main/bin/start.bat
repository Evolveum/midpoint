echo "Starting midPoint"
java -jar -Xms1024M -Xmx2048M -Dmidpoint.home=%~dp0%~1..\var %~dp0%~1..\lib\midpoint.war
