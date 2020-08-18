/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.common.ModelCommonBeans;

import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionValueMetadataCreator;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AutoAssignMappingCollector;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
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
    @Autowired public MatchingRuleRegistry matchingRuleRegistry;
    @Autowired public AutoAssignMappingCollector autoAssignMappingCollector;
    @Autowired public MappingEvaluator mappingEvaluator;
    @Autowired public MappingFactory mappingFactory;
    @Autowired public ModelCommonBeans commonBeans;
    @Autowired public ContextLoader contextLoader;
    @Autowired public CredentialsProcessor credentialsProcessor;
    @Autowired public Protector protector;
    @Autowired public ClockworkMedic medic;
    @Autowired public ProvisioningService provisioningService;
    @Autowired public ProjectionValueMetadataCreator projectionValueMetadataCreator;
    @Autowired public ActivationComputer activationComputer;
}
