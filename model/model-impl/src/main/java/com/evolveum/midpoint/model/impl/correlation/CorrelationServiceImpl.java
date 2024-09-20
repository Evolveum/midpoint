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
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.*;
import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorFactoryRegistryImpl;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
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
            @Nullable String archetypeOid,
            @NotNull Set<String> candidateOids,
            @NotNull CorrelatorDiscriminator discriminator,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        CompleteContext ctx = getCompleteContext(preFocus, archetypeOid, candidateOids, discriminator, task, result);
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
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return createCompleteResult(
                correlateInternal(rootCorrelatorContext, correlationContext, result),
                rootCorrelatorContext);
    }

    /**
     * As {@link #correlate(CorrelatorContext, CorrelationContext, OperationResult)} but does not end with
     * {@link CompleteCorrelationResult}, only {@link SimplifiedCorrelationResult}.
     * Useful for sub-object correlations.
     */
    public @NotNull SimplifiedCorrelationResult correlateLimited(
            @NotNull CorrelatorContext<?> rootCorrelatorContext,
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return createSimplifiedResult(
                correlateInternal(rootCorrelatorContext, correlationContext, result),
                rootCorrelatorContext);
    }

    /** The correlation itself. */
    private @NotNull CorrelationResult correlateInternal(
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
            return correlatorFactoryRegistry
                    .instantiateCorrelator(rootCorrelatorContext, correlationContext.getTask(), result)
                    .correlate(correlationContext, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private static @NotNull CompleteCorrelationResult createCompleteResult(
            CorrelationResult correlationResult, CorrelatorContext<?> correlatorContext) {
        CandidateOwners candidateOwners = correlationResult.getCandidateOwners();
        double definiteThreshold = correlatorContext.getDefiniteThreshold();
        Collection<CandidateOwner.ObjectBased> owners =
                CandidateOwner.ensureObjectBased(candidateOwners.selectWithConfidenceAtLeast(definiteThreshold));
        double candidateThreshold = correlatorContext.getCandidateThreshold();
        Collection<CandidateOwner.ObjectBased> eligibleCandidates =
                CandidateOwner.ensureObjectBased(candidateOwners.selectWithConfidenceAtLeast(candidateThreshold));

        LOGGER.debug("Determining overall result with 'definite' threshold of {}, candidate threshold of {}, "
                        + "definite (owner) candidates: {}, eligible candidates: {}, all candidates:\n{}",
                definiteThreshold, candidateThreshold, owners.size(), eligibleCandidates.size(),
                DebugUtil.toStringCollectionLazy(candidateOwners.values(), 1));

        ResourceObjectOwnerOptionsType optionsBean = new ResourceObjectOwnerOptionsType();
        for (CandidateOwner.ObjectBased eligibleCandidate : eligibleCandidates) {
            String candidateId = Objects.requireNonNullElse(
                    eligibleCandidate.getExternalId(),
                    eligibleCandidate.getOid());
            optionsBean.getOption().add(
                    new ResourceObjectOwnerOptionType()
                            .identifier(
                                    OwnerOptionIdentifier.forExistingOwner(candidateId).getStringValue())
                            .candidateOwnerRef(
                                    ObjectTypeUtil.createObjectRef(eligibleCandidate.getValue()))
                            .confidence(
                                    eligibleCandidate.getConfidence()));
        }
        if (owners.size() != 1) {
            optionsBean.getOption().add(
                    new ResourceObjectOwnerOptionType()
                            .identifier(OwnerOptionIdentifier.forNoOwner().getStringValue()));
        }

        CandidateOwners finalCandidates = CandidateOwners.from(eligibleCandidates);
        if (owners.size() == 1) {
            ObjectType owner = owners.iterator().next().getValue();
            return CompleteCorrelationResult.existingOwner(owner, finalCandidates, optionsBean);
        } else if (eligibleCandidates.isEmpty()) {
            return CompleteCorrelationResult.noOwner();
        } else {
            return CompleteCorrelationResult.uncertain(finalCandidates, optionsBean);
        }
    }

    private static @NotNull SimplifiedCorrelationResult createSimplifiedResult(
            CorrelationResult correlationResult, CorrelatorContext<?> correlatorContext) {
        CandidateOwners candidateOwners = correlationResult.getCandidateOwners();
        var owners = candidateOwners.selectWithConfidenceAtLeast(correlatorContext.getDefiniteThreshold());
        var eligibleCandidates = candidateOwners.selectWithConfidenceAtLeast(correlatorContext.getCandidateThreshold());

        LOGGER.debug("Determining overall simplified result with 'definite' threshold of {}, candidate threshold of {}, "
                        + "definite (owner) candidates: {}, eligible candidates: {}, all candidates:\n{}",
                correlatorContext.getDefiniteThreshold(), correlatorContext.getCandidateThreshold(), owners.size(), eligibleCandidates.size(),
                DebugUtil.toStringCollectionLazy(candidateOwners.values(), 1));

        if (owners.size() == 1) {
            return SimplifiedCorrelationResult.existingOwner(
                    owners.iterator().next().getValue());
        } else if (eligibleCandidates.isEmpty()) {
            return SimplifiedCorrelationResult.noOwner();
        } else {
            return SimplifiedCorrelationResult.uncertain(eligibleCandidates);
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
        FocusType preFocus = PreMappingsEvaluator.computePreFocus(
                shadowedResourceObject, synchronizationPolicy.getObjectTypeDefinition(), resource,
                candidateOwner.getClass(), task, result);
        CompleteContext ctx = CompleteContext.forShadow(
                shadowedResourceObject,
                resource,
                synchronizationPolicy.getObjectTypeDefinition(),
                synchronizationPolicy,
                preFocus,
                determineObjectTemplate(synchronizationPolicy.getArchetypeOid(), preFocus, null, task, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)),
                CorrelatorDiscriminator.forSynchronization(),
                task);
        double confidence = correlatorFactoryRegistry
                .instantiateCorrelator(ctx.correlatorContext, task, result)
                .checkCandidateOwner(ctx.correlationContext, candidateOwner, result)
                .getValue();
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

        FocusType preFocus = PreMappingsEvaluator.computePreFocus(
                shadow, policy.getObjectTypeDefinition(), resource, policy.getFocusClass(), task, result);

        return CompleteContext.forShadow(
                shadow,
                resource,
                policy.getObjectTypeDefinition(),
                policy,
                preFocus,
                determineObjectTemplate(policy.getArchetypeOid(), preFocus, null, task, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)),
                CorrelatorDiscriminator.forSynchronization(),
                task);
    }

    private @NotNull CompleteContext getCompleteContext(
            @NotNull FocusType preFocus,
            @Nullable String archetypeOid,
            @NotNull Set<String> candidateOids,
            @NotNull CorrelatorDiscriminator discriminator,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {

        // FIXME implement determination of the correlation definition
        CorrelationDefinitionType correlationDefinitionBean = new CorrelationDefinitionType();

        return CompleteContext.forFocus(
                correlationDefinitionBean,
                preFocus,
                archetypeOid,
                candidateOids,
                determineObjectTemplate(archetypeOid, preFocus, null, task, result),
                systemObjectCache.getSystemConfigurationBean(result),
                discriminator,
                task);
    }

    /**
     * Creates the root correlator context for given configuration.
     */
    public @NotNull CorrelatorContext<?> createRootCorrelatorContext(
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @Nullable ObjectTemplateType objectTemplate,
            @NotNull CorrelatorDiscriminator discriminator,
            @Nullable SystemConfigurationType systemConfiguration) throws ConfigurationException, SchemaException {
        return CorrelatorContextCreator.createRootContext(
                synchronizationPolicy.getCorrelationDefinition(),
                discriminator,
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
    public <F extends FocusType> ObjectTemplateType determineObjectTemplate(
            @Nullable String explicitArchetypeOid,
            @Nullable FocusType preFocus,
            @Nullable Class<F> objectType,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ArchetypePolicyType policy = null;
        if (explicitArchetypeOid != null) {
            policy = beans.archetypeManager.getPolicyForArchetype(explicitArchetypeOid, result);
        } else if (preFocus != null) {
            policy = beans.archetypeManager.determineArchetypePolicy(preFocus, result);
        } else if (objectType != null) {
            policy =beans.archetypeManager.determineObjectPolicyConfiguration(objectType, result);
        }
        LOGGER.trace("Determined archetype policy: {} (explicit archetype OID is: {})", policy, explicitArchetypeOid);
        String oid = policy != null ? getOid(policy.getObjectTemplateRef()) : null;
        LOGGER.trace("Determined object template OID: {}", oid);
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
                @NotNull CorrelatorDiscriminator discriminator,
                @NotNull Task task)
                throws SchemaException, ConfigurationException {
            var correlatorContext =
                    CorrelatorContextCreator.createRootContext(
                            synchronizationPolicy.getCorrelationDefinition(),
                            discriminator,
                            objectTemplate,
                            systemConfiguration);
            var correlationContext =
                    new CorrelationContext.Shadow(
                            shadow,
                            resource,
                            resourceObjectDefinition,
                            preFocus,
                            null,
                            systemConfiguration,
                            task);
            return new CompleteContext(correlatorContext, correlationContext);
        }

        static CompleteContext forFocus(
                @NotNull CorrelationDefinitionType correlationDefinitionBean,
                @NotNull FocusType preFocus,
                @Nullable String archetypeOid,
                @NotNull Set<String> candidateOids,
                @Nullable ObjectTemplateType objectTemplate,
                @Nullable SystemConfigurationType systemConfiguration,
                @NotNull CorrelatorDiscriminator discriminator,
                @NotNull Task task)
                throws SchemaException, ConfigurationException {
            var correlatorContext =
                    CorrelatorContextCreator.createRootContext(
                            correlationDefinitionBean,
                            discriminator,
                            objectTemplate,
                            systemConfiguration);
            var correlationContext =
                    new CorrelationContext.Focus(
                            preFocus,
                            archetypeOid,
                            candidateOids,
                            systemConfiguration,
                            task);
            return new CompleteContext(correlatorContext, correlationContext);
        }
    }

    //TODO to many parameters, refactor to some context object.
    @Override
    public PathSet determineCorrelatorConfiguration(
            @NotNull CorrelatorDiscriminator discriminator,
            @Nullable String archetypeOid,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        //todo how to deal the situation when archetypeOid is null ? we need to take correlators
        // from the default object template. should determineObjectTemplate method cope with this situation?
        // and while getting default object template do we consider UserType to be default?

        ObjectTemplateType template = determineObjectTemplate(archetypeOid, null, UserType.class, task, result);
        CorrelatorContext<?> ctx = CorrelatorContextCreator.createRootContext(
                null,
                discriminator,
                template,
                systemObjectCache.getSystemConfigurationBean(result));

        return ctx.getConfiguration().getCorrelationItemPaths();
    }
}
