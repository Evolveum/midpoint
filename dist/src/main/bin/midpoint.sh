#!/usr/bin/env bash
# Portions Copyright (C) 2017-2020 Evolveum and contributors
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.
#
# Script should run on bash 3.2 to allow OS X usage.

### Default values ###

#JAVA_HOME

JAVA_def_Xms="2048M"
JAVA_def_Xmx="4096M"
JAVA_def_python_cachedir="tmp"
JAVA_def_trustStore="keystore.jceks"
JAVA_def_trustStoreType="jceks"

USE_NOHUP="true"

######################

set -eu

if [[ -z ${1:-} ]]; then
  echo "Usage: midpoint.sh start|stop [other args...]"
  exit 1
fi

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd -P)
BASE_DIR=$(cd "${SCRIPT_DIR}/.." && pwd -P)

# setting default to MIDPOINT_HOME if not set
: "${MIDPOINT_HOME:="${BASE_DIR}/var"}"

mkdir -p "${MIDPOINT_HOME}/log"

# shellcheck disable=SC2034  # ORIG_JAVA_OPTS can be used used in setenv.sh lower
ORIG_JAVA_OPTS="${JAVA_OPTS:-}"

#############################
# Originally Docker related #
#############################

################
# entry point for source of the files to copy into midpoint home directory before the application start
################

if [ "${MP_ENTRY_POINT:-}" != "" ]	# /opt/midpoint-dirs-docker-entrypoint
then
	if [ -e ${MP_ENTRY_POINT} ]
	then
		echo "Processing ${MP_ENTRY_POINT} directory..."
		for i in $( find ${MP_ENTRY_POINT} -mindepth 1 -maxdepth 1 -type d )
		do
			l_name=$(basename ${i})
			[ ! -e ${MIDPOINT_HOME}/${l_name} ] && mkdir -p ${MIDPOINT_HOME}/${l_name}
			for s in $( find ${i} -mindepth 1 -maxdepth 1 -type f -follow -exec basename \{\} \; )
			do
				if [ ! -e ${MIDPOINT_HOME}/${l_name}/${s} -a ! -e ${MIDPOINT_HOME}/${l_name}/${s}.done ]
				then
					echo "COPY ${i}/${s} => ${MIDPOINT_HOME}/${l_name}/${s}"
					cp ${i}/${s} ${MIDPOINT_HOME}/${l_name}/${s}
				else
					echo "SKIP: ${i}/${s}"
				fi
			done
		done
		echo "- - - - - - - - - - - - - - - - - - - - -"
		unset l_name
	fi
fi

################

[ "${MP_MEM_MAX:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Xmx[^[:space:]]*//") -Xmx${MP_MEM_MAX}"
[ "${MP_MEM_INIT:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Xms[^[:space:]]*//") -Xms${MP_MEM_INIT}"

[ "${REPO_PORT:-}" != "" ] && db_port=${REPO_PORT}
if [ "${REPO_DATABASE_TYPE:-}" != "" ]
then
	JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.database=[^[:space:]]*//") -Dmidpoint.repository.database=${REPO_DATABASE_TYPE}"
	[ "${db_port:-}" == "default" ] && db_port=""
	case ${REPO_DATABASE_TYPE} in
		h2)
			[ "${db_port:-}" == "" ] && db_port=5437
			db_prefix="jdbc:h2:tcp://"
			db_path="/${REPO_DATABASE:-midpoint}"
			;;
		mariadb)
			[ "${db_port:-}" == "" ] && db_port=3306
			db_prefix="jdbc:mariadb://"
			db_path="/${REPO_DATABASE:-midpoint}?characterEncoding=utf8"
			;;
		mysql)
			[ "${db_port:-}" == "" ] && db_port=3306
			db_prefix="jdbc:mysql://"
			db_path="/${REPO_DATABASE:-midpoint}?characterEncoding=utf8"
			;;
		oracle)
			[ "${db_port:-}" == "" ] && db_port=1521
			db_prefix="jdbc:oracle:thin:@"
			db_path="/xe"
			;;
		postgresql)
			[ "${db_port:-}" == "" ] && db_port=5432
			db_prefix="jdbc:postgresql://"
			db_path="/${REPO_DATABASE:-midpoint}"
			;;
		sqlserver)
			[ "${db_port:-}" == "" ] && db_port=1433
			db_prefix="jdbc:sqlserver://"
			db_path=";databse=${REPO_DATABASE:-midpoint}"
			;;
		*)
			if [ "${db_port:-}" == "" -a "${REPO_URL:-}" == "" ]
			then
				echo "~~~~~ please supply JDBC port for your repository ~~~~~"
				exit 1
			fi
			;;
	esac
	if [ "${REPO_URL:-}" != "" ]
	then
		JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.jdbcUrl=[^[:space:]]*//") -Dmidpoint.repository.jdbcUrl=${REPO_URL}"
	else
		JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.jdbcUrl=[^[:space:]]*//") -Dmidpoint.repository.jdbcUrl=${db_prefix}${REPO_HOST:-localhost}:${db_port}${db_path}"
	fi
