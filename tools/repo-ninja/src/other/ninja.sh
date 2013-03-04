#!/bin/bash
MIDPOINT_HOME=/var/opt/midpoint
NINJA_JAR=repo-ninja-2.2-SNAPSHOT.jar

NINJA_HOME="`dirname \"$0\"`"

exec java -Dmidpoint.home=$MIDPOINT_HOME -jar $NINJA_HOME/$NINJA_JAR "$@"
