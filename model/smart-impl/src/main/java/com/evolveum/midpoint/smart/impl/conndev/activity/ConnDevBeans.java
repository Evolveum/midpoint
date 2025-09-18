package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;

import com.evolveum.midpoint.repo.api.RepositoryService;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ServiceClient;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.exception.SystemException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConnDevBeans {

    private static ConnDevBeans instance;

    @Autowired public ModelService modelService;
    @Autowired public RepositoryService repositoryService;
    @Autowired public ConnectorInstallationService connectorService;
    @Autowired public ProvisioningService provisioningService;
    @Autowired public SystemObjectCache systemObjectCache;



    @PostConstruct
    public void init() {
        instance = this;

    }

    @PreDestroy
    void preDestroy() {
        instance = null;
    }

    public static ConnDevBeans get() {
        return instance;
    }

    public String getServiceUrl(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfiguration != null && systemConfiguration.getSmartIntegration() != null) {
                return systemConfiguration.getSmartIntegration().getConnectorGenerationUrl();
            }
        } catch (SchemaException e) {
            throw new SystemException("Could not get system configuration.", e);
        }
        return null;
    }

    public String getFrameworkUrl(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfiguration != null && systemConfiguration.getSmartIntegration() != null) {
                return systemConfiguration.getSmartIntegration().getConnectorFrameworkUrl();
            }
        } catch (SchemaException e) {
            throw new SystemException("Could not get system configuration.", e);
        }
        return null;
    }

    public ServiceClient client(OperationResult result) {
        var apiBase = getServiceUrl(result);
        if (apiBase == null) {
            throw new SystemException("Connector Generation Service  not configured.");
        }
        return new ServiceClient(apiBase);
    }

    public boolean isOffline() {
        return getServiceUrl(new OperationResult("isOffline")) == null;
    }
}
