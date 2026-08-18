package com.evolveum.midpoint.smart.impl;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

@Component
public class ConfigBasedServiceClientFactory implements ServiceClientFactory {

    private final SystemObjectCache systemObjectCache;
    private final AuditHelper auditHelper;

    ConfigBasedServiceClientFactory(SystemObjectCache systemObjectCache,
            AuditHelper auditHelper) {
        this.systemObjectCache = systemObjectCache;
        this.auditHelper = auditHelper;
    }

    @Override
    public ServiceClient getServiceClient(OperationResult parentResult) throws SchemaException, ConfigurationException {
        var smartIntegrationConfiguration =
                SystemConfigurationTypeUtil.getSmartIntegrationConfiguration(
                        systemObjectCache.getSystemConfigurationBean(parentResult));
        return new AuditingServiceClient(
                new DefaultServiceClientImpl(smartIntegrationConfiguration),
                auditHelper);
    }

}
