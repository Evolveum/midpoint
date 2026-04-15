package com.evolveum.midpoint.smart.impl;

import org.springframework.stereotype.Component;

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

    public ConfigBasedServiceClientFactory(SystemObjectCache systemObjectCache) {
        this.systemObjectCache = systemObjectCache;
    }

    @Override
    public ServiceClient getServiceClient(OperationResult parentResult) throws SchemaException, ConfigurationException {
        var smartIntegrationConfiguration =
                SystemConfigurationTypeUtil.getSmartIntegrationConfiguration(
                        systemObjectCache.getSystemConfigurationBean(parentResult));
        return new DefaultServiceClientImpl(smartIntegrationConfiguration);
    }

}
