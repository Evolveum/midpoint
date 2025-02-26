/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.common.util.OperationExecutionWriter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import com.evolveum.midpoint.task.api.ExecutionSupport;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * <p>Generates "operation execution" records that are then written into objects by
 * {@link OperationExecutionWriter}.</p>
 *
 * <p>Note that here we record so called <i>simple operation executions</i>, i.e. operations that were executed
 * against focus and target resource objects. We do not deal with complex executions, i.e. executions
 * of the whole synchronization or other iterative-task-related operation, that are recorded by respective
 * task handling code.</p>
 *
 * <p>(Support for writing complex executions of ad-hoc operations, like the ones requested from GUI,
 * REST, and APIs, is planned for the future.)</p>
 *
 * <p>In order not to lose any relevant operation execution record, we apply the following algorithm:</p>
 *
 * <ol>
 *     <li>We sort out the executed deltas (regardless of their source: focus, projection, or rotten deltas)
 *     into groups by object OID.</li>
 *     <li>We try to write the deltas into respective objects.</li>
 *     <li>If the corresponding object does not exist, we try to write these deltas to the focus.</li>
 *     <li>And if the focus does not exist as well, we simply discard them. We assume that the most relevant
 *     information - that is, the complex operation execution record - will be written by the task handler.</li>
 * </ol>
 */
@Component
class OperationExecutionRecorderForClockwork {

    private static final Trace LOGGER = TraceManager.getTrace(OperationExecutionRecorderForClockwork.class);

    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private OperationExecutionWriter writer;

    private static final String OP_RECORD_OPERATION_EXECUTIONS =
            OperationExecutionRecorderForClockwork.class.getName() + ".recordOperationExecutions";

