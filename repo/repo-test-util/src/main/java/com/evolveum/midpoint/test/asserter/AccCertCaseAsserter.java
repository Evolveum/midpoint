/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class AccCertCaseAsserter<RA> extends PrismContainerValueAsserter<AccessCertificationCaseType,RA> {

    private final AccessCertificationCaseType aCase;

    public AccCertCaseAsserter(AccessCertificationCaseType aCase) {
        //noinspection unchecked
        super(aCase.asPrismContainerValue());
        this.aCase = aCase;
    }

    public AccCertCaseAsserter(AccessCertificationCaseType aCase, String details) {
        //noinspection unchecked
        super(aCase.asPrismContainerValue(), details);
        this.aCase = aCase;
    }

    public AccCertCaseAsserter(AccessCertificationCaseType aCase, RA returnAsserter, String details) {
        //noinspection unchecked
        super(aCase.asPrismContainerValue(), returnAsserter, details);
        this.aCase = aCase;
    }

    public static AccCertCaseAsserter<Void> forCase(AccessCertificationCaseType aCase) {
        return new AccCertCaseAsserter<>(aCase);
    }

    public static AccCertCaseAsserter<Void> forCase(AccessCertificationCaseType aCase, String details) {
        return new AccCertCaseAsserter<>(aCase, details);
    }

    public AccCertCaseAsserter<RA> assertApproved() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
    }

    public AccCertCaseAsserter<RA> assertRejected() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
    }

    private AccCertCaseAsserter<RA> assertOutcome(String expected) {
        MidPointAsserts.assertUriMatches(aCase.getOutcome(), "outcome", expected);
        return this;
    }

    private ObjectReferenceType getTargetRef() {
        return aCase.getTargetRef();
    }

    private String getOutcome() {
        return aCase.getOutcome();
    }

    public AccCertCaseAsserter<RA> assertObjectRef(@NotNull ObjectReferenceType ref) {
        return assertObjectRef(ref.getOid(), ref.getType());
    }

    public AccCertCaseAsserter<RA> assertObjectRef(String oid, QName typeName) {
        return assertReference(aCase.getObjectRef(), "objectRef", oid, typeName);
    }

    public AccCertCaseAsserter<RA> assertTargetRef(@NotNull ObjectReferenceType ref) {
        return assertTargetRef(ref.getOid(), ref.getType());
    }

    public AccCertCaseAsserter<RA> assertTargetRef(String oid, QName typeName) {
        return assertReference(getTargetRef(), "targetRef", oid, typeName);
    }

    public AccCertCaseAsserter<RA> assertNoTargetRef() {
        assertThat(getTargetRef())
                .withFailMessage("targetRef present in case even if it shouldn't be: " + desc())
                .isNull();
        return this;
    }

    private AccCertCaseAsserter<RA> assertReference(ObjectReferenceType ref, String desc, String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(ref, desc, oid, typeName);
        return this;
    }

    public AccCertCaseAsserter<RA> assertStageNumber(int expected) {
        assertThat(aCase.getStageNumber())
                .as("stage number")
                .isEqualTo(expected);
        return this;
    }

    public CaseEventsAsserter<RA> events() {
        throw new UnsupportedOperationException("TODO");
//        CaseEventsAsserter<RA> asserter = new CaseEventsAsserter<>(this, aCase.getEvent(), getDetails());
//        copySetupTo(asserter);
//        return asserter;
    }

    public CaseWorkItemsAsserter<AccCertCaseAsserter<RA>, AccessCertificationWorkItemType> workItems() {
        var asserter = new CaseWorkItemsAsserter<>(this, aCase.getWorkItem(), getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
