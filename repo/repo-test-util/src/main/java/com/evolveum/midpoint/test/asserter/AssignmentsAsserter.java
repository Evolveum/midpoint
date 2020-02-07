/*
 * Copyright (c) 2018 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author semancik
 *
 */
public class AssignmentsAsserter<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> extends AbstractAsserter<FA> {

    private FA focusAsserter;
    private List<AssignmentType> assignments;

    public AssignmentsAsserter(FA focusAsserter) {
        super();
        this.focusAsserter = focusAsserter;
    }

    public AssignmentsAsserter(FA focusAsserter, String details) {
        super(details);
        this.focusAsserter = focusAsserter;
    }

    public static <F extends FocusType> AssignmentsAsserter<F,FocusAsserter<F,Void>,Void> forFocus(PrismObject<F> focus) {
        return new AssignmentsAsserter<>(FocusAsserter.forFocus(focus));
    }

    List<AssignmentType> getAssignments() {
        if (assignments == null) {
            assignments = getFocus().asObjectable().getAssignment();
        }
        return assignments;
    }

    public AssignmentsAsserter<F, FA, RA> assertAssignments(int expected) {
        assertEquals("Wrong number of assignments in " + desc(), expected, getAssignments().size());
        return this;
    }

    public AssignmentsAsserter<F, FA, RA> assertNone() {
        assertAssignments(0);
        return this;
    }

    AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> forAssignment(AssignmentType assignment, PrismObject<?> target) {
        AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> asserter = new AssignmentAsserter<>(assignment, target, this, "assignment in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> single() {
        assertAssignments(1);
        return forAssignment(getAssignments().get(0), null);
    }

    PrismObject<F> getFocus() {
        return focusAsserter.getObject();
    }

    @Override
    public FA end() {
        return focusAsserter;
    }

    @Override
    protected String desc() {
        return descWithDetails("assignments of "+getFocus());
    }

    public AssignmentFinder<F,FA,RA> by() {
        return new AssignmentFinder<>(this);
    }

    public AssignmentAsserter<AssignmentsAsserter<F,FA,RA>> forRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        return by()
            .targetOid(roleOid)
            .targetType(RoleType.COMPLEX_TYPE)
            .find();
    }

    public AssignmentsAsserter<F,FA,RA> assertRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(roleOid)
            .targetType(RoleType.COMPLEX_TYPE)
            .find();
        return this;
    }

    public AssignmentsAsserter<F,FA,RA> assertRole(String roleOid, QName relation) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(roleOid)
            .targetType(RoleType.COMPLEX_TYPE)
            .targetRelation(relation)
            .find();
        return this;
    }

    public AssignmentsAsserter<F,FA,RA> assertNoRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(roleOid)
            .targetType(RoleType.COMPLEX_TYPE)
            .assertNone();
        return this;
    }

    public AssignmentsAsserter<F,FA,RA> assertNoRole() throws ObjectNotFoundException, SchemaException {
        by()
            .targetType(RoleType.COMPLEX_TYPE)
            .assertNone();
        return this;
    }

    public AssignmentsAsserter<F,FA,RA> assertOrg(String orgOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(orgOid)
            .targetType(OrgType.COMPLEX_TYPE)
            .find();
        return this;
    }

    public AssignmentsAsserter<F,FA,RA> assertArchetype(String archetypeOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(archetypeOid)
            .targetType(ArchetypeType.COMPLEX_TYPE)
            .find();
        return this;
    }

}
