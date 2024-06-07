/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import static com.evolveum.midpoint.prism.Referencable.getOid;

/**
 * @author semancik
 */
public class AssignmentFinder<AH extends AssignmentHolderType, AHA extends AssignmentHolderAsserter<AH, RA>,RA> {

    private final AssignmentsAsserter<AH, AHA,RA> assignmentsAsserter;
    private String targetOid;
    private QName targetType;
    private QName targetRelation;
    private String resourceOid;
    private ShadowKindType shadowKind;
    private QName holderType;
    private String identifier;

    AssignmentFinder(AssignmentsAsserter<AH, AHA, RA> assignmentsAsserter) {
        this.assignmentsAsserter = assignmentsAsserter;
    }

    public AssignmentFinder<AH, AHA, RA> targetOid(String targetOid) {
        this.targetOid = targetOid;
        return this;
    }

    public AssignmentFinder<AH, AHA, RA> targetRelation(QName targetRelation) {
        this.targetRelation = targetRelation;
        return this;
    }

    public AssignmentFinder<AH, AHA, RA> targetType(QName targetType) {
        this.targetType = targetType;
        return this;
    }

    public AssignmentFinder<AH, AHA, RA> roleOid(String oid) {
        return targetOid(oid)
                .targetType(RoleType.COMPLEX_TYPE);
    }

    public AssignmentFinder<AH, AHA, RA> orgOid(String oid) {
        return targetOid(oid)
                .targetType(OrgType.COMPLEX_TYPE);
    }

    public AssignmentFinder<AH, AHA, RA> resourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
        return this;
    }

    public AssignmentFinder<AH, AHA, RA> shadowKind(ShadowKindType value) {
        this.shadowKind = value;
        return this;
    }

    public AssignmentFinder<AH, AHA, RA> accountOn(String resourceOid) {
        return resourceOid(resourceOid)
                .shadowKind(ShadowKindType.ACCOUNT);
    }

    public AssignmentFinder<AH, AHA, RA> assignmentRelationHolder(QName holderType) {
        this.holderType = holderType;
        return this;
    }

    public AssignmentFinder<AH, AHA, RA> identifier(@NotNull String identifier) {
        this.identifier = identifier;
        return this;
    }

    public AssignmentAsserter<AssignmentsAsserter<AH, AHA, RA>> find() throws ObjectNotFoundException, SchemaException {
        AssignmentType found = null;
        for (AssignmentType assignment: assignmentsAsserter.getAssignments()) {
            if (matches(assignment)) {
                if (found == null) {
                    found = assignment;
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
        for (AssignmentType assignment : assignmentsAsserter.getAssignments()) {
            if (matches(assignment)) {
                fail("Found assignment target while not expecting it: " + formatTarget(assignment));
            }
        }
        return assignmentsAsserter;
    }

    public AssignmentsAsserter<AH, AHA,RA> assertAll() throws ObjectNotFoundException, SchemaException {
        for (AssignmentType assignment : assignmentsAsserter.getAssignments()) {
            if (!matches(assignment)) {
                fail("Found assignment that does not match search criteria: " + formatTarget(assignment));
            }
        }
        return assignmentsAsserter;
    }

    private String formatTarget(AssignmentType assignment) {
        return assignment.getTargetRef().toString();
    }

    private boolean matches(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
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
            var construction = assignment.getConstruction();
            if (construction == null || !resourceOid.equals(getOid(construction.getResourceRef()))) {
                return false;
            }
        }

        if (shadowKind != null) {
            var construction = assignment.getConstruction();
            if (construction == null ||
                    shadowKind != Objects.requireNonNullElse(construction.getKind(), ShadowKindType.ACCOUNT)) {
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

        //noinspection RedundantIfStatement
        if (identifier != null && !identifier.equals(assignment.getIdentifier())) {
            return false;
        }

        // TODO: more criteria
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }
}
