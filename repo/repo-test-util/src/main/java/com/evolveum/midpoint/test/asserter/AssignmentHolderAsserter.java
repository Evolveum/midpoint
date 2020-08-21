/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import static org.testng.AssertJUnit.assertEquals;

public class AssignmentHolderAsserter<AH extends AssignmentHolderType, RA> extends PrismObjectAsserter<AH,RA> {

    public AssignmentHolderAsserter(PrismObject<AH> focus) {
        super(focus);
    }

    public AssignmentHolderAsserter(PrismObject<AH> focus, String details) {
        super(focus, details);
    }

    public AssignmentHolderAsserter(PrismObject<AH> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static <AH extends AssignmentHolderType> AssignmentHolderAsserter<AH, Void> forAssignemntHolder(PrismObject<AH> assignmentHolder) {
        return new AssignmentHolderAsserter<>(assignmentHolder);
    }

    public static <AH extends AssignmentHolderType> AssignmentHolderAsserter<AH, Void> forAssignemntHolder(PrismObject<AH> assignmentHolder, String details) {
        return new AssignmentHolderAsserter<>(assignmentHolder, details);
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertIndestructible(Boolean expected) {
        super.assertIndestructible(expected);
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertIndestructible() {
        super.assertIndestructible();
        return this;
    }

    @Override
    public AssignmentHolderAsserter<AH,RA> assertDestructible() {
        super.assertDestructible();
        return this;
    }

    public AssignmentsAsserter<AH, ? extends AssignmentHolderAsserter<AH,RA>, RA> assignments() {
        AssignmentsAsserter<AH,AssignmentHolderAsserter<AH,RA>,RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
