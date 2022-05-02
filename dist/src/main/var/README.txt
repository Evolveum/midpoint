This is the default "midPoint home" directory.
MidPoint home contains initial configuration, optional libraries and logs.

SPECIFYING MIDPOINT HOME LOCATION

MidPoint home location can be changed with MIDPOINT_HOME environment variable
before using script in the bin directory.
This can also be set in bin/setenv.sh (or BAT) along with any other variables
(the file needs to be created if required).

If you use custom MIDPOINT_HOME, this directory can be ignored or even removed.
You'll miss this README though.

INITIAL CONFIGURATION FILE

Initial configuration file, config.xml, is also placed here.
It is automatically created if missing during start (H2 is used in that case).
Example of config.xml for Native PG repository is provided in doc/config.
You can copy it here, don't forget to rename it to config.xml.

OPTIONAL/ADDITIONAL LIBS

Place any additional libs/JARs (like JDBC drivers) into $MIDPOINT_HOME/lib.
DO NOT place them into lib in the midPoint distribution root, in other words,
do not put anything next to midpoint.jar, that's the wrong place.

MORE INFORMATION

For more information about MIDPOINT_HOME, see:
https://docs.evolveum.com/midpoint/reference/deployment/midpoint-home-directory/
