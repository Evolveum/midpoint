#!/usr/bin/env bash
# Portions Copyright (C) 2017-2023 Evolveum and contributors
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

ENV_MAP_PREFIX="MP_SET_"
ENV_UNMAP_PREFIX="MP_UNSET_"

######################
#  Handling SIGnals
######################
#
# Without this definition the signal received by bash (script) is silently
# ignored - not passed to child processes.
#
######################
_sigterm() {
	echo " :SIGNAL PROCESSING: Receiving SIGTERM signal! Passing to the midPoint instance." >&2
	if [ -s "${PID_FILE}" ]
	then
		kill -TERM $(cat "${PID_FILE}") 2>/dev/null
	else
		echo "Unfortunatelly the PID file is empty..." >&2
	fi
}

trap _sigterm SIGTERM

_sigkill() {
	echo " : SIGNAL PROCESSING: Receiving SIGKILL signal! Passing to the midPoint instance." >&2
	if [ -s "${PID_FILE}" ]
	then
		kill -KILL $(cat "${PID_FILE}") 2>/dev/null
	else
		echo "Unfortunatelly the PID file is empty..." >&2
	fi
}

trap _sigkill SIGKILL
######################
set -eu

if [[ -z ${1:-} ]]; then
  echo "Usage: midpoint.sh start|stop|generate|container|init-native [other args...]"
  exit 1
fi

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd -P)
BASE_DIR=$(cd "${SCRIPT_DIR}/.." && pwd -P)

# setting default to MIDPOINT_HOME if not set
: "${MIDPOINT_HOME:="${BASE_DIR}/var"}"

