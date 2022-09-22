/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Moved from MidPointAsserts and test-util, made non-static to allow prismContext in constructor.
 * It does not extend from AbstractAsserter which I found after it was moved from test-util,
 * where it was unnecessarily high in Maven module order.
 * <p>
 * TODO: rethink the name or eliminate somehow, there is already another AssignmentAsserter.
 * Most usages are one-liners in AbstractModelIntegrationTest, but that class is already so big,
 * I didn't want to inline it there.
 */
public class AssignmentAsserts {

    private final PrismContext prismContext;

    public AssignmentAsserts(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public <F extends AssignmentHolderType> AssignmentType assertAssigned(
            PrismObject<F> user, String targetOid, QName refType) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (refType.equals(targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid())) {
                        return assignmentType;
                    }
                }
            }
        }
        AssertJUnit.fail(user + " does not have assigned " + refType.getLocalPart() + " " + targetOid);
        return null; // not reachable
    }

    public void assertAssigned(
            PrismObject<? extends FocusType> focus, String targetOid, QName refType, QName relation) {
        FocusType focusType = focus.asObjectable();
        for (AssignmentType assignmentType : focusType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (refType.equals(targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid()) &&
                            prismContext.relationMatches(targetRef.getRelation(), relation)) {
                        return;
                    }
                }
            }
        }
        AssertJUnit.fail(focus + " does not have assigned " + refType.getLocalPart()
                + " " + targetOid + ", relation " + relation);
    }

    public <R extends AbstractRoleType> AssignmentType assertInduced(
            PrismObject<R> user, String targetOid, QName refType) {
        R userType = user.asObjectable();
        for (AssignmentType inducementType : userType.getInducement()) {
            ObjectReferenceType targetRef = inducementType.getTargetRef();
            if (targetRef != null) {
                if (refType.equals(targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid())) {
                        return inducementType;
                    }
                }
            }
        }
        AssertJUnit.fail(user + " does not have assigned " + refType.getLocalPart() + " " + targetOid);
        return null; // not reachable
    }

    public <F extends FocusType> void assertNotAssigned(
            PrismObject<F> user, String targetOid, QName refType) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (QNameUtil.match(refType, targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid())) {
                        AssertJUnit.fail(user + " does have assigned " + refType.getLocalPart()
                                + " " + targetOid + " while not expecting it");
                    }
                }
            }
        }
    }

    public <F extends FocusType> void assertNotAssigned(
            PrismObject<F> user, String targetOid, QName refType, QName relation) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (QNameUtil.match(refType, targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid()) &&
                            prismContext.relationMatches(targetRef.getRelation(), relation)) {
                        AssertJUnit.fail(user + " does have assigned " + refType.getLocalPart()
                                + " " + targetOid + ", relation " + relation + "while not expecting it");
                    }
                }
            }
        }
    }

    public <F extends AssignmentHolderType> void assertAssignments(
            PrismObject<F> user, int expectedNumber) {
        F userType = user.asObjectable();
        assertEquals("Unexpected number of assignments in " + user + ": "
                + userType.getAssignment(), expectedNumber, userType.getAssignment().size());
    }

    public <R extends AbstractRoleType> void assertInducements(
            PrismObject<R> role, int expectedNumber) {
        R roleType = role.asObjectable();
        assertEquals("Unexpected number of inducements in " + role + ": " + roleType.getInducement(),
                expectedNumber, roleType.getInducement().size());
    }

    public <F extends AssignmentHolderType> void assertAssignments(
            PrismObject<F> user, Class<?> expectedType, int expectedNumber) {
        F userType = user.asObjectable();
        int actualAssignments = 0;
        List<AssignmentType> assignments = userType.getAssignment();
        for (AssignmentType assignment : assignments) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null) {
                QName type = targetRef.getType();
                if (type != null) {
                    Class<? extends ObjectType> assignmentTargetClass =
                            ObjectTypes.getObjectTypeFromTypeQName(type).getClassDefinition();
                    if (expectedType.isAssignableFrom(assignmentTargetClass)) {
                        actualAssignments++;
                    }
                }
            }
        }
        assertEquals("Unexpected number of assignments of type " + expectedType + " in " + user
                + ": " + userType.getAssignment(), expectedNumber, actualAssignments);
    }

    public <F extends FocusType> void assertNoAssignments(PrismObject<F> user) {
        F userType = user.asObjectable();
        List<AssignmentType> assignments = userType.getAssignment();
        assertTrue(user + " does have assignments " + assignments + " while not expecting it", assignments.isEmpty());
    }

    public <F extends FocusType> AssignmentType assertAssignedRole(PrismObject<F> user, String roleOid) {
        return assertAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
    }

    public <F extends FocusType> void assertNotAssignedRole(PrismObject<F> user, String roleOid) {
        assertNotAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
    }

    public <F extends FocusType> void assertAssignedRoles(PrismObject<F> user, String... roleOids) {
        assertAssignedTargets(user, "roles", RoleType.COMPLEX_TYPE, roleOids);
    }

    public <F extends FocusType> void assertAssignedRoles(
            PrismObject<F> user, Collection<String> roleOids) {
        assertAssignedTargets(user, "roles", RoleType.COMPLEX_TYPE, roleOids);
    }

    public <F extends FocusType> void assertAssignedOrgs(
            PrismObject<F> user, String... orgOids) {
        assertAssignedTargets(user, "orgs", OrgType.COMPLEX_TYPE, orgOids);
    }

    public <F extends FocusType> void assertAssignedTargets(
            PrismObject<F> user, String typeDesc, QName type, String... expectedTargetOids) {
        List<String> haveTargetOids = getAssignedOids(user, type);
        com.evolveum.midpoint.prism.util.PrismAsserts.assertSets("Wrong " + typeDesc + " in " + user, haveTargetOids, expectedTargetOids);
    }

    public <F extends FocusType> void assertAssignedTargets(PrismObject<F> user, String typeDesc, QName type, Collection<String> expectedTargetOids) {
        List<String> haveTagetOids = getAssignedOids(user, type);
        com.evolveum.midpoint.prism.util.PrismAsserts.assertSets("Wrong " + typeDesc + " in " + user, haveTagetOids, expectedTargetOids);
    }

    public <R extends AbstractRoleType> AssignmentType assertInducedRole(PrismObject<R> user, String roleOid) {
        return assertInduced(user, roleOid, RoleType.COMPLEX_TYPE);
    }

    private <F extends FocusType> List<String> getAssignedOids(PrismObject<F> user, QName type) {
        F userType = user.asObjectable();
        List<String> haveTagetOids = new ArrayList<>();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (type.equals(targetRef.getType())) {
                    haveTagetOids.add(targetRef.getOid());
                }
            }
        }
        return haveTagetOids;
    }

    public <F extends FocusType> void assertNotAssignedResource(PrismObject<F> user, String resourceOid) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            if (assignmentType.getConstruction() == null) {
                continue;
            }
            ObjectReferenceType targetRef = assignmentType.getConstruction().getResourceRef();
            if (targetRef != null) {
                if (resourceOid.equals(targetRef.getOid())) {
                    AssertJUnit.fail(user + " does have assigned resource " + resourceOid + " while not expecting it");
                }
            }
        }
    }

    public <F extends FocusType> void assertAssignedResource(PrismObject<F> user, String resourceOid) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            if (assignmentType.getConstruction() == null) {
                continue;
            }
            ObjectReferenceType targetRef = assignmentType.getConstruction().getResourceRef();
            if (targetRef != null) {
                if (resourceOid.equals(targetRef.getOid())) {
                    return;
                }
            }
        }
        AssertJUnit.fail(user + " does NOT have assigned resource " + resourceOid + " while expecting it");
    }

    public <F extends FocusType> void assertNotAssignedOrg(PrismObject<F> user, String orgOid, QName relation) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (OrgType.COMPLEX_TYPE.equals(targetRef.getType())) {
                    if (orgOid.equals(targetRef.getOid()) && prismContext.relationMatches(relation, targetRef.getRelation())) {
                        AssertJUnit.fail(user + " does have assigned OrgType " + orgOid + " with relation " + relation + " while not expecting it");
                    }
                }
            }
        }
    }

    public AssignmentType assertAssignedOrg(PrismObject<? extends FocusType> focus, String orgOid) {
        return assertAssigned(focus, orgOid, OrgType.COMPLEX_TYPE);
    }

    public void assertNotAssignedOrg(PrismObject<? extends FocusType> focus, String orgOid) {
        assertNotAssigned(focus, orgOid, OrgType.COMPLEX_TYPE);
    }

    public void assertAssignedOrg(PrismObject<? extends FocusType> focus, String orgOid, QName relation) {
        assertAssigned(focus, orgOid, OrgType.COMPLEX_TYPE, relation);
    }

    public <O extends ObjectType> void assertHasOrg(PrismObject<O> object, String orgOid) {
        for (ObjectReferenceType orgRef : object.asObjectable().getParentOrgRef()) {
            if (orgOid.equals(orgRef.getOid())) {
                return;
            }
        }
        AssertJUnit.fail(object + " does not have org " + orgOid);
    }

    public <O extends ObjectType> boolean hasOrg(PrismObject<O> user, String orgOid, QName relation) {
        for (ObjectReferenceType orgRef : user.asObjectable().getParentOrgRef()) {
            if (orgOid.equals(orgRef.getOid()) &&
                    prismContext.relationMatches(relation, orgRef.getRelation())) {
                return true;
            }
        }
        return false;
    }

    public <O extends ObjectType> void assertHasOrg(PrismObject<O> user, String orgOid, QName relation) {
        AssertJUnit.assertTrue(user + " does not have org " + orgOid + ", relation " + relation, hasOrg(user, orgOid, relation));
    }

    public <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user, String orgOid, QName relation) {
        AssertJUnit.assertFalse(user + " has org " + orgOid + ", relation " + relation + " even if should NOT have it", hasOrg(user, orgOid, relation));
    }

    public <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user) {
        assertTrue(user + " does have orgs " + user.asObjectable().getParentOrgRef() + " while not expecting them", user.asObjectable().getParentOrgRef().isEmpty());
    }

    public <O extends ObjectType> void assertHasOrgs(PrismObject<O> user, int expectedNumber) {
        O userType = user.asObjectable();
        assertEquals("Unexpected number of orgs in " + user + ": " + userType.getParentOrgRef(), expectedNumber, userType.getParentOrgRef().size());
    }

    public <O extends AssignmentHolderType> void assertHasArchetypes(PrismObject<O> object, int expectedNumber) {
        O objectable = object.asObjectable();
        assertEquals("Unexpected number of archetypes in " + object + ": " + objectable.getArchetypeRef(), expectedNumber, objectable.getArchetypeRef().size());
    }

    public <AH extends AssignmentHolderType> void assertHasArchetype(PrismObject<AH> object, String oid) {
        AssertJUnit.assertTrue(object + " does not have archetype " + oid, ObjectTypeUtil.hasArchetypeRef(object, oid));
    }
}
