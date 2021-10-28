/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnIdOperationsReportDefinitionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityReportsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnIdOperationRecordType;

/**
 * Represents a report on ConnId operations.
 */
public class ConnIdOperationsReport extends AbstractReport {

    private static final ItemPath STATE_ITEM_PATH =
            ItemPath.create(ActivityStateType.F_REPORTS, ActivityReportsType.F_CONN_ID_OPERATIONS);

    /**
     * Guarded by this.
     */
    @NotNull private final List<ConnIdOperationRecordType> bufferedRecords = new ArrayList<>();

    public ConnIdOperationsReport(ConnIdOperationsReportDefinitionType definition, @NotNull CurrentActivityState<?> activityState) {
        super(definition, ConnIdOperationRecordType.COMPLEX_TYPE, activityState);
    }

    @Override
    String getReportType() {
        return "ConnId operations";
    }

    @Override
    @NotNull ItemPath getStateItemPath() {
        return STATE_ITEM_PATH;
    }

    public synchronized void addRecord(@NotNull ConnIdOperationRecordType record) {
        bufferedRecords.add(record);
    }

    public void flush(@NotNull RunningTask task, @NotNull OperationResult result) {

        List<ConnIdOperationRecordType> gatheredRecords;
        synchronized (this) {
            gatheredRecords = new ArrayList<>(bufferedRecords);
            bufferedRecords.clear();
        }

        List<ConnIdOperationRecordType> filteredRecords = gatheredRecords.stream()
                .filter(r -> !isRejected(r, task, result))
                .collect(Collectors.toList());

        if (!filteredRecords.isEmpty()) {
            synchronized (this) {
                openIfClosed(result);
                writeRecords(filteredRecords);
            }
        }
    }
}
