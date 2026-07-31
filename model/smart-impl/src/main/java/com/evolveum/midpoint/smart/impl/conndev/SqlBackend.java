package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.IOException;
import java.util.List;

public class SqlBackend extends ConnectorDevelopmentBackend {

    private static final Trace LOGGER = TraceManager.getTrace(SqlBackend.class);

    private static final String NOT_YET_IMPLEMENTED = "SQL connector generation is not yet implemented.";

    public SqlBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public ConnDevApplicationInfoType discoverBasicInformation(boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return new ConnDevApplicationInfoType();
    }

    @Override
    public List<ConnDevAuthInfoType> discoverAuthorizationInformation(boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation(boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }

    @Override
    public ConnDevArtifactType generateArtifact(ConnDevGenerateArtifactDefinitionType artifactSpec, boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return new ConnDevArtifactType();
    }

    @Override
    public ConnDevArtifactType generateObjectClassArtifact(ConnDevGenerateArtifactDefinitionType artifactSpec, boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return new ConnDevArtifactType();
    }

    @Override
    public List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(
            List<ConnDevBasicObjectClassInfoType> connectorDiscovered, boolean includeUnrelated, boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass, boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }

    @Override
    public List<ConnDevAttributeInfoType> discoverObjectClassAttributes(String objectClass, boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverConnectivityEndpoints(boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }

    @Override
    public void processDocumentation(boolean skipCache) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            PolicyViolationException, ObjectAlreadyExistsException, SubscriptionComplianceException {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
    }

    @Override
    public List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(
            List<ConnDevBasicObjectClassInfoType> discovered, boolean skipCache) {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }

    @Override
    protected void restoreSession(ServiceClient.RestorationClient client) throws IOException {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
    }

    @Override
    protected List<ProcessedDocumentation> synchronizeDocumentation(List<DevShadowDocument> documentation) throws CommonException {
        // Not yet implemented: SQL discovery/generation logic is a separate follow-up task.
        LOGGER.warn(NOT_YET_IMPLEMENTED);
        return List.of();
    }
}
