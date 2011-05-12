/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.integration.identityconnector;

import com.evolveum.midpoint.test.util.DerbyManager;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;

/**
 *
 * @author elek
 */
public class ConnectorFactory extends DerbyManager {

    public ConnectorFacade createTestDbConnector() throws Exception {

        URL bundle = getClass().getResource("/META-INF/bundles/org.identityconnectors.databasetable-1.1.4958.jar");
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();

        ConnectorInfoManager manager = factory.getLocalManager(bundle);

        ConnectorKey ffKey = new ConnectorKey("org.identityconnectors.databasetable",
                "1.1.4958",
                "org.identityconnectors.databasetable.DatabaseTableConnector");
        ConnectorInfo ffConInfo = manager.findConnectorInfo(ffKey);

        APIConfiguration ffConfig = ffConInfo.createDefaultAPIConfiguration();

        ConfigurationProperties ffConfigProps = ffConfig.getConfigurationProperties();

        ffConfigProps.setPropertyValue("jdbcDriver", "org.apache.derby.jdbc.EmbeddedDriver");
        ffConfigProps.setPropertyValue("jdbcUrlTemplate", conn);
        ffConfigProps.setPropertyValue("table", "account");
        ffConfigProps.setPropertyValue("keyColumn", "id");
        ffConfigProps.setPropertyValue("passwordColumn", "password");
        ffConfigProps.setPropertyValue("changeLogColumn", "changelog");

        ConnectorFacade ffConnector = ConnectorFacadeFactory.getInstance().newInstance(ffConfig);

        System.out.println(ffConnector.getClass());

        return ffConnector;
    }

    public ConnectorFacade createTestICFConnector() throws Exception {
        System.out.println(new File(".").getAbsolutePath());
        File bundleDirectory = new File("src/test/bundles");

        URL flatfileUrl = IOUtil.makeURL(bundleDirectory, "org.identityconnectors.flatfile-1.0.x.jar");

        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager manager = factory.getLocalManager(flatfileUrl);

        ConnectorKey ffKey = new ConnectorKey("org.identityconnectors.flatfile",
                "1.0.x",
                "org.identityconnectors.flatfile.FlatFileConnector");
        ConnectorInfo ffConInfo = manager.findConnectorInfo(ffKey);

        APIConfiguration ffConfig = ffConInfo.createDefaultAPIConfiguration();

        ConfigurationProperties ffConfigProps = ffConfig.getConfigurationProperties();

        ffConfigProps.setPropertyValue("uniqueAttributeName", "uid");
        ffConfigProps.setPropertyValue("file", new File("../src/test/resources/data.txt"));

        ConnectorFacade ffConnector = ConnectorFacadeFactory.getInstance().newInstance(ffConfig);
        return ffConnector;
    }
}