if [ "${1}" = "init-native" ]; then
  if [ "${MP_CHECK:-}" != "" ]; then
    touch "${MP_CHECK}"
  fi
  echo "Initializing native structure of the db..."
  if [ "${MP_INIT_DB:-}" = "" -a "${MP_INIT_DB_CONCAT:-}" = "" ]; then
    echo "MP_INIT_DB variable with target for DB init files was not set - skipping db init file processing..." >&2
  else
    if [ "${MP_INIT_DB_CONCAT:-}" = "" ]; then
      if [ -e "${BASE_DIR}/doc/config/sql/native" ]; then
        find "${BASE_DIR}/doc/config/sql/native/" -type f -name "postgres*.sql" ! -name "postgres-upgrade.sql" -exec cp \{\} "${MP_INIT_DB}/" \;
      else
        echo "Location with sql init structure (source) have not been found..." >&2
        exit 1
      fi
    else
      if [ -e "${BASE_DIR}/doc/config/sql/native" ]; then

        [ -e "${BASE_DIR}/doc/config/sql/native/postgres.sql" ] &&
          cp "${BASE_DIR}/doc/config/sql/native/postgres.sql" "${MP_INIT_DB_CONCAT}"
        [ -e "${BASE_DIR}/doc/config/sql/native/postgres-audit.sql" ] &&
          cat "${BASE_DIR}/doc/config/sql/native/postgres-audit.sql" >>"${MP_INIT_DB_CONCAT}"
        [ -e "${BASE_DIR}/doc/config/sql/native/postgres-quartz.sql" ] &&
          cat "${BASE_DIR}/doc/config/sql/native/postgres-quartz.sql" >>"${MP_INIT_DB_CONCAT}"
      else
        echo "Location with sql init structure (source) have not been found..." >&2
        exit 1
      fi

    fi
  fi
  if [ "${MP_INIT_CFG:-}" = "" ]; then
    echo "MP_INIT_CFG variable with target for config.xml was not set - skipping config.xml file processing..."
  else
    if [ -e "${BASE_DIR}/doc/config/config-native.xml" ]; then
      if [ ! -e ${MP_INIT_CFG}/config.xml ]; then
        cp "${BASE_DIR}/doc/config/config-native.xml" ${MP_INIT_CFG}/config.xml
      else
        echo "File already exists... Skipping"
      fi
    else
      echo "Source location with config.xml have not been found..." >&2
      exit 1
    fi
  fi
  if [ "${MP_DB_PW:-}" != "" ]; then
    if [ ! -e ${MP_DB_PW} ]; then
      base64 /dev/urandom | tr -d -c [:alnum:] | head -c 24 2>/dev/null >${MP_DB_PW}
      echo "DB Password generated..."
    else
      echo "Destination file with DB Password already exists... Skipping" >&2
    fi
  fi
  if [ "${MP_PW:-}" != "" ]; then
    if [ ! -e ${MP_PW} ]; then
      base64 /dev/urandom | tr -d -c [:alnum:] | head -c 24 2>/dev/null >${MP_PW}
      echo "MP Password generated..."
    else
      echo "Destination file with the generated MP Password already exists... Skipping" >&2
    fi
  fi
  if [ "${MP_PW_DEF:-}" != "" ]; then
    if [ ! -e ${MP_PW_DEF} ]; then
      echo -n "changeit" >${MP_PW_DEF}
      echo "Default MP Password stored..."
    else
      echo "Destination file with the default MP Password already exists... Skipping" >&2
    fi
  fi
  if [ "${MP_CERT:-}" != "" ]; then
    if [ "${MP_KEYSTORE:-}" = "" ]; then
      echo "Keystore path has not been set..." >&2
      exit 1
    fi
    keystorepw="${MP_PW:-${MP_PW_DEF:-}}"
    if [ "${keystorepw}" = "" ]; then
      echo "Keystore password file path has not been set..." >&2
      exit 1
    fi
    if [ ! -e "${MP_KEYSTORE:-}" ]; then
      keytool -genseckey -alias default -keystore "${MP_KEYSTORE}" -storetype jceks -keypass midpoint -storepass:file ${keystorepw} -keyalg AES -keysize 128 2>/dev/null
    fi
    echo "${MP_CERT}" >"${MP_KEYSTORE}_"
    grep -n " CERTIFICATE-----" ${MP_KEYSTORE}_ | cut -d : -f 1 | paste - - | sed "s/\([0-9]*\)[^0-9]*\([0-9]*\)/\1,\2p/" | while read certRange; do
      sed -n "${certRange}" "${MP_KEYSTORE}_" >"${MP_KEYSTORE}__"
      echo "- - - - -" >&2
      subject="$(keytool -printcert -file "${MP_KEYSTORE}__" 2>/dev/null | grep "Owner: " | sed "s/[^:]*: \(.*\)/\1/")"
      echo "${subject}"
      keytool -printcert -file "${MP_KEYSTORE}__" 2>/dev/null |
        sed -n "/Certificate fingerprints:/,/^[A-Z]/p" |
        grep -v "^[A-Z]" |
        sed "s/[[:space:]][^:]*: //" | while read line; do
          touch "${MP_KEYSTORE}__.exists"
          if $(keytool -list -keystore ${MP_KEYSTORE} -storetype jceks -storepass:file ${keystorepw} 2>/dev/null | grep -q " ${line}$"); then
            echo "${line} .:. Found" >&2
            touch "${MP_KEYSTORE}__.found"
          else
            echo "${line} .:. Not Found" >&2
          fi
        done
      if [ -e "${MP_KEYSTORE}__.exists" ]; then
        rm "${MP_KEYSTORE}__.exists"
        if [ -e "${MP_KEYSTORE}__.found" ]; then
          echo "Fingerprint found in the certstore - certificate exists..." >&2
          rm "${MP_KEYSTORE}__.found"
        else
          echo "Adding cert to certstore..." >&2
          keytool -importcert -noprompt -trustcacerts -alias "${subject}" -file "${MP_KEYSTORE}__" -keystore "${MP_KEYSTORE}" -storetype jceks -storepass:file "${keystorepw}" 2>/dev/null
          sleep 1
        fi
      else
        echo "Certificate did not found in the file..." >&2
      fi
      [ -e "${MP_KEYSTORE}__" ] && rm "${MP_KEYSTORE}__"
    done
    [ -e "${MP_KEYSTORE}_" ] && rm -f "${MP_KEYSTORE}_"
    echo "- - - - -"
    keytool -list -keystore ${MP_KEYSTORE} -storetype jceks -storepass:file ${keystorepw} 2>/dev/null
    echo "- - - - -"
  fi
  echo "All requested operation has been done - init files are ready on requested location..."
  if [ "${MP_CHECK:-}" != "" ]; then
    rm "${MP_CHECK}"
  fi
  if [ "${MP_INIT_LOOP:-}" != "" ]; then
    echo "Looping to keep container UP"
    tail -f /dev/null
  fi
  exit 0
