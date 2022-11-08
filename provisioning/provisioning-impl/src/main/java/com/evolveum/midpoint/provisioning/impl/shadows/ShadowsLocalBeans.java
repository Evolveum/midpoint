/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Beans useful for non-Spring components within this package.
 */
@Experimental
@Component
class ShadowsLocalBeans {

    @Autowired AccessChecker accessChecker;
    @Autowired ShadowedObjectConstructionHelper shadowedObjectConstructionHelper;
    @Autowired ShadowAcquisitionHelper shadowAcquisitionHelper;
    @Autowired CommonHelper commonHelper;
    @Autowired ClassificationHelper classificationHelper;
    @Autowired ShadowsFacade shadowsFacade;
    @Autowired ShadowCaretaker shadowCaretaker;
    @Autowired ShadowManager shadowManager;
    @Autowired ExpressionFactory expressionFactory;

    @Autowired
    @Qualifier("cacheRepositoryService")
    RepositoryService repositoryService;

    @Autowired ErrorHandlerLocator errorHandlerLocator;
    @Autowired ResourceManager resourceManager;
    @Autowired Clock clock;
    @Autowired ResourceObjectConverter resourceObjectConverter;
    @Autowired ProvisioningContextFactory ctxFactory;
    @Autowired RefreshHelper refreshHelper;
}
