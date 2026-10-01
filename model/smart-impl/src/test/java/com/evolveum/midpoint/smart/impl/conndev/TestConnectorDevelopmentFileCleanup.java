/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.AbstractSmartIntegrationTest;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedDocumentationType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Tests that deleting a {@link ConnectorDevelopmentType} object removes the files that were stored
 * for it on the file system (see {@link ConnectorDevelopmentCacheInvalidationListener}).
 *
 * Requires the sqale repository (the {@code -sqale} Maven profile): only the sqale repository
 * provides the deleted object's content in the invalidation event, which is what the cleanup uses.
 */
@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestConnectorDevelopmentFileCleanup extends AbstractSmartIntegrationTest {

    private static final String UUID_REMOVED = "11111111-2222-3333-4444-555555555555";
    private static final String UUID_KEPT = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Autowired private MidpointConfiguration configuration;

    @Test
    public void test050DeletingDevelopmentRemovesItsDocumentationFiles() throws Exception {
        given("documentation files stored on the file system");
        File docsDir = new File(configuration.getMidpointHome(), ProcessedDocumentation.STORAGE_DIR_NAME);
        Files.createDirectories(docsDir.toPath());
        File removedFile = new File(docsDir, UUID_REMOVED);
        File keptFile = new File(docsDir, UUID_KEPT);
        Files.write(removedFile.toPath(), "stored documentation".getBytes());
        Files.write(keptFile.toPath(), "stored documentation".getBytes());

        and("a connector development referencing one of them");
        repoAdd(CommonInitialObjects.ARCHETYPE_UTILITY_TASK, getTestOperationResult());
        String developmentOid = addObject(new ConnectorDevelopmentType()
                .name("cleanup-test")
                .processedDocumentation(new ProcessedDocumentationType()
                        .uri("http://example.com/api/docs")
                        .uuid(UUID_REMOVED)
                        .contentType("text/html"))
                , getTestTask(), getTestOperationResult());
        assertSuccess(getTestOperationResult());

        when("the connector development object is deleted");
        deleteObject(ConnectorDevelopmentType.class, developmentOid);
        assertSuccess(getTestOperationResult());

        then("the object is gone");
        OperationResult getResult = new OperationResult("getObjectAfterDelete");
        assertThatThrownBy(() ->
                modelService.getObject(ConnectorDevelopmentType.class, developmentOid, null, getTestTask(), getResult))
                .isInstanceOf(ObjectNotFoundException.class);

        and("the documentation file of the deleted development is removed");
        assertThat(removedFile).doesNotExist();

        and("documentation files not belonging to it are kept");
        assertThat(keptFile).exists();
    }
}
