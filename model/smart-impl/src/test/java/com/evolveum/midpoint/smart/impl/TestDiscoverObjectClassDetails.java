/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.evolveum.midpoint.smart.impl.testdata.DiscoverObjectClassDetailsTestData;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Integration test for discover object class details (attributes + endpoints in parallel) using WireMock
 * to simulate the external Smart service (RestBackend).
 */
@ContextConfiguration(locations = {"classpath:ctx-smart-integration-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDiscoverObjectClassDetails extends AbstractSmartIntegrationTest {

    private static final String CONN_DEV_OID = "00000000-d00d-4444-adad-000000000001";
    private static final String OBJECT_CLASS = "User";

    private WireMockServer wireMockServer;

    @AfterClass
    public void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // Configure system to be "online" so RestBackend is used instead of OfflineBackend
        String wiremockUrl = "http://localhost:" + wireMockServer.port();
        repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                prismContext.deltaFor(SystemConfigurationType.class)
                        .item(SystemConfigurationType.F_SMART_INTEGRATION, SmartIntegrationConfigurationType.F_CONNECTOR_GENERATION_URL)
                        .replace(wiremockUrl)
                        .asItemDeltas(),
                initResult);
    }

    @BeforeMethod
    public void prepareTestData() throws Exception {
        OperationResult result = createOperationResult("prepareTestData");
        // Create a dummy ConnectorDevelopmentType object
        ConnectorDevelopmentType connDev = new ConnectorDevelopmentType();
        connDev.setOid(CONN_DEV_OID);
        connDev.setName(createPolyStringType("test-conn-dev"));

        ConnDevApplicationInfoType appInfo = new ConnDevApplicationInfoType();
        appInfo.setApplicationName(createPolyStringType("test-app"));
        appInfo.setIntegrationType(ConnDevIntegrationType.REST);

        ConnDevSchemaType detectedSchema = new ConnDevSchemaType();
        ConnDevObjectClassInfoType ocInfo = new ConnDevObjectClassInfoType();
        ocInfo.setName(OBJECT_CLASS);
        ocInfo.setRelevant(true);
        ocInfo.setAbstract(false);
        ocInfo.setEmbedded(false);
        detectedSchema.getObjectClass().add(ocInfo);
        appInfo.setDetectedSchema(detectedSchema);

        connDev.setApplication(appInfo);

        ConnDevConnectorType connector = new ConnDevConnectorType();
        connDev.setConnector(connector);

        repositoryService.addObject(connDev.asPrismObject(), null, result);
    }

    @AfterMethod
    public void cleanupTestData() throws Exception {
        OperationResult result = createOperationResult("cleanupTestData");
        repositoryService.deleteObject(ConnectorDevelopmentType.class, CONN_DEV_OID, result);
    }

    @Test
    public void testSuccessDiscoverObjectClassDetailsSessionExists() throws Exception {
        // GIVEN
        Task task = createTask("testDiscoverDetailsSuccess");
        OperationResult result = task.getResult();

        DiscoverObjectClassDetailsTestData.stubSessionHeadSuccess(wireMockServer, CONN_DEV_OID);
        DiscoverObjectClassDetailsTestData.stubEndpointsJobSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-endpoints-123");
        DiscoverObjectClassDetailsTestData.stubEndpointsPollSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-endpoints-123");
        DiscoverObjectClassDetailsTestData.stubAttributesJobSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-attributes-123");
        DiscoverObjectClassDetailsTestData.stubAttributesPollSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-attributes-123");

        // WHEN
        ConnectorDevelopmentType connDevType = repositoryService.getObject(ConnectorDevelopmentType.class, CONN_DEV_OID, null, result).asObjectable();
        String taskOid = connectorDevelopmentService.continueFrom(connDevType)
                .submitDiscoverObjectClassDetails(OBJECT_CLASS, task, result);

        Task finishedTask = waitForTaskFinish(taskOid);

        // THEN
        assertSuccess(finishedTask.getResult());

        PrismObject<ConnectorDevelopmentType> connDev = repositoryService.getObject(ConnectorDevelopmentType.class, CONN_DEV_OID, null, result);
        ConnectorDevelopmentType updatedConnDev = connDev.asObjectable();

        assertFalse(getAppObjectClass(updatedConnDev).getEndpoint().isEmpty(), "Endpoints list should not be empty");
        assertFalse(getConnObjectClass(updatedConnDev).getAttribute().isEmpty(), "Attributes list should not be empty");
    }

    @Test
    public void testSuccessDiscoverObjectClassDetailsSessionNotExists() throws Exception {
        // GIVEN
        Task task = createTask("testDiscoverDetailsWithSessionCreation");
        OperationResult result = task.getResult();

        wireMockServer.resetAll();
        DiscoverObjectClassDetailsTestData.stubSessionHeadNotFound(wireMockServer, CONN_DEV_OID);
        DiscoverObjectClassDetailsTestData.stubSessionPostSuccess(wireMockServer, CONN_DEV_OID);

        DiscoverObjectClassDetailsTestData.stubEndpointsJobSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-endpoints-456");
        DiscoverObjectClassDetailsTestData.stubEndpointsPollSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-endpoints-456");
        DiscoverObjectClassDetailsTestData.stubAttributesJobSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-attributes-456");
        DiscoverObjectClassDetailsTestData.stubAttributesPollSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-attributes-456");

        // WHEN
        ConnectorDevelopmentType connDevType = repositoryService.getObject(ConnectorDevelopmentType.class, CONN_DEV_OID, null, result).asObjectable();
        String taskOid = connectorDevelopmentService.continueFrom(connDevType)
                .submitDiscoverObjectClassDetails(OBJECT_CLASS, task, result);

        Task finishedTask = waitForTaskFinish(taskOid);

        // THEN
        assertSuccess(finishedTask.getResult());

        PrismObject<ConnectorDevelopmentType> connDev = repositoryService.getObject(ConnectorDevelopmentType.class, CONN_DEV_OID, null, result);
        ConnectorDevelopmentType updatedConnDev = connDev.asObjectable();

        assertFalse(getAppObjectClass(updatedConnDev).getEndpoint().isEmpty(), "Endpoints list should not be empty (session created)");
        assertFalse(getConnObjectClass(updatedConnDev).getAttribute().isEmpty(), "Attributes list should not be empty (session created)");
    }

    @Test
    public void testFailDiscoverObjectClassDetailsAttribute() throws Exception {
        // GIVEN
        Task task = createTask("testDiscoverDetailsWithFailure");
        OperationResult result = task.getResult();

        wireMockServer.resetAll();
        DiscoverObjectClassDetailsTestData.stubSessionHeadSuccess(wireMockServer, CONN_DEV_OID);
        DiscoverObjectClassDetailsTestData.stubEndpointsJobSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-endpoints-789");
        DiscoverObjectClassDetailsTestData.stubEndpointsPollSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-endpoints-789");
        DiscoverObjectClassDetailsTestData.stubAttributesJobSuccess(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-attributes-789");
        DiscoverObjectClassDetailsTestData.stubAttributesPollFailure(wireMockServer, CONN_DEV_OID, OBJECT_CLASS, "job-attributes-789");

        // WHEN
        ConnectorDevelopmentType connDevType = repositoryService.getObject(ConnectorDevelopmentType.class, CONN_DEV_OID, null, result).asObjectable();
        String taskOid = connectorDevelopmentService.continueFrom(connDevType)
                .submitDiscoverObjectClassDetails(OBJECT_CLASS, task, result);

        Task finishedTask = waitForTaskFinish(taskOid, 20000, true);

        // THEN
        assertFailure(finishedTask.getResult());

        PrismObject<ConnectorDevelopmentType> connDev = repositoryService.getObject(ConnectorDevelopmentType.class, CONN_DEV_OID, null, result);
        ConnectorDevelopmentType updatedConnDev = connDev.asObjectable();

        // In case of failure of one part, the overall state should probably remain empty if the task is atomic/sequential
        assertTrue(getConnObjectClass(updatedConnDev).getAttribute().isEmpty(), "Attributes list should be empty (polling attributes failed)");
    }
    private ConnDevObjectClassInfoType getAppObjectClass(ConnectorDevelopmentType connDev) {
        return connDev.getApplication().getDetectedSchema().getObjectClass().stream()
                .filter(o -> OBJECT_CLASS.equals(o.getName())).findFirst().orElseThrow();
    }

    private ConnDevObjectClassInfoType getConnObjectClass(ConnectorDevelopmentType connDev) {
        return connDev.getConnector().getObjectClass().stream()
                .filter(o -> OBJECT_CLASS.equals(o.getName())).findFirst().orElseThrow();
    }
}
