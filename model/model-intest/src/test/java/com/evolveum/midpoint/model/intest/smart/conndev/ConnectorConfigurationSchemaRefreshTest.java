package com.evolveum.midpoint.model.intest.smart.conndev;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ConnectorSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for refreshing the connector configuration schema after the connector
 * bundle has been modified (configuration options disabled).
 *
 * <p>The whole flow runs against the real provisioning service (full Spring context): the
 * connector is downloaded and installed through the connector development flow, its
 * configuration override is modified to disable a number of configuration options, and then
 * the connector configuration schema is refreshed. The refreshed schema is then verified by
 * reading the connector through the real provisioning service.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConnectorConfigurationSchemaRefreshTest extends AbstractEmptyModelIntegrationTest {

    private static final int TIMEOUT = 300_000;

    // Generic SCIM/REST connector coordinates (taken from the bundle manifest)
    private static final String GROUP_ID = "com.evolveum.polygon.scimrest";
    private static final String ARTIFACT_ID = "connector-scimrest-generic";
    private static final String VERSION = "0.2-SNAPSHOT";

    // Location of the built connector bundle; overridable via a system property.
    private static final String CONNECTOR_JAR = System.getProperty(
            "scimrest.generic.connector.jar",
            "/home/tony/.m2/repository/com/evolveum/polygon/scimrest/connector-scimrest-generic/0.2-SNAPSHOT/connector-scimrest-generic-0.2-SNAPSHOT.jar");

    @Autowired
    protected ConnectorDevelopmentService connectorService;

    @Autowired
    protected ProvisioningService provisioningService;

    protected String developmentOid;
    private String connectorOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);

        // Remove any editable connector left over from a previous run so the flow is idempotent.
        // (Note: the connector framework also scans this directory when the Spring context starts,
        // so this is a safety net; the @AfterClass hook below is what guarantees idempotency.)
        cleanEditableConnector();

        // Point the connector framework at the local (file) connector bundle. No connector
        // generation URL is configured, so the (offline) backend is used.
        var systemDelta = deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_SMART_INTEGRATION)
                .replace(new SmartIntegrationConfigurationType()
                        .connectorFrameworkUrl(new File(CONNECTOR_JAR).toURI().toString())
                ).<SystemConfigurationType>asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value());
        executeChanges(systemDelta, null, initTask, initResult);

        developmentOid = addObject(new ConnectorDevelopmentType()
                        .name("scimrest-schema-refresh")
                        .application(new ConnDevApplicationInfoType()
                                .applicationName("SCIM REST Test Application")
                                .integrationType(ConnDevIntegrationType.REST)
                        )
                        .connector(new ConnDevConnectorType()
                                .groupId(GROUP_ID)
                                .artifactId(ARTIFACT_ID)
                                .version(VERSION)
                                .integrationType(ConnDevIntegrationType.REST)
                        )
                        .asPrismObject(), initTask, initResult);
    }

    private ConnectorDevelopmentOperation continueDevelopment() throws CommonException {
        var task = getTestTask();
        var result = getTestOperationResult();
        var development = modelService.getObject(
                ConnectorDevelopmentType.class, developmentOid, null, task, result).asObjectable();
        return connectorService.continueFrom(development);
    }

    @Test
    public void test100CreateConnector() throws Exception {
        var task = createTask("createConnector");
        var result = createOperationResult();
        var development = continueDevelopment();
        var token = development.submitCreateConnector(task, result);
        assertThat(token).isNotNull();

        waitForFinish(
                () -> connectorService.getCreateConnectorStatus(token, task, result),
                TIMEOUT);

        var dev = continueDevelopment().getObject();
        assertThat(dev.getConnector().getDirectory()).isNotNull();
        assertThat(dev.getConnector().getConnectorRef()).isNotNull();
        connectorOid = dev.getConnector().getConnectorRef().getOid();
        assertThat(connectorOid).isNotNull();
    }

    @Test
    public void test200BaselineSchema() throws Exception {
        // Before any modification the stored schema contains all configuration options.
        var names = connectorConfigurationPropertyNames();
        assertThat(names).contains(
                "baseAddress", "trustAllCertificates", "restTestEndpoint",
                "restUsername", "restPassword",
                "restApiKey", "restApiKeyName", "restApiKeyLocation",
                "restTokenValue", "restHawkId");
    }

    @Test
    public void test300DisableConfigurationOptions() throws Exception {
        var task = getTestTask();
        var result = getTestOperationResult();
        var development = continueDevelopment();

        // Select only API key authentication; the other authentication options will be disabled.
        var delta = deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_AUTH)
                .add(new ConnDevAuthInfoType().type(ConnDevHttpAuthTypeType.API_KEY))
                .<ConnectorDevelopmentType>asObjectDelta(developmentOid);
        executeChanges(List.of(delta), null, task, result);

        // Reload so the backend sees the updated authentication selection, then regenerate the
        // configuration override file (marks the non-selected authentication options as `ignore`).
        development = continueDevelopment();
        development.authenticationSelectionUpdated(task, result);

        // Additionally disable a (non-authentication) option through the backend override merge.
        var backend = ConnectorDevelopmentBackend.backendFor(developmentOid, task, result);
        var overrides = new Properties();
        overrides.setProperty("restTestEndpoint", "ignore");
        backend.saveConfigurationOverride(overrides);

        // The stored schema is still stale at this point - it is refreshed in the next test.
        var names = connectorConfigurationPropertyNames();
        assertThat(names).contains("restUsername", "restHawkId", "restTestEndpoint");
    }

    @Test
    public void test400RefreshConnectorSchema() throws Exception {
        var task = createTask("refreshConnectorSchema");
        var result = createOperationResult();
        var development = continueDevelopment();
        var token = development.submitRefreshConnectorSchema(task, result);
        assertThat(token).isNotNull();

        waitForFinish(
                () -> connectorService.getRefreshConnectorSchemaStatus(token, task, result),
                TIMEOUT);
    }

    @Test
    public void test500AssertRefreshedSchema() throws Exception {
        var names = connectorConfigurationPropertyNames();

        // The selected (API key) authentication options and the common options are retained.
        assertThat(names).contains(
                "baseAddress", "trustAllCertificates",
                "restApiKey", "restApiKeyName", "restApiKeyLocation");

        // The disabled (non-selected) authentication options are no longer part of the schema.
        assertThat(names).doesNotContain("restUsername", "restPassword", "restTokenValue", "restHawkId");

        // The option disabled via the configuration override merge is gone as well.
        assertThat(names).doesNotContain("restTestEndpoint");
    }

    /**
     * Returns the set of ICF connector configuration property names of the stored connector
     * schema, reading the connector object through the real provisioning service.
     */
    @NotNull
    private Set<String> connectorConfigurationPropertyNames() throws Exception {
        var task = getTestTask();
        var result = getTestOperationResult();
        var connector = provisioningService.getObject(
                ConnectorType.class, connectorOid, null, task, result).asObjectable();

        ConnectorSchema schema = ConnectorTypeUtil.parseConnectorSchema(connector);
        PrismContainerDefinition<?> configuration = schema.getConnectorConfigurationContainerDefinition();
        PrismContainerDefinition<?> configurationProperties =
                configuration.findContainerDefinition(SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME);
        assertThat(configurationProperties)
                .as("No <configurationProperties> container in the connector schema of %s", connectorOid)
                .isNotNull();

        return configurationProperties.getPropertyDefinitions().stream()
                .map(propertyDefinition -> propertyDefinition.getItemName().getLocalPart())
                .collect(Collectors.toSet());
    }

    /**
     * Removes the editable connector at the end of the class so that the next run starts with a
     * clean {@code connid-connectors} directory. The connector framework scans that directory when
     * the Spring context starts, so a leftover connector would otherwise be seen as "already
     * installed" and the re-creation would fail.
     */
    @AfterClass
    public void cleanUpEditableConnector() {
        cleanEditableConnector();
    }

    /**
     * Removes the editable connector directory (and its {@code .tmp} variant) left under
     * {@code ${midpoint.home}/connid-connectors} by a previous run, so the connector can be
     * re-created from scratch.
     */
    private void cleanEditableConnector() {
        var connectorsDir = new File(System.getProperty("midpoint.home", "target/midpoint-home"), "connid-connectors");
        var name = GROUP_ID + "." + ARTIFACT_ID + "." + VERSION;
        for (var candidate : new File[]{ new File(connectorsDir, name), new File(connectorsDir, name + ".tmp") }) {
            if (!candidate.isDirectory()) {
                continue;
            }
            try (var paths = Files.walk(candidate.toPath())) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't clean up leftover editable connector " + candidate, e);
            }
        }
    }
}
