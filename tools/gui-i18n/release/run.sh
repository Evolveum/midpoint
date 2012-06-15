#!/bin/bash

if [ $# != 1 ]; then
    echo "Locale was not defined. Example parameter en_US or sk_SK."
	exit
fi

PROJECT_NAME="admin-gui-${1//_/-}"
echo "Updating project with locale '$1', project '$PROJECT_NAME'."

PROJECT_DIR="../../../gui/$PROJECT_NAME"

if [ ! -d $PROJECT_DIR ]; then
	echo "Project file '$PROJECT_DIR' does not exist."
	exit
fi 

RESOURCE_DIR="$PROJECT_DIR/src/main/resources"
if [ ! -d $RESOURCE_DIR ]; then
	echo "Project file '$RESOURCE_DIR' does not exist."
	exit
fi

java -jar gui-i18n.jar -t $RESOURCE_DIR -b ../../../gui/admin-gui/src/main -l $1 -r java/com/evolveum/midpoint/web -db
