/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowUpdater;
import com.evolveum.midpoint.repo.api.RepositoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;

/**
 *  TODO
 */
@Component
public class ChangeProcessingBeans {

    @Autowired public PrismContext prismContext;
    @Autowired public ShadowCaretaker shadowCaretaker;
    @Autowired public ShadowUpdater shadowUpdater;
    @Autowired public ShadowFinder shadowFinder;
    @Autowired public ShadowsFacade shadowsFacade;
    @Autowired public RelationRegistry relationRegistry;
    @Autowired public SchemaService schemaService;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired
    @Qualifier("cacheRepositoryService")
    public RepositoryService repositoryService;
}
