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

DATE=`date '+%Y-%m-%d-%H:%M:%S'`
command="$1"

OPTIONS=""
EXP_OPTIONS=""

while [ "$2" != "" ];
do
case $2 in
 -c|--charset) 	
	OPTIONS+="$2 $3 "
	;;
 -h|--help)    
	OPTIONS+="$2 "
	;;
 -m|--midpoint-home) 	
	echo "INFO: midpoint.home set by default"	
	;;
 -p|--password) 	
	OPTIONS+="$2 $3 "
	;;
 -P|--password-ask) 	
	OPTIONS+="$2 $3 "
	;;
 -s|--silent) 	
	OPTIONS+="$2 "
	;;
 -U|--url) 	
	OPTIONS+="$2 $3 "
	;;
 -u|--username) 	
	OPTIONS+="$2 $3 "
	;;
 -v|--verbose) 	
	OPTIONS+="$2 "
	;;
 -V|--version) 	
	OPTIONS+="$2 "
	;;
 -f|--filter)
	if [ "$command" = "export" ] ; then	
	EXP_OPTIONS+="$2 $3 "
	fi
	if [ "$command" = "import" ] ; then	
	IMP_OPTIONS+="$2 $3 "
	fi
	;;
 -o|--oid) 
	if [ "$command" = "export" ] ; then	
	EXP_OPTIONS+="$2 $3 "
	fi
	if [ "$command" = "import" ] ; then	
	IMP_OPTIONS+="$2 $3 "
	fi
	;;
 -O|--output|--overwrite) 
	if [ "$command" = "export" ] ; then		
	echo "INFO: Output file already set by default"
	fi
	if [ "$command" = "import" ] ; then
	echo "INFO: Overwrite option already set by default"	
	fi
	;;
 -r|--raw)
	if [ "$command" = "export" ] ; then		
	EXP_OPTIONS+="$2 "
	fi
	if [ "$command" = "import" ] ; then
	IMP_OPTIONS+="$2 "
	fi
	;;
 -t|--type)
	if [ "$command" = "export" ] ; then		
	EXP_OPTIONS+="$2 $3 "
	fi
	if [ "$command" = "import" ] ; then
	IMP_OPTIONS+="$2 $3 "
	fi
	;;
 -z|--zip)	
	if [ "$command" = "export" ] ; then
	EXP_OPTIONS+="$2 "
	fi
	if [ "$command" = "import" ] ; then
	IMP_OPTIONS+="$2 "
	fi
	;;
 -e|--allowUnencryptedValues)
	IMP_OPTIONS+="$2 "
	shift
	;;
 -i|--input)
	IMP_OPTIONS+="$2 $3 "
	;;
 -k|--key-password)
	KEY_OPTIONS+="$2 $3 "
	;;
 -K|--key-password-ask)
	KEY_OPTIONS+="$2 "
	;;
esac

shift

done


if [ "$command" = "export" ] ; then
	exec "$_RUNJAVA" -jar $SCRIPT_PATH../lib/ninja.jar $OPTIONS -m $MIDPOINT_HOME export $EXP_OPTIONS -O "export.$DATE.xml"
fi
if [ "$command" = "import" ] ; then
	exec "$_RUNJAVA" -jar $SCRIPT_PATH../lib/ninja.jar $OPTIONS -m $MIDPOINT_HOME import $IMP_OPTIONS -O
fi
if [ "$command" = "keys" ] ; then
	exec "$_RUNJAVA" -jar $SCRIPT_PATH../lib/ninja.jar $OPTIONS -m $MIDPOINT_HOME keys $KEY_OPTIONS
fi
