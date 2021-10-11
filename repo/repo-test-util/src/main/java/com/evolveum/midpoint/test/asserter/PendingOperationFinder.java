/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.testng.AssertJUnit;

/**
 * @author semancik
 */
public class PendingOperationFinder<R> {

    private final PendingOperationsAsserter<R> pendingOperationsAsserter;
    private PendingOperationExecutionStatusType executionStatus;
    private OperationResultStatusType resultStatus;
    private ChangeTypeType changeType;
    private ItemPath itemPath;

    public PendingOperationFinder(PendingOperationsAsserter<R> pendingOperationsAsserter) {
        this.pendingOperationsAsserter = pendingOperationsAsserter;
    }

    public PendingOperationFinder<R> changeType(ChangeTypeType changeType) {
        this.changeType = changeType;
        return this;
    }

    public PendingOperationFinder<R> executionStatus(PendingOperationExecutionStatusType executionStatus) {
        this.executionStatus = executionStatus;
        return this;
    }

    public PendingOperationFinder<R> resultStatus(OperationResultStatusType resultStatus) {
        this.resultStatus = resultStatus;
        return this;
    }

    public PendingOperationFinder<R> item(ItemPath itemPath) {
        this.itemPath = itemPath;
        return this;
    }

    public PendingOperationFinder<R> item(Object... components) {
        return item(ItemPath.create(components));
    }

    public PendingOperationAsserter<R> find() {
        PendingOperationType found = null;
        for (PendingOperationType operation: pendingOperationsAsserter.getOperations()) {
            if (matches(operation)) {
                if (found == null) {
                    found = operation;
                } else {
                    fail("Found more than one operation that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no operation that matches search criteria");
        }
        return pendingOperationsAsserter.forOperation(found);
    }

    public PendingOperationsAsserter<R> assertNone() {
        for (PendingOperationType operation: pendingOperationsAsserter.getOperations()) {
            if (matches(operation)) {
                fail("Found operation that matches search criteria while expecting none");
            }
        }
        return pendingOperationsAsserter;
    }

    public PendingOperationsAsserter<R> assertAll() {
        for (PendingOperationType operation: pendingOperationsAsserter.getOperations()) {
            if (!matches(operation)) {
                fail("Found operation that does not match search criteria while expecting all operations to match");
            }
        }
        return pendingOperationsAsserter;
    }

    private boolean matches(PendingOperationType operation) {
        ObjectDeltaType delta = operation.getDelta();

        if (executionStatus != null) {
            if (!executionStatus.equals(operation.getExecutionStatus())) {
                return false;
            }
        }

        if (resultStatus != null) {
            if (!resultStatus.equals(operation.getResultStatus())) {
                return false;
            }
        }

        if (changeType != null) {
            if (delta == null) {
                return false;
            }
            if (!changeType.equals(delta.getChangeType())) {
                return false;
            }
        }

        if (itemPath != null) {
            if (delta == null) {
                return false;
            }
            if (!deltaContains(delta)) {
                return false;
            }
        }

        // TODO: more criteria
        return true;
    }

    private boolean deltaContains(ObjectDeltaType delta) {
        for (ItemDeltaType itemDelta: delta.getItemDelta()) {
            ItemPath deltaPath = itemDelta.getPath().getItemPath();
            if (itemPath.equivalent(deltaPath)) {
                return true;
            }
        }
        return false;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
