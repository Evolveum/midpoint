/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class MidPointAsserts {

    public static <F extends AssignmentHolderType> AssignmentType assertAssigned(PrismObject<F> user, String targetOid, QName refType) {
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

    public static void assertAssigned(PrismObject<? extends FocusType> focus, String targetOid, QName refType, QName relation) {
        FocusType focusType = focus.asObjectable();
        for (AssignmentType assignmentType : focusType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (refType.equals(targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid()) &&
                            getPrismContext().relationMatches(targetRef.getRelation(), relation)) {
                        return;
                    }
                }
            }
        }
        AssertJUnit.fail(focus + " does not have assigned " + refType.getLocalPart() + " " + targetOid + ", relation " + relation);
    }

    public static <R extends AbstractRoleType> AssignmentType assertInduced(PrismObject<R> user, String targetOid, QName refType) {
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

    public static <F extends FocusType> void assertNotAssigned(PrismObject<F> user, String targetOid, QName refType) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (QNameUtil.match(refType, targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid())) {
                        AssertJUnit.fail(user + " does have assigned " + refType.getLocalPart() + " " + targetOid + " while not expecting it");
                    }
                }
            }
        }
    }

    public static <F extends FocusType> void assertNotAssigned(PrismObject<F> user, String targetOid, QName refType, QName relation) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (QNameUtil.match(refType, targetRef.getType())) {
                    if (targetOid.equals(targetRef.getOid()) &&
                            getPrismContext().relationMatches(targetRef.getRelation(), relation)) {
                        AssertJUnit.fail(user + " does have assigned " + refType.getLocalPart() + " " + targetOid + ", relation " + relation + "while not expecting it");
                    }
                }
            }
        }
    }

    public static <F extends AssignmentHolderType> void assertAssignments(PrismObject<F> user, int expectedNumber) {
        F userType = user.asObjectable();
        assertEquals("Unexpected number of assignments in " + user + ": " + userType.getAssignment(), expectedNumber, userType.getAssignment().size());
    }

    public static <R extends AbstractRoleType> void assertInducements(PrismObject<R> role, int expectedNumber) {
        R roleType = role.asObjectable();
        assertEquals("Unexpected number of inducements in " + role + ": " + roleType.getInducement(),
                expectedNumber, roleType.getInducement().size());
    }

    public static <F extends AssignmentHolderType> void assertAssignments(
            PrismObject<F> user, Class<?> expectedType, int expectedNumber) {
        F userType = user.asObjectable();
        int actualAssignments = 0;
        List<AssignmentType> assignments = userType.getAssignment();
        for (AssignmentType assignment : assignments) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null) {
                QName type = targetRef.getType();
                if (type != null) {
                    Class<? extends ObjectType> assignmentTargetClass = ObjectTypes.getObjectTypeFromTypeQName(type).getClassDefinition();
                    if (expectedType.isAssignableFrom(assignmentTargetClass)) {
                        actualAssignments++;
                    }
                }
            }
        }
        assertEquals("Unexpected number of assignments of type " + expectedType + " in " + user
                + ": " + userType.getAssignment(), expectedNumber, actualAssignments);
    }

    public static <F extends FocusType> void assertNoAssignments(PrismObject<F> user) {
        F userType = user.asObjectable();
        List<AssignmentType> assignments = userType.getAssignment();
        assertTrue(user + " does have assignments " + assignments + " while not expecting it", assignments.isEmpty());
    }

    public static <F extends FocusType> AssignmentType assertAssignedRole(PrismObject<F> user, String roleOid) {
        return assertAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
    }

    public static <F extends FocusType> void assertNotAssignedRole(PrismObject<F> user, String roleOid) {
        assertNotAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
    }

    public static <F extends FocusType> void assertAssignedRoles(PrismObject<F> user, String... roleOids) {
        assertAssignedTargets(user, "roles", RoleType.COMPLEX_TYPE, roleOids);
    }

    public static <F extends FocusType> void assertAssignedRoles(PrismObject<F> user, Collection<String> roleOids) {
        assertAssignedTargets(user, "roles", RoleType.COMPLEX_TYPE, roleOids);
    }

    public static <F extends FocusType> void assertAssignedOrgs(PrismObject<F> user, String... orgOids) {
        assertAssignedTargets(user, "orgs", OrgType.COMPLEX_TYPE, orgOids);
    }

    public static <F extends FocusType> void assertAssignedTargets(PrismObject<F> user, String typeDesc, QName type, String... expectedTargetOids) {
        List<String> haveTagetOids = getAssignedOids(user, type);
        PrismAsserts.assertSets("Wrong " + typeDesc + " in " + user, haveTagetOids, expectedTargetOids);
    }

    public static <F extends FocusType> void assertAssignedTargets(PrismObject<F> user, String typeDesc, QName type, Collection<String> expectedTargetOids) {
        List<String> haveTagetOids = getAssignedOids(user, type);
        PrismAsserts.assertSets("Wrong " + typeDesc + " in " + user, haveTagetOids, expectedTargetOids);
    }

    public static <R extends AbstractRoleType> AssignmentType assertInducedRole(PrismObject<R> user, String roleOid) {
        return assertInduced(user, roleOid, RoleType.COMPLEX_TYPE);
    }

    private static <F extends FocusType> List<String> getAssignedOids(PrismObject<F> user, QName type) {
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

    public static <F extends FocusType> void assertNotAssignedResource(PrismObject<F> user, String resourceOid) {
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

    public static <F extends FocusType> void assertAssignedResource(PrismObject<F> user, String resourceOid) {
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

    public static <F extends FocusType> void assertNotAssignedOrg(PrismObject<F> user, String orgOid, QName relation) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (OrgType.COMPLEX_TYPE.equals(targetRef.getType())) {
                    if (orgOid.equals(targetRef.getOid()) && getPrismContext().relationMatches(relation, targetRef.getRelation())) {
                        AssertJUnit.fail(user + " does have assigned OrgType " + orgOid + " with relation " + relation + " while not expecting it");
                    }
                }
            }
        }
    }

    public static AssignmentType assertAssignedOrg(PrismObject<? extends FocusType> focus, String orgOid) {
        return assertAssigned(focus, orgOid, OrgType.COMPLEX_TYPE);
    }

    public static void assertNotAssignedOrg(PrismObject<? extends FocusType> focus, String orgOid) {
        assertNotAssigned(focus, orgOid, OrgType.COMPLEX_TYPE);
    }

    public static void assertAssignedOrg(PrismObject<? extends FocusType> focus, String orgOid, QName relation) {
        assertAssigned(focus, orgOid, OrgType.COMPLEX_TYPE, relation);
    }

    public static <O extends ObjectType> void assertHasOrg(PrismObject<O> object, String orgOid) {
        for (ObjectReferenceType orgRef : object.asObjectable().getParentOrgRef()) {
            if (orgOid.equals(orgRef.getOid())) {
                return;
            }
        }
        AssertJUnit.fail(object + " does not have org " + orgOid);
    }

    public static <O extends ObjectType> boolean hasOrg(PrismObject<O> user, String orgOid, QName relation) {
        for (ObjectReferenceType orgRef : user.asObjectable().getParentOrgRef()) {
            if (orgOid.equals(orgRef.getOid()) &&
                    getPrismContext().relationMatches(relation, orgRef.getRelation())) {
                return true;
            }
        }
        return false;
    }

    public static <O extends ObjectType> void assertHasOrg(PrismObject<O> user, String orgOid, QName relation) {
        AssertJUnit.assertTrue(user + " does not have org " + orgOid + ", relation " + relation, hasOrg(user, orgOid, relation));
    }

    public static <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user, String orgOid, QName relation) {
        AssertJUnit.assertFalse(user + " has org " + orgOid + ", relation " + relation + " even if should NOT have it", hasOrg(user, orgOid, relation));
    }

    public static <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user) {
        assertTrue(user + " does have orgs " + user.asObjectable().getParentOrgRef() + " while not expecting them", user.asObjectable().getParentOrgRef().isEmpty());
    }

    public static <O extends ObjectType> void assertHasOrgs(PrismObject<O> user, int expectedNumber) {
        O userType = user.asObjectable();
        assertEquals("Unexpected number of orgs in " + user + ": " + userType.getParentOrgRef(), expectedNumber, userType.getParentOrgRef().size());
    }

    public static <O extends AssignmentHolderType> void assertHasArchetypes(PrismObject<O> object, int expectedNumber) {
        O objectable = object.asObjectable();
        assertEquals("Unexpected number of archetypes in " + object + ": " + objectable.getArchetypeRef(), expectedNumber, objectable.getArchetypeRef().size());
    }

    public static <AH extends AssignmentHolderType> void assertHasArchetype(PrismObject<AH> object, String oid) {
        AssertJUnit.assertTrue(object + " does not have archetype " + oid, ObjectTypeUtil.hasArchetype(object, oid));
    }

    public static <O extends ObjectType> void assertVersionIncrease(
            PrismObject<O> objectOld, PrismObject<O> objectNew) {
        long versionOld = parseVersion(objectOld);
        long versionNew = parseVersion(objectNew);
        assertTrue("Version not increased (from " + versionOld + " to " + versionNew + ")",
                versionOld < versionNew);
    }

    public static <O extends ObjectType> long parseVersion(PrismObject<O> object) {
        String version = object.getVersion();
        return Long.parseLong(version);
    }

    public static <O extends ObjectType> void assertVersion(PrismObject<O> object, int expectedVersion) {
        assertVersion(object, Integer.toString(expectedVersion));
    }

    public static <O extends ObjectType> void assertVersion(PrismObject<O> object, String expectedVersion) {
        assertEquals("Wrong version for " + object, expectedVersion, object.getVersion());
    }

    public static <O extends ObjectType> void assertOid(PrismObject<O> object, String expectedOid) {
        assertEquals("Wrong OID for " + object, expectedOid, object.getOid());
    }

    @UnusedTestElement
    public static void assertContainsCaseIgnore(String message, Collection<String> actualValues, String expectedValue) {
        AssertJUnit.assertNotNull(message + ", expected " + expectedValue + ", got null", actualValues);
        for (String actualValue : actualValues) {
            if (StringUtils.equalsIgnoreCase(actualValue, expectedValue)) {
                return;
            }
        }
        AssertJUnit.fail(message + ", expected " + expectedValue + ", got " + actualValues);
    }

    public static void assertNotContainsCaseIgnore(String message, Collection<String> actualValues, String expectedValue) {
        if (actualValues == null) {
            return;
        }
        for (String actualValue : actualValues) {
            if (StringUtils.equalsIgnoreCase(actualValue, expectedValue)) {
                AssertJUnit.fail(message + ", expected that value " + expectedValue + " will not be present but it is");
            }
        }
    }

    public static void assertObjectClass(ShadowType shadow, QName expectedStructuralObjectClass, QName... expectedAuxiliaryObjectClasses) {
        assertEquals("Wrong object class in " + shadow, expectedStructuralObjectClass, shadow.getObjectClass());
        PrismAsserts.assertEqualsCollectionUnordered("Wrong auxiliary object classes in " + shadow, shadow.getAuxiliaryObjectClass(), expectedAuxiliaryObjectClasses);
    }

    public static void assertInstanceOf(String message, Object object, Class<?> expectedClass) {
        assertNotNull(message + " is null", object);
        assertTrue(message + " is not instance of " + expectedClass + ", it is " + object.getClass(),
                expectedClass.isAssignableFrom(object.getClass()));
    }

    private static PrismContext getPrismContext() {
        return TestSpringContextHolder.getPrismContext();
    }

    public static void assertThatReferenceMatches(ObjectReferenceType ref, String desc, String expectedOid, QName expectedType) {
        assertThat(ref).as(desc).isNotNull();
        assertThat(ref.getOid()).as(desc + ".oid").isEqualTo(expectedOid);
        assertThatTypeMatches(ref.getType(), desc + ".type", expectedType);
    }

    // here because of AssertJ dependency (consider moving)
    public static void assertThatTypeMatches(QName actualType, String desc, QName expectedType) {
        assertThat(actualType).as(desc)
                .matches(t -> QNameUtil.match(t, expectedType), "matches " + expectedType);
    }

    // here because of AssertJ dependency (consider moving)
    public static void assertUriMatches(String current, String desc, QName expected) {
        assertThat(current).as(desc)
                .isNotNull()
                .matches(s -> QNameUtil.match(QNameUtil.uriToQName(s, true), expected), "is " + expected);
    }

    // here because of AssertJ dependency (consider moving)
    public static void assertUriMatches(String current, String desc, String expected) {
        assertThat(current).as(desc)
                .isNotNull()
                .matches(s -> QNameUtil.matchUri(s, expected), "is " + expected);
    }
}
