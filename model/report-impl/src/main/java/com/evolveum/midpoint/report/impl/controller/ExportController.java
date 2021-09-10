package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

public interface ExportController<C> {

    void handleDataRecord(int sequentialNumber, C record, RunningTask workerTask, OperationResult result) throws CommonException;
}
