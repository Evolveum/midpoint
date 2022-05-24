/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;

import java.util.Collection;

import com.evolveum.midpoint.model.api.CorrelationProperty;
import com.evolveum.midpoint.model.impl.correlator.FullCorrelationContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SimplePreInboundsContextImpl;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluation;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;

import com.evolveum.midpoint.schema.processor.SynchronizationPolicyFactory;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
import com.evolveum.midpoint.util.MiscUtil;
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
                synchronizationPolicy.getResourceObjectDefinition(),
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
        Class<? extends FocusType> focusType = candidateOwner.getClass();
        SimplePreInboundsContextImpl<?> preInboundsContext = new SimplePreInboundsContextImpl<>(
                shadowedResourceObject,
                resource,
                PrismContext.get().createObjectable(focusType),
                ObjectTypeUtil.asObjectable(systemObjectCache.getSystemConfiguration(result)),
                task,
                synchronizationPolicy.getResourceObjectDefinition(),
                beans);
        new PreMappingsEvaluation<>(preInboundsContext, beans)
                .evaluate(result);
        FocusType preFocus = preInboundsContext.getPreFocus();

        FullCorrelationContext fullContext = getFullCorrelationContext(shadowedResourceObject, task, result);
        CorrelatorContext<?> correlatorContext = CorrelatorContextCreator.createRootContext(fullContext);
        CorrelationContext correlationContext = createCorrelationContext(fullContext, preFocus, task, result);
        return correlatorFactoryRegistry
                .instantiateCorrelator(correlatorContext, task, result)
                .checkCandidateOwner(correlationContext, candidateOwner, result);
    }

    private @NotNull FullCorrelationContext getFullCorrelationContext(
            @NotNull CaseType aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return getFullCorrelationContext(
                CorrelatorUtil.getShadowFromCorrelationCase(aCase),
                task, result);
    }

    private @NotNull FullCorrelationContext getFullCorrelationContext(ShadowType shadow, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        String resourceOid = ShadowUtil.getResourceOidRequired(shadow);
        ResourceType resource =
                beans.provisioningService.getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();

        // We expect that the shadow is classified + reasonably fresh (= not legacy), so it has kind+intent present.
        ShadowKindType kind = MiscUtil.requireNonNull(shadow.getKind(), () -> new IllegalStateException("No kind in " + shadow));
        String intent = MiscUtil.requireNonNull(shadow.getIntent(), () -> new IllegalStateException("No intent in " + shadow));
        // TODO check for "unknown" ?

        SynchronizationPolicy policy =
                SynchronizationPolicyFactory.forKindAndIntent(kind, intent, resource);
        if (policy == null) {
            throw new IllegalStateException(
                    "No " + kind + "/" + intent + " (kind/intent) type and synchronization definition in " + resource
                            + " (for " + shadow + ")");
        } else {
            return new FullCorrelationContext(
                    shadow,
                    resource,
                    policy.getResourceTypeDefinitionRequired(),
                    policy,
                    asObjectable(systemObjectCache.getSystemConfiguration(result)));
        }
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
                fullContext.typeDefinition,
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
            @Nullable SystemConfigurationType systemConfiguration) throws ConfigurationException, SchemaException {
        return CorrelatorContextCreator.createRootContext(
                synchronizationPolicy.getCorrelationDefinition(),
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
}
