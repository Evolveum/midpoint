rem Sets the basedir to the directory where libraries are located
set BASEDIR=%~dp0

java -classpath "%BASEDIR%*;%BASEDIR%lib\*" com.evolveum.midpoint.testing.model.client.sample.Upload %*
