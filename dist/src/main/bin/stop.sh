#!/usr/bin/env bash
#
# Copyright (C) 2017-2020 Evolveum and contributors
#
# Licensed under the EUPL-1.2 or later.
#

set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd -P)
EXECUTABLE=midpoint.sh

if [[ ! -x "${SCRIPT_DIR}/${EXECUTABLE}" ]]; then
  echo "Cannot find ${SCRIPT_DIR}/${EXECUTABLE}"
  echo "The file is absent or does not have execute permission"
  echo "This file is needed to run this program"
  exit 1
fi

exec "${SCRIPT_DIR}/${EXECUTABLE}" stop "$@"
