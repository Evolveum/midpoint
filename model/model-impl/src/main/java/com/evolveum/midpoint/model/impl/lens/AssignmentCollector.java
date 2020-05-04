/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author katka
 *
 */
@Component
public class AssignmentCollector {

    private final static Trace LOGGER = TraceManager.getTrace(AssignmentCollector.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
    @Autowired private MappingFactory mappingFactory;
    @Autowired private MappingEvaluator mappingEvaluator;
    @Autowired private ActivationComputer activationComputer;
    @Autowired private Clock clock;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private ContextLoader contextLoader;

    public <AH extends AssignmentHolderType> Collection<EvaluatedAssignment<AH>> collect(PrismObject<AH> assignmentHolder,
            boolean loginMode, Task task, OperationResult result) throws SchemaException {

        LensContext<AH> lensContext = createAuthenticationLensContext(assignmentHolder, result);

        AH assignmentHolderType = assignmentHolder.asObjectable();
        Collection<AssignmentType> forcedAssignments;
        try {
            forcedAssignments = LensUtil.getForcedAssignments(lensContext.getFocusContext().getLifecycleModel(),
                    assignmentHolderType.getLifecycleState(), objectResolver, prismContext, task, result);
        } catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException
                | ExpressionEvaluationException e1) {
            LOGGER.error("Forced assignments defined for lifecycle {} won't be evaluated", assignmentHolderType.getLifecycleState(), e1);
            forcedAssignments = null;
        }
        Collection<EvaluatedAssignment<AH>> evaluatedAssignments = new ArrayList<>();

        if (!assignmentHolderType.getAssignment().isEmpty() || forcedAssignments != null) {
            AssignmentEvaluator.Builder<AH> builder =
                    new AssignmentEvaluator.Builder<AH>()
                            .repository(repositoryService)
                            .focusOdo(new ObjectDeltaObject<>(assignmentHolder, null, assignmentHolder, assignmentHolder.getDefinition()))
                            .channel(null)
                            .objectResolver(objectResolver)
                            .systemObjectCache(systemObjectCache)
                            .relationRegistry(relationRegistry)
                            .prismContext(prismContext)
                            .mappingFactory(mappingFactory)
                            .mappingEvaluator(mappingEvaluator)
                            .contextLoader(contextLoader)
                            .activationComputer(activationComputer)
                            .now(clock.currentTimeXMLGregorianCalendar())
                            // We do need only authorizations + gui config. Therefore we not need to evaluate
                            // constructions and the like, so switching it off makes the evaluation run faster.
                            // It also avoids nasty problems with resources being down,
                            // resource schema not available, etc.
                            .loginMode(loginMode)
                            // We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
                            // will need something to push on the stack. So give them context placeholder.
                            .lensContext(lensContext);

            AssignmentEvaluator<AH> assignmentEvaluator = builder.build();

            evaluatedAssignments.addAll(evaluateAssignments(assignmentHolderType, assignmentHolderType.getAssignment(),
                    AssignmentOrigin.createInObject(), assignmentEvaluator,task, result));

            evaluatedAssignments.addAll(evaluateAssignments(assignmentHolderType, forcedAssignments,
                    AssignmentOrigin.createVirtual(), assignmentEvaluator, task, result));
        }

        return evaluatedAssignments;
    }

    private <AH extends AssignmentHolderType> Collection<EvaluatedAssignment<AH>> evaluateAssignments(AH assignmentHolder,
            Collection<AssignmentType> assignments, AssignmentOrigin origin, AssignmentEvaluator<AH> assignmentEvaluator, Task task, OperationResult result) {

        List<EvaluatedAssignment<AH>> evaluatedAssignments = new ArrayList<>();
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        try {
            PrismContainerDefinition<AssignmentType> standardAssignmentDefinition = prismContext.getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                    .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
            for (AssignmentType assignmentType: emptyIfNull(assignments)) {
                try {
                    //noinspection unchecked
                    PrismContainerDefinition<AssignmentType> definition = defaultIfNull(
                            assignmentType.asPrismContainerValue().getDefinition(), standardAssignmentDefinition);
                    ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi =
                            new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(assignmentType), definition);
                    EvaluatedAssignment<AH> assignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, assignmentHolder, assignmentHolder.toString(), origin, task, result);
                    evaluatedAssignments.add(assignment);
                } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | PolicyViolationException | SecurityViolationException | ConfigurationException | CommunicationException e) {
                    LOGGER.error("Error while processing assignment of {}: {}; assignment: {}",
                            assignmentHolder, e.getMessage(), assignmentType, e);
                }
            }
        } finally {
            RepositoryCache.exitLocalCaches();
        }
        return evaluatedAssignments;
    }

    private <AH extends AssignmentHolderType> LensContext<AH> createAuthenticationLensContext(PrismObject<AH> user, OperationResult result) throws SchemaException {
        LensContext<AH> lensContext = new LensContextPlaceholder<>(user, prismContext);
        ArchetypePolicyType policyConfigurationType = determineObjectPolicyConfiguration(user, result);
        lensContext.getFocusContext().setArchetypePolicyType(policyConfigurationType);
        return lensContext;
    }

    private <AH extends AssignmentHolderType> ArchetypePolicyType determineObjectPolicyConfiguration(PrismObject<AH> user, OperationResult result) throws SchemaException {
        ArchetypePolicyType archetypePolicy;
        try {
            archetypePolicy = archetypeManager.determineArchetypePolicy(user, result);
        } catch (ConfigurationException e) {
            throw new SchemaException(e.getMessage(), e);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Selected policy configuration from subtypes {}:\n{}",
                    FocusTypeUtil.determineSubTypes(user), archetypePolicy==null?null:archetypePolicy.asPrismContainerValue().debugDump(1));
        }

        return archetypePolicy;
    }
}
