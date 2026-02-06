/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MappingActivityRun extends SearchBasedActivityRun<ShadowType, MappingWorkDefinition, MappingActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingActivityRun.class);

    private final PrismContext prismContext;
    private final ProvisioningService provisioningService;
    private final List<InlineMappingDefinitionType> mappings;

    public MappingActivityRun(
            ActivityRunInstantiationContext<MappingWorkDefinition, MappingActivityHandler> ctx,
            ProvisioningService provisioningService, PrismContext prismContext) {
        super(ctx, "Mapping Simulation");
        this.prismContext = prismContext;
        this.provisioningService = provisioningService;
        this.mappings = ctx.getActivity().getWorkDefinition().provideMappings();
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
    public boolean processItem(
            @NotNull ShadowType shadow,
            @NotNull ItemProcessingRequest<ShadowType> request,
            RunningTask workerTask,
            OperationResult result) throws CommonException {

        List<PrismObject<FocusType>> linkedFocuses = findLinkedFocuses(shadow, result);

        if (linkedFocuses.isEmpty()) {
            LOGGER.trace("No linked focus found for shadow {}, skipping", shadow);
            return true;
        }

        if (linkedFocuses.size() > 1) {
            LOGGER.trace("Multiple focuses ({}) linked to shadow {}, using first one",
                    linkedFocuses.size(), shadow);
        }

        PrismObject<FocusType> targetFocus = linkedFocuses.get(0);

        return true;
    }

    private List<PrismObject<FocusType>> findLinkedFocuses(ShadowType shadow, OperationResult result)
            throws CommonException {

        ObjectQuery query = PrismContext.get().queryFor(FocusType.class)
                .item(FocusType.F_LINK_REF)
                .ref(shadow.getOid())
                .build();

        return getBeans().repositoryService.searchObjects(
                FocusType.class,
                query,
                null,
                result);
    }
}
