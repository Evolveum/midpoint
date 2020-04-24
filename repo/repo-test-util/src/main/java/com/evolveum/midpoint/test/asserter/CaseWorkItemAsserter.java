/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Asserts about CaseWorkItemType.
 */
public class CaseWorkItemAsserter<RA> extends PrismContainerValueAsserter<CaseWorkItemType, RA> {

    public CaseWorkItemAsserter(CaseWorkItemType workItem) {
        //noinspection unchecked
        super(workItem.asPrismContainerValue());
    }

    public CaseWorkItemAsserter(CaseWorkItemType workItem, String details) {
        //noinspection unchecked
        super(workItem.asPrismContainerValue(), details);
    }

    public CaseWorkItemAsserter(CaseWorkItemType workItem, RA returnAsserter, String details) {
        //noinspection unchecked
        super(workItem.asPrismContainerValue(), returnAsserter, details);
    }

    @NotNull
    private CaseWorkItemType getWorkItem() {
        return getPrismValue().asContainerable();
    }

    public CaseWorkItemAsserter<RA> assertOriginalAssigneeRef(String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(getWorkItem().getOriginalAssigneeRef(), "originalAssigneeRef", oid, typeName);
        return this;
    }

    public CaseWorkItemAsserter<RA> assertPerformerRef(String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(getWorkItem().getPerformerRef(), "performerRef", oid, typeName);
        return this;
    }

    public CaseWorkItemAsserter<RA> assertAssignees(String... oids) {
        PrismAsserts.assertReferenceValues(getWorkItem().getAssigneeRef(), oids);
        return this;
    }

    public CaseWorkItemAsserter<RA> assertStageNumber(Integer expected) {
        assertThat(getWorkItem().getStageNumber()).as("stage number").isEqualTo(expected);
        return this;
    }

    public CaseWorkItemAsserter<RA> assertOutcome(String expected) {
        AbstractWorkItemOutputType output = getWorkItem().getOutput();
        assertThat(output).as("output").isNotNull();
        MidPointAsserts.assertUriMatches(output.getOutcome(), "outcome", expected);
        return this;
    }

    public CaseWorkItemAsserter<RA> assertNameOrig(String expected) {
        assertThat(getWorkItem().getName().getOrig()).as("name.orig").isEqualTo(expected);
        return this;
    }

    public CaseWorkItemAsserter<RA> assertRejected() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
    }

    public CaseWorkItemAsserter<RA> assertClosed() {
        assertThat(getWorkItem().getCloseTimestamp()).as("closeTimestamp").isNotNull();
        return this;
    }
}
