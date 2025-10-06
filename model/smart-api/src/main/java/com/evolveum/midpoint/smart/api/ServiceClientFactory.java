package com.evolveum.midpoint.smart.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface ServiceClientFactory {
    ServiceClient getServiceClient(OperationResult parentResult) throws SchemaException, ConfigurationException;
}
