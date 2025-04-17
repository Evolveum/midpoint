/*
 * Copyright (C) 2018-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AssignmentsAsserter<AH extends AssignmentHolderType, AHA extends AssignmentHolderAsserter<AH, RA>, RA> extends AbstractAsserter<AHA> {

    private final AHA focusAsserter;
    private List<AssignmentType> assignments;

    public AssignmentsAsserter(AHA focusAsserter) {
        super();
        this.focusAsserter = focusAsserter;
    }

    public AssignmentsAsserter(AHA focusAsserter, String details) {
        super(details);
        this.focusAsserter = focusAsserter;
    }

    public static <AH extends AssignmentHolderType> AssignmentsAsserter<AH, AssignmentHolderAsserter<AH, Void>, Void> forFocus(PrismObject<AH> focus) {
        return new AssignmentsAsserter<>(AssignmentHolderAsserter.forAssignmentHolder(focus));
    }

    List<AssignmentType> getAssignments() {
        if (assignments == null) {
            assignments = getFocus().asObjectable().getAssignment();
        }
        return assignments;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertAssignments(int expected) {
        assertEquals("Wrong number of assignments in " + desc(), expected, getAssignments().size());
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertNone() {
        assertAssignments(0);
        return this;
    }

    AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> forAssignment(AssignmentType assignment) {
        AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> asserter = new AssignmentAsserter<>(assignment, this, "assignment in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> single() {
        assertAssignments(1);
        return forAssignment(getAssignments().get(0));
    }

    PrismObject<AH> getFocus() {
        return focusAsserter.getObject();
    }

    @Override
    public AHA end() {
        return focusAsserter;
    }

    @Override
    protected String desc() {
        return descWithDetails("assignments of " + getFocus());
    }

    public AssignmentFinder<AH, AHA, RA> by() {
        return new AssignmentFinder<>(this);
    }

    public AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> forRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        return by()
                .targetOid(roleOid)
                .targetType(RoleType.COMPLEX_TYPE)
                .find();
    }

    public AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> forOrg(String orgOid) throws ObjectNotFoundException, SchemaException {
        return by()
                .targetOid(orgOid)
                .targetType(OrgType.COMPLEX_TYPE)
                .find();
    }

    public AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> forService(String serviceOid) throws ObjectNotFoundException, SchemaException {
        return by()
                .targetOid(serviceOid)
                .targetType(ServiceType.COMPLEX_TYPE)
                .find();
    }

    public AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> forArchetype(String archetypeOid) throws ObjectNotFoundException, SchemaException {
        return by()
                .targetOid(archetypeOid)
                .targetType(ArchetypeType.COMPLEX_TYPE)
                .find();
    }

    public AssignmentsAsserter<AH, AHA, RA> assertRolePresence(String roleOid, boolean expected)
            throws ObjectNotFoundException, SchemaException {
        return expected ? assertRole(roleOid) : assertNoRole(roleOid);
    }

    public AssignmentsAsserter<AH, AHA, RA> assertRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(roleOid)
                .targetType(RoleType.COMPLEX_TYPE)
                .find();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertRole(String roleOid, QName relation) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(roleOid)
                .targetType(RoleType.COMPLEX_TYPE)
                .targetRelation(relation)
                .find();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertNoRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(roleOid)
                .targetType(RoleType.COMPLEX_TYPE)
                .assertNone();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertNoRole() throws ObjectNotFoundException, SchemaException {
        by()
                .targetType(RoleType.COMPLEX_TYPE)
                .assertNone();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertOrg(String orgOid) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(orgOid)
                .targetType(OrgType.COMPLEX_TYPE)
                .find();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertPolicy(String policyOid) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(policyOid)
                .targetType(PolicyType.COMPLEX_TYPE)
                .find();
        return this;
    }


    public AssignmentsAsserter<AH, AHA, RA> assertNoPolicy() throws ObjectNotFoundException, SchemaException {
        by()
                .targetType(PolicyType.COMPLEX_TYPE)
                .assertNone();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertArchetype(String archetypeOid) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(archetypeOid)
                .targetType(ArchetypeType.COMPLEX_TYPE)
                .find();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertAssignmentRelationHolder(QName holderType) throws SchemaException, ObjectNotFoundException {
        by().assignmentRelationHolder(holderType).find();
        return this;
    }

    public AssignmentsAsserter<AH, AHA, RA> assertAccount(String resourceOid) throws ObjectNotFoundException, SchemaException {
        by().accountOn(resourceOid).find();
        return this;
    }
}
