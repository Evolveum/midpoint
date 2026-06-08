/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.CorrelationSimulationData;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelationSimulationActivityRun
        extends SearchBasedActivityRun<ShadowType, CorrelationWorkDefinition, CorrelationSimulationActivityHandler, AbstractActivityWorkStateType> {

    private final CorrelationService correlationService;
    private final ProvisioningService provisioningService;
    private final RepositoryService repositoryService;
    private final PrismContext prismContext;
    private CorrelationDefinitionType correlationDefinition;
    private List<AdditionalCorrelationItemMappingType> additionalMappings;
    private ResourceType resource;
    private ResourceObjectTypeDefinition objectTypeDefinition;

    public CorrelationSimulationActivityRun(
            ActivityRunInstantiationContext<CorrelationWorkDefinition, CorrelationSimulationActivityHandler> ctx,
            CorrelationService correlationService, ProvisioningService provisioningService,
            RepositoryService repositoryService, PrismContext prismContext) {
        super(ctx, "Correlation");
        this.correlationService = correlationService;
        this.provisioningService = provisioningService;
        this.repositoryService = repositoryService;
        this.prismContext = prismContext;
        setInstanceReady();
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }
        if (!isAnyPreview()) {
            throw new ActivityRunException("This activity is supported only in preview execution mode",
                    OperationResultStatus.FATAL_ERROR, ActivityRunResultStatus.PERMANENT_ERROR);
        }
        this.correlationDefinition = getWorkDefinition().provideCorrelators(result);
        this.additionalMappings = getWorkDefinition().getAdditionalCorrelationMappings();
        precomputeResourceSchemaAndContext(result);
        return true;
    }

    private void precomputeResourceSchemaAndContext(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        final ResourceObjectSetType resourceObjectSet = getWorkDefinition().getResourceObjectSetSpecification();
        final String resourceOid = resourceObjectSet.getResourceRef().getOid();
        final ResourceObjectTypeIdentification objectTypeId = ResourceObjectTypeIdentification.of(
                resourceObjectSet.getKind(), resourceObjectSet.getIntent());

        this.resource = this.provisioningService.getObject(
                ResourceType.class, resourceOid, null, getRunningTask(), result).asObjectable();

        final ResourceSchemaExtender schemaExtender = ResourceSchemaFactory.schemaExtenderFor(this.resource);
        for (AdditionalCorrelationItemMappingType additionalMapping : this.additionalMappings) {
            final ResourceAttributeDefinitionType attrDef = new ResourceAttributeDefinitionType().ref(
                    additionalMapping.getRef());
            // Without the cloning it throws exception about resetting parent of a value.
            CloneUtil.cloneMembersToCollection(attrDef.getInbound(), additionalMapping.getInbound());
            schemaExtender.addAttributeDefinition(objectTypeId, attrDef);
        }
        this.objectTypeDefinition = schemaExtender.addCorrelationDefinition(objectTypeId, this.correlationDefinition)
                .extend()
                .getObjectTypeDefinitionRequired(objectTypeId);
    }

    @Override
    public boolean processItem(@NotNull ShadowType shadow, @NotNull ItemProcessingRequest<ShadowType> req,
            RunningTask task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        final CompleteCorrelationResult correlationResult = this.correlationService.correlate(shadow,
                this.resource, this.objectTypeDefinition, this.correlationDefinition, task, result);

        final Collection<ItemDelta<?, ?>> correlationItemDeltas = correlationResult.toDeltaItems(this.prismContext,
                shadow);

        try {
            this.repositoryService.modifyObject(ShadowType.class, shadow.getOid(), correlationItemDeltas, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception while persisting correlation state to shadow " + shadow
                    + ": " + e.getMessage(), e);
        }

        final SimulationTransaction simulationTransaction = getSimulationTransaction();
        if (simulationTransaction != null) {
            final ObjectDelta<ShadowType> correlationDelta = createDelta(correlationItemDeltas, shadow);
            simulationTransaction.writeSimulationData(new CorrelationSimulationData(shadow, correlationDelta), task,
                    result);
        }

        return true;
    }

    private ObjectDelta<ShadowType> createDelta(Collection<ItemDelta<?, ?>> itemDeltas, ShadowType shadow) {
        return this.prismContext.deltaFactory().object().createModifyDelta(shadow.getOid(),
                itemDeltas,
                ShadowType.class);
    }

}
