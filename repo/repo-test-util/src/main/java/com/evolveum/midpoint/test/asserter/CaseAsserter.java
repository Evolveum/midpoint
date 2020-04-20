/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.asserter.prism.PolyStringAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.prism.PrismObject.asObjectableList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author semancik
 */
public class CaseAsserter<RA> extends PrismObjectAsserter<CaseType,RA> {

    public CaseAsserter(PrismObject<CaseType> focus) {
        super(focus);
    }

    public CaseAsserter(PrismObject<CaseType> focus, String details) {
        super(focus, details);
    }

    public CaseAsserter(PrismObject<CaseType> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static CaseAsserter<Void> forCase(PrismObject<CaseType> object) {
        return new CaseAsserter<>(object);
    }

    public static CaseAsserter<Void> forCase(PrismObject<CaseType> object, String details) {
        return new CaseAsserter<>(object, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public CaseAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertNameOrig(String expectedOrig) {
        return (CaseAsserter<RA>) super.assertNameOrig(expectedOrig);
    }

    @Override
    public CaseAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public CaseAsserter<RA> display() {
        super.display();
        return this;
    }

    public CaseAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }


    @Override
    public PolyStringAsserter<CaseAsserter<RA>> name() {
        //noinspection unchecked
        return (PolyStringAsserter<CaseAsserter<RA>>)super.name();
    }

    @Override
    public CaseAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertArchetypeRef(String expectedArchetypeOid) {
        return (CaseAsserter<RA>) super.assertArchetypeRef(expectedArchetypeOid);
    }

    @Override
    public CaseAsserter<RA> assertNoArchetypeRef() {
        return (CaseAsserter<RA>) super.assertNoArchetypeRef();
    }

    public CaseAsserter<RA> assertOperationRequestArchetype() {
        return assertArchetypeRef(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value());
    }

    public CaseAsserter<RA> assertApprovalCaseArchetype() {
        return assertArchetypeRef(SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
    }

    public CaseAsserter<RA> assertClosed() {
        return assertState(SchemaConstants.CASE_STATE_CLOSED_QNAME);
    }

    private CaseAsserter<RA> assertState(QName expected) {
        MidPointAsserts.assertUriMatches(getObjectable().getState(), "state", expected);
        return this;
    }

    public CaseAsserter<RA> assertApproved() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
    }

    public CaseAsserter<RA> assertRejected() {
        return assertOutcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT);
    }

    private CaseAsserter<RA> assertOutcome(String expected) {
        MidPointAsserts.assertUriMatches(getObjectable().getOutcome(), "outcome", expected);
        return this;
    }

    public SubcasesAsserter<RA> subcases() {
        OperationResult result = new OperationResult(CaseAsserter.class.getName() + ".subcases");
        ObjectQuery query = getPrismContext().queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF).ref(getOid())
                .build();
        SearchResultList<PrismObject<CaseType>> subcases;
        try {
            subcases = getRepositoryService().searchObjects(CaseType.class, query, null, result);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        SubcasesAsserter<RA> asserter = new SubcasesAsserter<>(this, asObjectableList(subcases), getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public CaseAsserter<RA> assertObjectRef(String oid, QName typeName) {
        return assertReference(getObjectable().getObjectRef(), "objectRef", oid, typeName);
    }

    public CaseAsserter<RA> assertTargetRef(String oid, QName typeName) {
        return assertReference(getObjectable().getTargetRef(), "targetRef", oid, typeName);
    }

    private CaseAsserter<RA> assertReference(ObjectReferenceType ref, String desc, String oid, QName typeName) {
        MidPointAsserts.assertThatReferenceMatches(ref, desc, oid, typeName);
        return this;
    }

    public CaseAsserter<RA> assertStageNumber(int expected) {
        assertThat(getObjectable().getStageNumber())
                .as("stage number")
                .isEqualTo(expected);
        return this;
    }

    public CaseEventsAsserter<RA> events() {
        CaseEventsAsserter<RA> asserter = new CaseEventsAsserter<>(this, getObjectable().getEvent(), getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public CaseWorkItemsAsserter<RA> workItems() {
        CaseWorkItemsAsserter<RA> asserter = new CaseWorkItemsAsserter<>(this, getObjectable().getWorkItem(), getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
