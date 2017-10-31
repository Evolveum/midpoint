#!/bin/bash
echo "Starting midPoint"
path=$(cd $(dirname "$0") && pwd -P)/$(basename "$1")
java -jar -Xms1024M -Xmx2048M -Dmidpoint.home=$path../var $path../lib/midpoint.war
