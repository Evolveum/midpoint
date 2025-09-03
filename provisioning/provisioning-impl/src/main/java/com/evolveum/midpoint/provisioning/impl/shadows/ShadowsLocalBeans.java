/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.provisioning.impl.shadows.classification.ResourceObjectClassifier;
import com.evolveum.midpoint.repo.common.security.SecurityPolicyFinder;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.OperationResultRecorder;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowCreator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowUpdater;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.security.CredentialsStorageManager;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Beans useful for non-Spring components within this package and its children.
 */
@Experimental
@Component
public class ShadowsLocalBeans {

    private static ShadowsLocalBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ShadowsLocalBeans get() {
        return instance;
    }

    @Autowired AccessChecker accessChecker;
    @Autowired ClassificationHelper classificationHelper;
    @Autowired ResourceObjectClassifier classifier;
    @Autowired ShadowsFacade shadowsFacade;
    @Autowired public ShadowFinder shadowFinder;
    @Autowired OperationResultRecorder operationResultRecorder;
    @Autowired ShadowUpdater shadowUpdater;
    @Autowired ShadowCreator shadowCreator;
    @Autowired AssociationsHelper associationsHelper;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;
    @Autowired ErrorHandlerLocator errorHandlerLocator;
    @Autowired ResourceManager resourceManager;
    @Autowired public Clock clock;
    @Autowired ResourceObjectConverter resourceObjectConverter;
    @Autowired ProvisioningContextFactory ctxFactory;
    @Autowired EventDispatcher eventDispatcher;
    public @Autowired Protector protector;

    @Autowired CacheConfigurationManager cacheConfigurationManager;

    @Autowired public SecurityPolicyFinder securityPolicyFinder;
    @Autowired public CredentialsStorageManager credentialsStorageManager;
}
