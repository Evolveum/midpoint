/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.ResourceOperationCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import org.jetbrains.annotations.NotNull;

/**
 * Options for the {@link ProvisioningService#synchronize(ResourceOperationCoordinates, LiveSyncOptions, LiveSyncTokenStorage,
 * LiveSyncEventHandler, Task, OperationResult)} operation.
 */
public class LiveSyncOptions {

    /**
     * It is better to provide execution mode explicitly here than to rely on the setting in the task.
     * The reason is that execution mode in the task is currently set only during processing of an item,
     * not in items preparation phase.
     */
    @NotNull private final ExecutionModeType executionMode;

    private final Integer batchSize;
    private final boolean updateLiveSyncTokenInDryRun;
    private final boolean updateLiveSyncTokenInPreviewMode;

    public LiveSyncOptions() {
        this(ExecutionModeType.FULL, null, false, false);
    }

    public LiveSyncOptions(
            @NotNull ExecutionModeType executionMode,
            Integer batchSize,
            boolean updateLiveSyncTokenInDryRun,
            boolean updateLiveSyncTokenInPreviewMode) {
        this.executionMode = executionMode;
        this.batchSize = batchSize;
        this.updateLiveSyncTokenInDryRun = updateLiveSyncTokenInDryRun;
        this.updateLiveSyncTokenInPreviewMode = updateLiveSyncTokenInPreviewMode;
    }

    public @NotNull ExecutionModeType getExecutionMode() {
        return executionMode;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public boolean isUpdateLiveSyncTokenInDryRun() {
        return updateLiveSyncTokenInDryRun;
    }

    public boolean isUpdateLiveSyncTokenInPreviewMode() {
        return updateLiveSyncTokenInPreviewMode;
    }
}
