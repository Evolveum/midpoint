/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author semancik
 */
public class PendingOperationsAsserter<R> extends AbstractAsserter<ShadowAsserter<R>> {

    private ShadowAsserter<R> shadowAsserter;

    public PendingOperationsAsserter(ShadowAsserter<R> shadowAsserter) {
        super();
        this.shadowAsserter = shadowAsserter;
    }

    public PendingOperationsAsserter(ShadowAsserter<R> shadowAsserter, String details) {
        super(details);
        this.shadowAsserter = shadowAsserter;
    }

    public static PendingOperationsAsserter<Void> forShadow(PrismObject<ShadowType> shadow) {
        return new PendingOperationsAsserter<>(ShadowAsserter.forShadow(shadow));
    }

    List<PendingOperationType> getOperations() {
        return shadowAsserter.getObject().asObjectable().getPendingOperation();
    }

    public PendingOperationsAsserter<R> assertOperations(int expectedNumber) {
        assertEquals("Unexpected number of pending operations in "+shadowAsserter.getObject(), expectedNumber, getOperations().size());
        return this;
    }

    PendingOperationAsserter<R> forOperation(PendingOperationType operation) {
        PendingOperationAsserter<R> asserter = new PendingOperationAsserter<>(this, operation, idToString(operation.getId()), getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private String idToString(Long id) {
        if (id == null) {
            return "";
        } else {
            return id.toString();
        }
    }

    public PendingOperationAsserter<R> singleOperation() {
        assertOperations(1);
        return forOperation(getOperations().get(0));
    }

    public PendingOperationsAsserter<R> assertNone() {
        assertOperations(0);
        return this;
    }

    public PendingOperationAsserter<R> modifyOperation() {
        return by()
            .changeType(ChangeTypeType.MODIFY)
            .find();
    }

    public PendingOperationAsserter<R> addOperation() {
        return by()
            .changeType(ChangeTypeType.ADD)
            .find();
    }

    public PendingOperationAsserter<R> deleteOperation() {
        return by()
            .changeType(ChangeTypeType.DELETE)
            .find();
    }

    public PendingOperationsAsserter<R> assertUnfinishedOperation() {
        for (PendingOperationType operation: getOperations()) {
            if (isUnfinished(operation)) {
                return this;
            }
        }
        fail("No unfinished operations in "+desc());
        return null; // not reached
    }

    private boolean isUnfinished(PendingOperationType operation) {
        return operation.getExecutionStatus() != PendingOperationExecutionStatusType.COMPLETED;
    }

    public PendingOperationFinder<R> by() {
        return new PendingOperationFinder<>(this);
    }

    PrismObject<ShadowType> getShadow() {
        return shadowAsserter.getObject();
    }

    @Override
    public ShadowAsserter<R> end() {
        return shadowAsserter;
    }

    @Override
    protected String desc() {
        return descWithDetails("pending operations of "+shadowAsserter.getObject());
    }

}
