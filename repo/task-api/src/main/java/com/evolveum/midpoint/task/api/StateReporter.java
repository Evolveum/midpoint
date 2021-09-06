/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * TODO better name (ProgressReporter ? StatisticsReporter ? ...)
 *
 * Used to report state, progress and performance statistics to upper layers.
 * Generally a Task is the place where such information are reported and collected.
 *
 * However, because of a complex nature of some operations (namely, search) it tries
 * to remember the state of an operation.
 *
 * TODO maybe this could be simplified in the future.
 *
 * @author Pavol Mederly
 */
public class StateReporter {

    private static final Trace LOGGER = TraceManager.getTrace(StateReporter.class);

    @NotNull private final LightweightIdentifierGenerator lightweightIdentifierGenerator;

    private Task task;
    private String resourceOid;
    private String resourceName; // lazily set when available

    public StateReporter(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        this.lightweightIdentifierGenerator = lightweightIdentifierGenerator;
    }

    public StateReporter(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, String resourceOid, Task task) {
        this.lightweightIdentifierGenerator = lightweightIdentifierGenerator;
        this.resourceOid = resourceOid;
        this.task = task;
    }

    private String getResourceName() {
        if (resourceName != null) {
            return resourceName;
        } else {
            return resourceOid;
        }
    }

    private ObjectReferenceType getResourceRef() {
        return new ObjectReferenceType()
                .type(ResourceType.COMPLEX_TYPE)
                .oid(resourceOid)
                .targetName(resourceName);
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    // Operational information

    /**
     * Operation currently being executed. (It is at most one at any given time, so it can be stored in a single field.)
     */
    private ConnIdOperation currentOperation;

    public @NotNull ConnIdOperation recordIcfOperationStart(@NotNull ProvisioningOperation operationKind,
            @Nullable ObjectClassComplexTypeDefinition objectClassDef, @Nullable String uid) {
        ConnIdOperation operation = ConnIdOperation.ConnIdOperationBuilder.aConnIdOperation()
                .withIdentifier(lightweightIdentifierGenerator.generate().toString())
                .withOperation(operationKind)
                .withResourceRef(getResourceRef())
                .withObjectClassDef(objectClassDef)
                .withUid(uid)
                .build();

        LOGGER.trace("recordIcfOperationStart: {} in {}", operation, task);
        if (currentOperation != null) {
            LOGGER.warn("Unfinished operation: {}", currentOperation);
        }
        currentOperation = operation;

        if (task != null) {
            task.onConnIdOperationStart(operation);
        } else {
            reportNoTask(operation);
        }

        String object = "";
        if (uid != null) {
            object = " " + uid;
        }
        recordState("Starting " + operationKind + " of " + getObjectClassName(objectClassDef) + object + " on " + getResourceName());

        return operation;
    }

    public void recordIcfOperationSuspend(@NotNull ConnIdOperation operation) {
        ObjectClassComplexTypeDefinition objectClassDef = operation.getObjectClassDef();
        if (operation != currentOperation) {
            LOGGER.warn("Suspending operation other than current: suspending {}, recorded current {}, task {}",
                    operation, currentOperation, task);
        } else {
            if (task != null) {
                task.onConnIdOperationSuspend(operation);
            } else {
                reportNoTask(currentOperation);
            }
        }
        currentOperation = null;
        recordState("Returned from " + operation + " of " + getObjectClassName(objectClassDef) + " on " + getResourceName());
    }

    public void recordIcfOperationResume(ConnIdOperation operation) {
        operation.onResume();
        if (currentOperation != null) {
            LOGGER.warn("Unfinished operation: {} in {}", currentOperation, task);
        } else {
            if (task != null) {
                task.onConnIdOperationResume(operation);
            } else {
                reportNoTask(operation);
            }
        }
        currentOperation = operation;
        recordState("Continuing " + operation + " of " + getObjectClassName(operation.getObjectClassDef()) + " on " + getResourceName());
    }

    private String getObjectClassName(ObjectClassComplexTypeDefinition objectClassDef) {
        return objectClassDef != null ? objectClassDef.getTypeName().getLocalPart() : "(null)";
    }

    private QName getObjectClassQName(ObjectClassComplexTypeDefinition objectClassDef) {
        return objectClassDef != null ? objectClassDef.getTypeName() : null;
    }

    public void recordIcfOperationEnd(ConnIdOperation operation, Throwable ex) {
        LOGGER.trace("recordIcfOperationEnd: operation={}, currentOperation={}, task={}", operation, currentOperation, task);
        operation.onEnd();

        boolean relevant;
        if (currentOperation != operation) {
            LOGGER.warn("Finishing operation other than current: finishing {}, last recorded {}, task {}",
                    operation, currentOperation, task, new RuntimeException("here"));
            relevant = false;
        } else {
            relevant = true;
        }

        String finished;
        if (ex == null) {
            finished = "Successfully finished";
        } else {
            finished = "Finished (unsuccessfully)";
        }
        String durationString;
        if (relevant) {
            durationString = String.format("in %.0f ms", operation.getDuration()) +
                    (operation.wasSuspended() ? String.format(" (net time %d ms)", operation.getNetRunningTime()) : "");
        } else {
            durationString = "";
        }
        String object = "";
        if (operation.getUid() != null) {
            object = " " + operation.getUid();
        }
        final String stateMessage =
                finished + " " + operation + " of " + getObjectClassName(operation.getObjectClassDef()) + object +
                        " on " + getResourceName() + durationString;
        recordState(stateMessage);
        if (task != null) {
            task.onConnIdOperationEnd(operation);
        } else {
            reportNoTask(currentOperation);
        }
        currentOperation = null;
    }

    private void reportNoTask(ConnIdOperation operation) {
        LOGGER.warn("Couldn't report execution of ConnId operation {} because there is no task assigned.",
                operation);
    }

    private void recordState(String message) {
        if (task != null) {
            task.recordStateMessage(message);
        }
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public boolean canRun() {
        return !(task instanceof RunningTask) || ((RunningTask) task).canRun();
    }
}
