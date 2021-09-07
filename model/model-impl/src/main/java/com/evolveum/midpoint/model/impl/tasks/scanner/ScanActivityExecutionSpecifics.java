/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.BaseSearchBasedExecutionSpecificsImpl;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityEventLoggingOptionType.NONE;

/**
 * Things that we want to remember for all task scanners, like scanning timestamps.
 */
public abstract class ScanActivityExecutionSpecifics<
        O extends ObjectType,
        WD extends WorkDefinition,
        MAH extends ModelActivityHandler<WD, MAH>>
        extends BaseSearchBasedExecutionSpecificsImpl<O, WD, MAH> {

    private static final Trace LOGGER = TraceManager.getTrace(ScanActivityExecutionSpecifics.class);

    XMLGregorianCalendar lastScanTimestamp;
    protected XMLGregorianCalendar thisScanTimestamp;

    public ScanActivityExecutionSpecifics(@NotNull SearchBasedActivityExecution<O, WD, MAH, ?> activityExecution) {
        super(activityExecution);
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
    public void beforeExecution(OperationResult opResult) {
        lastScanTimestamp = getActivityState().getWorkStatePropertyRealValue(ScanWorkStateType.F_LAST_SCAN_TIMESTAMP,
                XMLGregorianCalendar.class);
        thisScanTimestamp = getModelBeans().clock.currentTimeXMLGregorianCalendar();
        LOGGER.debug("lastScanTimestamp = {}, thisScanTimestamp = {}", lastScanTimestamp, thisScanTimestamp);
    }

    @Override
    public void afterExecution(OperationResult opResult) throws SchemaException, ActivityExecutionException {
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
            getActivityState().flushPendingModificationsChecked(opResult);
        }
    }

    public XMLGregorianCalendar getLastScanTimestamp() {
        return lastScanTimestamp;
    }

    public XMLGregorianCalendar getThisScanTimestamp() {
        return thisScanTimestamp;
    }

    @NotNull
    public ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
