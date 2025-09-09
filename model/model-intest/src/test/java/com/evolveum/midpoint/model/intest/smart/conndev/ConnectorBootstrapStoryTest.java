package com.evolveum.midpoint.model.intest.smart.conndev;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConnectorBootstrapStoryTest extends AbstractEmptyModelIntegrationTest {

    private static final int TIMEOUT = 500_000;

    private static final String COMMUNITY_CONNECTOR = "com.evolveum.community.conndev";
    private static final String MOCK_CONNECTOR = "mock-connector";
    private static final String MOCK_SNAPSHOT= "0.1-SNAPSHOT";
    private static final String CONNECTOR_DIRECTORY = "com.evolveum.community.conndev.mock-connector.0.1-SNAPSHOT";

    @Autowired
    private ConnectorInstallationService installationService;

    @Autowired
    private ConnectorDevelopmentService connectorService;
    @Autowired private SmartIntegrationServiceImpl smartIntegrationService;
    private String developmentOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);
        developmentOid = repositoryService.addObject(new ConnectorDevelopmentType()
                .name("dummy")
                        .application(new ConnDevApplicationInfoType()
                                .applicationName("Test Dummy")
                                .integrationType(ConnDevIntegrationType.DUMMY)
                        ).asPrismObject()
                , null, initResult);
    }


    private ConnectorDevelopmentType reloadDevelopment(@NotNull Task task, @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        return modelService.getObject(ConnectorDevelopmentType.class, developmentOid, null, task, result).asObjectable();
    }

    private ConnectorDevelopmentOperation continueDevelopment(@NotNull Task task, @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        return connectorService.continueFrom(reloadDevelopment(task, result));
    }


    @Test
    public void test050DiscoverDocumentation() throws Exception {
        when();
        var development = continueDevelopment(getTestTask(), getTestOperationResult());
        var token = development.submitDiscoverDocumentation(getTestTask(), getTestOperationResult());
        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> connectorService.getDiscoverDocumentationStatus(token, getTestTask(), getTestOperationResult()),
                TIMEOUT);
        assertThat(response).isNotNull();
    }


    @Test
    public void test100DiscoverBasicInformation() throws CommonException {
        when();
        var development = continueDevelopment(getTestTask(), getTestOperationResult());
        var token = development.submitDiscoverBasicInformation(getTestTask(), getTestOperationResult());

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> connectorService.getDiscoverBasicInformationStatus(token, getTestTask(), getTestOperationResult()),
                TIMEOUT);

        assertThat(response).isNotNull();
        development = continueDevelopment(getTestTask(), getTestOperationResult());
        assertThat(development.getObject().getApplication().getAuth()).isNotEmpty();
    }

    @Test
    public void test200CreateConnector() throws Exception {
        var task = createTask("createConnector");
        var result = createOperationResult();

        modelService.executeChanges((List) prismContext.deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_CONNECTOR)
                .add(new ConnDevConnectorType()
                        .groupId(COMMUNITY_CONNECTOR)
                        .artifactId(MOCK_CONNECTOR)
                        .version(MOCK_SNAPSHOT)
                        .integrationType(ConnDevIntegrationType.DUMMY))
                .asObjectDeltas(developmentOid), null, getTestTask(), getTestOperationResult());
        var devObj = continueDevelopment(getTestTask(), getTestOperationResult());
        var token = devObj.submitCreateConnector(task, result);

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> connectorService.getCreateConnectorStatus(token, task, result),
                TIMEOUT);

        assertThat(response).isNotNull();
    }

    @Test
    public void test220ConfigureAuthentication() throws Exception {
        var task = createTask("createConnector");
        var result = createOperationResult();
        var development =  continueDevelopment(getTestTask(), getTestOperationResult());

        var availableAuths = development.getObject().getApplication().getAuth();

        // Selected Authorizations are coppied to connector / auth
        var selectedAuths = availableAuths.stream().filter(a -> a.getType().equals("apiKey"))
                .map(ConnDevAuthInfoType::clone)
                .map(ConnDevAuthInfoType::asPrismContainerValue).toList();
        var delta = prismContext.deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_AUTH)
                        .add(selectedAuths)
                .<ConnectorDevelopmentType>asObjectDelta(developmentOid);

        executeChanges(delta, null, task, result);
        // FIXME: Select auths and copy to connector

        // Lets refresh development type
        development = continueDevelopment(getTestTask(), getTestOperationResult());
        var token = development.submitGenerateAuthenticationScript(task, result);
        var response = waitForFinish(() -> connectorService.getGenerateArtifactStatus(token, task, result),
                TIMEOUT);
        assertThat(response).isNotNull();
        assertThat(response.getArtifact().getContent()).isNotEmpty();

        // response for editation

        development.saveAuthenticationScript(response.getArtifact(), task, result);

    }

    @Test
    public void test230ConfigureTestConnection() throws Exception {

    }

    @Test
    public void test300DiscoverObjectClasses() throws Exception {
        var task = getTestTask();
        var result = getTestOperationResult();
        var development =  continueDevelopment(getTestTask(), getTestOperationResult());

        var token = development.submitDiscoverObjectClasses(getTestTask(), getTestOperationResult());
        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> connectorService.getDiscoverObjectClassInformationStatus(token, task, result),
                TIMEOUT);

        assertThat(response).isNotNull();

        development = continueDevelopment(getTestTask(), getTestOperationResult());
        assertThat(development.getObject().getApplication().getDetectedSchema().getObjectClass()).isNotEmpty();
    }

    @Test
    public void test310GenerateUserSchema() throws Exception {
        var task = getTestTask();
        var result = getTestOperationResult();

        var development =  continueDevelopment(getTestTask(), getTestOperationResult());

        var detailsToken = development.submitDiscoverObjectClassDetails("User",getTestTask(), getTestOperationResult());

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> connectorService.getDiscoverObjectClassDetailsStatus(detailsToken, task, result),
                TIMEOUT);

        assertThat(response).isNotNull();

        development = continueDevelopment(getTestTask(), getTestOperationResult());
        var appObjectClass = development.getObject().getConnector().getObjectClass().stream().filter(o -> o.getName().equals("User")).findFirst().orElse(
                        null);

        assertThat(appObjectClass).isNotNull();
        assertThat(appObjectClass.getAttribute()).isNotEmpty();

        var scriptToken = development.submitGenerateNativeSchema("User", task, result);
        var scriptResponse = waitForFinish(
                () -> connectorService.getGenerateArtifactStatus(scriptToken, task, result),
                TIMEOUT);

        assertThat(scriptResponse.getArtifact()).isNotNull();
        // Here script should be displayed and provided to the user for checking
        development.saveNativeSchemaScript(scriptResponse.getArtifact(), task, result);

        var connidToken = development.submitGenerateConnIdSchema("User",getTestTask(), getTestOperationResult());
        var connidResponse = waitForFinish(
                () -> connectorService.getGenerateArtifactStatus(connidToken, task, result),
                TIMEOUT);

        assertThat(connidResponse.getArtifact()).isNotNull();

        development.saveConnIdSchemaScript(connidResponse.getArtifact(), task, result);
        assertThat(development.getObject().getApplication().getDetectedSchema().getObjectClass()).isNotEmpty();

    }

    @Test
    public void test325TestResourceSchema() throws Exception {
        // Test schema
        throw new SkipException("Skipped");
    }

    @Test
    public void test320GenerateSearchScript() throws Exception {
        var task = getTestTask();
        var result = getTestOperationResult();
        var development =  continueDevelopment(getTestTask(), getTestOperationResult());

        assertThat(development.getObject().getApplication().getDetectedSchema().getObjectClass()).isNotEmpty();

        List<ConnDevHttpEndpointType> suggested = development.suggestedEndpointsFor("User", ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_ALL_DEFINITION);
        assertThat(suggested).isNotEmpty();

        var token = development.submitGenerateSearchScript("User", task, result);

        var response = waitForFinish(
                () -> connectorService.getGenerateArtifactStatus(token, task, result),
                TIMEOUT);

        assertThat(response.getArtifact()).isNotNull();

        development.saveSearchAllScript(response.getArtifact(), task, result);
        assertThat(development.getObject().getApplication().getDetectedSchema().getObjectClass()).isNotEmpty();

    }

    @Test
    public void test330TestSearchScript() throws Exception {
        var task = getTestTask();
        var result = getTestOperationResult();
        var development =  continueDevelopment(getTestTask(), getTestOperationResult());
        throw new SkipException("Skipped");
    }


}
