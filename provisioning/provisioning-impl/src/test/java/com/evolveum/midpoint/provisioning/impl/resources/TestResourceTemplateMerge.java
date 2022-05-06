/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.test.IntegrationTestTools.DUMMY_CONNECTOR_TYPE;
import static com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3.ResultsHandlerConfigurationType.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.testng.annotations.Test;

import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class TestResourceTemplateMerge extends AbstractProvisioningIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/merge");

    private static final TestResource<ResourceType> RESOURCE_TEMPLATE_BASIC = new TestResource<>(
            TEST_DIR, "resource-template-basic.xml", "2d1bbd38-8292-4895-af07-15de1ae423ec");
    private static final TestResource<ResourceType> RESOURCE_BASIC_1 = new TestResource<>(
            TEST_DIR, "resource-basic-1.xml", "b6f77fb9-8bdf-42de-b7d4-639c77fa6805");
    private static final TestResource<ResourceType> RESOURCE_BASIC_2 = new TestResource<>(
            TEST_DIR, "resource-basic-2.xml", "969d0587-b049-4067-a749-2fe61d5fb2f6");

    private static final TestResource<ResourceType> RESOURCE_TEMPLATE_ADDITIONAL_CONNECTORS = new TestResource<>(
            TEST_DIR, "resource-template-additional-connectors.xml", "e17dfe38-727f-41b6-ab1c-9106c0bb046d");
    private static final TestResource<ResourceType> RESOURCE_ADDITIONAL_CONNECTORS_1 = new TestResource<>(
            TEST_DIR, "resource-additional-connectors-1.xml", "dcf805dc-afff-46c1-bf8c-876777ef4af5");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_TEMPLATE_BASIC, List.of(DUMMY_CONNECTOR_TYPE), initResult);
        repoAdd(RESOURCE_BASIC_1, initResult); // No connectorRef is here, so basic add is OK
        repoAdd(RESOURCE_BASIC_2, initResult); // No connectorRef is here, so basic add is OK

        initDummyResource(
                RESOURCE_TEMPLATE_ADDITIONAL_CONNECTORS,
                List.of(DUMMY_CONNECTOR_TYPE, DUMMY_CONNECTOR_TYPE, DUMMY_CONNECTOR_TYPE),
                initResult);
        initDummyResource(
                RESOURCE_ADDITIONAL_CONNECTORS_1,
                Arrays.asList(null, null, DUMMY_CONNECTOR_TYPE),
                initResult); // No connectorRef is here, so basic add is OK
    }

    /** Provides connector OID externally. */
    private void initDummyResource(TestResource<ResourceType> resource, List<String> connectorTypes, OperationResult result)
            throws CommonException, EncryptionException, IOException {
        addResourceFromFile(resource.file, connectorTypes, false, result);
        resource.reload(result);
    }

    @Test
    public void test100Basic1() throws CommonException {
        OperationResult result = getTestOperationResult();

        when("basic1 is expanded");
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                RESOURCE_BASIC_1.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);

        then("expanded version is OK");
        // @formatter:off
        assertResource(expansionOperation.getExpandedResource(), "after")
                .assertName("Basic 1")
                .assertNotAbstract()
                .configurationProperties()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("supportValidity"), false) // overridden
                    .assertPropertyEquals(new ItemName("instanceId"), "basic") // from template
                    .assertPropertyEquals(new ItemName("uselessString"), "Shiver me timbers!") // from template
                    .assertPropertyEquals(new ItemName("uselessGuardedString"),
                            new ProtectedStringType().clearValue("Dead men tell no tales")) // from template
                .end()
                .resultsHandlerConfiguration()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(F_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE, true) // added by basic-1
                    .assertPropertyEquals(F_ENABLE_FILTERED_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_NORMALIZING_RESULTS_HANDLER, false) // from template
                .end()
                .assertConnectorRef(RESOURCE_TEMPLATE_BASIC.getObjectable().getConnectorRef());
        // @formatter:on

        and("ancestors are OK");
        assertThat(expansionOperation.getAncestorsOids())
                .as("ancestors OIDs")
                .containsExactly(RESOURCE_TEMPLATE_BASIC.oid);
    }

    @Test
    public void test110Basic2() throws CommonException {
        OperationResult result = getTestOperationResult();

        when("basic2 is expanded");
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                RESOURCE_BASIC_2.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);

        then("expanded version is OK");
        // @formatter:off
        assertResource(expansionOperation.getExpandedResource(), "after")
                .assertName("Basic 2")
                .assertNotAbstract()
                .configurationProperties()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("supportValidity"), false) // overridden (Basic 1)
                    .assertPropertyEquals(new ItemName("instanceId"), "basic") // from template
                    .assertPropertyEquals(new ItemName("uselessString"), "False!") // overridden (Basic 2)
                    .assertPropertyEquals(new ItemName("uselessGuardedString"),
                            new ProtectedStringType().clearValue("Dead men tell no tales")) // from template
                .end()
                .resultsHandlerConfiguration()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(F_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE, true) // added by basic-1
                    .assertPropertyEquals(F_ENABLE_FILTERED_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_NORMALIZING_RESULTS_HANDLER, false) // from template
                .end()
                .assertConnectorRef(RESOURCE_TEMPLATE_BASIC.getObjectable().getConnectorRef());
        // @formatter:on

        and("ancestors are OK");
        assertThat(expansionOperation.getAncestorsOids())
                .as("ancestors OIDs")
                .containsExactlyInAnyOrder(RESOURCE_TEMPLATE_BASIC.oid, RESOURCE_BASIC_1.oid);
    }

    @Test
    public void test120MultipleConnectors() throws CommonException {
        OperationResult result = getTestOperationResult();

        when("additional-connectors-1 is expanded");
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                RESOURCE_ADDITIONAL_CONNECTORS_1.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);

        then("expanded version is OK");
        // @formatter:off
        assertResource(expansionOperation.getExpandedResource(), "after")
                .assertName("With additional connectors 1")
                .assertNotAbstract()
                .configurationProperties()
                    .assertSize(2)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "main") // from template
                    .assertPropertyEquals(new ItemName("supportValidity"), true) // from specific
                .end()
                .assertConnectorRef(RESOURCE_TEMPLATE_ADDITIONAL_CONNECTORS.getObjectable().getConnectorRef())
                .assertAdditionalConnectorsCount(3)
                .configurationProperties("first")
                    .assertSize(2)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "first") // from template
                    .assertPropertyEquals(new ItemName("uselessString"), "merged") // from specific
                .end()
                .configurationProperties("second") // from template
                    .assertSize(1)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "second")
                .end()
                .configurationProperties("third") // from specific
                    .assertSize(1)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "third")
                .end();
        // @formatter:on

        and("ancestors are OK");
        assertThat(expansionOperation.getAncestorsOids())
                .as("ancestors OIDs")
                .containsExactly(RESOURCE_TEMPLATE_ADDITIONAL_CONNECTORS.oid);
    }

    private ResourceType expand(TestResource<ResourceType> raw, OperationResult result) throws CommonException {
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                raw.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);
        return expansionOperation.getExpandedResource();
    }
}
