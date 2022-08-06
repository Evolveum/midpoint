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

import com.evolveum.midpoint.model.api.CorrelationProperty;
import com.evolveum.midpoint.model.impl.correlator.FullCorrelationContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SimplePreInboundsContextImpl;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluation;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;

import com.evolveum.midpoint.schema.processor.SynchronizationPolicyFactory;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Experimental
@Component
public class CorrelationServiceImpl implements CorrelationService {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(CorrelationServiceImpl.class);

    @Autowired ModelBeans beans;
    @Autowired CorrelatorFactoryRegistry correlatorFactoryRegistry;
    @Autowired SystemObjectCache systemObjectCache;
    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;

    @Override
    public @NotNull CorrelationResult correlate(
            @NotNull ShadowType shadowedResourceObject,
            @Nullable FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        FullCorrelationContext fullContext = getFullCorrelationContext(shadowedResourceObject, task, result);
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext);
        CorrelationContext correlationContext = createCorrelationContext(fullContext, preFocus, task, result);
        return correlatorFactoryRegistry
                .instantiateCorrelator(correlatorContext, task, result)
                .correlate(correlationContext, result);
    }

    @Override
    public @NotNull CorrelationResult correlate(
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
                determineObjectTemplate(synchronizationPolicy, preFocus, result),
                asObjectable(systemObjectCache.getSystemConfiguration(result)));
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext);
        CorrelationContext correlationContext = createCorrelationContext(fullContext, preFocus, task, result);
        return correlatorFactoryRegistry
                .instantiateCorrelator(correlatorContext, task, result)
                .checkCandidateOwner(correlationContext, candidateOwner, result);
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
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext);
        return correlatorFactoryRegistry.instantiateCorrelator(correlatorContext, task, result);
    }

    private CorrelationContext createCorrelationContext(
            @NotNull FullCorrelationContext fullContext,
            @Nullable FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException {
        return new CorrelationContext(
                fullContext.shadow,
                preFocus != null ?
                        preFocus :
                        PrismContext.get().createObjectable(
                                fullContext.synchronizationPolicy.getFocusClass()),
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
    public Collection<CorrelationProperty> getCorrelationProperties(
            @NotNull CaseType aCase, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        FullCorrelationContext fullCorrelationContext = getFullCorrelationContext(aCase, task, result);
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullCorrelationContext);
        return new CorrelationPropertiesCreator(correlatorContext, fullCorrelationContext, aCase)
                .createProperties();
    }

    @Override
    public @NotNull CorrelatorContext<?> createRootCorrelatorContext(
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration) throws ConfigurationException, SchemaException {
        return CorrelatorContextCreator.createRootContext(
                synchronizationPolicy.getCorrelationDefinition(),
                objectTemplate,
                systemConfiguration);
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
}
