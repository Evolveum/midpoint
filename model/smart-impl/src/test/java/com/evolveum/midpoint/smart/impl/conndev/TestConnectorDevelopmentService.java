/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.smart.impl.AbstractSmartIntegrationTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverDocumentationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

/**
 * Tests the connector development service queries against an already created connector:
 * the status queries without a task token (the operation was never submitted) must not
 * throw and must report the status as unknown, and the connector coordinates uniqueness
 * check must detect both an existing connector type object and a leftover bundle directory.
 */
@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestConnectorDevelopmentService extends AbstractSmartIntegrationTest {

    private static final String GROUP_ID = "test.conndev";
    private static final String VERSION = "9.9-SNAPSHOT";

    @Autowired
    private ConnectorInstallationService connectorInstallationService;

    @Test
    public void test100DiscoverDocumentationStatusWithoutTokenIsUnknown() throws Exception {
        given("a connector development for which the documentation discovery was never submitted");

        when("the documentation status is requested without a task token");
        OperationResult result = new OperationResult("discoverDocumentationStatus");
        StatusInfo<ConnDevDiscoverDocumentationResultType> status = connectorDevelopmentService
                .getDiscoverDocumentationStatus(null, getTestTask(), result);

        then("the status is unknown with no result and no error");
        assertThat(status.getStatus()).isEqualTo(OperationResultStatusType.UNKNOWN);
        assertThat(status.getResult()).isNull();
        assertSuccess(result);
    }

    @Test
    public void test110DiscoverDocumentationStatusWithBlankTokenIsUnknown() throws Exception {
        when("the documentation status is requested with a blank task token");
        OperationResult result = new OperationResult("discoverDocumentationStatusBlank");
        StatusInfo<ConnDevDiscoverDocumentationResultType> status = connectorDevelopmentService
                .getDiscoverDocumentationStatus("  ", getTestTask(), result);

        then("the status is unknown with no result and no error");
        assertThat(status.getStatus()).isEqualTo(OperationResultStatusType.UNKNOWN);
        assertThat(status.getResult()).isNull();
        assertSuccess(result);
    }

    @Test
    public void test120RemoveDiscoveredDocumentationWithoutTokenDoesNothing() throws Exception {
        when("discovered documentation is removed without a task token");
        OperationResult result = new OperationResult("removeDiscoveredDocumentation");
        connectorDevelopmentService.removeDiscoveredDocumentation(null, "some-documentation", getTestTask(), result);

        then("no error is reported");
        assertSuccess(result);
    }

    @Test
    public void test200ConnectorNotPresentForUnknownCoordinates() throws Exception {
        when("the coordinates uniqueness is checked for unknown coordinates");
        OperationResult result = new OperationResult("checkUnknownCoordinates");
        boolean present = connectorDevelopmentService
                .isConnectorAlreadyPresent(GROUP_ID, "unknownconnector", VERSION, getTestTask(), result);

        then("the connector is reported as not present");
        assertSuccess(result);
        assertThat(present).isFalse();
    }

    @Test
    public void test210ConnectorPresentWhenConnectorTypeExists() throws Exception {
        given("a connector type with the same bundle and version in the repository");
        String artifactId = "existingconnector";
        String connectorOid = addObject(new ConnectorType()
                .name(GROUP_ID + "." + artifactId + " " + VERSION)
                .connectorBundle(GROUP_ID + "." + artifactId)
                .connectorType("com.example.ExistingConnector")
                .connectorVersion(VERSION),
                getTestTask(), getTestOperationResult());
        assertSuccess(getTestOperationResult());

        when("the coordinates uniqueness is checked for these coordinates");
        OperationResult result = new OperationResult("checkExistingCoordinates");
        boolean present = connectorDevelopmentService
                .isConnectorAlreadyPresent(GROUP_ID, artifactId, VERSION, getTestTask(), result);

        then("the connector is reported as already present");
        assertSuccess(result);
        assertThat(present).isTrue();

        and("the connector type is removed again");
        deleteObject(ConnectorType.class, connectorOid);
        assertSuccess(getTestOperationResult());
    }

    @Test
    public void test220ConnectorPresentWhenBundleDirectoryExists() throws Exception {
        String artifactId = "dironlyconnector";
        File scanDirectory = (File) ReflectionTestUtils.getField(connectorInstallationService, "downloadDirectory");
        assertThat(scanDirectory).as("connector scan directory").isNotNull();
        File bundleDirectory = new File(scanDirectory, GROUP_ID + "." + artifactId + "." + VERSION);
        FileUtils.deleteQuietly(bundleDirectory);

        given("a bundle directory for the coordinates without a connector type object");
        assertThat(bundleDirectory).doesNotExist();
        assertThat(bundleDirectory.mkdirs()).isTrue();

        when("the coordinates uniqueness is checked for these coordinates");
        OperationResult result = new OperationResult("checkDirectoryCoordinates");
        boolean present;
        try {
            present = connectorDevelopmentService
                    .isConnectorAlreadyPresent(GROUP_ID, artifactId, VERSION, getTestTask(), result);
        } finally {
            FileUtils.deleteQuietly(bundleDirectory);
        }

        then("the connector is reported as already present");
        assertSuccess(result);
        assertThat(present).isTrue();
    }
}
