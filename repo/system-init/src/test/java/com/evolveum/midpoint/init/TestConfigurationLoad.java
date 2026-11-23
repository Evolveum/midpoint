/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import static org.testng.Assert.*;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.configuration.api.ActuatorConfigurationSection;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RestConfigurationSection;
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

    @Test
    public void test030RestAndActuatorDefaults() {
        RestConfigurationSection rest = RestConfigurationSectionImpl.create(new BaseConfiguration());
        assertFalse(rest.apiEnabled(), "REST API must be disabled by default");
        assertNull(rest.clusterEnabled(), "intra-cluster default must be left to the clustered flag");

        ActuatorConfigurationSection actuator = ActuatorConfigurationSectionImpl.create(new BaseConfiguration());
        assertTrue(actuator.isEndpointEnabled("health"));
        assertFalse(actuator.isEndpointEnabled("metrics"));
        assertTrue(actuator.isAnyEndpointEnabled(), "the health actuator should be enabled by default");
    }

    @Test
    public void test040ActuatorAllOption() {
        BaseConfiguration allEnabled = new BaseConfiguration();
        allEnabled.setProperty("all.enabled", "true");
        allEnabled.setProperty("heapdump.enabled", "false");
        ActuatorConfigurationSection section = ActuatorConfigurationSectionImpl.create(allEnabled);
        assertTrue(section.isEndpointEnabled("metrics"));
        assertTrue(section.isEndpointEnabled("health"));
        assertFalse(section.isEndpointEnabled("heapdump"), "the endpoint's own option must win over the all option");

        BaseConfiguration allDisabled = new BaseConfiguration();
        allDisabled.setProperty("all.enabled", "false");
        section = ActuatorConfigurationSectionImpl.create(allDisabled);
        assertFalse(section.isEndpointEnabled("health"));
        assertFalse(section.isAnyEndpointEnabled(), "discovery page must be disabled without enabled endpoints");

        allDisabled.setProperty("info.enabled", "true");
        assertTrue(ActuatorConfigurationSectionImpl.create(allDisabled).isAnyEndpointEnabled());
    }

    @Test
    public void test050RestAndActuatorStartupOptions() {
        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, "target/midPointHomeEndpoints/");
        System.setProperty(MidpointConfiguration.REST_API_ENABLED_PROPERTY, "true");
        System.setProperty(MidpointConfiguration.REST_CLUSTER_ENABLED_PROPERTY, "false");
        System.setProperty("midpoint.actuator.metrics.enabled", "true");
        System.setProperty("midpoint.actuator.health.enabled", "false");
        try {
            StartupConfiguration configuration = new StartupConfiguration();
            configuration.init();

            RestConfigurationSection rest = configuration.getRestConfigurationSection();
            assertTrue(rest.apiEnabled());
            assertEquals(rest.clusterEnabled(), Boolean.FALSE);

            ActuatorConfigurationSection actuator = configuration.getActuatorConfigurationSection();
            assertTrue(actuator.isEndpointEnabled("metrics"));
            assertFalse(actuator.isEndpointEnabled("health"));
        } finally {
            System.clearProperty(MidpointConfiguration.REST_API_ENABLED_PROPERTY);
            System.clearProperty(MidpointConfiguration.REST_CLUSTER_ENABLED_PROPERTY);
            System.clearProperty("midpoint.actuator.metrics.enabled");
            System.clearProperty("midpoint.actuator.health.enabled");
            System.clearProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
        }
    }
}
