/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.List;

/**
 * @author semancik
 *
 */
public class AssignmentFinder<AH extends AssignmentHolderType, AHA extends AssignmentHolderAsserter<AH, RA>,RA> {

    private final AssignmentsAsserter<AH, AHA,RA> assignmentsAsserter;
    private String targetOid;
    private QName targetType;
    private QName targetRelation;
    private String resourceOid;
    private QName holderType;

    public AssignmentFinder(AssignmentsAsserter<AH, AHA,RA> assignmentsAsserter) {
        this.assignmentsAsserter = assignmentsAsserter;
    }

    public AssignmentFinder<AH, AHA,RA> targetOid(String targetOid) {
        this.targetOid = targetOid;
        return this;
    }

    public AssignmentFinder<AH, AHA,RA> targetRelation(QName targetRelation) {
        this.targetRelation = targetRelation;
        return this;
    }

    public AssignmentFinder<AH, AHA,RA> targetType(QName targetType) {
        this.targetType = targetType;
        return this;
    }

    public AssignmentFinder<AH, AHA,RA> resourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
        return this;
    }

    public AssignmentFinder<AH, AHA, RA> assignmentRelationHolder(QName holderType) {
        this.holderType = holderType;
        return this;
    }

    public AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> find() throws ObjectNotFoundException, SchemaException {
        AssignmentType found = null;
        PrismObject<?> foundTarget = null;
        for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
            PrismObject<ShadowType> assignmentTarget = null;
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
        return assignmentsAsserter.forAssignment(found);
    }

    public AssignmentsAsserter<AH, AHA,RA> assertNone() throws ObjectNotFoundException, SchemaException {
        for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
            PrismObject<ShadowType> assignmentTarget = null;
//            PrismObject<ShadowType> assignmentTarget = assignmentsAsserter.getTarget(assignment.getOid());
            if (matches(assignment, assignmentTarget)) {
                fail("Found assignment target while not expecting it: "+formatTarget(assignment, assignmentTarget));
            }
        }
        return assignmentsAsserter;
    }

    public AssignmentsAsserter<AH, AHA,RA> assertAll() throws ObjectNotFoundException, SchemaException {
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

        if (resourceOid != null) {
            if (assignment.getConstruction() == null || assignment.getConstruction().getResourceRef() == null
                    || !resourceOid.equals(assignment.getConstruction().getResourceRef().getOid())) {
                return false;
            }
        }

        if (holderType != null) {
            if (CollectionUtils.isEmpty(assignment.getAssignmentRelation())) {
                return false;
            }
            for (AssignmentRelationType assignmentRelationType : assignment.getAssignmentRelation()) {
                List<QName> holderTypes = assignmentRelationType.getHolderType();
                for (QName holder : holderTypes) {
                    if (QNameUtil.match(holder, holderType)) {
                        return true;
                    }
                }
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
