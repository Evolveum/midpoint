package com.evolveum.midpoint.model.intest.smart.conndev;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.api.conndev.ScimRestConfigurationProperties;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
public class ConnectorBootstrapOnlineStoryTest extends ConnectorBootstrapStoryTest {

    private static final int TIMEOUT = 500_000;

    private static final String COMMUNITY_CONNECTOR = "com.evolveum.community.conndev";
    private static final String MOCK_CONNECTOR = "mock-connector";
    private static final String MOCK_SNAPSHOT= "0.1-SNAPSHOT";
    private static final String CONNECTOR_DIRECTORY = "com.evolveum.community.conndev.mock-connector.0.1-SNAPSHOT";


    protected ConnDevIntegrationType targetConnectorIntegration() {
        return ConnDevIntegrationType.REST;
    }

    protected String targetConnectorVersion() {
        return MOCK_SNAPSHOT;
    }

    protected String targetConnectorName() {
        return MOCK_CONNECTOR;
    }

    @Override
    protected String applicationName() {
        return "OpenProject";
    }

    /*
    @Override
    public void test050DiscoverDocumentation() throws Exception {
        // Skip for now
        throw new SkipException("disabled");
    }

    @Override
    public void test100DiscoverBasicInformation() throws CommonException {
        // Skip for now
    }

    @Override
    public void test220ConfigureAuthentication() throws Exception {

    }

    @Override
    public void test230ConfigureTestConnection() throws Exception {

    }
    */

    @Test
    public void test070ProcessDocumentation() throws Exception {
        // FIXME: process documentation
        when();
        var development = continueDevelopment();

        var links = List.of("https://www.openproject.org/docs/api/",
                "https://www.openproject.org/docs/api/endpoints/versions/",
                "https://www.openproject.org/docs/api/endpoints/projects/",
                "https://www.openproject.org/docs/api/endpoints/work-packages/",
                "https://www.openproject.org/docs/api/example/",
                "https://www.openproject.org/docs/api/introduction/",
                "https://community.openproject.org/topics/12564",
                "https://www.openproject.org/blog/open-api/",
                "https://www.openproject.org/docs")
                .stream().map(l -> new ConnDevDocumentationSourceType().uri(l)).toList();

        var delta = deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_DOCUMENTATION_SOURCE).addRealValues(links)
                .<ConnectorDevelopmentType>asObjectDelta(developmentOid);
        executeChanges(List.of(delta), null, getTestTask(),getTestOperationResult());

        var token = development.submitProcessDocumentation(getTestTask(), getTestOperationResult());
        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> connectorService.getProcessDocumentationStatus(token, getTestTask(), getTestOperationResult()),
                TIMEOUT);
        assertThat(response).isNotNull();


    }
}
