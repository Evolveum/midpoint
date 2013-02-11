#!/bin/sh

#uncomment and set proper midpoint home if you want to validate repository
#OPTS="-Dmidpoint.home=/home/lazyman/Work/evolveum/midpoint/trunk/tools/repo-ninja/src/test/resources/midpoint-home"

java $OPTS -cp *:./lib/* com.evolveum.midpoint.tools.ninja.Main "$@"