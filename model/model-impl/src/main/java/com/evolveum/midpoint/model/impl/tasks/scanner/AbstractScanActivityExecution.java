/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.tasks.AbstractModelSearchActivityExecution;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScanWorkStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskLoggingOptionType;

import org.jetbrains.annotations.NotNull;

/**
 * Things that we want to remember for all task scanners, like scanning timestamps.
 */
public class AbstractScanActivityExecution<
        O extends ObjectType,
        WD extends WorkDefinition,
        MAH extends ModelActivityHandler<WD, MAH>>
        extends AbstractModelSearchActivityExecution<
        O,
        WD,
        MAH,
        AbstractScanActivityExecution<O, WD, MAH>,
        ScanWorkStateType> {

    protected XMLGregorianCalendar lastScanTimestamp;
    protected XMLGregorianCalendar thisScanTimestamp;

    public AbstractScanActivityExecution(@NotNull ExecutionInstantiationContext<WD, MAH> context, @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .preserveStatistics(true)
                .defaultDetermineExpectedTotal(false) // To avoid problems like in MID-6934.
                .defaultBucketCompletionLogging(TaskLoggingOptionType.NONE); // To avoid log noise.
    }

    @Override
    protected void initializeExecution(OperationResult opResult) {
        lastScanTimestamp = activityState.getPropertyRealValue(ScanWorkStateType.F_LAST_SCAN_TIMESTAMP, XMLGregorianCalendar.class);
        thisScanTimestamp = getModelBeans().clock.currentTimeXMLGregorianCalendar();
    }

    @Override
    protected void finishExecution(OperationResult opResult) throws SchemaException, ActivityExecutionException {
        if (getRunningTask().canRun()) {
            /*
             *  We want to update last scan timestamp only if the task has finished its current duties.
             *  Otherwise we might skip e.g. some triggers or validity boundaries - those that the task
             *  has not reached yet. They would be left unprocessed forever. See MID-4474.
             *
             *  TODO what is the following?
             *    "Note this is not the whole solution. It is necessary to review AbstractSearchIterativeResultHandler.handle()
             *     and shouldStop() methods and use 'stop' flag at this place as well. Hopefully such stopping is (very probably)
             *     not requested by any scanner task handlers."
             */
            activityState.setWorkStateItemRealValues(ScanWorkStateType.F_LAST_SCAN_TIMESTAMP, thisScanTimestamp);
            activityState.flushPendingModificationsChecked(opResult);
        }
    }

    public XMLGregorianCalendar getLastScanTimestamp() {
        return lastScanTimestamp;
    }

    public XMLGregorianCalendar getThisScanTimestamp() {
        return thisScanTimestamp;
    }
}
