/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.util;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

/**
 * Prepares complex OperationExecutionType records for search-iterative and live-sync/async-update tasks.
 *
 * Uses {@link OperationExecutionWriter} for the actual writing of the records.
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
     *
     * TODO implement redirection also for (rightfully) deleted objects?
     *
     * TODO move redirection to the writer level?
     */
    public void recordOperationExecution(Target target, Throwable e, RunningTask task, OperationResult result) {

        OperationExecutionType executionToAdd = createExecutionRecord(e, task, result);
        if (target.canWriteToObject()) {
            recordOperationExecutionDirectly(target, executionToAdd, task, result);
        } else {
            recordRedirectedOperationExecution(target, executionToAdd, task, result);
        }
    }

    private void recordOperationExecutionDirectly(Target target, OperationExecutionType executionToAdd,
            RunningTask task, OperationResult result) {
        ObjectType objectable = target.targetObject.asObjectable();
        try {
            writer.write(objectable.getClass(), objectable.getOid(), executionToAdd, objectable.getOperationExecution(), true, result);
        } catch (Exception e) {
            LOGGER.warn("Couldn't write operation execution for {} in {}, trying backup object", objectable, task, e);
            recordRedirectedOperationExecution(target, executionToAdd, task, result);
        }
    }

    private void recordRedirectedOperationExecution(Target target, OperationExecutionType executionToAdd, RunningTask task,
            OperationResult result) {
        executionToAdd.beginRedirection()
                .redirected(true)
                .identification(target.getTargetIdentification())
                .objectType(target.targetType);
        try {
            writer.write(target.backupObjectClass, target.backupObjectOid, executionToAdd, null, false, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error while writing operation execution for {} into {} in {}", e,
                    target.targetIdentification, target.backupObjectOid, task);
        }
    }

    // TODO ignoring e?
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

    /**
     * Specification where to write operation execution record.
     */
    public static class Target {

        /** The primary target i.e. the object (shadow, user, ...) that was processed (if it exists). */
        private final PrismObject<? extends ObjectType> targetObject;

        /** Type of the primary target. Needed mainly if it's null. */
        private final QName targetType;

        /** Identification of the primary target, e.g. account UID. Needed mainly if the target object is null. */
        private final String targetIdentification;

        /** OID of the backup object, to which the record is to be written. Usually task OID. */
        private final String backupObjectOid;

        /** Class of the backup object. Usually TaskType. */
        private final Class<? extends ObjectType> backupObjectClass;

        public Target(PrismObject<? extends ObjectType> targetObject, QName targetType, String targetIdentification,
                String backupObjectOid, Class<? extends ObjectType> backupObjectClass) {
            this.targetObject = targetObject;
            this.targetType = targetType;
            this.targetIdentification = targetIdentification;
            this.backupObjectOid = backupObjectOid;
            this.backupObjectClass = backupObjectClass;
        }

        private boolean canWriteToObject() {
            return targetObject != null && targetObject.getOid() != null;
        }

        public String getTargetIdentification() {
            if (targetIdentification != null) {
                return targetIdentification;
            } else if (targetObject != null) {
                return PolyString.getOrig(targetObject.asObjectable().getName());
            } else {
                return null;
            }
        }
    }
}