fi
# end of init-native

mkdir -p "${MIDPOINT_HOME}/log"

# To prevent error for setenv.sh processing (set -u).
JAVA_OPTS="${JAVA_OPTS:- }"

# Apply bin/setenv.sh if it exists. This setenv.sh does not depend on MIDPOINT_HOME.
# The script can either append or overwrite JAVA_OPTS, e.g. to set -Dmidpoint.nodeId.
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

#############################
# Originally Docker related #
#############################

################
# entry point for source of the files to copy into midpoint home directory before the application start
################

if [ "${MP_ENTRY_POINT:-}" != "" ]; then # /opt/midpoint-dirs-docker-entrypoint
  if [ -e "${MP_ENTRY_POINT}" ]; then
    echo "Processing ${MP_ENTRY_POINT} directory..."
    for i in $(find "${MP_ENTRY_POINT}" -mindepth 1 -maxdepth 1 -type d); do
      l_name="$(basename "${i}")"
      [ ! -e "${MIDPOINT_HOME}/${l_name}" ] && mkdir -p "${MIDPOINT_HOME}/${l_name}"
      for s in $(find "${i}" -mindepth 1 -maxdepth 1 -type f -follow -exec basename \{\} \;); do
        if [ ! -e "${MIDPOINT_HOME}/${l_name}/${s}" -a ! -e "${MIDPOINT_HOME}/${l_name}/${s}.done" ]; then
          echo "COPY ${i}/${s} => ${MIDPOINT_HOME}/${l_name}/${s}"
          cp "${i}/${s}" "${MIDPOINT_HOME}/${l_name}/${s}"
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

###### Backward compatibility for ENV variables ####

