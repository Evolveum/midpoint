/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.task.AbstractTaskExecution;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Things that we want to remember for all task scanners, like scanning timestamps.
 */
public class AbstractScannerTaskExecution
        <TH extends AbstractScannerTaskHandler<TH, TE>,
                TE extends AbstractScannerTaskExecution<TH, TE>>
        extends AbstractTaskExecution<TH, TE> {

    protected XMLGregorianCalendar lastScanTimestamp;
    protected XMLGregorianCalendar thisScanTimestamp;

    public AbstractScannerTaskExecution(
            TH taskHandler,
            RunningTask localCoordinatorTask, WorkBucketType workBucket,
            TaskPartitionDefinitionType partDefinition,
            TaskWorkBucketProcessingResult previousRunResult) {
        super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
    }

    public void initialize(OperationResult opResult) throws TaskException, CommunicationException, SchemaException,
            ObjectNotFoundException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        super.initialize(opResult);
        lastScanTimestamp = getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
        thisScanTimestamp = taskHandler.getClock().currentTimeXMLGregorianCalendar();
    }

    @Override
    protected void finish(OperationResult opResult, Throwable t) throws SchemaException, TaskException {
        super.finish(opResult, t);

        if (localCoordinatorTask.canRun()) {
            /*
             *  We want to update last scan timestamp only if the task has finished its current duties.
             *  Otherwise we might skip e.g. some triggers or validity boundaries - those that the task
             *  has not reached yet. They would be left unprocessed forever. See MID-4474.
             *
             *  Note this is not the whole solution. It is necessary to review AbstractSearchIterativeResultHandler.handle()
             *  and shouldStop() methods and use 'stop' flag at this place as well. Hopefully such stopping is (very probably)
             *  not requested by any scanner task handlers.
             */
            PrismPropertyDefinition<XMLGregorianCalendar> lastScanTimestampDef = getPrismContext().definitionFactory().createPropertyDefinition(
                    SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME,
                    DOMUtil.XSD_DATETIME);
            PropertyDelta<XMLGregorianCalendar> lastScanTimestampDelta = getPrismContext().deltaFactory().property().create(
                    ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME),
                    lastScanTimestampDef);
            lastScanTimestampDelta.setRealValuesToReplace(thisScanTimestamp);
            localCoordinatorTask.modify(lastScanTimestampDelta);
        }
    }

    public XMLGregorianCalendar getLastScanTimestamp() {
        return lastScanTimestamp;
    }

    public XMLGregorianCalendar getThisScanTimestamp() {
        return thisScanTimestamp;
    }

}
