#!/usr/bin/env bash
# Copyright (C) 2017-2023 Evolveum and contributors
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.

set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd -P)
BASE_DIR=$(cd "${SCRIPT_DIR}/.." && pwd -P)

# setting default to MIDPOINT_HOME if not set
: "${MIDPOINT_HOME:="${BASE_DIR}/var"}"

if [[ ! -f "${BASE_DIR}/lib/ninja.jar" ]]; then
  echo "ERROR: ninja.jar not found in ${BASE_DIR}/lib directory" >&2
  exit 1
fi

# Set UMASK unless it has been overridden
: "${UMASK:="0027"}"
umask ${UMASK}

# Support for JAVA_OPTS + SET/UNSET environment variables, for better Docker support
# To prevent error for setenv.sh processing (set -u).
JAVA_OPTS="${JAVA_OPTS:- }"

JAVA_def_Xms="1g"
JAVA_def_Xmx="2g"
ENV_MAP_PREFIX="MP_SET_"
ENV_UNMAP_PREFIX="MP_UNSET_"

# Apply bin/setenv.sh if it exists. This setenv.sh does not depend on MIDPOINT_HOME.
# The script can either append or overwrite JAVA_OPTS, e.g. to set -Dmidpoint.nodeId.
if [[ -r "${SCRIPT_DIR}/setenv.sh" ]]; then
  echo "Applying setenv.sh from ${SCRIPT_DIR} directory." >&2
  # shellcheck disable=SC1091
  . "${SCRIPT_DIR}/setenv.sh"
fi

# Apply $MIDPOINT_HOME/setenv.sh if it exists. This is flexible and related to chosen MIDPOINT_HOME.
if [[ -r "${MIDPOINT_HOME}/setenv.sh" ]]; then
  echo "Applying setenv.sh from ${MIDPOINT_HOME} directory." >&2
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
    echo "Processing ${MP_ENTRY_POINT} directory..." >&2
    for i in $(find "${MP_ENTRY_POINT}" -mindepth 1 -maxdepth 1 -type d); do
      l_name="$(basename "${i}")"
      [ ! -e "${MIDPOINT_HOME}/${l_name}" ] && mkdir -p "${MIDPOINT_HOME}/${l_name}"
      for s in $(find "${i}" -mindepth 1 -maxdepth 1 -type f -follow -exec basename \{\} \;); do
        if [ ! -e "${MIDPOINT_HOME}/${l_name}/${s}" -a ! -e "${MIDPOINT_HOME}/${l_name}/${s}.done" ]; then
          echo "COPY ${i}/${s} => ${MIDPOINT_HOME}/${l_name}/${s}" >&2
          cp "${i}/${s}" "${MIDPOINT_HOME}/${l_name}/${s}"
        else
          echo "SKIP: ${i}/${s}" >&2
        fi
      done
    done
    echo "- - - - - - - - - - - - - - - - - - - - -" >&2
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
        echo "~~~~~ please supply JDBC port for your repository ~~~~~" >&2
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

if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Dmidpoint.home="); then JAVA_OPTS="${JAVA_OPTS:-} -Dmidpoint.home=\"${MIDPOINT_HOME}\""; fi

# clean up white spaces in case of key/value removal from the original JAVA_OPTS parameter set
JAVA_OPTS="$(echo "${JAVA_OPTS:-}" | tr -s [[:space:]] " " | sed "s/^[[:space:]]//;s/[[:space:]]$//")"

# can't use -v here because of bash 3.2 support
if [[ -z "${JAVA_HOME:-}" ]]; then
  _RUNJAVA=java
else
  _RUNJAVA="${JAVA_HOME}/bin/java"
fi

while getopts ":j:" opt; do
  # shellcheck disable=SC2220
  case $opt in
  j)
    JDBC_DRIVER=$OPTARG
    ;;
  esac
done

# Technically we could do one exec, but then we can't quote -Dloader.path argument, because it
# would be empty and considered a class name by the "java -jar" command.
if [ -n "${JDBC_DRIVER:-}" ]; then
  echo "Using JDBC driver path: ${JDBC_DRIVER}" >&2
  eval "${_RUNJAVA}" ${JAVA_OPTS} "-Dloader.path=${JDBC_DRIVER}" -jar "${BASE_DIR}/lib/ninja.jar" -m "${MIDPOINT_HOME}" \"\$@\"
else
  eval "${_RUNJAVA}" ${JAVA_OPTS} -jar "${BASE_DIR}/lib/ninja.jar" -m "${MIDPOINT_HOME}" \"\$@\"
fi
