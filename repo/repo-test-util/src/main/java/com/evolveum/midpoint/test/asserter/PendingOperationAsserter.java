/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.prism.ObjectDeltaTypeAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

/**
 * @author semancik
 */
public class PendingOperationAsserter<R> extends AbstractAsserter<PendingOperationsAsserter<R>> {

    private final PendingOperationsAsserter<R> pendingOperationsAsserter;
    private final PendingOperationType pendingOperation;
    private String operationDesc;

    public PendingOperationAsserter(PendingOperationsAsserter<R> pendingOperationsAsserter, PendingOperationType pendingOperation) {
        super();
        this.pendingOperationsAsserter = pendingOperationsAsserter;
        this.pendingOperation = pendingOperation;
    }

    public PendingOperationAsserter(PendingOperationsAsserter<R> pendingOperationsAsserter, PendingOperationType pendingOperation, String operationDesc, String details) {
        super(details);
        this.pendingOperationsAsserter = pendingOperationsAsserter;
        this.pendingOperation = pendingOperation;
        this.operationDesc = operationDesc;
    }

    public PendingOperationAsserter<R> assertRequestTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("Request timestamp in " + desc(), start, end, pendingOperation.getRequestTimestamp());
        return this;
    }

    public PendingOperationAsserter<R> assertCompletionTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("Completion timestamp in " + desc(), start, end, pendingOperation.getCompletionTimestamp());
        return this;
    }

    public PendingOperationAsserter<R> assertHasCompletionTimestamp() {
        assertNotNull("No completion timestamp in " + desc(), pendingOperation.getCompletionTimestamp());
        return this;
    }

    public PendingOperationAsserter<R> assertLastAttemptTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("Last attempt timestamp in " + desc(), start, end, pendingOperation.getLastAttemptTimestamp());
        return this;
    }

    public PendingOperationAsserter<R> assertOperationStartTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("Operation start timestamp in " + desc(), start, end, pendingOperation.getOperationStartTimestamp());
        return this;
    }

    public PendingOperationAsserter<R> assertExecutionStatus(PendingOperationExecutionStatusType expected) {
        assertEquals("Wrong execution status in " + desc(), expected, pendingOperation.getExecutionStatus());
        return this;
    }

    public PendingOperationAsserter<R> assertResultStatus(OperationResultStatusType expected) {
        assertEquals("Wrong result status in " + desc(), expected, pendingOperation.getResultStatus());
        return this;
    }

    public PendingOperationAsserter<R> assertAttemptNumber(Integer expected) {
        assertEquals("Wrong attempt number in " + desc(), expected, pendingOperation.getAttemptNumber());
        return this;
    }

    public PendingOperationAsserter<R> assertType(PendingOperationTypeType expected) {
        assertEquals("Wrong type in " + desc(), expected, pendingOperation.getType());
        return this;
    }

    public PendingOperationAsserter<R> assertAsynchronousOperationReference(String expected) {
        assertEquals("Wrong asynchronous operation reference in " + desc(), expected, pendingOperation.getAsynchronousOperationReference());
        return this;
    }

    public PendingOperationAsserter<R> assertId() {
        assertNotNull("No id in " + desc(), pendingOperation.getId());
        return this;
    }

    public PendingOperationAsserter<R> assertId(Long expected) {
        assertEquals("Wrong id in " + desc(), expected, pendingOperation.getId());
        return this;
    }

    public ObjectDeltaTypeAsserter<PendingOperationAsserter<R>> delta() {
        return new ObjectDeltaTypeAsserter<>(pendingOperation.getDelta(), this, "delta in " + desc());
    }

    protected String desc() {
        return descWithDetails("pending operation " + operationDesc + " in " + pendingOperationsAsserter.getShadow());
    }

    public PendingOperationAsserter<R> display() {
        display(desc());
        return this;
    }

    public PendingOperationAsserter<R> display(String message) {
        IntegrationTestTools.display(message, pendingOperation);
        return this;
    }

    @Override
    public PendingOperationsAsserter<R> end() {
        return pendingOperationsAsserter;
    }

    public PendingOperationType getOperation() {
        return pendingOperation;
    }
}
