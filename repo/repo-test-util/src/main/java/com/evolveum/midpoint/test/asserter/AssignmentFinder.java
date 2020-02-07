/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class AssignmentFinder<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> {

    private final AssignmentsAsserter<F,FA,RA> assignmentsAsserter;
    private String targetOid;
    private QName targetType;
    private QName targetRelation;

    public AssignmentFinder(AssignmentsAsserter<F,FA,RA> assignmentsAsserter) {
        this.assignmentsAsserter = assignmentsAsserter;
    }

    public AssignmentFinder<F,FA,RA> targetOid(String targetOid) {
        this.targetOid = targetOid;
        return this;
    }

    public AssignmentFinder<F,FA,RA> targetRelation(QName targetRelation) {
        this.targetRelation = targetRelation;
        return this;
    }

    public AssignmentFinder<F,FA,RA> targetType(QName targetType) {
        this.targetType = targetType;
        return this;
    }

    public AssignmentAsserter<AssignmentsAsserter<F, FA, RA>> find() throws ObjectNotFoundException, SchemaException {
        AssignmentType found = null;
        PrismObject<?> foundTarget = null;
        for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
            PrismObject<ShadowType> assignmentTarget = null;
//            PrismObject<ShadowType> assignmentTarget = assignmentsAsserter.getTarget(assignment.getOid());
            if (matches(assignment, assignmentTarget)) {
                if (found == null) {
                    found = assignment;
                    foundTarget = assignmentTarget;
                } else {
                    fail("Found more than one assignment that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no assignment that matches search criteria");
        }
        return assignmentsAsserter.forAssignment(found, foundTarget);
    }

    public AssignmentsAsserter<F,FA,RA> assertNone() throws ObjectNotFoundException, SchemaException {
        for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
            PrismObject<ShadowType> assignmentTarget = null;
//            PrismObject<ShadowType> assignmentTarget = assignmentsAsserter.getTarget(assignment.getOid());
            if (matches(assignment, assignmentTarget)) {
                fail("Found assignment target while not expecting it: "+formatTarget(assignment, assignmentTarget));
            }
        }
        return assignmentsAsserter;
    }

    public AssignmentsAsserter<F,FA,RA> assertAll() throws ObjectNotFoundException, SchemaException {
        for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
            PrismObject<ShadowType> assignmentTarget = null;
//            PrismObject<ShadowType> assignmentTarget = assignmentsAsserter.getTarget(assignment.getOid());
            if (!matches(assignment, assignmentTarget)) {
                fail("Found assignment that does not match search criteria: "+formatTarget(assignment, assignmentTarget));
            }
        }
        return assignmentsAsserter;
    }

    private String formatTarget(AssignmentType assignment, PrismObject<ShadowType> assignmentTarget) {
        if (assignmentTarget != null) {
            return assignmentTarget.toString();
        }
        return assignment.getTargetRef().toString();
    }

    private boolean matches(AssignmentType assignment, PrismObject<?> targetObject) throws ObjectNotFoundException, SchemaException {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        ObjectType targetObjectType = null;
        if (targetObject != null) {
            targetObjectType = (ObjectType) targetObject.asObjectable();
        }

        if (targetOid != null) {
            if (targetRef == null || !targetOid.equals(targetRef.getOid())) {
                return false;
            }
        }

        if (targetType != null) {
            if (targetRef == null || !QNameUtil.match(targetType, targetRef.getType())) {
                return false;
            }
        }

        if (targetRelation != null) {
            if (targetRef == null || !QNameUtil.match(targetRelation, targetRef.getRelation())) {
                return false;
            }
        }

        // TODO: more criteria
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
