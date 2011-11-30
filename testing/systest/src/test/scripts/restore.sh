#!/bin/bash

# shutdown and restore tomcat
/opt/tomcat/bin/shutdown.sh
rm -rf /opt/tomcat/webapps/idm*
rm -rf /opt/tomcat/temp/*  /opt/tomcat/work/* /opt/tomcat/logs/*


#shutdown and restore LDAP
/opt/ldap1/OpenDJ-2.4.4/bin/stop-ds
/opt/ldap2/OpenDJ-2.4.4/bin/stop-ds
rm -rf /opt/ldap1/
rm -rf /opt/ldap2/

(cd /opt; unzip ldaps.zip)
/opt/ldap1/OpenDJ-2.4.4/bin/start-ds | grep "started successfully"
/opt/ldap2/OpenDJ-2.4.4/bin/start-ds | grep "started successfully"
/opt/ldap1/OpenDJ-2.4.4/bin/dsreplication status --baseDN "dc=systest,dc=com" \
  --adminUID admin --adminPassword password -X -n | grep -v "\[.\]"

#remove midpoint home
rm -rf /opt/midpoint/*
