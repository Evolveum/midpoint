/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;

/**
 * Collects [evaluated] assignments during login process.
 *
 * @author katka
 */
@Component
public class LoginAssignmentCollector {

    private static final Trace LOGGER = TraceManager.getTrace(LoginAssignmentCollector.class);

    @Autowired private ReferenceResolver referenceResolver;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
    @Autowired private Clock clock;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    public <AH extends AssignmentHolderType> Collection<EvaluatedAssignment> collect(
            @NotNull PrismObject<AH> focus, Task task, OperationResult result) throws SchemaException, ConfigurationException {

        LensContext<AH> lensContext = createAuthenticationLensContext(focus, result);

        AH focusBean = focus.asObjectable();

        Collection<EvaluatedAssignment> evaluatedAssignments = new ArrayList<>();

        List<ConfigurationItem<AssignmentType>> explicitAssignments =
                ConfigurationItem.ofListEmbedded(
                        focusBean.getAssignment());

        Collection<ConfigurationItem<AssignmentType>> forcedAssignments =
                collectForcedAssignments(
                        focusBean, lensContext.getFocusContext().getLifecycleModel(), task, result);

        if (!explicitAssignments.isEmpty() || !forcedAssignments.isEmpty()) {

            AssignmentEvaluator.Builder<AH> builder =
                    new AssignmentEvaluator.Builder<AH>()
                            .referenceResolver(referenceResolver)
                            .focusOdo(new ObjectDeltaObject<>(focus, null, focus, focus.getDefinition()))
                            .now(clock.currentTimeXMLGregorianCalendar())
                            // We do need only authorizations + gui config. Therefore we not need to evaluate
                            // constructions and the like, so switching it off makes the evaluation run faster.
                            // It also avoids nasty problems with resources being down,
                            // resource schema not available, etc.
                            .loginMode(true)
                            // We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
                            // will need something to push on the stack. So give them context placeholder.
                            .lensContext(lensContext);

            AssignmentEvaluator<AH> assignmentEvaluator = builder.build();

            evaluatedAssignments.addAll(
                    evaluateAssignments(
                            focusBean,
                            explicitAssignments,
                            origin -> AssignmentOrigin.inObject(origin),
                            assignmentEvaluator, task, result));

            evaluatedAssignments.addAll(
                    evaluateAssignments(
                            focusBean,
                            forcedAssignments,
                            origin -> AssignmentOrigin.virtual(origin),
                            assignmentEvaluator, task, result));
        }

        return evaluatedAssignments;
    }

    private <AH extends AssignmentHolderType> Collection<EvaluatedAssignment> evaluateAssignments(
            @NotNull AH focus,
            @NotNull Collection<ConfigurationItem<AssignmentType>> assignments,
            @NotNull Function<ConfigurationItemOrigin, AssignmentOrigin> originFunction,
            AssignmentEvaluator<AH> assignmentEvaluator,
            Task task,
            OperationResult result) {

        List<EvaluatedAssignment> evaluatedAssignments = new ArrayList<>();
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        try {
            PrismContainerDefinition<AssignmentType> standardAssignmentDefinition = prismContext.getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                    .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
            for (ConfigurationItem<AssignmentType> assignmentWithOrigin : assignments) {
                AssignmentType assignment = assignmentWithOrigin.value();
                try {
                    //noinspection unchecked
                    PrismContainerDefinition<AssignmentType> definition = defaultIfNull(
                            assignment.asPrismContainerValue().getDefinition(), standardAssignmentDefinition);
                    var assignmentIdi =
                            new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(assignment), definition);
                    evaluatedAssignments.add(
                            assignmentEvaluator.evaluate(
                                    assignmentIdi,
                                    PlusMinusZero.ZERO,
                                    false,
                                    focus,
                                    focus.toString(),
                                    originFunction.apply(assignmentWithOrigin.origin()),
                                    task,
                                    result));
                } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | PolicyViolationException
                        | SecurityViolationException | ConfigurationException | CommunicationException e) {
                    // We continue because of login mode.
                    LOGGER.error("Error while processing assignment of {}: {}; assignment: {}",
                            focus, e.getMessage(), assignment, e);
                }
            }
        } finally {
            RepositoryCache.exitLocalCaches();
        }
        return evaluatedAssignments;
    }

    private <AH extends AssignmentHolderType> LensContext<AH> createAuthenticationLensContext(
            PrismObject<AH> user, OperationResult result) throws SchemaException, ConfigurationException {
        LensContext<AH> lensContext = new LensContextPlaceholder<>(user, TaskExecutionMode.PRODUCTION);
        ArchetypePolicyType archetypePolicy = determineArchetypePolicy(user, result);
        lensContext.getFocusContext().setArchetypePolicy(archetypePolicy);
        return lensContext;
    }

    private <AH extends AssignmentHolderType> @NotNull Collection<ConfigurationItem<AssignmentType>> collectForcedAssignments(
            AH focusBean, LifecycleStateModelType lifecycleModel, Task task, OperationResult result) {
        String lifecycleState = focusBean.getLifecycleState();
        try {
            return LensUtil.getForcedAssignments(
                    lifecycleModel, lifecycleState, objectResolver, prismContext, task, result);
        } catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException
                | ExpressionEvaluationException | SchemaException e) {
            // We continue because of login mode.
            LOGGER.error("Forced assignments defined for lifecycle state '{}' won't be evaluated for {} because of an error: {}",
                    lifecycleState, focusBean, e.getMessage(), e);
            return List.of();
        }
    }

    private <AH extends AssignmentHolderType> ArchetypePolicyType determineArchetypePolicy(
            PrismObject<AH> user, OperationResult result) throws SchemaException, ConfigurationException {
        ArchetypePolicyType archetypePolicy = archetypeManager.determineArchetypePolicy(user, result);
        LOGGER.trace("Archetype policy for {}:\n{}", user, DebugUtil.debugDumpLazily(archetypePolicy, 1));
        return archetypePolicy;
    }
}
