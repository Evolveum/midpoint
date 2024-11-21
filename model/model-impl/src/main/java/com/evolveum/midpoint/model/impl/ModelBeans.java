/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.expression.ExpressionProfileManager;
import com.evolveum.midpoint.model.impl.controller.transformer.DataAccessProcessor;
import com.evolveum.midpoint.model.impl.lens.tasks.TaskOperationalDataManager;
import com.evolveum.midpoint.model.impl.security.AuthorizationMigrator;

import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;

import com.evolveum.midpoint.repo.common.security.CredentialsStorageManager;
import com.evolveum.midpoint.repo.common.security.SecurityPolicyFinder;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ProjectionContextKeyFactory;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.common.MarkManager;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorFactoryRegistryImpl;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.identities.IdentitiesManager;
import com.evolveum.midpoint.model.impl.lens.indexing.IndexingManager;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AutoAssignMappingCollector;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionMappingSetEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionValueMetadataCreator;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor.PolicyRuleScriptExecutor;
import com.evolveum.midpoint.model.impl.migrator.Migrator;
import com.evolveum.midpoint.model.impl.simulation.SimulationResultManagerImpl;
import com.evolveum.midpoint.model.impl.sync.SynchronizationService;
import com.evolveum.midpoint.model.impl.sync.reactions.SynchronizationActionFactory;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.SynchronizationSorterEvaluator;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
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

    private static ModelBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ModelBeans get() {
        return instance;
    }

    @Autowired public PrismContext prismContext;
    @Autowired public SchemaService schemaService;
    @Autowired public ModelObjectResolver modelObjectResolver;
    @Autowired public ReferenceResolver referenceResolver;
    @Autowired public ModelService modelService;
    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService cacheRepositoryService;
    @Autowired public MatchingRuleRegistry matchingRuleRegistry;
    @Autowired public AutoAssignMappingCollector autoAssignMappingCollector;
    @Autowired public MappingEvaluator mappingEvaluator;
    @Autowired public ProjectionMappingSetEvaluator projectionMappingSetEvaluator;
    @Autowired public MappingFactory mappingFactory;
    @Autowired public ModelCommonBeans commonBeans;
    @Autowired public ContextLoader contextLoader;
    @Autowired public CredentialsProcessor credentialsProcessor;
    @Autowired public CredentialsStorageManager credentialsStorageManager;
    @Autowired public Protector protector;
    @Autowired public ClockworkMedic medic;
    @Autowired public ProvisioningService provisioningService;
    @Autowired public ProjectionValueMetadataCreator projectionValueMetadataCreator;
    @Autowired public ActivationComputer activationComputer;
    @Autowired public Clock clock;
    @Autowired public SecurityEnforcer securityEnforcer;
    @Autowired public SecurityContextManager securityContextManager;
    @Autowired public OperationalDataManager metadataManager;
    @Autowired public TaskOperationalDataManager taskOperationalDataManager;
    @Autowired public IdentitiesManager identitiesManager;
    @Autowired public IndexingManager indexingManager;
    @Autowired public TaskManager taskManager;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired(required = false) public CaseManager caseManager; // not available e.g. during tests
    @Autowired public ClockworkConflictResolver clockworkConflictResolver;
    @Autowired public ContextFactory contextFactory;
    @Autowired public Clockwork clockwork;
    @Autowired public SyncTaskHelper syncTaskHelper;
    @Autowired public EventDispatcher eventDispatcher;
    @Autowired public SystemObjectCache systemObjectCache;
    @Autowired public CacheConfigurationManager cacheConfigurationManager;
    @Autowired public SynchronizationService synchronizationService;
    @Autowired public ClockworkAuditHelper clockworkAuditHelper;
    @Autowired public PolicyRuleScriptExecutor policyRuleScriptExecutor;
    @Autowired public Migrator migrator;
    @Autowired public PersonaProcessor personaProcessor;
    @Autowired public ChangeExecutor changeExecutor;
    @Autowired public Projector projector;
    @Autowired public PolicyRuleProcessor policyRuleProcessor;
    @Autowired public ClockworkHookHelper clockworkHookHelper;
    @Autowired public SecurityPolicyFinder securityPolicyFinder;
    @Autowired public CorrelatorFactoryRegistryImpl correlatorFactoryRegistry;
    @Autowired public CorrelationCaseManager correlationCaseManager;
    @Autowired public CorrelationService correlationService;
    @Autowired public CorrelationServiceImpl correlationServiceImpl;
    @Autowired public SynchronizationSorterEvaluator synchronizationSorterEvaluator;
    @Autowired public SynchronizationActionFactory synchronizationActionFactory;
    @Autowired public ProjectionContextKeyFactory projectionContextKeyFactory;
    @Autowired public ArchetypeManager archetypeManager;
    @Autowired public ExpressionProfileManager expressionProfileManager;
    @Autowired public AuditHelper auditHelper;
    @Autowired public ModelController modelController;
    @Autowired public SimulationResultManagerImpl simulationResultManager;
    @Autowired public MarkManager markManager;
    @Autowired public AuthorizationMigrator authorizationMigrator;
    @Autowired public ActivityHandlerRegistry activityHandlerRegistry;
    @Autowired public DataAccessProcessor dataAccessProcessor; // temporary
    @Autowired public RoleAnalysisService roleAnalysisService;
}
