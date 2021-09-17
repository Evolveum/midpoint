/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityEventLoggingOptionType.NONE;

import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityItemCountingOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityOverallItemCountingOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScanWorkStateType;

/**
 * Things that we want to remember for all task scanners, like scanning timestamps.
 */
public abstract class ScanActivityExecution<
        O extends ObjectType,
        WD extends WorkDefinition,
        MAH extends ModelActivityHandler<WD, MAH>>
        extends SearchBasedActivityExecution<O, WD, MAH, ScanWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ScanActivityExecution.class);

    XMLGregorianCalendar lastScanTimestamp;
    protected XMLGregorianCalendar thisScanTimestamp;

    public ScanActivityExecution(@NotNull ExecutionInstantiationContext<WD, MAH> context, String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .persistentStatistics(true)
                .defaultDetermineBucketSize(ActivityItemCountingOptionType.NEVER) // To avoid problems like in MID-6934.
                .defaultDetermineOverallSize(ActivityOverallItemCountingOptionType.NEVER)
                .defaultBucketCompletionLogging(NONE); // To avoid log noise.
    }

    @Override
    public void beforeExecution(OperationResult result) {
        lastScanTimestamp = getActivityState().getWorkStatePropertyRealValue(ScanWorkStateType.F_LAST_SCAN_TIMESTAMP,
                XMLGregorianCalendar.class);
        thisScanTimestamp = getModelBeans().clock.currentTimeXMLGregorianCalendar();
        LOGGER.debug("lastScanTimestamp = {}, thisScanTimestamp = {}", lastScanTimestamp, thisScanTimestamp);
    }

    @Override
    public void afterExecution(OperationResult result) throws SchemaException, ActivityExecutionException {
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
            getActivityState().setWorkStateItemRealValues(ScanWorkStateType.F_LAST_SCAN_TIMESTAMP, thisScanTimestamp);
            getActivityState().flushPendingTaskModificationsChecked(result);
        }
    }

    public XMLGregorianCalendar getLastScanTimestamp() {
        return lastScanTimestamp;
    }

    public XMLGregorianCalendar getThisScanTimestamp() {
        return thisScanTimestamp;
    }

    public @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
