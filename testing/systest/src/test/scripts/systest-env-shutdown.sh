#!/bin/bash

# shutdown tomcat
/opt/tomcat/bin/shutdown.sh

#shutdown LDAP
/opt/ldap1/OpenDJ-2.4.4/bin/stop-ds
/opt/ldap2/OpenDJ-2.4.4/bin/stop-ds
