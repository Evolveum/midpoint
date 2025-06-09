/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.task.ActivityPath;

import org.jetbrains.annotations.NotNull;
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
     * Writes an operation execution record.
     *
     * @param target Where to write the record to.
     * @param task Related task. It could be any task that has the correct root task OID filled-in.
     * @param activityPath Path of the related activity.
     * @param resultToRecord This is the result that we want to write to the object (i.e. it must have
     * already computed status and message
     * @param result This is the result we use for our own writing operations.
     *
     * TODO implement redirection also for (rightfully) deleted objects? Currently deleteOk=true means no exception
     *  is propagated from the writer. Overall, it is questionable if we want to write such information at all.
     *
     * TODO move redirection to the writer level?
     */
    public void recordOperationExecution(@NotNull Target target, @NotNull RunningTask task, @NotNull ActivityPath activityPath,
            @NotNull OperationResult resultToRecord, @NotNull OperationResult result) {
        OperationExecutionType recordToAdd = createExecutionRecord(task, activityPath, resultToRecord);
        if (target.canWriteToObject()) {
            recordOperationExecutionToOwner(target, recordToAdd, task, result);
        } else {
            recordOperationExecutionToBackupHolder(target, recordToAdd, task, result);
        }
    }

    public int getMaximumRecordsPerTask() {
        return writer.getMaximumRecordsPerTask();
    }

    private void recordOperationExecutionToOwner(Target target, OperationExecutionType recordToAdd,
            RunningTask task, OperationResult result) {
        ObjectType owner = target.ownerObject.asObjectable();
        try {
            OperationExecutionWriter.Request<? extends ObjectType> request =
                    new OperationExecutionWriter.Request<>(owner.getClass(), owner.getOid(), recordToAdd,
                            owner.getOperationExecution(), true);
            writer.write(request, result);
        } catch (Exception e) {
            LOGGER.warn("Couldn't write operation execution for {} in {}, trying backup holder", owner, task, e);
            recordOperationExecutionToBackupHolder(target, recordToAdd, task, result);
        }
    }

    private void recordOperationExecutionToBackupHolder(Target target, OperationExecutionType recordToAdd, RunningTask task,
            OperationResult result) {
        recordToAdd.beginRealOwner()
                .identification(target.getOwnerIdentification())
                .objectType(target.ownerType);
        try {
            OperationExecutionWriter.Request<? extends ObjectType> request =
                    new OperationExecutionWriter.Request<>(target.backupHolderClass, target.backupHolderOid, recordToAdd,
                            null, false);
            writer.write(request, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Error while writing operation execution for {} into backup holder {} in {}", e,
                    target.ownerIdentification, target.backupHolderOid, task);
        }
    }

    private OperationExecutionType createExecutionRecord(RunningTask task, ActivityPath activityPath,
            OperationResult resultToRecord) {
        OperationExecutionType operation = new OperationExecutionType(prismContext);
        operation.setRecordType(OperationExecutionRecordTypeType.COMPLEX);
        operation.setTaskRef(ObjectTypeUtil.createObjectRef(task.getRootTaskOid(), ObjectTypes.TASK));
        operation.setActivityPath(activityPath.toBean());
        operation.setStatus(resultToRecord.getStatus().createStatusType());
        operation.setMessage(resultToRecord.getMessage());
        // TODO what if the real initiator is different? (e.g. when executing approved changes)
        operation.setInitiatorRef(ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()));
        operation.setChannel(task.getChannel());
        operation.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar());
        return operation;
    }

    /**
     * Specification of where to write operation execution record.
     */
    public static class Target {

        /**
         * The real owner i.e. the object (shadow, user, ...) that was processed.
         * Can be null if that object no longer exists in repository.
         */
        private final PrismObject<? extends ObjectType> ownerObject;

        /**
         * Owner type. Needed only if {@link #ownerObject} is null.
         */
        private final QName ownerType;

        /**
         * Alternative identification of the owner. Needed only if {@link #ownerObject} is null.
         * An example: account UID; but can be basically any relevant free-form string.
         */
        private final String ownerIdentification;

        /**
         * OID of the backup object, to which the record is to be written when the real owner does not exist
         * or if it cannot be written to. Usually it is the task OID.
         */
        private final String backupHolderOid;

        /**
         * Type of the backup holder. Usually it is TaskType.
         */
        private final Class<? extends ObjectType> backupHolderClass;

        public Target(PrismObject<? extends ObjectType> ownerObject, QName ownerType, String ownerIdentification,
                String backupHolderOid, Class<? extends ObjectType> backupHolderClass) {
            this.ownerObject = ownerObject;
            this.ownerType = ownerType;
            this.ownerIdentification = ownerIdentification;
            this.backupHolderOid = backupHolderOid;
            this.backupHolderClass = backupHolderClass;
        }

        private boolean canWriteToObject() {
            return ownerObject != null && ownerObject.getOid() != null;
        }

        public String getOwnerIdentification() {
            if (ownerIdentification != null) {
                return ownerIdentification;
            } else if (ownerObject != null) {
                return PolyString.getOrig(ownerObject.asObjectable().getName());
            } else {
                return null;
            }
        }
    }
}
