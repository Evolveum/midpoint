/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collection;

import com.evolveum.midpoint.prism.util.PrismAsserts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestConnectorManager extends AbstractIntegrationTest {

    private static final String CONNID_FRAMEWORK_VERSION = "1.5.0.10";

    @Autowired private ProvisioningService provisioningService;
    @Autowired private ConnectorManager connectorManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // do NOT postInit provisioning. postInit would start connector discovery
        // we want to test the state before discovery
//        provisioningService.postInit(initResult);
    }

    @Test
    public void test100ListConnectorFactories() throws Exception {
        final String TEST_NAME = "test100ListConnectorFactories";

        OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName() + "." + TEST_NAME);

        // WHEN
        displayWhen(TEST_NAME);
        Collection<ConnectorFactory> connectorFactories = connectorManager.getConnectorFactories();

        // THEN
        displayThen(TEST_NAME);
        assertNotNull("Null connector factories", connectorFactories);
        assertFalse("No connector factories found", connectorFactories.isEmpty());
        display("Found "+connectorFactories.size()+" connector factories");

        assertSuccess(result);


        for (ConnectorFactory connectorFactory : connectorFactories) {
            display("Found connector factory " +connectorFactory, connectorFactory);
        }

        PrismAsserts.assertEqualsUnordered("Wrong connector factories",
                connectorFactories.stream().map(x -> x.getClass().getName()),
                "com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorFactoryConnIdImpl",
                "com.evolveum.midpoint.provisioning.ucf.impl.builtin.ConnectorFactoryBuiltinImpl");
    }

    @Test
    public void test110SelfTest() throws Exception {
        final String TEST_NAME = "test100ListConnectorFactories";

        Task task = taskManager.createTaskInstance(TestConnectorDiscovery.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        connectorManager.connectorFrameworkSelfTest(result, task);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
    }

    @Test
    public void test120FrameworkVersion() throws Exception {
        final String TEST_NAME = "test120FrameworkVersion";

        // WHEN
        String frameworkVersion = connectorManager.getFrameworkVersion();

        // THEN
        assertEquals("Unexpected framework version", CONNID_FRAMEWORK_VERSION, frameworkVersion);

    }

}
