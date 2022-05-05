/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.test.IntegrationTestTools;

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

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_TEMPLATE_BASIC, initResult);
        repoAdd(RESOURCE_BASIC_1, initResult); // No connectorRef is here, so basic add is OK
    }

    /** Provides connector OID externally. */
    private void initDummyResource(TestResource<ResourceType> resource, OperationResult result)
            throws CommonException, EncryptionException, IOException {
        addResourceFromFile(resource.file, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, result);
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
        assertResource(expansionOperation.getExpandedResource(), "after")
                .assertName("Basic 1")
                .assertNotAbstract();

        and("ancestors are OK");
        assertThat(expansionOperation.getAncestorsOids())
                .as("ancestors OIDs")
                .containsExactly(RESOURCE_TEMPLATE_BASIC.oid);
    }

    private ResourceType expand(TestResource<ResourceType> raw, OperationResult result) throws CommonException {
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                raw.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);
        return expansionOperation.getExpandedResource();
    }
}
