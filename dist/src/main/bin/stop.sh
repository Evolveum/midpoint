#!/bin/sh
# Copyright (C) 2017-2020 Evolveum and contributors
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.

set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd -P)
EXECUTABLE=midpoint.sh

if [ ! -x "${SCRIPT_DIR}/${EXECUTABLE}" ]; then
  echo "Cannot find ${SCRIPT_DIR}/${EXECUTABLE}"
  echo "The file is absent or does not have execute permission"
  echo "This file is needed to run this program"
  exit 1
fi

exec "${SCRIPT_DIR}/${EXECUTABLE}" stop "$@"
