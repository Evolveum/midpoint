/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CORRELATION_OWNER_OPTIONS_PATH;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.CORRELATION_RESULTING_OWNER_PATH;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.CORRELATION_SITUATION_PATH;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.provisioning.api.ShadowSimulationData;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelationActivityRun
        extends SearchBasedActivityRun<ShadowType, CorrelationWorkDefinition, CorrelationActivityHandler, AbstractActivityWorkStateType> {

    private final CorrelationService correlationService;
    private final PrismContext prismContext;
    private CorrelationDefinitionType correlationDefinition;

    public CorrelationActivityRun(
            ActivityRunInstantiationContext<CorrelationWorkDefinition, CorrelationActivityHandler> ctx,
            CorrelationService correlationService, PrismContext prismContext) {
        super(ctx, "Correlation");
        this.correlationService = correlationService;
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
                    OperationResultStatus.FATAL_ERROR, TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
        }
        this.correlationDefinition = getWorkDefinition().provideCorrelators(result);
        return true;
    }

    @Override
    public boolean processItem(@NotNull ShadowType shadow, @NotNull ItemProcessingRequest<ShadowType> req,
            RunningTask task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        final CompleteCorrelationResult correlationResult = this.correlationService.correlate(shadow,
                this.correlationDefinition, task, result);

        final SimulationTransaction simulationTransaction = getSimulationTransaction();
        if (simulationTransaction != null) {
            final List<ItemDelta<?, ?>> correlationDelta = createDelta(correlationResult, shadow);
            simulationTransaction.writeSimulationData(ShadowSimulationData.of(shadow, correlationDelta), task, result);
        }

        return true;
    }

    private List<ItemDelta<?, ?>> createDelta(CompleteCorrelationResult correlationResult, ShadowType shadow)
            throws SchemaException {
        S_ItemEntry builder = this.prismContext.deltaFor(ShadowType.class)
                .oldObject(shadow)
                .optimizing();
        if (correlationResult.isError()) {
            builder = builder.item(CORRELATION_SITUATION_PATH)
                    .replace(CorrelationSituationType.ERROR);
        } else {
            if (ownerOptionsChanged(shadow, correlationResult.getOwnerOptions())) {
                builder = builder.item(CORRELATION_OWNER_OPTIONS_PATH)
                        .replace(correlationResult.getOwnerOptions());
            }
            builder = builder.item(CORRELATION_SITUATION_PATH)
                    .replace(correlationResult.getSituation())
                    .item(CORRELATION_RESULTING_OWNER_PATH)
                    .replace(ObjectTypeUtil.createObjectRef(correlationResult.getOwner()));
        }
        return builder.asItemDeltas();
    }

    private boolean ownerOptionsChanged(ShadowType shadow, @Nullable ResourceObjectOwnerOptionsType newOwnerOptions) {
        final ShadowCorrelationStateType oldCorrelation = shadow.getCorrelation();
        final ResourceObjectOwnerOptionsType oldOptions = oldCorrelation != null
                ? oldCorrelation.getOwnerOptions()
                : null;

        if (oldOptions == null) {
            return newOwnerOptions != null;
        } else {
            if (newOwnerOptions == null) {
                return true;
            } else {
                // We have to ignore auto-generated PCV IDs
                return !oldOptions.asPrismContainerValue().equals(
                        newOwnerOptions.asPrismContainerValue(), EquivalenceStrategy.REAL_VALUE);
            }
        }
    }

}