if [ "${MP_NO_ENV_COMPAT:-}" != "1" ]; then
  [ "${REPO_PORT:-}" != "" ] && db_port=${REPO_PORT}
  if [ "${REPO_DATABASE_TYPE:-}" != "" ]; then
    export ${ENV_MAP_PREFIX}midpoint_repository_database="${REPO_DATABASE_TYPE}"
    [ "${db_port:-}" == "default" ] && db_port=""
    case ${REPO_DATABASE_TYPE} in
    h2)
      [ "${db_port:-}" == "" ] && db_port=5437
      db_prefix="jdbc:h2:tcp://"
      db_path="/${REPO_DATABASE:-midpoint}"
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
      if [ "${db_port:-}" == "" -a "${REPO_URL:-}" == "" ]; then
        echo "~~~~~ please supply JDBC port for your repository ~~~~~"
        exit 1
      fi
      ;;
    esac
    [ "${REPO_URL:-}" = "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_jdbcUrl="${db_prefix}${REPO_HOST:-localhost}:${db_port}${db_path}"
  fi

  [ "${REPO_URL:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_jdbcUrl="${REPO_URL}"
  [ "${REPO_USER:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_jdbcUsername="${REPO_USER}"
  [ "${REPO_PASSWORD_FILE:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_jdbcPassword_FILE="${REPO_PASSWORD_FILE}"
  [ "${REPO_MISSING_SCHEMA_ACTION:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_missingSchemaAction="${REPO_MISSING_SCHEMA_ACTION}"
  [ "${REPO_UPGRADEABLE_SCHEMA_ACTION:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_upgradeableSchemaAction="${REPO_UPGRADEABLE_SCHEMA_ACTION}"
  [ "${REPO_SCHEMA_VARIANT:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_schemaVariant="${REPO_SCHEMA_VARIANT}"
  [ "${REPO_SCHEMA_VERSION_IF_MISSING:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_repository_schemaVersionIfMissing="${REPO_SCHEMA_VERSION_IF_MISSING}"

  [ "${MP_KEYSTORE_PASSWORD_FILE:-}" != "" ] && export ${ENV_MAP_PREFIX}midpoint_keystore_keyStorePassword_FILE="${MP_KEYSTORE_PASSWORD_FILE}"
fi

#############################

###### ENV Variables mapping ######

JAVA_OPTS=" ${JAVA_OPTS:-}"

while read line; do
  _to_process="${line:${#ENV_MAP_PREFIX}}"
  _key="$(echo -n "${_to_process}" | cut -d = -f 1 | sed "s/_/./g")"
  _val="$(echo -n "${_to_process}" | cut -d = -f 2-)"

  ### exception for *_FILE key name ###
  [ "${_key: -5}" = ".FILE" ] && _key="${_key::$((${#_key} - 5))}_FILE"
  ###

  if [ "${_key: -7}" = "assword" ]
  then
    echo "Processing variable (MAP) ... ${_key} .:. *****" >&2
  else
    echo "Processing variable (MAP) ... ${_key} .:. ${_val}" >&2
  fi

  if [ "${_key:0:1}" = "." ]; then
    JAVA_OPTS="${JAVA_OPTS:-} -D${_key:1}=\"${_val}\""
  else
    JAVA_OPTS="$(echo -n "${JAVA_OPTS:-}" | sed "s/ -D${_key}=\"[^\"]*\"//g;s/ -D${_key}=[^[:space:]]*//g") -D${_key}=\"${_val}\""
  fi
done < <(env | grep "^${ENV_MAP_PREFIX}")

while read line; do
  _to_process="${line:${#ENV_UNMAP_PREFIX}}"
  _key="$(echo -n "${_to_process}" | cut -d = -f 1 | sed "s/_/./g")"
  _val="$(echo -n "${_to_process}" | cut -d = -f 2-)"

  ### exception for *_FILE key name ###
  [ "${_key: -5}" = ".FILE" ] && _key="${_key::$((${#_key} - 5))}_FILE"
  ###

  if [ "${_key: -7}" = "assword" ]
  then
    echo "Processing variable (UNMAP) ... ${_key} .:. *****" >&2
  else
    echo "Processing variable (UNMAP) ... ${_key} .:. ${_val}" >&2
  fi

  JAVA_OPTS="$(echo -n "${JAVA_OPTS:-}" | sed "s/ -D${_key}=\"[^\"]*\"//g;s/ -D${_key}=[^[:space:]]*//g")"
done < <(env | grep "^${ENV_UNMAP_PREFIX}")

###################################

# Check for the default JAVA_OPTS values. In case the specific key is already set the value is kept untouched.
# To prevent Xms to be set pass the --Xms to JAVA_OPTS (double dash).
# To prevent Xmx to be set pass the --Xmx to JAVA_OPTS (double dash).

if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Xmx[0-9]"); then
  if $(echo "${JAVA_OPTS:-}" | grep -q "\-\-Xmx"); then
    JAVA_OPTS="$(echo "${JAVA_OPTS:-}" | sed "s/\-\-Xmx//")"
  else
    JAVA_OPTS="-Xmx${JAVA_def_Xmx} ${JAVA_OPTS:-}"
  fi
fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Xms[0-9]"); then
  if $(echo "${JAVA_OPTS:-}" | grep -q "\-\-Xms"); then
    JAVA_OPTS="$(echo "${JAVA_OPTS:-}" | sed "s/\-\-Xms//")"
  else
    JAVA_OPTS="-Xms${JAVA_def_Xms} ${JAVA_OPTS:-}"
  fi
fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Dpython.cachedir="); then JAVA_OPTS="${JAVA_OPTS:-} -Dpython.cachedir=\"${MIDPOINT_HOME}/${JAVA_def_python_cachedir}\""; fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djavax.net.ssl.trustStore="); then JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStore=\"${MIDPOINT_HOME}/${JAVA_def_trustStore}\""; fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djavax.net.ssl.trustStoreType="); then JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStoreType=${JAVA_def_trustStoreType}"; fi

if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Dmidpoint.home="); then JAVA_OPTS="${JAVA_OPTS:-} -Dmidpoint.home=\"${MIDPOINT_HOME}\""; fi

# TODO probably useless from Tomcat 7 and before history, remove and check LDAP/ConnId logging
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djava.util.logging.manager="); then JAVA_OPTS="-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager ${JAVA_OPTS:-}"; fi

# clean up white spaces in case of key/value removal from the original JAVA_OPTS parameter set
JAVA_OPTS="$(echo "${JAVA_OPTS:-}" | tr -s [[:space:]] " " | sed "s/^[[:space:]]//;s/[[:space:]]$//")"

: "${BOOT_OUT:="${MIDPOINT_HOME}/log/midpoint.out"}"
: "${PID_FILE:="${MIDPOINT_HOME}/log/midpoint.pid"}"

if [[ ! -f "${BASE_DIR}/lib/midpoint.jar" ]]; then
  echo "ERROR: midpoint.jar is not in /lib directory"
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

if [ "${MP_GEN_SYSTEMD:-}" != "" -o "${1}" = "generate" ]; then
  JAVA_OPTS="$(echo ${JAVA_OPTS:-} | sed "s/-genSystemd//" | tr -s [[:space:]] " " | sed "s/^[[:space:]]//;s/[[:space:]]$//")"
  echo "Place following content to /etc/systemd/system/midpoint.service file" >&2
  [ "${MP_GEN_SYSTEMD:-}" != "" ] && echo "MP_GEN_SYSTEMD=1 ${0} ${@} | sudo tee /etc/systemd/system/midpoint.service" >&2
  [ "${1:-}" = "generate" ] && echo "${0} ${@} | sudo tee /etc/systemd/system/midpoint.service" >&2
  [ "${1}" = "generate" ] && shift
  echo >&2
  userToUse="${MP_USER:-$(whoami)}"
  output="[Unit]
Description=MidPoint Standalone Service
###Requires=postgresql.service
###After=postgresql.service

[Service]
User=${userToUse:-midpoint}
WorkingDirectory=${BASE_DIR}
ExecStart=\"${_RUNJAVA}\" ${JAVA_OPTS} -jar \"${BASE_DIR}/lib/midpoint.jar\" \"$@\"
SuccessExitStatus=143
###TimeoutStopSec=120s

[Install]
WantedBy=multi-user.target
"
  echo -n "${output}" | sed "s/\" \"\"/\"/g" | grep -v "^#"
  echo >&2
  echo -e "sudo systemctl daemon-reload\nsudo systemctl enable midpoint\nsudo systemctl start midpoint" >&2
  exit 0
fi

#############################################################

if [ "${MP_CHECK:-}" != "" ]; then
  while [ -e "${MP_CHECK}" ]; do
    sleep 1
  done
fi

if [[ "$1" == "container" ]]; then
  if ! which "${_RUNJAVA}" &>/dev/null; then
    echo "${_RUNJAVA} not found (or not executable). Start aborted."
    exit 1
  fi

  shift

  echo "Starting midPoint..."
  echo "MIDPOINT_HOME=${MIDPOINT_HOME}"

  echo -e "\nStarting at $(date)\nMIDPOINT_HOME=${MIDPOINT_HOME}\nJava binary: ${_RUNJAVA}\nJava version:"
  "${_RUNJAVA}" -version 2>&1

  # shellcheck disable=SC2086
  if [ "${1:-}" = "" ]; then
    eval "\"${_RUNJAVA}\"" \
      ${JAVA_OPTS} \
      -jar "\"${BASE_DIR}/lib/midpoint.jar\"" "&" 2>&1
  else
    eval "\"${_RUNJAVA}\"" \
      ${JAVA_OPTS} \
      -jar "\"${BASE_DIR}/lib/midpoint.jar\"" \
      \"\$@\" "&" 2>&1
  fi

  if [[ -n "${PID_FILE}" ]]; then
    echo $! >"${PID_FILE}"
    wait $(cat ${PID_FILE})
  else
    wait $!
  fi

  echo "The midPoint process has been terminated..." >&2
fi
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
    -jar "\"${BASE_DIR}/lib/midpoint.jar\"" \
    \"\$@\" \
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
