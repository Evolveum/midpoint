package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;

import com.evolveum.midpoint.repo.api.RepositoryService;

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

}
