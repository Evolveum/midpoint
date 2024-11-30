/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import static org.testng.Assert.*;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public class TestConfigurationLoad extends AbstractUnitTest {

    @Test
    public void test010SimpleConfigTest() {
        // Originally null was used to default to midpoint under homedir, but this is not good.
        // Test fails if developer has something else already there (different repo factory).
        // It also can't be cleaned properly - you don't want to delete it if it existed.
        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, "target/midPointHomeSimple/");
        logger.info("midpoint.home => {}", System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY));

        StartupConfiguration sc = new StartupConfiguration();
        sc.init();
        Configuration c = sc.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION);
        assertEquals(c.getString("type"), "native");
        logger.info("{}", sc);

        Iterator<String> i = c.getKeys();
        while (i.hasNext()) {
            String key = i.next();
            logger.info("  " + key + " = " + c.getString(key));
        }

        String type = c.getString("type");
        if ("native".equals(type)) {
            return;
        }

        String repositoryServiceFactoryClass = c.getString("repositoryServiceFactoryClass");
        if (StringUtils.isEmpty(repositoryServiceFactoryClass) || !repositoryServiceFactoryClass.startsWith("com\\.evolveum\\.midpoint\\.repo\\.sql")) {
            return;
        }

        // database added via system properties (since default config.xml contains just placeholders that are commented out)
        String database = c.getString("database");
        assertTrue(StringUtils.isNotEmpty(database));
    }

    /**
     * MID-3349
     */
    @Test
    public void test020DirectoryAndExtractionTest() throws Exception {
        File midpointHome = new File("target/midPointHome");
        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, "target/midPointHome/");
        StartupConfiguration sc = new StartupConfiguration();
        sc.init();

        assertNotNull(midpointHome);
        assertTrue(midpointHome.exists(), "existence");
        assertTrue(midpointHome.isDirectory(), "type directory");

        File configFile = new File(midpointHome, "config.xml");
        assertTrue(configFile.exists(), "existence");
        assertTrue(configFile.isFile(), "type file");
        TestUtil.assertPrivateFilePermissions(configFile);

        ConfigurableProtectorFactory keystoreFactory = new ConfigurableProtectorFactory();
        keystoreFactory.setConfiguration(sc);
        keystoreFactory.init();

        File keystoreFile = new File(midpointHome, "keystore.jceks");
        assertTrue(keystoreFile.exists(), "existence");
        assertTrue(keystoreFile.isFile(), "type file");
        TestUtil.assertPrivateFilePermissions(keystoreFile);

        //cleanup
        System.clearProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
    }
}
