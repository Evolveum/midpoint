/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
 * . Provides current {@link ResourceType} object - for reporting purposes.
 *
 * . Reports executed operations to a given {@link Task} (currently only ICF/ConnId operations are reported this way):
 * both statistics and state message. This includes creation of {@link ConnIdOperation} objects carrying e.g. the lightweight
 * identifier.
 *
 * . Lets the called method know if the task was suspended (see {@link #canRun()}).
 *
 * Because of a complex nature of some operations (search and sync) this object has to keep some information
 * about the current operation being executed.
 */
public class UcfExecutionContext {

    private static final Trace LOGGER = TraceManager.getTrace(UcfExecutionContext.class);

    @NotNull private final LightweightIdentifierGenerator lightweightIdentifierGenerator;

    /** Resource - used only for reporting purposes. */
    @NotNull private final ResourceType resource;

    @NotNull private final Task task;

    /**
     * Operation currently being executed. (It is at most one at any given time, so it can be stored in a single field.)
     */
    private ConnIdOperation currentOperation;

    public UcfExecutionContext(
            @NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ResourceType resource,
            @NotNull Task task) {
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

        task.onConnIdOperationStart(operation);

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
            task.onConnIdOperationSuspend(operation);
        }
        currentOperation = null;
        recordState("Returned from " + operation + " of " + getObjectClassName(objectClassDef) + " on " + getResourceName());
    }

    public void recordIcfOperationResume(ConnIdOperation operation) {
        operation.onResume();
        if (currentOperation != null) {
            LOGGER.warn("Unfinished operation: {} in {}", currentOperation, task);
        } else {
            task.onConnIdOperationResume(operation);
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
        task.onConnIdOperationEnd(operation);
        currentOperation = null;
    }

    private String getResourceName() {
        return getOrig(resource.getName());
    }

    public String getResourceOid() {
        return resource.getOid();
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    private String getObjectClassName(ResourceObjectDefinition objectClassDef) {
        return objectClassDef != null ? objectClassDef.getTypeName().getLocalPart() : "(null)";
    }

    private void recordState(String message) {
        task.recordStateMessage(message);
    }

    public boolean canRun() {
        return !(task instanceof RunningTask runningTask) || runningTask.canRun();
    }

    public @NotNull Task getTask() {
        return task;
    }

    public void checkExecutionFullyPersistent() {
        if (!task.isExecutionFullyPersistent()) {
            LOGGER.error("MidPoint tried to execute an operation on {}. This is unexpected, as the task is running in simulation"
                            + " mode ({}). Please report this as a bug.",
                    resource, task.getExecutionMode());
            throw new IllegalStateException("Invoking 'modifying' connector operation while being in a simulation mode");
        }
    }

    public static void checkExecutionFullyPersistent(@Nullable UcfExecutionContext ctx) {
        if (ctx != null) {
            ctx.checkExecutionFullyPersistent();
        }
    }
}
