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

JAVA_def_trustStore="keystore.jceks"
JAVA_def_trustStoreType="jceks"

if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djavax.net.ssl.trustStore="); then JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStore=\"${MIDPOINT_HOME}/${JAVA_def_trustStore}\""; fi
if $(echo "${JAVA_OPTS:-}" | grep -v -q "\-Djavax.net.ssl.trustStoreType="); then JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStoreType=${JAVA_def_trustStoreType}"; fi

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
  eval "${_RUNJAVA}" ${JAVA_OPTS} "-Dloader.path=${JDBC_DRIVER}" -jar "${BASE_DIR}/lib/ninja.jar" -m "${MIDPOINT_HOME}" "$@"
else
  eval "${_RUNJAVA}" ${JAVA_OPTS} -jar "${BASE_DIR}/lib/ninja.jar" -m "${MIDPOINT_HOME}" "$@"
fi
