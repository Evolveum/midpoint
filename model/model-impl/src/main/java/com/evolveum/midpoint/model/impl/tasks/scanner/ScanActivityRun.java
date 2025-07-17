/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityEventLoggingOptionType.NONE;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;

import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityOverallItemCountingOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScanWorkStateType;

/**
 * Things that we want to remember for all task scanners, like scanning timestamps.
 */
public abstract class ScanActivityRun<
        O extends ObjectType,
        WD extends WorkDefinition,
        MAH extends ModelActivityHandler<WD, MAH>>
        extends SearchBasedActivityRun<O, WD, MAH, ScanWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ScanActivityRun.class);

    XMLGregorianCalendar lastScanTimestamp;
    protected XMLGregorianCalendar thisScanTimestamp;

    public ScanActivityRun(@NotNull ActivityRunInstantiationContext<WD, MAH> context, String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .determineOverallSizeDefault(ActivityOverallItemCountingOptionType.NEVER) // To avoid problems like in MID-6934.
                .bucketCompletionLoggingDefault(NONE); // To avoid log noise.
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }
        lastScanTimestamp = getActivityState().getWorkStatePropertyRealValue(ScanWorkStateType.F_LAST_SCAN_TIMESTAMP,
                XMLGregorianCalendar.class);
        thisScanTimestamp = getModelBeans().clock.currentTimeXMLGregorianCalendar();
        LOGGER.debug("lastScanTimestamp = {}, thisScanTimestamp = {}", lastScanTimestamp, thisScanTimestamp);
        return true;
    }

    @Override
    public void afterRun(OperationResult result) throws SchemaException, ActivityRunException {
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
