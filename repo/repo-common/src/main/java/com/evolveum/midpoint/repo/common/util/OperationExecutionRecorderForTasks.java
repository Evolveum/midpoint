/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

/**
 * <p>Prepares complex OperationExecutionType records for search-iterative and live-sync/async-update tasks.</p>
 *
 * <p>Uses {@link OperationExecutionWriter}</p> for the actual writing of the records.
 */
@Component
public class OperationExecutionRecorderForTasks {

    @Autowired private OperationExecutionWriter writer;
    @Autowired private PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(OperationExecutionRecorderForTasks.class);

    /**
     * The task need not to be the specific worker LAT. It could be any task that has the correct root task OID filled-in.
     *
     * Precondition: result has already computed status.
     */
    public <O extends ObjectType> void recordOperationExecution(PrismObject<O> object, Throwable e, RunningTask task, OperationResult result) {

        O objectable = object.asObjectable();
        try {
            OperationExecutionType executionToAdd = createExecutionRecord(e, task, result);
            writer.write(objectable.getClass(), object.getOid(), executionToAdd, objectable.getOperationExecution(), true, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error while writing operation execution for {} in {}", t, object, task);
            // TODO
        }
    }

    private <O extends ObjectType> OperationExecutionType createExecutionRecord(Throwable e, RunningTask task,
            OperationResult result) {
        OperationExecutionType operation = new OperationExecutionType(prismContext);
        operation.setRecordType(OperationExecutionRecordTypeType.COMPLEX);
        operation.setTaskRef(ObjectTypeUtil.createObjectRef(task.getRootTaskOid(), ObjectTypes.TASK));
        operation.setStatus(result.getStatus().createStatusType());
        // TODO what if the real initiator is different? (e.g. when executing approved changes)
        operation.setInitiatorRef(ObjectTypeUtil.createObjectRef(task.getOwner(), prismContext));
        operation.setChannel(task.getChannel());
        operation.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar());
        return operation;
    }
}
