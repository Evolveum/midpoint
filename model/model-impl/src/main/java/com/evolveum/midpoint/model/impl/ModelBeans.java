/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.model.impl.lens.projector.focus.AutoAssignMappingCollector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingSetEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Commonly-used beans for model-impl module.
 *
 * This class is intended to be used in classes that are not managed by Spring.
 * (To avoid massive transfer of references to individual beans from Spring-managed class
 * to the place where the beans are needed.)
 */
@Experimental
@Component
public class ModelBeans {

    @Autowired public PrismContext prismContext;
    @Autowired public ModelObjectResolver modelObjectResolver;
    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService cacheRepositoryService;
    @Autowired public MappingSetEvaluator mappingSetEvaluator;
    @Autowired public MatchingRuleRegistry matchingRuleRegistry;
    @Autowired public AutoAssignMappingCollector autoAssignMappingCollector;

}
