/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.resources.ConnectorManager;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceCache;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceOperationalStateManager;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;

@Component
public class CommonBeans {

    private static CommonBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static CommonBeans get() {
        return instance;
    }

    @Autowired public MatchingRuleRegistry matchingRuleRegistry;
    @Autowired public RelationRegistry relationRegistry;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired public PrismContext prismContext;
    @Autowired public ShadowsFacade shadowsFacade;
    @Autowired public ResourceObjectConverter resourceObjectConverter;
    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService repositoryService;
    @Autowired public ProvisioningServiceImpl provisioningService;
    @Autowired public ResourceManager resourceManager;
    @Autowired public ResourceCache resourceCache;
    @Autowired public ConnectorManager connectorManager;
    @Autowired public ResourceOperationalStateManager operationalStateManager;
    @Autowired public SystemObjectCache systemObjectCache;
    @Autowired public ObjectOperationPolicyHelper shadowMarkManager;
    @Autowired public Clock clock;
}
