/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.security.api.SecurityContextManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Beans to be used within classes in this package. */
@Component
class Beans {

    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService repositoryService;
    @Autowired @Qualifier("securityContextManager") public SecurityContextManager securityContextManager;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired public PrismContext prismContext;
    @Autowired public RelationRegistry relationRegistry;
}