fi
[ "${REPO_USER:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.jdbcUsername=[^[:space:]]*//") -Dmidpoint.repository.jdbcUsername=${REPO_USER}"
[ "${REPO_PASSWORD_FILE:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.jdbcPassword_FILE=[^[:space:]]*//") -Dmidpoint.repository.jdbcPassword_FILE=${REPO_PASSWORD_FILE}"
[ "${REPO_MISSING_SCHEMA_ACTION:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.missingSchemaAction=[^[:space:]]*//") -Dmidpoint.repository.missingSchemaAction=${REPO_MISSING_SCHEMA_ACTION}"
[ "${REPO_UPGRADEABLE_SCHEMA_ACTION:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.upgradeableSchemaAction=[^[:space:]]*//") -Dmidpoint.repository.upgradeableSchemaAction=${REPO_UPGRADEABLE_SCHEMA_ACTION}"
[ "${REPO_SCHEMA_VARIANT:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.schemaVariant=[^[:space:]]*//") -Dmidpoint.repository.schemaVariant=${REPO_SCHEMA_VARIANT}"
[ "${REPO_SCHEMA_VERSION_IF_MISSING:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.repository.schemaVersionIfMissing=[^[:space:]]*//") -Dmidpoint.repository.schemaVersionIfMissing=${REPO_SCHEMA_VERSION_IF_MISSING}"

[ "${MP_KEYSTORE_PASSWORD_FILE:-}" != "" ] && JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-Dmidpoint.keystore.keyStorePassword_FILE=[^[:space:]]*//") -Dmidpoint.keystore.keyStorePassword_FILE=${MP_KEYSTORE_PASSWORD_FILE}"

if [ -e /.dockerenv ]
then
	JAVA_OPTS="${JAVA_OPTS:-} -Dmidpoint.repository.hibernateHbm2ddl=none"
	JAVA_OPTS="${JAVA_OPTS:-} -Dmidpoint.repository.initializationFailTimeout=60000"

	JAVA_OPTS="${JAVA_OPTS:-} -Dfile.encoding=UTF8"
	JAVA_OPTS="${JAVA_OPTS:-} -Dmidpoint.logging.alt.enabled=true"
fi

#############################

# Check for the default JAVA_OPTS values. In case the specific key is already set the value is kept untouched.
# To prevent Xms to be set pass the --Xms to JAVA_OPTS (double dash).
# To prevent Xmx to be set pass the --Xmx to JAVA_OPTS (double dash).

if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Xmx[0-9]")
then
        if $(echo "${JAVA_OPTS:-}" | grep -q "\-\-Xmx")
        then
                JAVA_OPTS="$(echo "${JAVA_OPTS:-}" | sed "s/\-\-Xmx//")"
        else
                JAVA_OPTS="-Xmx${JAVA_def_Xmx} ${JAVA_OPTS:-}"
        fi
fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Xms[0-9]")
then
        if $(echo "${JAVA_OPTS:-}" | grep -q "\-\-Xms")
        then
                JAVA_OPTS="$(echo "${JAVA_OPTS:-}" | sed "s/\-\-Xms//")"
        else
                JAVA_OPTS="-Xms${JAVA_def_Xms} ${JAVA_OPTS:-}"
        fi
fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Dpython.cachedir=")  ; then  JAVA_OPTS="${JAVA_OPTS:-} -Dpython.cachedir=${MIDPOINT_HOME}/${JAVA_def_python_cachedir}" ; fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djavax.net.ssl.trustStore=") ; then  JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStore=${MIDPOINT_HOME}/${JAVA_def_trustStore}" ; fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djavax.net.ssl.trustStoreType=") ; then JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStoreType=${JAVA_def_trustStoreType}" ; fi

if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Dmidpoint.home=") ; then JAVA_OPTS="${JAVA_OPTS:-} -Dmidpoint.home=${MIDPOINT_HOME}" ; fi

