/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.mapping.metadata.MetadataMappingEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Commonly-used beans for model-common module.
 *
 * This class is intended to be used in classes that are not managed by Spring.
 * (To avoid massive transfer of references to individual beans from Spring-managed class
 * to the place where the beans are needed.)
 */
@Experimental
@Component
public class ModelCommonBeans {

    private static ModelCommonBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ModelCommonBeans get() {
        return instance;
    }

    @Autowired public PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService cacheRepositoryService;
    @Autowired public MatchingRuleRegistry matchingRuleRegistry;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired public ObjectResolver objectResolver;
    @Autowired public MetadataMappingEvaluator metadataMappingEvaluator; // FIXME
    @Autowired public SecurityContextManager securityContextManager; // in order to get c:actor variable
    @Autowired public Protector protector;
    @Autowired public CacheConfigurationManager cacheConfigurationManager;
    @Autowired public ModelService modelService;
    @Autowired public ModelInteractionService modelInteractionService;
    @Autowired public TagManager tagManager;
}
