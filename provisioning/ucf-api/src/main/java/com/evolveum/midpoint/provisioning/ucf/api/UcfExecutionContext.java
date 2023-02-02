/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Broader context of the execution of an UCF operation.
 *
 * Responsibilities:
 *
 * 1. Reports executed operations to a given {@link Task} (currently only ICF/ConnId operations are reported this way):
 * both statistics and state message. This includes creation of {@link ConnIdOperation} objects carrying e.g. the lightweight
 * identifier.
 *
 * 2. Lets the called method know if the task was suspended (see {@link #canRun()}).
 *
 * Because of a complex nature of some operations (search and sync) this task has to keep some information
 * about the current operation being executed.
 */
public class UcfExecutionContext {

    private static final Trace LOGGER = TraceManager.getTrace(UcfExecutionContext.class);

    @NotNull private final LightweightIdentifierGenerator lightweightIdentifierGenerator;

    /** Resource - used only for reporting purposes. */
    @NotNull private final ResourceType resource;

    /** TODO can be null? */
    private final Task task;

    /**
     * Operation currently being executed. (It is at most one at any given time, so it can be stored in a single field.)
     */
    private ConnIdOperation currentOperation;

    public UcfExecutionContext(
            @NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ResourceType resource,
            Task task) {
        this.lightweightIdentifierGenerator = lightweightIdentifierGenerator;
        this.resource = resource;
        this.task = task;
    }

    public @NotNull ConnIdOperation recordIcfOperationStart(@NotNull ProvisioningOperation operationKind,
            @Nullable ResourceObjectDefinition objectClassDef, @Nullable String uid) {
        ConnIdOperation operation = ConnIdOperation.ConnIdOperationBuilder.aConnIdOperation()
                .withIdentifier(lightweightIdentifierGenerator.generate().toString())
                .withOperation(operationKind)
                .withResourceRef(ObjectTypeUtil.createObjectRef(resource))
                .withObjectClassDef(objectClassDef != null ? objectClassDef.getObjectClassDefinition() : null)
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
        ResourceObjectClassDefinition objectClassDef = operation.getObjectClassDef();
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

    private String getResourceName() {
        return getOrig(resource.getName());
    }

    public String getResourceOid() {
        return resource.getOid();
    }

    private String getObjectClassName(ResourceObjectDefinition objectClassDef) {
        return objectClassDef != null ? objectClassDef.getTypeName().getLocalPart() : "(null)";
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

    public boolean canRun() {
        return !(task instanceof RunningTask) || ((RunningTask) task).canRun();
    }

    public Task getTask() {
        return task;
    }

    public void checkNotInSimulation() {
        if (!task.isPersistentExecution()) {
            LOGGER.error("MidPoint tried to execute an operation on {}. This is unexpected, as the task is running in simulation"
                            + " mode ({}). Please report this as a bug.",
                    resource, task.getExecutionMode());
            throw new IllegalStateException("Invoking 'modifying' connector operation while being in a simulation mode");
        }
    }

    public static void checkNotInSimulation(@Nullable UcfExecutionContext ctx) {
        if (ctx != null) {
            ctx.checkNotInSimulation();
        }
    }
}
