/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.impl.lens.projector.projection.outbounds.OutboundMappingProcessing;
import com.evolveum.midpoint.model.impl.simulation.MappingSimulationData;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity run for the simulation of outbound mappings.
 *
 * NOTE: In order to simulate the mappings against correct (shadow, owner) pairs, this activity runs a correlation
 * process for each shadow in the given resource and object type. This process does not support bucketing, and it
 * is done as whole as part of the {@link SearchBasedActivityRun#beforeRun} method.
 *
 * If the processed focus does not have any projection from the given resource and object type, nor correlated focus,
 * we simulate the mappings with an empty "fake" shadow object.
 *
 * == Limitations
 *
 * Current implementation does not support mapping simulation of more than one attribute and more than one outbound
 * mapping. This is a major difference from the {@link InboundMappingsSimulationActivityRun}.
 */
public class OutboundMappingSimulationActivityRun extends
        SearchBasedActivityRun<FocusType, MappingSimulationWorkDef<OutboundMappingType>,
                OutboundMappingsSimulationActivityHandler, AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundMappingSimulationActivityRun.class);
    private final ProvisioningService provisioningService;
    private final CorrelationService correlationService;
    private final SystemObjectCache systemObjectCache;
    private final OutboundMappingProcessing outboundProcessing;

    private final ItemPath attributeRef;
    private final OutboundMappingType mapping;
    private final String resourceOid;
    private final ResourceObjectTypeIdentification objectTypeId;

    private Map<String, ShadowType> focusShadowMap;
    private Resource resource;
    private @Nullable SystemConfigurationType systemConfigurationBean;

    public OutboundMappingSimulationActivityRun(
            ActivityRunInstantiationContext<MappingSimulationWorkDef<OutboundMappingType>,
                    OutboundMappingsSimulationActivityHandler> context,
            ProvisioningService provisioningService,
            CorrelationService correlationService, SystemObjectCache systemObjectCache,
            OutboundMappingProcessing outboundProcessing) {
        super(context, "Outbound Mapping Simulation");
        this.provisioningService = provisioningService;
        this.correlationService = correlationService;
        this.systemObjectCache = systemObjectCache;
        this.outboundProcessing = outboundProcessing;
        this.focusShadowMap = new HashMap<>();

        final MappingSimulationWorkDef<OutboundMappingType> workDefinition = context.getActivity().getWorkDefinition();
        this.resourceOid = workDefinition.resourceOid();
        this.objectTypeId = workDefinition.resolveObjectTypeId();

        final Map.Entry<ItemPath, List<OutboundMappingType>> attributeMappings = extractMapping(
                workDefinition.provideMappings());
        this.attributeRef = attributeMappings.getKey();
        this.mapping = attributeMappings.getValue().get(0);
        setInstanceReady();
    }

    private Map.Entry<ItemPath, List<OutboundMappingType>> extractMapping(
            Map<ItemPath, List<OutboundMappingType>> mappings) {
        if (mappings.size() > 1) {
            throw new UnsupportedOperationException("Mapping simulation of more than one attribute is not supported.");
        }
        final Map.Entry<ItemPath, List<OutboundMappingType>> attributeMappings = mappings.entrySet().iterator().next();
        if (attributeMappings.getValue().size() > 1) {
            throw new UnsupportedOperationException("Simulation of more than one outbound mapping is not supported");
        }
        return attributeMappings;
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }

        if (!isAnyPreview()) {
            throw new ActivityRunException(
                    "This activity is supported only in preview execution mode",
                    OperationResultStatus.FATAL_ERROR,
                    ActivityRunResultStatus.PERMANENT_ERROR);
        }

        final ResourceType resourceBean = this.provisioningService.getObject(ResourceType.class, this.resourceOid, null,
                getRunningTask(), result).asObjectable();
        this.systemConfigurationBean = this.systemObjectCache.getSystemConfigurationBean(result);
        this.resource = Resource.of(resourceBean);
        final ObjectQuery shadowsQuery = this.resource.queryFor(this.objectTypeId).build();
        this.focusShadowMap = correlateShadows(shadowsQuery, result);

        return true;
    }

    @Override
    public boolean processItem(@NotNull FocusType item, @NotNull ItemProcessingRequest<FocusType> request,
            RunningTask task, OperationResult result) throws CommonException, ActivityRunException {
        final FocusType focus = request.getItem();
        final ShadowType targetShadow = getOrCreateTargetShadow(focus);

        final OperationResult evaluationResult = result.createSubresult("Evaluation of outbound mappings on focus "
                + focus);
        final ObjectDelta<ShadowType> objectDelta;
        try {
            final Collection<ItemDelta<?, ?>> deltas = this.outboundProcessing.executeToDeltas(this.attributeRef,
                    this.mapping, targetShadow, this.resource.getBean(), focus, this.systemConfigurationBean, task,
                    evaluationResult);
            objectDelta = createObjectDelta(deltas, targetShadow);
        } catch (CommonException e) {
            writeErrorSimulationData(targetShadow, evaluationResult, task, result, e);
            throw e;
        } finally {
            if (!evaluationResult.isClosed()) {
                evaluationResult.close();
            }
        }

        final SimulationTransaction simulationTransaction = Objects.requireNonNull(getSimulationTransaction(),
                "Required simulation transaction does not exist.");
        simulationTransaction.writeSimulationData(new MappingSimulationData<>(targetShadow, objectDelta,
                evaluationResult), task, result);

        return true;
    }

    private @Nullable ObjectDelta<ShadowType> createObjectDelta(Collection<ItemDelta<?, ?>> deltas,
            ShadowType targetShadow) {
        if (deltas.isEmpty()) {
            return null;
        }
        final ObjectDelta<ShadowType> objectDelta = targetShadow.asPrismObject().createModifyDelta();
        objectDelta.addModifications(deltas);
        return objectDelta;
    }

    private void writeErrorSimulationData(ShadowType targetShadow, OperationResult evaluationResult,
            RunningTask task, OperationResult result, CommonException cause) throws CommonException {
        // Result must be closed before writing simulation data because close() calls computeStatus() which
        // propagates error message from sub results to root result
        evaluationResult.close();
        final SimulationTransaction simulationTransaction = Objects.requireNonNull(getSimulationTransaction(),
                "Required simulation transaction does not exist.");
        try {
            simulationTransaction.writeSimulationData(
                    new MappingSimulationData<>(targetShadow, null, evaluationResult), task, result);
        } catch (Exception writeException) {
            cause.addSuppressed(writeException);
        }
        throw cause;
    }

    private ShadowType getOrCreateTargetShadow(FocusType focus) {
        final String focusOid = focus.getOid();
        if (this.focusShadowMap.containsKey(focusOid)) {
            return this.focusShadowMap.get(focusOid);
        } else {
            return new ShadowType()
                    .resourceRef(this.resourceOid, ResourceType.COMPLEX_TYPE)
                    .intent(this.objectTypeId.getIntent())
                    .kind(this.objectTypeId.getKind());
        }
    }

    private Map<String, ShadowType> correlateShadows(ObjectQuery shadowsQuery, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, SubscriptionComplianceException {
        final Map<String, ShadowType> focusesToShadows = new HashMap<>();
        this.provisioningService.searchShadowsIterative(shadowsQuery, SelectorOptions.createCollection(
                GetOperationOptions.createReadOnly()), findOwner(focusesToShadows), getRunningTask(), result);
        return focusesToShadows;
    }

    private ObjectHandler<AbstractShadow> findOwner(Map<String, ShadowType> focusesToShadows) {
        return (shadow, lResult) -> {
            try {
                this.correlationService
                        .findLinkedOrCorrelatedFocus(shadow.getBean(), this.resource.getBean(), getRunningTask(),
                                lResult)
                        .map(FocusType::getOid)
                        .ifPresent(correlatedFocusOid -> focusesToShadows.put(correlatedFocusOid, shadow.getBean()));
            } catch (CommonException e) {
                LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, shadow);
            } finally {
                lResult.computeStatusIfUnknown();
                lResult.setSummarizeSuccesses(true);
                lResult.summarize();
            }
            return true;
        };
    }
}
