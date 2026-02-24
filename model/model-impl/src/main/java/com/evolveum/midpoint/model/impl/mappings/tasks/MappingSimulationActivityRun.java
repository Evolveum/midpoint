/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.DefaultSingleShadowInboundsProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.model.impl.simulation.MappingSimulationData;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchemaExtender;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MappingSimulationActivityRun extends SearchBasedActivityRun<ShadowType, MappingWorkDefinition, MappingSimulationActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingSimulationActivityRun.class);

    private final ProvisioningService provisioningService;
    private final CorrelationService correlationService;
    private final SystemObjectCache systemObjectCache;
    private final PrismContext prismContext;

    private final List<InlineMappingDefinitionType> mappings;
    private final boolean excludeExistingMappings;
    private final String resourceOid;
    private final ResourceObjectTypeIdentification objectTypeId;

    private ResourceType resource;
    private ResourceObjectTypeDefinition objectTypeDefinition;
    private @Nullable SystemConfigurationType systemConfigurationBean;

    public MappingSimulationActivityRun(
            ActivityRunInstantiationContext<MappingWorkDefinition, MappingSimulationActivityHandler> ctx,
            ProvisioningService provisioningService, CorrelationService correlationService,
            SystemObjectCache systemObjectCache, PrismContext prismContext) {
        super(ctx, "Mapping Simulation");
        this.provisioningService = provisioningService;
        this.correlationService = correlationService;
        this.systemObjectCache = systemObjectCache;
        this.prismContext = prismContext;

        final MappingWorkDefinition workDefinition = ctx.getActivity().getWorkDefinition();
        this.mappings = workDefinition.provideMappings();
        this.excludeExistingMappings = workDefinition.excludeExistingMappings();
        this.resourceOid = workDefinition.resourceOid();
        this.objectTypeId = workDefinition.resolveObjectTypeId();
        setInstanceReady();
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

        this.resource = this.provisioningService.getObject(ResourceType.class, this.resourceOid, null, getRunningTask(),
                result).asObjectable();
        this.objectTypeDefinition = adjustObjectTypeDefinition(this.resource, objectTypeId);
        this.systemConfigurationBean = this.systemObjectCache.getSystemConfigurationBean(result);
        return true;
    }

    @Override
    public boolean processItem(@NotNull ShadowType shadow, @NotNull ItemProcessingRequest<ShadowType> request,
            RunningTask task, OperationResult result) throws CommonException {

        final FocusType targetFocus;
        try {
            targetFocus = this.correlationService.findLinkedOrCorrelatedFocus(shadow, result)
                    .orElseGet(emptyFocusSupplier(shadow));
        } catch (TunnelException e) {
            throw new SchemaException(e.getMessage(), e.getCause());
        }

        final OperationResult evaluationResult = result.createSubresult("Evaluation of inbound mappings on shadow "
                + shadow);
        final ObjectDelta<FocusType> objectDelta;
        try {
            final Collection<ItemDelta<?, ?>> deltas = evaluateMappings(shadow, targetFocus, evaluationResult, task);
            if (!deltas.isEmpty()) {
                objectDelta = (ObjectDelta<FocusType>) targetFocus.asPrismObject().createModifyDelta();
                objectDelta.addModifications(deltas);
            } else {
                objectDelta = null;
            }
        } finally {
            evaluationResult.close();
        }

        final SimulationTransaction simulationTransaction = Objects.requireNonNull(getSimulationTransaction(),
                "Required simulation transaction does not exist.");
        simulationTransaction.writeSimulationData(new MappingSimulationData(targetFocus, shadow, objectDelta,
                evaluationResult), task, result);

        return true;
    }

    private @NotNull Supplier<FocusType> emptyFocusSupplier(@NotNull ShadowType shadow) {
        return () -> {
            LOGGER.debug("Shadow {} has no linked, nor correlated owner. Mapping will be simulated with an "
                    + "empty focus.", shadow);
            final QName focusType = this.objectTypeDefinition.getFocusTypeName() != null
                    ? this.objectTypeDefinition.getFocusTypeName()
                    : UserType.COMPLEX_TYPE;
            final Class<FocusType> focusClass = this.prismContext.getSchemaRegistry().determineClassForType(
                    focusType);
            try {
                return this.prismContext.createObjectable(focusClass);
            } catch (SchemaException e) {
                throw new TunnelException("Unable to create empty focus object for simulation of mapping of shadow: "
                        + shadow, e);
            }
        };
    }

    private Collection<ItemDelta<?, ?>> evaluateMappings(ShadowType shadow, FocusType targetFocus,
            OperationResult result, RunningTask task)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        final MappingEvaluationEnvironment evaluationEnvironment = new MappingEvaluationEnvironment(
                "simulating inbounds processing of " + shadow,
                ModelBeans.get().clock.currentTimeXMLGregorianCalendar(), task);

        final DefaultSingleShadowInboundsProcessingContextImpl<FocusType> context =
                new DefaultSingleShadowInboundsProcessingContextImpl<>(
                        AbstractShadow.of(shadow),
                        this.resource,
                        new InboundMappingContextSpecification(
                                objectTypeDefinition.getTypeIdentification(),
                                null,
                                shadow.getTag()),
                        targetFocus,
                        this.systemConfigurationBean,
                        task,
                        objectTypeDefinition,
                        objectTypeDefinition,
                        false);
        return new SingleShadowInboundsProcessing<>(context, evaluationEnvironment)
                .executeToDeltas(result).stream()
                .filter(delta -> !delta.isEmpty())
                .toList();
    }

    private @NotNull ResourceObjectTypeDefinition adjustObjectTypeDefinition(@NotNull ResourceType resource,
            @NotNull ResourceObjectTypeIdentification objectTypeId)
            throws ConfigurationException, SchemaException {

        final ResourceType resourceWithWantedMappings = excludeExistingMappingsIfNeeded(resource);
        final ResourceSchemaExtender resourceSchemaExtender = ResourceSchemaFactory.schemaExtenderFor(
                resourceWithWantedMappings);

        for (InlineMappingDefinitionType mapping : this.mappings) {
            final ResourceAttributeDefinitionType attrDef = new ResourceAttributeDefinitionType().ref(
                    mapping.getRef());
            // Without the cloning it throws exception about resetting parent of a value.
            CloneUtil.cloneMembersToCollection(attrDef.getInbound(), mapping.getInbound());
            resourceSchemaExtender.addAttributeDefinition(objectTypeId, attrDef);
        }
        return resourceSchemaExtender.extend().getObjectTypeDefinitionRequired(objectTypeId);
    }

    /**
     * Removes existing mappings from the resource, if the exclusion flag is set to true.
     *
     * This method intentionally removes mappings from **all** object types, to be sure we remove also mappings from
     * whole object type inheritance hierarchy (if present).
     *
     * @param resource The resource from which you want to exclude existing mappings.
     * @return The clone of the provided resource with excluded mappings, or the same instance as was provided if the
     * exclusion is not desired.
     */
    private ResourceType excludeExistingMappingsIfNeeded(@NotNull ResourceType resource) {
        if (!this.excludeExistingMappings) {
            return resource;
        } else {
            final ResourceType clonedResource = resource.clone();
            clonedResource.getSchemaHandling().getObjectType().stream()
                    .flatMap(objectType -> objectType.getAttribute().stream())
                    .forEach(attr -> {
                        if (!attr.getInbound().isEmpty()) {
                            attr.getInbound().clear();
                        }
                    });
            return clonedResource;
        }
    }

}
