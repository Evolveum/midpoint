/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

/**
 * Primary goal of this class is to support asynchronous operations.
 * The call to operation may return even if the resource operation
 * is still in progress. The IN_PROGRESS status will be indicated in
 * this class in the operation result. The result may also include
 * the asynchronous operation reference in the operational status.
 * This reference may be later used to check the status of the
 * operation.
 *
 * This may seems too simple and maybe pointless now. But we expect
 * that it may later evolve to something like future/promise.
 *
 * FIXME this class looks to be heavily bound to the needs of the provisioning-impl module.
 *
 * @author semancik
 */
public class AsynchronousOperationResult implements ShortDumpable {

    private OperationResult operationResult;

    /** TODO what exactly is the meaning of this? */
    private PendingOperationTypeType operationType;

    /**
     * Quantum operation is an operation where the results may not be immediately obvious.
     * E.g. delete on a semi-manual resource. The resource object is in fact deleted, but
     * it is not yet applied to the state that we see in the backing store (CSV file).
     */
    private boolean quantumOperation;

    public OperationResult getOperationResult() {
        return operationResult;
    }

    public void setOperationResult(OperationResult operationResult) {
        this.operationResult = operationResult;
    }

    public PendingOperationTypeType getOperationType() {
        return operationType;
    }

    public void setOperationType(PendingOperationTypeType operationType) {
        this.operationType = operationType;
    }

    public boolean isQuantumOperation() {
        return quantumOperation;
    }

    public void setQuantumOperation(boolean quantumOperation) {
        this.quantumOperation = quantumOperation;
    }

    public static AsynchronousOperationResult wrap(OperationResult result) {
        AsynchronousOperationResult ret = new AsynchronousOperationResult();
        ret.setOperationResult(result);
        return ret;
    }

    public boolean isInProgress() {
        return operationResult.isInProgress();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (operationType != null) {
            sb.append("type=").append(operationType.value()).append(",");
        }
        if (quantumOperation) {
            sb.append("QUANTUM,");
        }
        if (operationResult != null) {
            sb.append("status=").append(operationResult.getStatus());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AsynchronousOperationResult(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

}
