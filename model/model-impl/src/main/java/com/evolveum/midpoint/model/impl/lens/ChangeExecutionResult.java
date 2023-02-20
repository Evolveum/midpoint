/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Various information related to the execution of changes in an execution wave.
 */
public class ChangeExecutionResult<O extends ObjectType> {

    /** Operation attempted and the result of its execution. */
    private LensObjectDeltaOperation<O> objectDeltaOperation;

    /**
     * The change execution resulted in a state that requires the recomputation of this projection.
     * Originally, this flag was passed between layers of processing; so it ended here.
     */
    private boolean projectionRecomputationRequested;

    public void setExecutedOperation(@NotNull LensObjectDeltaOperation<O> objectDeltaOperation) {
        stateCheck(
                this.objectDeltaOperation == null,
                "Object delta operation already set? %s", objectDeltaOperation);
        this.objectDeltaOperation = objectDeltaOperation;
    }

    static <O extends ObjectType> boolean hasExecutedDelta(ChangeExecutionResult<O> changeExecutionResult) {
        return changeExecutionResult != null && changeExecutionResult.objectDeltaOperation != null;
    }

    /** We don't care if the delta was executed successfully or not. */
    static <O extends ObjectType> ObjectDelta<O> getExecutedDelta(ChangeExecutionResult<O> changeExecutionResult) {
        if (changeExecutionResult == null) {
            return null;
        }
        LensObjectDeltaOperation<O> odo = changeExecutionResult.objectDeltaOperation;
        return odo != null ? odo.getObjectDelta() : null;
    }

    public void setProjectionRecomputationRequested() {
        this.projectionRecomputationRequested = true;
    }

    static boolean isProjectionRecomputationRequested(ChangeExecutionResult<ShadowType> changeExecutionResult) {
        return changeExecutionResult != null && changeExecutionResult.projectionRecomputationRequested;
    }
}
