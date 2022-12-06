/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author katka
 */
@Component
public class AssignmentCollector {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentCollector.class);

    @Autowired private ReferenceResolver referenceResolver;
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
    @Autowired private ModelBeans modelBeans;

    public <AH extends AssignmentHolderType> Collection<EvaluatedAssignment> collect(PrismObject<AH> focus,
            boolean loginMode, Task task, OperationResult result) throws SchemaException {

        LensContext<AH> lensContext = createAuthenticationLensContext(focus, result);

        AH focusBean = focus.asObjectable();

        Collection<EvaluatedAssignment> evaluatedAssignments = new ArrayList<>();

        Collection<AssignmentType> forcedAssignments = collectForcedAssignments(focusBean, lensContext.getFocusContext().getLifecycleModel(), task, result);
        if (!focusBean.getAssignment().isEmpty() || forcedAssignments != null) {
            AssignmentEvaluator.Builder<AH> builder =
                    new AssignmentEvaluator.Builder<AH>()
                            .referenceResolver(referenceResolver)
                            .focusOdo(new ObjectDeltaObject<>(focus, null, focus, focus.getDefinition()))
                            .channel(null)
                            .modelBeans(modelBeans)
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

            evaluatedAssignments.addAll(evaluateAssignments(focusBean, focusBean.getAssignment(),
                    AssignmentOrigin.createInObject(), assignmentEvaluator, task, result));

            evaluatedAssignments.addAll(evaluateAssignments(focusBean, forcedAssignments,
                    AssignmentOrigin.createVirtual(), assignmentEvaluator, task, result));
        }

        return evaluatedAssignments;
    }

    private <AH extends AssignmentHolderType> Collection<EvaluatedAssignment> evaluateAssignments(AH focus,
            Collection<AssignmentType> assignments, AssignmentOrigin origin, AssignmentEvaluator<AH> assignmentEvaluator, Task task, OperationResult result) {

        List<EvaluatedAssignment> evaluatedAssignments = new ArrayList<>();
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        try {
            PrismContainerDefinition<AssignmentType> standardAssignmentDefinition = prismContext.getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                    .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
            for (AssignmentType assignmentType : emptyIfNull(assignments)) {
                try {
                    //noinspection unchecked
                    PrismContainerDefinition<AssignmentType> definition = defaultIfNull(
                            assignmentType.asPrismContainerValue().getDefinition(), standardAssignmentDefinition);
                    ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi =
                            new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(assignmentType), definition);
                    EvaluatedAssignment assignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, focus, focus.toString(), origin, task, result);
                    evaluatedAssignments.add(assignment);
                } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | PolicyViolationException | SecurityViolationException | ConfigurationException | CommunicationException e) {
                    LOGGER.error("Error while processing assignment of {}: {}; assignment: {}",
                            focus, e.getMessage(), assignmentType, e);
                }
            }
        } finally {
            RepositoryCache.exitLocalCaches();
        }
        return evaluatedAssignments;
    }

    private <AH extends AssignmentHolderType> LensContext<AH> createAuthenticationLensContext(PrismObject<AH> user, OperationResult result) throws SchemaException {
        LensContext<AH> lensContext = new LensContextPlaceholder<>(user);
        ArchetypePolicyType policyConfigurationType = determineObjectPolicyConfiguration(user, result);
        lensContext.getFocusContext().setArchetypePolicy(policyConfigurationType);
        return lensContext;
    }

    private <AH extends AssignmentHolderType> Collection<AssignmentType> collectForcedAssignments(AH focusBean, LifecycleStateModelType lifecycleModel, Task task, OperationResult result) {
        try {
            return LensUtil.getForcedAssignments(lifecycleModel,
                    focusBean.getLifecycleState(), objectResolver, prismContext, task, result);
        } catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException
                | ExpressionEvaluationException | SchemaException e) {
            LOGGER.error("Forced assignments defined for lifecycle {} won't be evaluated", focusBean.getLifecycleState(), e);
            return null;
        }
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
                    FocusTypeUtil.determineSubTypes(user), archetypePolicy == null ? null : archetypePolicy.asPrismContainerValue().debugDump(1));
        }

        return archetypePolicy;
    }
}
