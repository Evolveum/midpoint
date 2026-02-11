/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.DefaultSingleShadowInboundsProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.model.impl.simulation.FocusSimulationData;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ShadowSimulationData;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchemaExtender;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MappingActivityRun extends SearchBasedActivityRun<ShadowType, MappingWorkDefinition, MappingActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingActivityRun.class);

    private final PrismContext prismContext;
    private final ProvisioningService provisioningService;
    private final List<InlineMappingDefinitionType> mappings;
    private final boolean excludeExistingMappings;

    public MappingActivityRun(
            ActivityRunInstantiationContext<MappingWorkDefinition, MappingActivityHandler> ctx,
            ProvisioningService provisioningService, PrismContext prismContext) {
        super(ctx, "Mapping Simulation");
        this.prismContext = prismContext;
        this.provisioningService = provisioningService;
        this.mappings = ctx.getActivity().getWorkDefinition().provideMappings();
        this.excludeExistingMappings = ctx.getActivity().getWorkDefinition().excludeExistingMappings();
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

        return true;
    }

    @Override
    public boolean processItem(@NotNull ShadowType shadow, @NotNull ItemProcessingRequest<ShadowType> request,
            RunningTask task, OperationResult result) throws CommonException {
        final ResourceType resource = getResource(shadow, task, result);
        ResourceObjectTypeIdentification objectTypeId = ResourceObjectTypeIdentification.of(shadow.getKind(), shadow.getIntent());

        var objectTypeDefinition = getObjectTypeDefinition(resource, objectTypeId);

        List<PrismObject<FocusType>> linkedFocuses = findOwner(shadow, result);

        // TODO Should we instead return `true` here, what basically skip this shadow, or throw an exception? Because
        //  this approach of taking the first focus does not seem correct to me.
        if (linkedFocuses.size() > 1) {
            LOGGER.trace("Multiple focuses ({}) linked to shadow {}, using first one", linkedFocuses.size(), shadow);
        }

        if (linkedFocuses.isEmpty()) {
            // TODO here we should probably add some new mark about shadows with missing focus.
            LOGGER.debug("Mappings of shadow {} are skipped, because shadow is not linked with any focus.", shadow);
            return true;
        }

        FocusType targetFocus = linkedFocuses.get(0).asObjectable();

        final MappingEvaluationEnvironment evaluationEnvironment = new MappingEvaluationEnvironment(
                "simulating inbounds processing of " + shadow,
                ModelBeans.get().clock.currentTimeXMLGregorianCalendar(), task);

        final DefaultSingleShadowInboundsProcessingContextImpl<FocusType> context =
                new DefaultSingleShadowInboundsProcessingContextImpl<>(
                        AbstractShadow.of(shadow),
                        resource,
                        new InboundMappingContextSpecification(
                                objectTypeDefinition.getTypeIdentification(),
                                null,
                                shadow.getTag()),
                        targetFocus,
                        ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                        task,
                        objectTypeDefinition,
                        objectTypeDefinition,
                        false);
        final Collection<ItemDelta<?, ?>> deltas = new SingleShadowInboundsProcessing<>(context, evaluationEnvironment)
                .executeToDeltas(result).stream()
                .filter(delta -> !delta.isEmpty())
                .toList();
        final ObjectDelta<FocusType> objectDelta;
        if (!deltas.isEmpty()) {
            objectDelta = (ObjectDelta<FocusType>) targetFocus.asPrismObject().createModifyDelta();
            objectDelta.addModifications(deltas);
        } else {
            objectDelta = null;
        }

        final SimulationTransaction simulationTransaction = getSimulationTransaction();
        if (simulationTransaction != null) {
            simulationTransaction.writeSimulationData(ShadowSimulationData.of(shadow, null), task, result);
            //if (objectDelta != null) {
                simulationTransaction.writeSimulationData(new FocusSimulationData(targetFocus, objectDelta), task,
                        result);
            //}
        }

        return true;
    }

    private List<PrismObject<FocusType>> findOwner(ShadowType shadow, OperationResult result) throws CommonException {

        ObjectQuery query = this.prismContext.queryFor(FocusType.class)
                .item(FocusType.F_LINK_REF)
                .ref(shadow.getOid())
                .build();

        final SearchResultList<PrismObject<FocusType>> linkedFocuses = getBeans().repositoryService.searchObjects(
                FocusType.class, query, null, result);
        if (!linkedFocuses.isEmpty()) {
            return linkedFocuses;
        }

        final String ownerCandidateOid = Optional.ofNullable(shadow.getCorrelation())
                .map(ShadowCorrelationStateType::getResultingOwner)
                .map(AbstractReferencable::getOid)
                .orElse(null);
        if (ownerCandidateOid == null) {
            return Collections.emptyList();
        }
        return List.of(getBeans().repositoryService.getObject(FocusType.class, ownerCandidateOid, null, result));
    }


    private @NotNull ResourceObjectTypeDefinition getObjectTypeDefinition(@NotNull ResourceType resource,
            @NotNull ResourceObjectTypeIdentification objectTypeId)
            throws ConfigurationException, SchemaException {

        final ResourceType clonedResource = resource.clone();
        if (this.excludeExistingMappings) {
            clonedResource.getSchemaHandling().getObjectType().stream()
                    .filter(objectType -> objectType.getKind() == objectTypeId.getKind()
                            && objectTypeId.getIntent().equals(objectType.getIntent()))
                    .findFirst()
                    .map(objectType -> objectType.getAttribute().stream())
                    .orElse(Stream.empty())
                    .forEach(attr -> {
                        if (!attr.getInbound().isEmpty()) {
                            attr.getInbound().clear();
                        }
                    });
        }
        final ResourceSchemaExtender resourceSchemaExtender = ResourceSchemaFactory.schemaExtenderFor(clonedResource);

        final List<ResourceAttributeDefinitionType> attrDefs = this.mappings.stream()
                .map(mapping -> {
                    final ResourceAttributeDefinitionType attrDef = new ResourceAttributeDefinitionType().ref(
                            mapping.getRef());
                    // Without the cloning it throws exception about resetting parent of a value.
                    CloneUtil.cloneMembersToCollection(attrDef.getInbound(), mapping.getInbound());
                    return attrDef;
                })
                .toList();
        for (ResourceAttributeDefinitionType mapping : attrDefs) {
            resourceSchemaExtender.addAttributeDefinition(objectTypeId, mapping);
        }
        return  resourceSchemaExtender.extend().getObjectTypeDefinitionRequired(objectTypeId);
    }

    private @NotNull ResourceType getResource(@NotNull ShadowType shadow, @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        final String resourceOid = ShadowUtil.getResourceOidRequired(shadow);
        return this.provisioningService
                .getObject(ResourceType.class, resourceOid, null, task, result)
                .asObjectable();
    }
}
