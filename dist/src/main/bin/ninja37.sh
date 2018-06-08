#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Parts of this file Copyright (c) 2017 Evolveum
#

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

#cd "$SCRIPT_PATH../lib"

NINJA_JAR=ninja-3.7.2-SNAPSHOT.jar

if [ ! -f lib/$NINJA_JAR ] ; then
	echo "ERROR: $NINJA_JAR is not in /lib directory"
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

while getopts ":j:" opt; do
  case $opt in
    j)
      JDBC_DRIVER=$OPTARG
      ;;
  esac
done

if [ ! -z "$JDBC_DRIVER" ] ; then
   JDBC_DRIVER="-Dloader.path=$JDBC_DRIVER"
fi

exec "$_RUNJAVA" $JDBC_DRIVER -jar $SCRIPT_PATH../lib/$NINJA_JAR -m $MIDPOINT_HOME $@


