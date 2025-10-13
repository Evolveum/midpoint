/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.schema.constants.TestResourceOpNames;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Test if a resource without a schema can pass basic operations such as getObject and testResource.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSchemalessResource extends AbstractInitializedModelIntegrationTest {

    /**
     * Just test if this does not die on an exception.
     */
    @Test
    public void test001GetObject() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(
                ResourceType.class, RESOURCE_DUMMY_SCHEMALESS_OID, null, task, result);

        // THEN
        assertNotNull("Null resource returned", resource);
    }

    @Test
    public void test002TestConnection() throws Exception {
        Task task = getTestTask();

        // WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_SCHEMALESS_OID, task, task.getResult());

        // THEN
        display("Test result", testResult);
        OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INSTANTIATION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INITIALIZATION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_CONNECTION);
        assertSuccess(connectorResult);
        assertTestResourceFailure(testResult, TestResourceOpNames.RESOURCE_SCHEMA);
        assertFailure(testResult);
    }
}
