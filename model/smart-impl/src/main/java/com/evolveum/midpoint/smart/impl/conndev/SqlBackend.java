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
    private static final String NOT_APPLICABLE_TO_SQL = "This operation is HTTP-specific and does not apply to SQL connectors.";

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
        // HTTP auth-scheme discovery does not apply to SQL: JDBC credentials are plain
        // configuration properties, not a discovered auth scheme.
        throw new UnsupportedOperationException(NOT_APPLICABLE_TO_SQL);
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation(boolean skipCache) {
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
    public List<ConnDevHttpEndpointType> discoverConnectivityEndpoints(boolean skipCache) {
        // HTTP connectivity-endpoint discovery does not apply to SQL: the jdbcUrl is
        // entered directly as a configuration property.
        throw new UnsupportedOperationException(NOT_APPLICABLE_TO_SQL);
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
        restoreMetadata(client);
        ensureDocumentationIsUploaded(client);
        restoreObjectClasses(client);
        restoreRelations(client);
        restoreAttributes(client);
        restoreCodegenArtifacts(client);
    }
}
