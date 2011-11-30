#!/bin/bash

export JAVA_HOME=/opt/java/
export PATH=/opt/java/bin:$PATH

export LDAP_INST=~mamut/install/OpenDJ-2.4.4.zip


/opt/ldap1/OpenDJ-2.4.4/bin/stop-ds
/opt/ldap2/OpenDJ-2.4.4/bin/stop-ds
/opt/ldap1/OpenDJ-2.4.4/bin/status -D "cn=Directory Manager" -j rootUserPasswordFile -s | grep "Server Run Status" | grep Stopped
/opt/ldap2/OpenDJ-2.4.4/bin/status -D "cn=Directory Manager" -j rootUserPasswordFile -s | grep "Server Run Status" | grep Stopped
rm -rf /opt/ldap1/
rm -rf /opt/ldap2/

unzip $LDAP_INST -d /opt/ldap1/
unzip $LDAP_INST -d /opt/ldap2/
/opt/ldap1/OpenDJ-2.4.4/setup -i --no-prompt -D "cn=Directory Manager" -a -p 1389 --adminConnectorPort 14444 -j rootUserPasswordFile -h localhost -b "dc=example,dc=com" -b "dc=systest,dc=com"
/opt/ldap2/OpenDJ-2.4.4/setup -i --no-prompt -D "cn=Directory Manager" -a -p 2389 --adminConnectorPort 24444 -j rootUserPasswordFile -h localhost -b "dc=systest,dc=com"
/opt/ldap1/OpenDJ-2.4.4/bin/status -D "cn=Directory Manager" -j rootUserPasswordFile -s | grep "Server Run Status" | grep Started
/opt/ldap2/OpenDJ-2.4.4/bin/status -D "cn=Directory Manager" -j rootUserPasswordFile -s | grep "Server Run Status" | grep Started

/opt/ldap1/OpenDJ-2.4.4/bin/dsreplication enable \
	--host1 localhost \
	--port1 14444 \
	--bindDN1 "cn=directory manager" \
	--bindPassword1 `cat rootUserPasswordFile` \
	--replicationPort1 18989 \
	--host2 localhost \
	--port2 24444 \
	--bindDN2 "cn=directory manager" \
	--bindPassword2 `cat rootUserPasswordFile` \
	--replicationPort2 28989 \
 	--adminUID admin --adminPassword password \
	--baseDN "dc=systest,dc=com" -X -n


/opt/ldap1/OpenDJ-2.4.4/bin/dsreplication initialize --baseDN "dc=systest,dc=com" \
  --adminUID admin --adminPassword password \
  --hostSource localhost --portSource 14444 \
  --hostDestination localhost --portDestination 24444 -X -n

/opt/ldap1/OpenDJ-2.4.4/bin/dsreplication status --baseDN "dc=systest,dc=com" \
  --adminUID admin --adminPassword password -X -n | grep -v "\[.\]"

cd /opt/
/opt/ldap1/OpenDJ-2.4.4/bin/stop-ds
/opt/ldap2/OpenDJ-2.4.4/bin/stop-ds
zip -r ldaps.zip ./ldap1 ./ldap2/ 

rm /tmp/opends-setup*
rm /tmp/opends-replication*


