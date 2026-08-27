package com.evolveum.midpoint.smart.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

@Component
public class ConfigBasedServiceClientFactory implements ServiceClientFactory {

    private final SystemObjectCache systemObjectCache;
    private final AuditHelper auditHelper;
    private final SecurityContextManager securityContextManager;

    ConfigBasedServiceClientFactory(SystemObjectCache systemObjectCache, AuditHelper auditHelper,
            @Qualifier("securityContextManager") SecurityContextManager securityContextManager) {
        this.systemObjectCache = systemObjectCache;
        this.auditHelper = auditHelper;
        this.securityContextManager = securityContextManager;
    }

    @Override
    public ServiceClient getServiceClient(OperationResult parentResult) throws SchemaException, ConfigurationException {
        var systemConfiguration = systemObjectCache.getSystemConfigurationBean(parentResult);
        var smartIntegrationConfiguration = SystemConfigurationTypeUtil.getSmartIntegrationConfiguration(systemConfiguration);
        var auditConfiguration = auditHelper.getAuditConfiguration(systemConfiguration);
        return new AuditingServiceClient(
                new DefaultServiceClientImpl(smartIntegrationConfiguration),
                auditHelper,
                securityContextManager,
                auditConfiguration);
    }

}
