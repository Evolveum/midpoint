#!/bin/bash

#Test enviroment
CLASSPATH="../../gui/admin-gui/target/idm/WEB-INF/lib/schema-2.0-SNAPSHOT.jar:./lib/groovy-ldap.jar"
groovy -cp "$CLASSPATH" -d ./src/test/groovy/check-enviroment.groovy
