/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.util.DisplayableValue;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Test for connector configuration discovery, featuring OpenDJ server.
 *
 * This test works with a rather non-usual use case.
 * It stores all configuration changes in the repo.
 * Usual GUI wizard will not do that, it will keep resource in memory.
 * This test is supposed to test the least traveled path.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjDiscovery extends AbstractOpenDjTest {

    private Collection<PrismProperty<?>> discoveredProperties;

    @BeforeClass
    public void startLdap() throws Exception {
        doStartLdap();
    }

    @AfterClass
    public void stopLdap() {
        doStopLdap();
    }

    protected static final File RESOURCE_OPENDJ_DISCOVERY_FILE = new File(TEST_DIR, "resource-opendj-discovery.xml");

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_DISCOVERY_FILE;
    }


    @Test
    public void test010TestPartialConfiguration() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<ResourceType> resource =
                provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, task.getResult());

        when();
        OperationResult testResult = provisioningService.testPartialConfiguration(resource, task, result);

        then();
        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection failed", testResult);
    }

    @Test
    public void test012DiscoverConfiguration() throws Exception {
        Task task = getTestTask();
        PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, task.getResult());

        when();
        discoveredProperties =
                provisioningService.discoverConfiguration(resource, task.getResult())
                        .getDiscoveredProperties();

        then();
        display("Discovered properties", discoveredProperties);

        for (PrismProperty<?> discoveredProperty : discoveredProperties) {
            if (discoveredProperty.getElementName().getLocalPart().equals("baseContext")) {
                assertThat(discoveredProperty.getDefinition().getAllowedValues())
                        .as("suggested properties")
                        .hasSize(1)
                        .anyMatch(suggestion -> openDJController.getSuffix().equals(suggestion.getValue()));
            }
        }

        // TODO: assert discovered properties
    }

    /**
     * We are trying to test the connector with an incomplete configuration (we have not applied the discovered configuration yet).
     * Therefore the test should fal.
     */
    @Test
    public void test012TestConnectionFail() throws Exception {
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task, task.getResult());

        then();
        display("Test connection result", testResult);
        // baseContext is not configured, connector is not fully functional without it.
        // Hence the failure.
        TestUtil.assertFailure("Test connection result (failure expected)", testResult);
    }

    /**
     * Apply discovered properties to resource configuration.
     * We will be creating a delta which contains all the properties, updating Resource object.
     */
    @Test
    public void test016ApplyDiscoverConfiguration() throws Exception {
        Task task = getTestTask();

        List<ItemDelta<?, ?>> modifications = new ArrayList<>();
        for (PrismProperty<?> discoveredProperty : discoveredProperties) {
            ItemPath propertyPath = ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION,
                    SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME,
                    discoveredProperty.getElementName());
            PrismPropertyDefinition<?> def = discoveredProperty.getDefinition();
            Collection<? extends DisplayableValue<?>> suggestions = def.getAllowedValues() != null ? def.getAllowedValues() : def.getSuggestedValues();
            PropertyDelta<Object> propertyDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                    propertyPath,
                    discoveredProperty.getDefinition(),
                    suggestions.stream().map(displayValue -> displayValue.getValue()).toArray());
            modifications.add(propertyDelta);
        }

        display("Resource modifications", modifications);

        when();
        provisioningService.modifyObject(ResourceType.class, RESOURCE_OPENDJ_OID, modifications, null, null, task, task.getResult());

        then();
        PrismObject<ResourceType> resourceAfter = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, task.getResult());
        display("Resource after", resourceAfter);

        PrismAsserts.assertPropertyValue(resourceAfter,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME, "baseContext"),
                openDJController.getSuffix());
    }

    @Test
    public void test020TestConnection() throws Exception {
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task, task.getResult());

        then();
        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection failed", testResult);
    }
}