# TODO probably useless from Tomcat 7 and before history, remove and check LDAP/ConnId logging
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djava.util.logging.manager=") ; then JAVA_OPTS="-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager ${JAVA_OPTS:-}" ; fi

#clean up white spaces in case of key/value removal from the original JAVA_OPTS parameter set
JAVA_OPTS="$(echo "${JAVA_OPTS:-}" | tr -s [[:space:]] " " | sed "s/^[[:space:]]//;s/[[:space:]]$//" )"

# Apply bin/setenv.sh if it exists. This setenv.sh does not depend on MIDPOINT_HOME.
# The script can either append or overwrite JAVA_OPTS, e.g. to set -Dmidpoint.nodeId.
# It can also utilize ORIG_JAVA_OPTS that is original JAVA_OPTS before running midpoint.sh.
if [[ -r "${SCRIPT_DIR}/setenv.sh" ]]; then
  echo "Applying setenv.sh from ${SCRIPT_DIR} directory."
  # shellcheck disable=SC1091
  . "${SCRIPT_DIR}/setenv.sh"
fi

# Apply $MIDPOINT_HOME/setenv.sh if it exists. This is flexible and related to chosen MIDPOINT_HOME.
if [[ -r "${MIDPOINT_HOME}/setenv.sh" ]]; then
  echo "Applying setenv.sh from ${MIDPOINT_HOME} directory."
  # shellcheck disable=SC1091
  . "${MIDPOINT_HOME}/setenv.sh"
fi

: "${BOOT_OUT:="${MIDPOINT_HOME}/log/midpoint.out"}"
: "${PID_FILE:="${MIDPOINT_HOME}/log/midpoint.pid"}"

if [[ ! -f "${BASE_DIR}/lib/midpoint.war" ]]; then
  echo "ERROR: midpoint.war is not in /lib directory"
  exit 1
fi

# Set UMASK unless it has been overridden
: "${UMASK:="0027"}"
umask ${UMASK}

# can't use -v here because of bash 3.2 support
if [[ -z "${JAVA_HOME:-}" ]]; then
  _RUNJAVA=java
else
  _RUNJAVA="${JAVA_HOME}/bin/java"
fi

if [[ "${USE_NOHUP}" == "true" ]]; then
  _NOHUP="nohup"
else
  _NOHUP=""
fi

#############################################################
# Generate Systemd service definition
#####
# https://docs.evolveum.com/midpoint/install/systemd/
#############################################################

if [ "${MP_GEN_SYSTEMD:-}" != "" ]
then
	JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-genSystemd//" | tr -s [[:space:]] " " | sed "s/^[[:space:]]//;s/[[:space:]]$//" )"
	echo "Place following content to /etc/systemd/system/midpoint.service file" >&2
	echo "MP_GEN_SYSTEMD=1 ${0} ${@} | sudo tee /etc/systemd/system/midpoint.service" >&2
	echo >&2
	cat <<EOF
[Unit]
Description=MidPoint Standalone Service
###Requires=postgresql.service
###After=postgresql.service
[Service]
User=${MP_USER:-midpoint}
WorkingDirectory=${BASE_DIR}
ExecStart="${_RUNJAVA}" ${JAVA_OPTS} -jar "${BASE_DIR}/lib/midpoint.war" "$@"
SuccessExitStatus=143
###TimeoutStopSec=120s
[Install]
WantedBy=multi-user.target
EOF
	echo >&2
	echo -e "sudo systemctl daemon-reload\nsudo systemctl enable midpoint\nsudo systemctl start midpoint" >&2
	exit 0
fi

#############################################################

# ----- Execute The Requested Command -----------------------------------------

