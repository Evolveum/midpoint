package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpEndpointType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import java.util.List;

/**
 * Offline backend for SQL connectors. JDBC connectors have no HTTP auth schemes or endpoints
 * (both are plain configuration), and their object classes/attributes come from the development
 * mode ({@code conndev_ObjectClass}) metadata; the skeleton generator produces SQL-flavored YAML
 * (a {@code sql: { table }} block and {@code __UID__} ConnId mapping for the primary key).
 */
public class OfflineSqlBackend extends OfflineBackend {

    public OfflineSqlBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public List<ConnDevAuthInfoType> discoverAuthorizationInformation(boolean skipCache) {
        // HTTP auth-scheme discovery does not apply to SQL: JDBC credentials are plain
        // configuration properties, not a discovered auth scheme.
        return List.of();
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverConnectivityEndpoints(boolean skipCache) {
        // HTTP connectivity-endpoint discovery does not apply to SQL: the jdbcUrl is
        // entered directly as a configuration property.
        return List.of();
    }
}
