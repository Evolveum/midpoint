#!/usr/bin/env bash
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.
#
# Parts of this file Copyright (c) 2017 Evolveum and contributors
#

SCRIPT_PATH=$(cd $(dirname "$0") && pwd -P)/

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' >/dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done

# Get standard environment variable
PRGDIR=$(dirname "$PRG")

cd "$PRGDIR/.." >/dev/null

cd "$SCRIPT_PATH/.."

# honor MIDPOINT_HOME variable if it exists!
if [ -z "$MIDPOINT_HOME" ]; then
  if [ ! -d var ]; then
    echo "ERROR: midpoint.home directory desn't exist"
    exit 1
  else
    MIDPOINT_HOME=$(
      cd "$SCRIPT_PATH../var"
      pwd
    )
  fi

  [ -z "$MIDPOINT_HOME" ] && MIDPOINT_HOME=$(
    cd "$SCRIPT_PATH../var" >/dev/null
    pwd
  )
fi
#cd "$SCRIPT_PATH../lib"

if [ ! -f lib/ninja.jar ]; then
  echo "ERROR: ninja.jar is not in /lib directory"
  exit 1
fi

# Set UMASK unless it has been overridden
if [ -z "$UMASK" ]; then
  UMASK="0027"
fi
umask $UMASK

if [ -z "$JAVA_HOME" ]; then
  _RUNJAVA=java
else
  _RUNJAVA="$JAVA_HOME"/bin/java
fi

while getopts ":j:" opt; do
  case $opt in
  j)
    JDBC_DRIVER=$OPTARG
    ;;
  esac
done

if [ ! -z "$JDBC_DRIVER" ]; then
  JDBC_DRIVER="-Dloader.path=$JDBC_DRIVER"
fi

exec "$_RUNJAVA" $JDBC_DRIVER -jar $SCRIPT_PATH../lib/ninja.jar -m $MIDPOINT_HOME "$@"
