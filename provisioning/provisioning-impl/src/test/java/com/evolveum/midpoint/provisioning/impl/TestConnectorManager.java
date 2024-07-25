/*
 * Copyright (C) 2017-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static org.testng.AssertJUnit.*;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.impl.resources.ConnectorManager;
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

    private static final String CONNID_FRAMEWORK_VERSION = "1.6.0.0";

    @Autowired private ConnectorManager connectorManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // do NOT postInit provisioning. postInit would start connector discovery
        // we want to test the state before discovery
//        provisioningService.postInit(initResult);
    }

    @Test
    public void test100ListConnectorFactories() {
        OperationResult result = createOperationResult();

        when();
        Collection<ConnectorFactory> connectorFactories = connectorManager.getConnectorFactories();

        then();
        assertNotNull("Null connector factories", connectorFactories);
        assertFalse("No connector factories found", connectorFactories.isEmpty());
        display("Found " + connectorFactories.size() + " connector factories");

        assertSuccess(result);

        for (ConnectorFactory connectorFactory : connectorFactories) {
            displayValue("Found connector factory " + connectorFactory, connectorFactory);
        }

        PrismAsserts.assertEqualsUnordered("Wrong connector factories",
                connectorFactories.stream().map(x -> x.getClass().getName()),
                "com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorFactoryConnIdImpl",
                "com.evolveum.midpoint.provisioning.ucf.impl.builtin.ConnectorFactoryBuiltinImpl");
    }

    @Test
    public void test110SelfTest() {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        connectorManager.connectorFrameworkSelfTest(result, task);

        then();
        assertSuccess(result);
    }

    @Test
    public void test120FrameworkVersion() {
        when();
        String frameworkVersion = connectorManager.getConnIdFrameworkVersion();

        then();
        assertEquals("Unexpected framework version", CONNID_FRAMEWORK_VERSION, frameworkVersion);
    }
}