    <F extends ObjectType> void recordOperationExecutions(LensContext<F> lensContext, Task task, OperationResult parentResult) {

        if (!task.isExecutionFullyPersistent()) {
            LOGGER.trace("Skipping operation execution recording, as we are in simulated execution mode");
            return;
        }

        if (writer.shouldSkipAllOperationExecutionRecording(OperationExecutionRecordTypeType.SIMPLE)) {
            LOGGER.trace("Skipping operation execution recording (as set in system configuration)");
            return;
        }

        LOGGER.trace("recordOperationExecution starting; task = {}", task);
        Context<F> ctx = new Context<>(lensContext, task);

        OperationResult result = parentResult.subresult(OP_RECORD_OPERATION_EXECUTIONS)
                .setMinor()
                .build();
        try {
            for (LensProjectionContext projectionContext : lensContext.getProjectionContexts()) {
                ctx.addDeltasFromProjection(projectionContext);
            }
            ctx.addRottenProjectionDeltas();
            ctx.addDeltasFromFocus();

            writeCollectedDeltas(ctx, result);
        } catch (Throwable t) {
            processUnexpectedException(ctx, result, t);
            // Let us ignore this for the moment. It should not have happened, sure. But it's not that crucial.
            // Users will see the issue due to the operation result, and the administrator will be able to learn
            // the details from the log.
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Writes all collected deltas. The ones for shadows have to come first, because
     * if a shadow does not exist, its deltas are recorded into the focus.
     */
    private <F extends ObjectType> void writeCollectedDeltas(Context<F> ctx, OperationResult result) {
        writeShadowDeltas(ctx, result);
        writeFocusDeltas(ctx, result);
    }

    private <F extends ObjectType> void writeShadowDeltas(Context<F> ctx, OperationResult result) {
        for (String shadowOid : ctx.shadowDeltasMap.keySet()) {
            Collection<LensObjectDeltaOperation<?>> deltas = ctx.shadowDeltasMap.get(shadowOid);
            OperationExecutionType recordToAdd = createExecutionRecord(deltas, ctx);
            OperationExecutionWriter.Request<ShadowType> request =
                    new OperationExecutionWriter.Request<>(ShadowType.class, shadowOid, recordToAdd,
                            ctx.getExistingExecutions(shadowOid), ctx.isDeletedOk(shadowOid));
            try {
                writer.write(request, result);
            } catch (ObjectNotFoundException e) {
                LOGGER.debug("Shadow {} no longer exists. Deltas will be recorded to the focus object.", shadowOid, e);
                ctx.focusDeltas.addAll(deltas);
            } catch (SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
                // Note that there's no point in moving the deltas to the focus in case of SchemaException: most probably the
                // exception is caused by the deltas themselves. And the ObjectAlreadyExistsException is such a strange thing
                // that it deserves to be reported.
                //
                // We do not interrupt the processing here because we have a hope that deltas for other objects will be processed.
                processUnexpectedException(ctx, result, e);
            }
        }
    }

    private <F extends ObjectType> void writeFocusDeltas(Context<F> ctx, OperationResult result) {
        if (ctx.focusDeltas.isEmpty()) {
            return;
        }

        String focusOid = ctx.getFocusContext() != null ? ctx.getFocusContext().getOid() : null;
        if (focusOid == null) {
            LOGGER.debug("No focus context or focus object OID; {} delta(s) will not be recorded.", ctx.focusDeltas.size());
            return;
        }

        OperationExecutionType recordToAdd = createExecutionRecord(ctx.focusDeltas, ctx);
        Class<F> focusType = ctx.getFocusContext().getObjectTypeClass();
        OperationExecutionWriter.Request<F> request = new OperationExecutionWriter.Request<>(
                focusType, focusOid, recordToAdd, ctx.getExistingExecutions(focusOid), ctx.isDeletedOk(focusOid));
        try {
            writer.write(request, result);
        } catch (Throwable t) {
            // Even if the focus does not exist anymore, there is nothing we can do about it.
            // E.g. it is of no use to write operation execution to the synchronization source account.
            // The task will care of that during complex operation record processing.
            processUnexpectedException(ctx, result, t);
        }
    }

    private void processUnexpectedException(Context<?> ctx, OperationResult result, Throwable t) {
        result.recordFatalError(t);
        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't record operation execution. Model context:\n{}", t,
                ctx.lensContext.debugDump());
    }

    private OperationExecutionType createExecutionRecord(Collection<LensObjectDeltaOperation<?>> executedDeltas,
            Context<?> ctx) {
        assert !executedDeltas.isEmpty();

        OperationExecutionType record = new OperationExecutionType();
        record.setRecordType(OperationExecutionRecordTypeType.SIMPLE);
        OperationResult summaryResult = new OperationResult("recordOperationExecution");
        for (LensObjectDeltaOperation<?> deltaOperation : executedDeltas) {
            record.getOperation().add(createObjectDeltaOperation(deltaOperation));
            if (deltaOperation.getExecutionResult() != null) {
                // todo eliminate the following clone (but beware of modifying the subresult)
                summaryResult.addSubresult(deltaOperation.getExecutionResult().clone());
            }
        }
        createTaskRef(record, ctx);

        ExecutionSupport executionSupport = ctx.task.getExecutionSupport();
        if (executionSupport != null) {
            record.setActivityPath(
                    executionSupport.getActivityPath().toBean());
        }

        summaryResult.computeStatus();
        record.setStatus(summaryResult.getStatus().createStatusType());
        record.setMessage(summaryResult.getMessage());
        // TODO what if the real initiator is different? (e.g. when executing approved changes)
        record.setInitiatorRef(ObjectTypeUtil.createObjectRefCopy(ctx.task.getOwnerRef()));
        record.setChannel(ctx.lensContext.getChannel());
        record.setTimestamp(ctx.now);
        return record;
    }

    private void createTaskRef(OperationExecutionType operation, Context<?> ctx) {
        Task task = ctx.task;
        String rootTaskOid;
        if (task instanceof RunningTask) {
            rootTaskOid = ((RunningTask) task).getRootTaskOid();
        } else if (task.getParent() == null) {
            rootTaskOid = task.getOid(); // can be null
        } else {
            throw new IllegalStateException("Non-RunningTask with a parent? Impossible: " + task);
        }
        if (rootTaskOid != null) {
            operation.setTaskRef(ObjectTypeUtil.createObjectRef(rootTaskOid, ObjectTypes.TASK));
        }
    }

    private <F extends ObjectType> ObjectDeltaOperationType createObjectDeltaOperation(LensObjectDeltaOperation<F> deltaOperation) {
        ObjectDeltaOperationType odo;
        try {
            odo = simplifyOperation(deltaOperation).toLensObjectDeltaOperationBean().getObjectDeltaOperation();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create operation information", e);
            odo = new ObjectDeltaOperationType();
            OperationResult r = new OperationResult(Clockwork.class.getName() + ".createObjectDeltaOperation");
            r.recordFatalError("Couldn't create operation information: " + e.getMessage(), e);
            odo.setExecutionResult(r.createBeanReduced());
        }
        return odo;
    }

    private <F extends ObjectType> LensObjectDeltaOperation<F> simplifyOperation(ObjectDeltaOperation<F> operation) {
        LensObjectDeltaOperation<F> rv = new LensObjectDeltaOperation<>();
        rv.setObjectDelta(simplifyDelta(operation.getObjectDelta()));
        rv.setExecutionResult(OperationResult.keepRootOnly(operation.getExecutionResult()));
        rv.setObjectName(operation.getObjectName());
        rv.setResourceName(operation.getResourceName());
        rv.setResourceOid(operation.getResourceOid());
        rv.setShadowIntent(operation.getShadowIntent());
        rv.setShadowKind(operation.getShadowKind());
        return rv;
    }

    private <F extends ObjectType> ObjectDelta<F> simplifyDelta(ObjectDelta<F> delta) {
        return prismContext.deltaFactory().object().create(delta.getObjectTypeClass(), delta.getChangeType());
    }

    private class Context<F extends ObjectType> {

        /** Time of the recording: to be the same for all operation execution records. */
        private final XMLGregorianCalendar now;

        /** Current lens context: to avoid passing it through method calls. */
        private final LensContext<F> lensContext;

        /** Task: to avoid passing it through method calls. */
        private final Task task;

        private final MultiValuedMap<String, LensObjectDeltaOperation<?>> shadowDeltasMap = new ArrayListValuedHashMap<>();

        /** We assume focus OID does not change. */
        private final List<LensObjectDeltaOperation<?>> focusDeltas = new ArrayList<>();

        /** Objects - kept in order to know current state of their "operationExecutions" records. */
        private final Map<String, PrismObject<? extends ObjectType>> objectsMap = new HashMap<>();

        /** Objects that we expect to be deleted. Deltas for such objects (if they really do not exist) are simply ignored. */
        private final Set<String> deletedObjects = new HashSet<>();

        private Context(LensContext<F> lensContext, Task task) {
            this.lensContext = lensContext;
            this.now = clock.currentTimeXMLGregorianCalendar();
            this.task = task;
        }

        public LensFocusContext<F> getFocusContext() {
            return lensContext.getFocusContext();
        }

        private void addDeltasFromProjection(LensProjectionContext projectionContext) {
            for (LensObjectDeltaOperation<ShadowType> delta : projectionContext.getExecutedDeltas()) {
                addProjectionDelta(delta, projectionContext.getOid());
            }
            PrismObject<ShadowType> shadow = projectionContext.getObjectAny();
            if (shadow != null && shadow.getOid() != null) {
                objectsMap.put(shadow.getOid(), shadow);
            }
            if (projectionContext.isDelete() && projectionContext.getOid() != null) {
                // Let us ignore the situations where the shadow OID is different from current OID stored in proj. context.
                deletedObjects.add(projectionContext.getOid());
            }
        }

        // The defaultOid is used if the delta itself does not contain an OID (shouldn't occur, anyway)
        // And if the OID cannot be determined, delta is assigned to the focus.
        private void addProjectionDelta(LensObjectDeltaOperation<?> delta, String defaultOid) {
            if (delta == null || delta.getObjectDelta() == null) {
                return;
            }
            String oid = defaultIfNull(delta.getObjectDelta().getOid(), defaultOid);
            if (oid != null) {
                shadowDeltasMap.put(oid, delta);
            } else {
                focusDeltas.add(delta);
            }
            if (delta.getObjectDelta().isDelete() && oid != null) {
                deletedObjects.add(oid);
            }
        }

        private void addRottenProjectionDeltas() {
            for (LensObjectDeltaOperation<?> delta : lensContext.getRottenExecutedDeltas()) {
                addProjectionDelta(delta, null);

                // By default, we assume that objects referenced by rotten deltas may be deleted.
                // (For simplicity, we assume this even if a separate non-delete projection context for such OID exists.)
                String oid = delta.getOid();
                if (oid != null) {
                    deletedObjects.add(oid);
                }
            }
        }

        private void addDeltasFromFocus() {
            LensFocusContext<F> focusContext = getFocusContext();
            if (focusContext != null) {
                focusDeltas.addAll(focusContext.getExecutedDeltas());
                PrismObject<F> focus = focusContext.getObjectNew();
                if (focus != null && focus.getOid() != null) {
                    objectsMap.put(focus.getOid(), focus);
                }
                if (focusContext.isDelete() && focusContext.getOid() != null) {
                    deletedObjects.add(focusContext.getOid());
                }
            }
        }

        private Collection<OperationExecutionType> getExistingExecutions(String oid) {
            PrismObject<? extends ObjectType> object = objectsMap.get(oid);
            return object != null ? object.asObjectable().getOperationExecution() : null;
        }

        private boolean isDeletedOk(String oid) {
            return deletedObjects.contains(oid);
        }
    }
}
