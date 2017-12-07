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

SCRIPT_PATH=$(cd $(dirname "$0") && pwd -P)/$(basename "$2")
USE_NOHUP="true"
if [ -z "$MIDPOINT_HOME" ] ; then
	MIDPOINT_HOME="$SCRIPT_PATH../var"
fi
JAVA_OPTS="$JAVA_OPTS -Xms2048M -Xmx2048M -XX:PermSize=128m -XX:MaxPermSize=256m 
-Djavax.net.ssl.trustStore=$MIDPOINT_HOME/keystore.jceks -Djavax.net.ssl.trustStoreType=jceks 
-Dmidpoint.home=$MIDPOINT_HOME"

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

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

cd "$PRGDIR/.." >/dev/null

cd "$SCRIPT_PATH/.."

if [ ! -d var ] ; then
	mkdir var
	mkdir var/log
fi

if [ -z "$BOOT_OUT" ] ; then  
  BOOT_OUT="$SCRIPT_PATH"../var/log/springboot.out
fi

if [ -z "$SPRING_PID" ] ; then
 SPRING_PID="$SCRIPT_PATH"../var/log/spring.pid
fi

[ -z "$MIDPOINT_HOME" ] && MIDPOINT_HOME=`cd "$SCRIPT_PATH../var" >/dev/null; pwd`

cd "$SCRIPT_PATH../lib"

if [ ! -f midpoint.war ] ; then 
echo "ERROR: midpoint.war is not in /lib directory"
exit 1
fi 

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [ "`tty`" != "not a tty" ]; then
    have_tty=1
fi

if [ -z "$LOGGING_MANAGER" ]; then
  LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
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

if [ "$USE_NOHUP" = "true" ]; then
    _NOHUP=nohup
fi


# ----- Execute The Requested Command -----------------------------------------

#if [ $have_tty -eq 1 ]; then
#  if [ ! -z "$SPRING_PID" ]; then
#    echo "Using SPRING_PID:    $SPRING_PID"
#  fi
#fi

if [ "$1" = "start" ] ; then
 if [ ! -z "$SPRING_PID" ]; then
    if [ -f "$SPRING_PID" ]; then
      if [ -s "$SPRING_PID" ]; then
        echo "Existing PID file found during start."
        if [ -r "$SPRING_PID" ]; then
          PID=`cat "$SPRING_PID"`
          ps -p $PID >/dev/null 2>&1
          if [ $? -eq 0 ] ; then
            echo "Midpoint appears to still be running with PID $PID. Start aborted."
            echo "If the following process is not a midpoint process, remove the PID file and try again:"
            ps -f -p $PID
            exit 1
          else
            echo "Removing/clearing stale PID file."
            rm -f "$SPRING_PID" >/dev/null 2>&1
            if [ $? != 0 ]; then
              if [ -w "$SPRING_PID" ]; then
                cat /dev/null > "$SPRING_PID"
              else
                echo "Unable to remove or clear stale PID file. Start aborted."
                exit 1
              fi
            fi
          fi
        else
          echo "Unable to read PID file. Start aborted."
          exit 1
        fi
      else
        rm -f "$SPRING_PID" >/dev/null 2>&1
if [ $? != 0 ]; then
          if [ ! -w "$SPRING_PID" ]; then
            echo "Unable to remove or write to empty PID file. Start aborted."
            exit 1
          fi
        fi
      fi
    fi
  fi

  shift
  touch "$BOOT_OUT"

echo "Starting midPoint..."
echo "MIDPOINT_HOME=$MIDPOINT_HOME"

cd 
eval $_NOHUP "\"$_RUNJAVA\"" -jar $LOGGING_MANAGER $JAVA_OPTS \
	$SCRIPT_PATH../lib/midpoint.war \
      >> "$BOOT_OUT" 2>&1 "&"
      

if [ ! -z "$SPRING_PID" ]; then
    echo $! > "$SPRING_PID"
fi


elif [ "$1" = "stop" ] ; then

  shift

  SLEEP=10
  if [ ! -z "$1" ]; then
    echo $1 | grep "[^0-9]" >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      SLEEP=$1
      shift
    fi
  fi

  FORCE=0
  if [ "$1" = "-force" ]; then
    shift
    FORCE=1
  fi

  if [ ! -z "$SPRING_PID" ]; then
    if [ -f "$SPRING_PID" ]; then
      if [ -s "$SPRING_PID" ]; then
        kill -0 `cat "$SPRING_PID"` >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          echo "PID file found but no matching process was found. Stop aborted."
          exit 1
        fi
      else
        echo "PID file is empty and has been ignored."
      fi
    else
      echo "\$SPRING_PID was set but the specified file does not exist. Is Tomcat running? Stop aborted."
      exit 1
    fi
  fi
  
    if [ ! -z "$CATALINA_PID" ]; then
      echo "The stop command failed. Attempting to signal the process to stop through OS signal."
      kill -15 `cat "$CATALINA_PID"` >/dev/null 2>&1
    fi

    if [ ! -z "$SPRING_PID" ]; then
	echo "Stopping midPoint"
      kill -TERM `cat "$SPRING_PID"` >/dev/null 2>&1
    fi
  # stop failed. Shutdown port disabled? Try a normal kill.
    

  if [ ! -z "$SPRING_PID" ]; then
    if [ -f "$SPRING_PID" ]; then
      while [ $SLEEP -ge 0 ]; do
	
        kill -0 `cat "$SPRING_PID"` >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          rm -f "$SPRING_PID" >/dev/null 2>&1
          if [ $? != 0 ]; then
            if [ -w "$SPRING_PID" ]; then
              cat /dev/null > "$SPRING_PID"
              # If MidPoint has stopped don't try and force a stop with an empty PID file
              FORCE=0
            else
              echo "The PID file could not be removed or cleared."
            fi
          fi
          echo "Midpoint stopped."
          break
        fi
        if [ $SLEEP -gt 0 ]; then
          sleep 1
        fi

  if [ $SLEEP -eq 0 ]; then
          echo "Midpoint did not stop in time."
          if [ $FORCE -eq 0 ]; then
            echo "PID file was not removed."
          fi
          echo "To aid diagnostics a thread dump has been written to standard out."
          kill -3 `cat "$SPRING_PID"`
        fi
        SLEEP=`expr $SLEEP - 1 `
      done
    fi
  fi

  KILL_SLEEP_INTERVAL=5
  if [ $FORCE -eq 1 ]; then
    if [ -z "$SPRING_PID" ]; then
      echo "Kill failed: \$SPRING_PID not set"
    else
      if [ -f "$SPRING_PID" ]; then
        PID=`cat "$SPRING_PID"`
        echo "Killing midPoint with the PID: $PID"
        kill -9 $PID
        while [ $KILL_SLEEP_INTERVAL -ge 0 ]; do
            kill -0 `cat "$SPRING_PID"` >/dev/null 2>&1
            if [ $? -gt 0 ]; then
                rm -f "$SPRING_PID" >/dev/null 2>&1
                if [ $? != 0 ]; then
                    if [ -w "$SPRING_PID" ]; then
                        cat /dev/null > "$SPRING_PID"
                    else
                        echo "The PID file could not be removed."
                    fi
                fi
                echo "The MidPoint process has been killed."
                break
            fi
            if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                sleep 1
            fi
            KILL_SLEEP_INTERVAL=`expr $KILL_SLEEP_INTERVAL - 1 `
        done
        if [ $KILL_SLEEP_INTERVAL -lt 0 ]; then
            echo "MidPoint has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
        fi
      fi
    fi
  fi
fi

