/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Asserts about CaseWorkItemType.
 */
public class CaseWorkItemAsserter<RA, WI extends AbstractWorkItemType> extends PrismContainerValueAsserter<WI, RA> {

    private static final int DEADLINE_TOLERANCE = 60000;

    public CaseWorkItemAsserter(CaseWorkItemType workItem) {
        //noinspection unchecked
        super(workItem.asPrismContainerValue());
    }

    public CaseWorkItemAsserter(CaseWorkItemType workItem, String details) {
        //noinspection unchecked
        super(workItem.asPrismContainerValue(), details);
    }

    public CaseWorkItemAsserter(WI workItem, RA returnAsserter, String details) {
        //noinspection unchecked
        super(workItem.asPrismContainerValue(), returnAsserter, details);
    }

    @NotNull
    private WI getWorkItem() {
        return getPrismValue().asContainerable();
    }

    public CaseWorkItemAsserter<RA, WI> assertOriginalAssigneeRef(String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(getWorkItem().getOriginalAssigneeRef(), "originalAssigneeRef", oid, typeName);
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertPerformerRef(String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(getWorkItem().getPerformerRef(), "performerRef", oid, typeName);
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertAssignees(String... oids) {
        PrismAsserts.assertReferenceValues(getWorkItem().getAssigneeRef(), oids);
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertStageNumber(Integer expected) {
        assertThat(getWorkItem().getStageNumber()).as("stage number").isEqualTo(expected);
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertOutcome(String expected) {
        AbstractWorkItemOutputType output = getWorkItem().getOutput();
        assertThat(output).as("output").isNotNull();
        MidPointAsserts.assertUriMatches(output.getOutcome(), "outcome", expected);
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertNameOrig(String expected) {
        assertThat(getWorkItem().getName().getOrig()).as("name.orig").isEqualTo(expected);
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertRejected() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
    }

    public CaseWorkItemAsserter<RA, WI> assertClosed() {
        assertThat(getWorkItem().getCloseTimestamp()).as("closeTimestamp").isNotNull();
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertNotClosed() {
        assertThat(getWorkItem().getCloseTimestamp()).as("closeTimestamp").isNull();
        return this;
    }

    public CaseWorkItemAsserter<RA, WI> assertEscalationLevelNumber(int expected) {
        assertThat(getEscalationLevelNumber()).as("escalation level number").isEqualTo(expected);
        return this;
    }

    private int getEscalationLevelNumber() {
        return WorkItemTypeUtil.getEscalationLevelNumber(
                getWorkItem());
    }

    public CaseWorkItemAsserter<RA, WI> assertDeadlineApproximately(String duration) {
        if (duration == null) {
            assertThat(getDeadline()).as("deadline").isNull();
        } else {
            long expected = XmlTypeConverter.toMillis(
                    XmlTypeConverter.fromNow(duration));
            assertThat(getDeadlineMillis())
                    .as("deadline (millis)")
                    .isBetween(expected - DEADLINE_TOLERANCE, expected + DEADLINE_TOLERANCE);
        }
        return this;
    }

    private XMLGregorianCalendar getDeadline() {
        return getWorkItem().getDeadline();
    }

    // 0 if null
    private long getDeadlineMillis() {
        return XmlTypeConverter.toMillis(getDeadline());
    }

    public WI getRealValue() {
        return getWorkItem();
    }
}
