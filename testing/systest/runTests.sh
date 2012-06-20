#!/bin/bash

#Test enviroment
CLASSPATH="../../infra/schema/target/schema-2.0.jar:../../gui/admin-gui/target/idm/WEB-INF/lib/xml-resolver-1.2.jar:./lib/groovy-ldap.jar:../../gui/admin-gui/target/idm/WEB-INF/lib/commons-io-2.0.1.jar:../../gui/admin-gui/target/idm/WEB-INF/lib/cxf-common-utilities-2.4.3.jar:../../gui/admin-gui/target/idm/WEB-INF/lib/commons-lang-2.6.jar"
groovy -cp "$CLASSPATH" -d ./src/test/groovy/check-enviroment.groovy
groovy -cp "$CLASSPATH" -d ./src/test/groovy/import-resources.groovy
