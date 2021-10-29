/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsProcessingReportDefinitionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityReportsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingRecordType;

public class ItemsReport extends AbstractReport {

    private static final ItemPath STATE_ITEM_PATH = ItemPath.create(ActivityStateType.F_REPORTS, ActivityReportsType.F_ITEMS);

    private static final int MAX_ERROR_MESSAGE_SIZE = 1000;

    public ItemsReport(ItemsProcessingReportDefinitionType definition, @NotNull CurrentActivityState<?> activityState) {
        super(definition, ItemProcessingRecordType.COMPLEX_TYPE, activityState);
    }

    @Override
    String getReportType() {
        return "items";
    }

    /**
     * Records processing of an item to the report.
     */
    public void recordItemProcessed(@NotNull ItemProcessingRecordType record, @NotNull RunningTask task,
            @NotNull OperationResult result) {

        if (isRejected(record, task, result)) {
            return;
        }

        transformErrorMessage(record);

        // Synchronized because it can be called from multiple worker threads (LATs).
        synchronized (this) {
            openIfClosed(result);
            writeRecord(record);
        }
    }

    /**
     * Transforms error message to be safely storable into CSV:
     * shortens it and replaces line breaks with spaces.
     */
    @Experimental
    private void transformErrorMessage(@NotNull ItemProcessingRecordType record) {
        String message = record.getErrorMessage();
        if (message != null) {
            message = message.replaceAll("\\R+", " ");
            if (message.length() > MAX_ERROR_MESSAGE_SIZE) {
                message = message.substring(0, MAX_ERROR_MESSAGE_SIZE);
            }
            record.setErrorMessage(message);
        }
    }

    @Override
    @NotNull ItemPath getStateItemPath() {
        return STATE_ITEM_PATH;
    }
}
