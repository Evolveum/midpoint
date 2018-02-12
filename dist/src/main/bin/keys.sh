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


OPTIONS=""
KEY_OPTIONS=""

while getopts "c:hm:p:P:sU:u:vVk:K" option; do
 case "${option}" in
 c) c="-c $OPTARG "
	OPTIONS+="${c}"
 	;;
 h) h="-h "
	OPTIONS+="${h}"
	;;
 m) echo "midpoint.home set by default"	
	;;
 p) p="-p $OPTARG "
	OPTIONS+="${p}"
	;;
 P) P="-P $OPTARG "
	OPTIONS+="${P}"
	;;
 s) s="-s "
	OPTIONS+="${s}"
	;;
 U) U="-U $OPTARG "
	OPTIONS+="${U}"
	;;
 u) u="-u $OPTARG "
	OPTIONS+="${u}"
	;;
 v) v="-v "
	OPTIONS+="${v}"
	echo "${v}" 
	;;
 V) V="-V "
	OPTIONS+="${V}"
	;;
 k) k="-k $OPTARG "
	KEY_OPTIONS+="${k}"
	;;
 K) K="-K "
	KEY_OPTIONS+="${K}"
	;;

 esac
done

exec "$_RUNJAVA" -jar $SCRIPT_PATH../lib/ninja.jar $OPTIONS -m $MIDPOINT_HOME keys $KEY_OPTIONS