if [[ "$1" == "start" ]]; then
  if ! which "${_RUNJAVA}" &>/dev/null; then
    echo "${_RUNJAVA} not found (or not executable). Start aborted."
    exit 1
  fi
  if [[ -n "${PID_FILE}" ]]; then
    if [[ -f "${PID_FILE}" ]]; then
      if [[ -s "${PID_FILE}" ]]; then
        echo "Existing PID file found during start."
        if [[ -r "${PID_FILE}" ]]; then
          PID=$(cat "${PID_FILE}")
          if ps -p "${PID}" >/dev/null 2>&1; then
            echo "Midpoint appears to still be running with PID ${PID}. Start aborted."
            echo "If the following process is not a midpoint process, remove the PID file and try again:"
            ps -f -p "${PID}"
            exit 1
          else
            echo "Removing/clearing stale PID file."
            rm -f "${PID_FILE}" >/dev/null 2>&1 || {
              if [[ -w "${PID_FILE}" ]]; then
                cat /dev/null >"${PID_FILE}"
              else
                echo "Unable to remove or clear stale PID file. Start aborted."
                exit 1
              fi
            }
          fi
        else
          echo "Unable to read PID file. Start aborted."
          exit 1
        fi
      else
        rm -f "${PID_FILE}" >/dev/null 2>&1 || {
          if [[ ! -w "${PID_FILE}" ]]; then
            echo "Unable to remove or write to empty PID file. Start aborted."
            exit 1
          fi
        }
      fi
    fi
  fi

  shift
  touch "${BOOT_OUT}"

  echo "Starting midPoint..."
  echo "MIDPOINT_HOME=${MIDPOINT_HOME}"

  echo -e "\nStarting at $(date)\nMIDPOINT_HOME=${MIDPOINT_HOME}\nJava binary: ${_RUNJAVA}\nJava version:" >>"${BOOT_OUT}"
  # can't use &>> here because of bash 3.2 support
  "${_RUNJAVA}" -version >>"${BOOT_OUT}" 2>&1

  # shellcheck disable=SC2086
  eval "${_NOHUP}" "\"${_RUNJAVA}\"" \
    ${JAVA_OPTS} \
    -jar "${BASE_DIR}/lib/midpoint.war" \
    "$@" \
    "&" >>"${BOOT_OUT}" 2>&1

  if [[ -n "${PID_FILE}" ]]; then
    echo $! >"${PID_FILE}"
  fi

elif [[ "$1" == "stop" ]]; then

  shift

  FORCE=0
  if [[ "${1:-}" == "-force" ]]; then
    shift
    FORCE=1
  fi

  if [[ -f "${PID_FILE}" ]]; then
    if [[ -s "${PID_FILE}" ]]; then
      PID=$(cat "${PID_FILE}")
      kill -0 "${PID}" >/dev/null 2>&1 || {
        echo "PID file found but no matching process was found. Stop aborted."
        exit 1
      }
    else
      echo "PID file '${PID_FILE}' is empty?"
      exit 1
    fi
  else
    echo "PID file '${PID_FILE}' does not exist. MidPoint is probably stopped."
    exit 1
  fi

  echo "Stopping midPoint"
  kill -TERM "${PID}" >/dev/null 2>&1

  SLEEP=10
  if [[ -f "${PID_FILE}" ]]; then
    while ((SLEEP > 0)); do
      kill -0 "${PID}" >/dev/null 2>&1 || {
        rm -f "${PID_FILE}" >/dev/null 2>&1 || {
          if [[ -w "${PID_FILE}" ]]; then
            cat /dev/null >"${PID_FILE}"
            # If MidPoint has stopped don't try and force a stop with an empty PID file
            FORCE=0
          else
            echo "The PID file could not be removed or cleared."
          fi
        }
        echo "Midpoint stopped."
        break
      }
      if ((SLEEP > 0)); then
        sleep 1
      fi

      if ((SLEEP == 0)); then
        echo "Midpoint did not stop in time."
        if ((FORCE == 0)); then
          echo "PID file was not removed."
        fi
        echo "To aid diagnostics a thread dump has been written to standard out."
        kill -3 "${PID}"
      fi
      SLEEP=$((SLEEP - 1))
    done
  fi

  KILL_SLEEP_INTERVAL=5
  if ((FORCE == 1)); then
    if [[ -f "${PID_FILE}" ]]; then
      echo "Killing midPoint with the PID: ${PID}"
      kill -9 "${PID}"
      while ((KILL_SLEEP_INTERVAL > 0)); do
        kill -0 "${PID}" >/dev/null 2>&1 || {
          rm -f "${PID_FILE}" >/dev/null 2>&1 || {
            if [[ -w "${PID_FILE}" ]]; then
              cat /dev/null >"${PID_FILE}"
            else
              echo "The PID file could not be removed."
            fi
          }
          echo "The MidPoint process has been killed."
          break
        }
        if ((KILL_SLEEP_INTERVAL > 0)); then
          sleep 1
        fi
        KILL_SLEEP_INTERVAL=$((KILL_SLEEP_INTERVAL - 1))
      done
      if ((KILL_SLEEP_INTERVAL < 0)); then
        echo "MidPoint has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
      fi
    fi
  fi
fi
