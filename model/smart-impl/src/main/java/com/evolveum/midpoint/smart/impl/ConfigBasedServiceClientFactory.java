package com.evolveum.midpoint.smart.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationConfigurationType;

@Component
public class ConfigBasedServiceClientFactory implements ServiceClientFactory {

    private final SystemObjectCache systemObjectCache;
    private final AuditHelper auditHelper;
    private final SecurityContextManager securityContextManager;
    private final Protector protector;

    ConfigBasedServiceClientFactory(SystemObjectCache systemObjectCache, AuditHelper auditHelper,
            @Qualifier("securityContextManager") SecurityContextManager securityContextManager, Protector protector) {
        this.systemObjectCache = systemObjectCache;
        this.auditHelper = auditHelper;
        this.securityContextManager = securityContextManager;
        this.protector = protector;
    }

    @Override
    public ServiceClient getServiceClient(OperationResult parentResult) throws SchemaException, ConfigurationException {
        var systemConfiguration = systemObjectCache.getSystemConfigurationBean(parentResult);
        var smartIntegrationConfiguration = SystemConfigurationTypeUtil.getSmartIntegrationConfiguration(systemConfiguration);
        var auditConfiguration = auditHelper.getAuditConfiguration(systemConfiguration);
        return new AuditingServiceClient(
                new DefaultServiceClientImpl(smartIntegrationConfiguration, getServiceApiKey(smartIntegrationConfiguration)),
                auditHelper,
                securityContextManager,
                auditConfiguration);
    }

    private String getServiceApiKey(SmartIntegrationConfigurationType smartIntegrationConfiguration) {
        if (smartIntegrationConfiguration == null || smartIntegrationConfiguration.getServiceApiKey() == null) {
            return null;
        }
        try {
            return protector.decryptString(smartIntegrationConfiguration.getServiceApiKey());
        } catch (EncryptionException e) {
            throw new SystemException("Could not decrypt smart integration service API key.", e);
        }
    }

}
