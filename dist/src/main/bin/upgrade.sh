#!/usr/bin/env bash
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
# Parts of this file Copyright (c) 2018 Evolveum
#

if [ $# -eq 0 ]; then
    echo "No arguments supplied"
    exit 1
fi

SCRIPT_PATH=$(cd $(dirname "$0") && pwd -P)/

# resolve links - $0 may be a softlink
PRG="$0"

PRGDIR=`dirname "$PRG"`

MIDPOINT_EXISTING_FOLDER="$1"

if [ ! -d "$MIDPOINT_EXISTING_FOLDER" ]; then
    echo "'$MIDPOINT_EXISTING_FOLDER' is not a folder"
    exit 1
fi

# MOVE TO *37
#drwxr-xr-x  13 bin
#drwxr-xr-x   4 lib

mv $MIDPOINT_EXISTING_FOLDER/bin $MIDPOINT_EXISTING_FOLDER/bin.backup
mv $MIDPOINT_EXISTING_FOLDER/lib $MIDPOINT_EXISTING_FOLDER/lib.backup

# DELETE
#drwxr-xr-x   6 doc
rm -rf $MIDPOINT_EXISTING_FOLDER/doc

# DON'T TOUCH
#drwxr-xr-x  17 var

# REPLACE
#-rw-r--r--   1 INSTALL
#-rw-r--r--   1 LICENSE
#-rw-r--r--   1 NEWS
#-rw-r--r--   1 NOTICE
#-rw-r--r--   1 README
#-rw-r--r--   1 RELEASE-NOTES

# COPY everything from 3.8

cp -r "$PRGDIR/.."/* $MIDPOINT_EXISTING_FOLDER/