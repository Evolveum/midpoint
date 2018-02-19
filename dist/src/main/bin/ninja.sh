#!/bin/bash

SCRIPT_PATH=$(cd $(dirname "$0") && pwd -P)/

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variable
PRGDIR=`dirname "$PRG"`

cd "$PRGDIR/.." >/dev/null

cd "$SCRIPT_PATH/.."

if [ ! -d var ] ; then
	echo "ERROR: midpoint.home directory desn't exist"
	exit 1
else
	MIDPOINT_HOME=$(cd "$SCRIPT_PATH../var"; pwd)
fi

[ -z "$MIDPOINT_HOME" ] && MIDPOINT_HOME=`cd "$SCRIPT_PATH../var" >/dev/null; pwd`

cd "$SCRIPT_PATH../lib"

if [ ! -f ninja.jar ] ; then 
	echo "ERROR: ninja.jar is not in /lib directory"
	exit 1
fi 

# Set UMASK unless it has been overridden
if [ -z "$UMASK" ]; then
    UMASK="0027"
fi
umask $UMASK

if [ -z "$JAVA_HOME" ] ; then
  _RUNJAVA=java
else
  _RUNJAVA="$JAVA_HOME"/bin/java
fi


exec "$_RUNJAVA" -jar $SCRIPT_PATH../lib/ninja.jar -m $MIDPOINT_HOME $@


