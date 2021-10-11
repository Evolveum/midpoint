/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.RunningTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 *
 * @author Radovan Semancik
 *
 */
@Component
public abstract class AbstractScannerTaskHandler<O extends ObjectType, H extends AbstractScannerResultHandler<O>>
        extends AbstractSearchIterativeModelTaskHandler<O,H> {

    @Autowired protected Clock clock;

    public AbstractScannerTaskHandler(Class<O> type, String taskName, String taskOperationPrefix) {
        super(taskName, taskOperationPrefix);
    }

    @Override
    protected boolean initializeRun(H handler, TaskRunResult runResult,
            Task task, OperationResult opResult) {
        boolean cont = super.initializeRun(handler, runResult, task, opResult);
        if (!cont) {
            return false;
        }

        XMLGregorianCalendar lastScanTimestamp;
        PrismProperty<XMLGregorianCalendar> lastScanTimestampProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
        if (lastScanTimestampProperty != null) {
            lastScanTimestamp = lastScanTimestampProperty.getValue().getValue();
        } else {
            lastScanTimestamp = null;
        }
        handler.setLastScanTimestamp(lastScanTimestamp);

        handler.setThisScanTimestamp(clock.currentTimeXMLGregorianCalendar());

        return true;
    }

    @Override
    protected void finish(H handler, TaskRunResult runResult, RunningTask task, OperationResult opResult) throws SchemaException {
        super.finish(handler, runResult, task, opResult);

        if (task.canRun()) {
            /*
             *  We want to update last scan timestamp only if the task has finished its current duties.
             *  Otherwise we might skip e.g. some triggers or validity boundaries - those that the task
             *  has not reached yet. They would be left unprocessed forever. See MID-4474.
             *
             *  Note this is not the whole solution. It is necessary to review AbstractSearchIterativeResultHandler.handle()
             *  and shouldStop() methods and use 'stop' flag at this place as well. Hopefully such stopping is (very probably)
             *  not requested by any scanner task handlers.
             */
            PrismPropertyDefinition<XMLGregorianCalendar> lastScanTimestampDef = prismContext.definitionFactory().createPropertyDefinition(
                    SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME,
                    DOMUtil.XSD_DATETIME);
            PropertyDelta<XMLGregorianCalendar> lastScanTimestampDelta = prismContext.deltaFactory().property().create(
                    ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME),
                    lastScanTimestampDef);
            lastScanTimestampDelta.setRealValuesToReplace(handler.getThisScanTimestamp());
            task.modifyExtension(lastScanTimestampDelta);
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.SYSTEM;
    }
}
