#!/usr/bin/env bash
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.
#
# Parts of this file Copyright (c) 2018 Evolveum and contributors
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
