/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.model.impl.correlator.CorrelatorFactoryRegistryImpl;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SimplePreInboundsContextImpl;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicyFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.cases.CorrelationCaseUtil;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Provides correlation-related functionality, primarily on top of {@link Correlator} interface:
 *
 * . the correlation itself (`correlate`) - including creation of {@link CompleteCorrelationResult} out of correlator-provided
 * {@link CorrelationResult} object;
 * . determining candidate owner suitability (`checkCandidateOwner`);
 * . describing the correlation case (`describeCorrelationCase`);
 * . completing a correlation case;
 *
 * and further auxiliary methods.
 */
@Component
public class CorrelationServiceImpl implements CorrelationService {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationServiceImpl.class);

    private static final String OP_CORRELATE = CorrelationServiceImpl.class.getName() + ".correlate";
    private static final String OP_RESOLVE = CorrelationServiceImpl.class.getName() + ".resolve";

    @Autowired ModelBeans beans;
    @Autowired CorrelatorFactoryRegistryImpl correlatorFactoryRegistry;
    @Autowired SystemObjectCache systemObjectCache;
    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;

    /**
     * A limited convenience variant of {@link #correlate(CorrelatorContext, CorrelationContext, OperationResult)}
     * that starts with a single shadow only. Used in special cases, including testing.
     */
    public @NotNull CompleteCorrelationResult correlate(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        CompleteContext ctx = getCompleteContext(shadowedResourceObject, task, result);
        return correlate(ctx.correlatorContext, ctx.correlationContext, result);
    }

    @Override
    public @NotNull CompleteCorrelationResult correlate(
            @NotNull FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        CompleteContext ctx = getCompleteContext(preFocus, task, result);
        return correlate(ctx.correlatorContext, ctx.correlationContext, result);
    }

    /**
     * Executes the correlation in the standard way.
     *
     * The contexts ({@link CorrelatorContext} and {@link CorrelationContext}) are provided by the caller in order
     * to allow for more flexibility. The caller is responsible for their creation.
     */
    public @NotNull CompleteCorrelationResult correlate(
            @NotNull CorrelatorContext<?> rootCorrelatorContext,
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_CORRELATE)
                .addArbitraryObjectAsParam("rootCorrelatorContext", rootCorrelatorContext)
                .addArbitraryObjectAsParam("correlationContext", correlationContext)
                .build();
        try {
            CorrelationResult correlationResult = correlatorFactoryRegistry
                    .instantiateCorrelator(rootCorrelatorContext, correlationContext.getTask(), result)
                    .correlate(correlationContext, result);
            return createCompleteResult(correlationResult, rootCorrelatorContext);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private @NotNull CompleteCorrelationResult createCompleteResult(
            CorrelationResult correlationResult, CorrelatorContext<?> correlatorContext) {
        CandidateOwnersMap candidateOwnersMap = correlationResult.getCandidateOwnersMap();
        double definiteThreshold = correlatorContext.getDefiniteThreshold();
        Collection<CandidateOwner> owners = candidateOwnersMap.selectWithConfidenceAtLeast(definiteThreshold);
        double candidateThreshold = correlatorContext.getCandidateThreshold();
        Collection<CandidateOwner> eligibleCandidates = candidateOwnersMap.selectWithConfidenceAtLeast(candidateThreshold);

        LOGGER.debug("Determining overall result with 'definite' threshold of {}, candidate threshold of {}, "
                        + "definite (owner) candidates: {}, eligible candidates: {}, all candidates:\n{}",
                definiteThreshold, candidateThreshold, owners.size(), eligibleCandidates.size(),
                DebugUtil.toStringCollectionLazy(candidateOwnersMap.values(), 1));

        ResourceObjectOwnerOptionsType optionsBean = new ResourceObjectOwnerOptionsType();
        for (CandidateOwner eligibleCandidate : eligibleCandidates) {
            String candidateId = Objects.requireNonNullElse(
                    eligibleCandidate.getExternalId(),
                    eligibleCandidate.getOid());
            optionsBean.getOption().add(
                    new ResourceObjectOwnerOptionType()
                            .identifier(
                                    OwnerOptionIdentifier.forExistingOwner(candidateId).getStringValue())
                            .candidateOwnerRef(
                                    ObjectTypeUtil.createObjectRef(eligibleCandidate.getObject()))
                            .confidence(
                                    eligibleCandidate.getConfidence()));
        }
        if (owners.size() != 1) {
            optionsBean.getOption().add(
                    new ResourceObjectOwnerOptionType()
                            .identifier(OwnerOptionIdentifier.forNoOwner().getStringValue()));
        }

        CandidateOwnersMap finalCandidatesMap = CandidateOwnersMap.from(eligibleCandidates);
        if (owners.size() == 1) {
            ObjectType owner = owners.iterator().next().getObject();
            return CompleteCorrelationResult.existingOwner(owner, finalCandidatesMap, optionsBean);
        } else if (eligibleCandidates.isEmpty()) {
            return CompleteCorrelationResult.noOwner();
        } else {
            return CompleteCorrelationResult.uncertain(finalCandidatesMap, optionsBean);
        }
    }

    /**
     * Checks whether the supplied candidate owner would be the correlation result (if real correlation would take place).
     * Used for opportunistic synchronization.
     *
     * Why not doing the actual correlation? Because the owner may not exist in repository yet.
     */
    public boolean checkCandidateOwner(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceType resource,
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull FocusType candidateOwner,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, SecurityViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException {
        FocusType preFocus = computePreFocus(
                shadowedResourceObject, resource, synchronizationPolicy, candidateOwner.getClass(), task, result);
        CompleteContext ctx = CompleteContext.forShadow(
                shadowedResourceObject,
                resource,
                synchronizationPolicy.getObjectTypeDefinition(),
                synchronizationPolicy,
                preFocus,
                determineObjectTemplate(synchronizationPolicy.getArchetypeOid(), preFocus, task, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)),
                task);
        double confidence = correlatorFactoryRegistry
                .instantiateCorrelator(ctx.correlatorContext, task, result)
                .checkCandidateOwner(ctx.correlationContext, candidateOwner, result);
        return confidence >= ctx.correlatorContext.getDefiniteThreshold();
    }

    @Override
    public @NotNull CorrelationCaseDescription<?> describeCorrelationCase(
            @NotNull CaseType aCase,
            @Nullable CorrelationCaseDescriptionOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        CompleteContext ctx = getCompleteContext(aCase, task, result);

        List<ResourceObjectOwnerOptionType> ownerOptionsList = CorrelationCaseUtil.getOwnerOptionsList(aCase);
        String contextDesc = "correlation case " + aCase;
        return new CorrelationCaseDescriber<>(
                ctx.correlatorContext, ctx.correlationContext,
                ownerOptionsList, options, contextDesc, task, beans)
                .describe(result);
    }

    @VisibleForTesting
    public @NotNull CorrelationCaseDescription<?> describeCorrelationCase(
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull CorrelationContext correlationContext,
            @NotNull List<ResourceObjectOwnerOptionType> ownerOptionsList,
            @Nullable CorrelationCaseDescriptionOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return new CorrelationCaseDescriber<>(
                correlatorContext, correlationContext, ownerOptionsList, options, "test", task, beans)
                .describe(result);
    }

    @Override
    public void completeCorrelationCase(
            @NotNull CaseType currentCase,
            @NotNull CaseCloser caseCloser,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        correlationCaseManager.completeCorrelationCase(currentCase, caseCloser, task, result);
    }

    /**
     * Resolves the given correlation case - in the correlator.
     * (For majority of correlators this is no-op. See {@link Correlator#resolve(CaseType, String, Task, OperationResult)}.)
     *
     * Note that {@link CaseType#getOutcome()} must not be null.
     */
    void resolve(
            @NotNull CaseType aCase,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_RESOLVE);
        try {
            // We ignore the correlation context; but the overhead of creating it is negligible. Later we may improve this.
            CompleteContext ctx = getCompleteContext(aCase, task, result);
            correlatorFactoryRegistry
                    .instantiateCorrelator(ctx.correlatorContext, task, result)
                    .resolve(aCase, aCase.getOutcome(), task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @VisibleForTesting
    public <F extends FocusType> @NotNull F computePreFocus(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceType resource,
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull Class<F> focusClass,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        SimplePreInboundsContextImpl<F> preInboundsContext = new SimplePreInboundsContextImpl<>(
                shadowedResourceObject,
                resource,
                PrismContext.get().createObjectable(focusClass),
                ObjectTypeUtil.asObjectable(systemObjectCache.getSystemConfiguration(result)),
                task,
                synchronizationPolicy.getObjectTypeDefinition(),
                beans);
        new PreMappingsEvaluation<>(preInboundsContext, beans)
                .evaluate(result);
        return preInboundsContext.getPreFocus();
    }

    /**
     * Creates {@link CompleteContext} from the given {@link CaseType}.
     *
     * Note that this method intentionally ignores pre-focus stored in the case,
     * and computes it from scratch.
     */
    private @NotNull CompleteContext getCompleteContext(
            @NotNull CaseType correlationCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ShadowType shadow = CorrelatorUtil.getShadowFromCorrelationCase(correlationCase);
        beans.provisioningService.applyDefinition(shadow.asPrismObject(), task, result);
        return getCompleteContext(shadow, task, result);
    }

    private @NotNull CompleteContext getCompleteContext(
            @NotNull ShadowType shadow,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        String resourceOid = ShadowUtil.getResourceOidRequired(shadow);
        ResourceType resource =
                beans.provisioningService
                        .getObject(ResourceType.class, resourceOid, null, task, result)
                        .asObjectable();

        ShadowKindType kind = shadow.getKind();
        String intent = shadow.getIntent();

        stateCheck(ShadowUtil.isClassified(kind, intent), "Shadow %s is not classified: %s/%s", shadow, kind, intent);

        SynchronizationPolicy policy =
                MiscUtil.requireNonNull(
                        SynchronizationPolicyFactory.forKindAndIntent(kind, intent, resource),
                        () -> new IllegalStateException(
                                String.format("No %s/%s (kind/intent) type and synchronization definition in %s (for %s)",
                                        kind, intent, resource, shadow)));

        FocusType preFocus = computePreFocus(shadow, resource, policy, policy.getFocusClass(), task, result);

        return CompleteContext.forShadow(
                shadow,
                resource,
                policy.getObjectTypeDefinition(),
                policy,
                preFocus,
                determineObjectTemplate(policy.getArchetypeOid(), preFocus, task, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)),
                task);
    }

    private @NotNull CompleteContext getCompleteContext(
            @NotNull FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {

        // FIXME implement determination of the correlation definition
        CorrelationDefinitionType correlationDefinitionBean = new CorrelationDefinitionType();

        return CompleteContext.forFocus(
                correlationDefinitionBean,
                preFocus,
                determineObjectTemplate(null, preFocus, task, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)),
                task);
    }

    /**
     * Creates the root correlator context for given configuration.
     */
    public @NotNull CorrelatorContext<?> createRootCorrelatorContext(
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration) throws ConfigurationException, SchemaException {
        return CorrelatorContextCreator.createRootContext(
                synchronizationPolicy.getCorrelationDefinition(),
                objectTemplate,
                systemConfiguration);
    }

    /**
     * Clears the correlation state of a shadow.
     *
     * Does not do unlinking (if the shadow is linked)!
     *
     * Only for testing.
     */
    @VisibleForTesting // TODO consider what to do with this method
    public void clearCorrelationState(@NotNull String shadowOid, @NotNull OperationResult result)
            throws ObjectNotFoundException {
        try {
            repositoryService.modifyObject(
                    ShadowType.class,
                    shadowOid,
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_CORRELATION).replace()
                            .asItemDeltas(),
                    result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            throw SystemException.unexpected(e, "when clearing shadow correlation state");
        }
    }

    /**
     * Determines object template from pre-focus or from archetype reference.
     *
     * In the future we may allow explicit configuration of the template ref in the `correlation` section.
     *
     * TODO find better place for this method - it most probably does not belong here
     *
     * @param explicitArchetypeOid If present, it overrides the archetype OID from the pre-focus (that may or may not be there).
     *                             Used for shadow correlation when specified by the synchronization policy.
     */
    public ObjectTemplateType determineObjectTemplate(
            @Nullable String explicitArchetypeOid,
            @NotNull FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ArchetypePolicyType policy;
        if (explicitArchetypeOid != null) {
            policy = beans.archetypeManager.getPolicyForArchetype(explicitArchetypeOid, result);
        } else {
            policy = beans.archetypeManager.determineArchetypePolicy(preFocus, result);
        }
        LOGGER.trace("Determined archetype policy: {} (explicit archetype OID is: {})", policy, explicitArchetypeOid);
        String oid = policy != null ? getOid(policy.getObjectTemplateRef()) : null;
        LOGGER.trace("Determined archetype OID: {}", oid);
        return oid != null ?
                beans.archetypeManager.getExpandedObjectTemplate(oid, task.getExecutionMode(), result) : null;
    }

    /** Contains both {@link CorrelatorContext} and {@link CorrelationContext}. Usually we create both of them together. */
    private record CompleteContext(
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull CorrelationContext correlationContext) {

        static CompleteContext forShadow(
                @NotNull ShadowType shadow,
                @NotNull ResourceType resource,
                @NotNull ResourceObjectDefinition resourceObjectDefinition,
                @NotNull SynchronizationPolicy synchronizationPolicy,
                @NotNull FocusType preFocus,
                @Nullable ObjectTemplateType objectTemplate,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull Task task)
                throws SchemaException, ConfigurationException {
            var correlatorContext =
                    CorrelatorContextCreator.createRootContext(
                            synchronizationPolicy.getCorrelationDefinition(),
                            objectTemplate,
                            systemConfiguration);
            var correlationContext =
                    new CorrelationContext.Shadow(
                            shadow,
                            resource,
                            resourceObjectDefinition,
                            preFocus,
                            systemConfiguration,
                            task);
            return new CompleteContext(correlatorContext, correlationContext);
        }

        static CompleteContext forFocus(
                @NotNull CorrelationDefinitionType correlationDefinitionBean,
                @NotNull FocusType preFocus,
                @Nullable ObjectTemplateType objectTemplate,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull Task task)
                throws SchemaException, ConfigurationException {
            var correlatorContext =
                    CorrelatorContextCreator.createRootContext(
                            correlationDefinitionBean,
                            objectTemplate,
                            systemConfiguration);
            var correlationContext =
                    new CorrelationContext.Focus(
                            preFocus,
                            systemConfiguration,
                            task);
            return new CompleteContext(correlatorContext, correlationContext);
        }
    }


    @Override
    public CorrelatorConfiguration determineCorrelatorConfiguration(@NotNull ObjectTemplateType objectTemplate,
            SystemConfigurationType systemConfiguration) {
        try {
            return CorrelatorContextCreator.createRootContext(new CorrelationDefinitionType(),
                    objectTemplate, systemConfiguration).getConfiguration();
        } catch (SchemaException|ConfigurationException ex) {
            //todo
        }
        return null;
    }

}
