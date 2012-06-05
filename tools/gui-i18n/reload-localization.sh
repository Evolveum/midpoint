#sample usage for localization tool

./java -jar target/gui-i18n.jar com.evolveum.midpoint.tools.gui.Main \
    -t ./tools/gui-i18n/sample \
    -b /home/lazyman/Work/evolveum/midpoint/trunk/gui/admin-gui/src/main \
    -l sk_SK \
    -r /java/com/evolveum/midpoint/web

#maven configuration, not used now
#<plugin>
#    <groupId>org.codehaus.mojo</groupId>
#    <artifactId>exec-maven-plugin</artifactId>
#    <version>1.2.1</version>
#    <executions>
#        <execution>
#            <goals>
#                <goal>java</goal>
#            </goals>
#        </execution>
#    </executions>
#    <configuration>
#        <executable>java</executable>
#        <mainClass>com.evolveum.midpoint.tools.gui.Main</mainClass>
#        <classpath>
#            <dependency>commons-io:commons-io</dependency>
#            <dependency>commons-lang:commons-lang</dependency>
#        </classpath>
#        <workingDirectory>/tmp</workingDirectory>
#        <arguments>
#            <argument>-t ./src/main/resources</argument>
#            <argument>-b ./src/main/java</argument>
#            <argument>-r /com/evolveum/midpoint/web</argument>
#            <argument>-l sk_SK</argument>
#            <argument>-l en_US</argument>
#        </arguments>
#    </configuration>
#    <dependencies>
#        <dependency>
#            <groupId>com.evolveum.midpoint</groupId>
#            <artifactId>gui-i18n</artifactId>
#            <version>2.0-SNAPSHOT</version>
#        </dependency>
#    </dependencies>
#</plugin>