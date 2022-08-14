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

import java.util.List;
import java.util.Objects;

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
import com.evolveum.midpoint.model.impl.correlator.FullCorrelationContext;
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

@Component
public class CorrelationServiceImpl implements CorrelationService {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationServiceImpl.class);

    private static final String OP_CORRELATE = CorrelationServiceImpl.class.getName() + ".correlate";

    @Autowired ModelBeans beans;
    @Autowired CorrelatorFactoryRegistry correlatorFactoryRegistry;
    @Autowired SystemObjectCache systemObjectCache;
    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;

    @Override
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
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public @NotNull CompleteCorrelationResult correlate(
            @NotNull ShadowType shadowedResourceObject,
            @Nullable FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        FullCorrelationContext fullContext = getFullCorrelationContext(shadowedResourceObject, task, result);
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext, beans);
        CorrelationContext correlationContext = createCorrelationContext(fullContext, task, result);
        return correlate(correlatorContext, correlationContext, result);
    }

    private @NotNull CompleteCorrelationResult createCompleteResult(
            CorrelationResult correlationResult, CorrelatorContext<?> correlatorContext) {
        CandidateOwnersMap candidateOwnersMap = correlationResult.getCandidateOwnersMap();
        double ownerThreshold = correlatorContext.getOwnerThreshold();
        var owners =
                candidateOwnersMap.selectWithConfidenceAtLeast(
                        ownerThreshold);
        double candidateThreshold = correlatorContext.getCandidateThreshold();
        var eligibleCandidates =
                candidateOwnersMap.selectWithConfidenceAtLeast(
                        candidateThreshold);

        LOGGER.info("Determining overall result with owner threshold of {}, candidate threshold of {}, "
                        + "owners: {}, eligible candidates: {}, all candidates:\n{}",
                ownerThreshold, candidateThreshold, owners.size(), eligibleCandidates.size(),
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

    @Override
    public @NotNull CompleteCorrelationResult correlate(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceType resource,
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull Class<? extends FocusType> focusType,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        SimplePreInboundsContextImpl<?> preInboundsContext = new SimplePreInboundsContextImpl<>(
                shadowedResourceObject,
                resource,
                PrismContext.get().createObjectable(focusType),
                ObjectTypeUtil.asObjectable(systemObjectCache.getSystemConfiguration(result)),
                task,
                synchronizationPolicy.getObjectTypeDefinition(),
                beans);
        new PreMappingsEvaluation<>(preInboundsContext, beans)
                .evaluate(result);

        return correlate(shadowedResourceObject, preInboundsContext.getPreFocus(), task, result);
    }

    CorrelationExplanation explain(
            @NotNull FocusType candidate,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull CorrelationContext correlationContext,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return correlatorFactoryRegistry
                .instantiateCorrelator(correlatorContext, task, result)
                .explain(correlationContext, candidate, result);
    }

    @Override
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
        FullCorrelationContext fullContext = new FullCorrelationContext(
                shadowedResourceObject,
                resource,
                synchronizationPolicy.getObjectTypeDefinition(),
                synchronizationPolicy,
                preFocus,
                determineObjectTemplate(synchronizationPolicy, preFocus, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)));
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext, beans);
        CorrelationContext correlationContext = createCorrelationContext(fullContext, task, result);
        double confidence = correlatorFactoryRegistry
                .instantiateCorrelator(correlatorContext, task, result)
                .checkCandidateOwner(correlationContext, candidateOwner, result);
        return confidence >= correlatorContext.getOwnerThreshold();
    }

    @Override
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

    /** Ignores pre-focus stored in the case. TODO is that OK? */
    private @NotNull FullCorrelationContext getFullCorrelationContext(
            @NotNull CaseType aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        ShadowType shadow = CorrelatorUtil.getShadowFromCorrelationCase(aCase);
        beans.provisioningService.applyDefinition(shadow.asPrismObject(), task, result);
        return getFullCorrelationContext(shadow, task, result);
    }

    private @NotNull FullCorrelationContext getFullCorrelationContext(
            @NotNull ShadowType shadow,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        String resourceOid = ShadowUtil.getResourceOidRequired(shadow);
        ResourceType resource =
                beans.provisioningService.getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();

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
        return new FullCorrelationContext(
                shadow,
                resource,
                policy.getObjectTypeDefinition(),
                policy,
                preFocus,
                determineObjectTemplate(policy, preFocus, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)));
    }

    @Override
    public Correlator instantiateCorrelator(
            @NotNull CaseType aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        FullCorrelationContext fullContext = getFullCorrelationContext(aCase, task, result);
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext, beans);
        return correlatorFactoryRegistry.instantiateCorrelator(correlatorContext, task, result);
    }

    private CorrelationContext createCorrelationContext(
            @NotNull FullCorrelationContext fullContext,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException {
        return new CorrelationContext(
                fullContext.shadow,
                fullContext.preFocus,
                fullContext.resource,
                fullContext.resourceObjectDefinition,
                fullContext.objectTemplate,
                asObjectable(systemObjectCache.getSystemConfiguration(result)),
                task);
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

    @Override
    public @NotNull CorrelatorContext<?> createRootCorrelatorContext(
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration) throws ConfigurationException, SchemaException {
        return CorrelatorContextCreator.createRootContext(
                synchronizationPolicy.getCorrelationDefinition(),
                objectTemplate,
                systemConfiguration,
                beans);
    }

    @Override
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
     */
    @Override
    public ObjectTemplateType determineObjectTemplate(
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull FocusType preFocus,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ArchetypePolicyType policy;
        String explicitArchetypeOid = synchronizationPolicy.getArchetypeOid();
        if (explicitArchetypeOid != null) {
            policy = beans.archetypeManager.getPolicyForArchetype(explicitArchetypeOid, result);
        } else {
            policy = beans.archetypeManager.determineArchetypePolicy(preFocus, result);
        }
        LOGGER.trace("Determined archetype policy: {} (explicit archetype OID is: {})", policy, explicitArchetypeOid);
        String oid = policy != null ? getOid(policy.getObjectTemplateRef()) : null;
        LOGGER.trace("Determined archetype OID: {}", oid);
        return oid != null ?
                beans.archetypeManager.getExpandedObjectTemplate(oid, result) : null;
    }

    @Override
    public @NotNull CorrelationCaseDescription<?> describeCorrelationCase(
            @NotNull CaseType aCase,
            @Nullable CorrelationCaseDescriptionOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        FullCorrelationContext fullContext = getFullCorrelationContext(aCase, task, result);
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext, beans);
        CorrelationContext correlationContext = createCorrelationContext(fullContext, task, result);
        List<ResourceObjectOwnerOptionType> ownerOptionsList = CorrelationCaseUtil.getOwnerOptionsList(aCase);
        String contextDesc = "correlation case " + aCase;
        return new CorrelationCaseDescriber<>(
                correlatorContext, correlationContext, ownerOptionsList, options, contextDesc, task, beans)
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
}
