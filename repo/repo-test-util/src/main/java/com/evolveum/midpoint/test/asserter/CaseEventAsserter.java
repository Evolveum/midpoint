/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseEventType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventType;

import org.jetbrains.annotations.NotNull;

/**
 * Asserts about CaseEventType.
 */
public class CaseEventAsserter<RA> extends PrismContainerValueAsserter<CaseEventType, RA> {

    public CaseEventAsserter(CaseEventType event) {
        //noinspection unchecked
        super(event.asPrismContainerValue());
    }

    public CaseEventAsserter(CaseEventType event, String details) {
        //noinspection unchecked
        super(event.asPrismContainerValue(), details);
    }

    public CaseEventAsserter(CaseEventType event, RA returnAsserter, String details) {
        //noinspection unchecked
        super(event.asPrismContainerValue(), returnAsserter, details);
    }

    public CaseEventAsserter<RA> assertInitiatorRef(String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(getEvent().getInitiatorRef(), "initiatorRef", oid, typeName);
        return this;
    }

    public CaseEventAsserter<RA> assertAttorneyRef(String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(getEvent().getAttorneyRef(), "attorneyRef", oid, typeName);
        return this;
    }

    public CaseEventAsserter<RA> assertOriginalAssigneeRef(String oid, QName typeName) {
        CaseEventType event = getEvent();
        assertThat(event).isInstanceOf(WorkItemEventType.class);
        ObjectReferenceType originalAssigneeRef = ((WorkItemEventType) event).getOriginalAssigneeRef();
        MidPointAsserts.assertThatReferenceMatches(originalAssigneeRef, "originalAssigneeRef", oid, typeName);
        return this;
    }

    @NotNull
    private CaseEventType getEvent() {
        return getPrismValue().asContainerable();
    }
}
