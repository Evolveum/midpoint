/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.task.api.Tracer;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

/** Beans useful for non-Spring components within this package. */
@Component
class ResourceObjectsBeans {

    private static ResourceObjectsBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ResourceObjectsBeans get() {
        return instance;
    }

    // Local ones
    @Autowired ResourceObjectConverter resourceObjectConverter;
    @Autowired FakeIdentifierGenerator fakeIdentifierGenerator;
    @Autowired ResourceObjectReferenceResolver resourceObjectReferenceResolver;

    // From other parts of the code
    @Autowired CacheConfigurationManager cacheConfigurationManager;
    @Autowired ExpressionFactory expressionFactory;
    @Autowired LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired ShadowAuditHelper shadowAuditHelper;
    @Autowired MatchingRuleRegistry matchingRuleRegistry;
    @Autowired Tracer tracer;
}
