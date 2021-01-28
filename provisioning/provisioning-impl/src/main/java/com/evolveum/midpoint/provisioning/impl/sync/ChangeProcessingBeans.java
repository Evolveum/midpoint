/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.repo.api.RepositoryService;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaHelper;

/**
 *  TODO
 */
@Component
public class ChangeProcessingBeans {

    @Autowired public ShadowCaretaker shadowCaretaker;
    @Autowired public ShadowManager shadowManager;
    @Autowired public ShadowCache shadowCache;
    @Autowired public MatchingRuleRegistry matchingRuleRegistry;
    @Autowired public RelationRegistry relationRegistry;
    @Autowired public SchemaHelper schemaHelper;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired
    @Qualifier("cacheRepositoryService")
    public RepositoryService repositoryService;

    /**
     * FIXME migrate somehow
     *
     * Local sequence number of a change that is being processed in the current thread.
     * Actually, it is a hack to enable testing: The code in mappings can obtain this
     * information and do some asserts on it. When the information will be propagated into
     * e.g. lensContext, we should remove this hack.
     */
    @VisibleForTesting
    public static final ThreadLocal<Integer> CHANGE_BEING_PROCESSED = new ThreadLocal<>();
}
