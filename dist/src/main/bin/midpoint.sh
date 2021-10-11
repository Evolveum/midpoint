#!/bin/bash
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.
#
# Parts of this file Copyright (c) 2017 Evolveum and contributors
#

SCRIPT_PATH=$(cd $(dirname "$0") && pwd -P)/$(basename "$2")
USE_NOHUP="true"
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
fi

if [ ! -d var/log ] ; then
    mkdir var/log
fi

if [ -z "$MIDPOINT_HOME" ] ; then
    MIDPOINT_HOME=$(cd "$SCRIPT_PATH../var"; pwd)
fi
JAVA_OPTS="$JAVA_OPTS
-Xms2048M
-Xmx4096M
-Dpython.cachedir=$MIDPOINT_HOME/tmp
-Djavax.net.ssl.trustStore=$MIDPOINT_HOME/keystore.jceks
-Djavax.net.ssl.trustStoreType=jceks
-Dmidpoint.home=$MIDPOINT_HOME"

# apply setenv.sh if it exists. This can be used for -Dmidpoint.nodeId etc.
# the script can either append or overwrite JAVA_OPTS
if [ -r "$SCRIPT_PATH/setenv.sh" ]; then
    echo "Applying setenv.sh from $SCRIPT_PATH directory."
    . "$SCRIPT_PATH/setenv.sh"
fi

if [ -z "$BOOT_OUT" ] ; then
  BOOT_OUT="$SCRIPT_PATH"../var/log/midpoint.out
fi

if [ -z "$PID_FILE" ] ; then
 PID_FILE="$SCRIPT_PATH"../var/log/midpoint.pid
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

if [ "$1" = "start" ] ; then
  if ! which "$_RUNJAVA" &> /dev/null; then
    echo "$_RUNJAVA not found (or not executable). Start aborted."
    exit 1
  fi
  if [ ! -z "$PID_FILE" ]; then
    if [ -f "$PID_FILE" ]; then
      if [ -s "$PID_FILE" ]; then
        echo "Existing PID file found during start."
        if [ -r "$PID_FILE" ]; then
          PID=`cat "$PID_FILE"`
          ps -p $PID >/dev/null 2>&1
          if [ $? -eq 0 ] ; then
            echo "Midpoint appears to still be running with PID $PID. Start aborted."
            echo "If the following process is not a midpoint process, remove the PID file and try again:"
            ps -f -p $PID
            exit 1
          else
            echo "Removing/clearing stale PID file."
            rm -f "$PID_FILE" >/dev/null 2>&1
            if [ $? != 0 ]; then
              if [ -w "$PID_FILE" ]; then
                cat /dev/null > "$PID_FILE"
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
        rm -f "$PID_FILE" >/dev/null 2>&1
        if [ $? != 0 ]; then
          if [ ! -w "$PID_FILE" ]; then
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

  if [ ! -z "$PID_FILE" ]; then
      echo $! > "$PID_FILE"
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

  if [ ! -z "$PID_FILE" ]; then
    if [ -f "$PID_FILE" ]; then
      if [ -s "$PID_FILE" ]; then
        kill -0 `cat "$PID_FILE"` >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          echo "PID file found but no matching process was found. Stop aborted."
          exit 1
        fi
      else
        echo "PID file is empty and has been ignored."
      fi
    else
      echo "\$PID_FILE was set but the specified file does not exist. Is Tomcat running? Stop aborted."
      exit 1
    fi
  fi

    if [ ! -z "$CATALINA_PID" ]; then
      echo "The stop command failed. Attempting to signal the process to stop through OS signal."
      kill -15 `cat "$CATALINA_PID"` >/dev/null 2>&1
    fi

    if [ ! -z "$PID_FILE" ]; then
    echo "Stopping midPoint"
      kill -TERM `cat "$PID_FILE"` >/dev/null 2>&1
    fi
  # stop failed. Shutdown port disabled? Try a normal kill.


  if [ ! -z "$PID_FILE" ]; then
    if [ -f "$PID_FILE" ]; then
      while [ $SLEEP -ge 0 ]; do

        kill -0 `cat "$PID_FILE"` >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          rm -f "$PID_FILE" >/dev/null 2>&1
          if [ $? != 0 ]; then
            if [ -w "$PID_FILE" ]; then
              cat /dev/null > "$PID_FILE"
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
          kill -3 `cat "$PID_FILE"`
        fi
        SLEEP=`expr $SLEEP - 1 `
      done
    fi
  fi

  KILL_SLEEP_INTERVAL=5
  if [ $FORCE -eq 1 ]; then
    if [ -z "$PID_FILE" ]; then
      echo "Kill failed: \$PID_FILE not set"
    else
      if [ -f "$PID_FILE" ]; then
        PID=`cat "$PID_FILE"`
        echo "Killing midPoint with the PID: $PID"
        kill -9 $PID
        while [ $KILL_SLEEP_INTERVAL -ge 0 ]; do
            kill -0 `cat "$PID_FILE"` >/dev/null 2>&1
            if [ $? -gt 0 ]; then
                rm -f "$PID_FILE" >/dev/null 2>&1
                if [ $? != 0 ]; then
                    if [ -w "$PID_FILE" ]; then
                        cat /dev/null > "$PID_FILE"
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

