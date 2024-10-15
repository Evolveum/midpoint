/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author semancik
 */
public class RoleAsserter<RA> extends AbstractRoleAsserter<RoleType,RA> {

    public RoleAsserter(PrismObject<RoleType> focus) {
        super(focus);
    }

    public RoleAsserter(PrismObject<RoleType> focus, String details) {
        super(focus, details);
    }

    public RoleAsserter(PrismObject<RoleType> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static RoleAsserter<Void> forRole(PrismObject<RoleType> focus) {
        return new RoleAsserter<>(focus);
    }

    public static RoleAsserter<Void> forRole(PrismObject<RoleType> focus, String details) {
        return new RoleAsserter<>(focus, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public RoleAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public RoleAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public RoleAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertRiskLevel(String expected) {
        super.assertRiskLevel(expected);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    @Override
    public RoleAsserter<RA> assertAdministrativeStatus(ActivationStatusType expected) {
        super.assertAdministrativeStatus(expected);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertEffectiveMark(String oid) {
        super.assertEffectiveMark(oid);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertEffectiveMarks(String... oids) {
        super.assertEffectiveMarks(oids);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertNoEffectiveMark(String oid) {
        super.assertNoEffectiveMark(oid);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertNoEffectiveMarks() {
        super.assertNoEffectiveMarks();
        return this;
    }

    @Override
    public RoleAsserter<RA> display() {
        super.display();
        return this;
    }

    @Override
    public RoleAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public ActivationAsserter<RoleAsserter<RA>> activation() {
        ActivationAsserter<RoleAsserter<RA>> asserter = new ActivationAsserter<>(getObject().asObjectable().getActivation(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public LinksAsserter<RoleType, RoleAsserter<RA>, RA> links() {
        LinksAsserter<RoleType, RoleAsserter<RA>, RA> asserter = new LinksAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public RoleAsserter<RA> assertLiveLinks(int expected) {
        super.assertLiveLinks(expected);
        return this;
    }

    @Override
    public AssignmentsAsserter<RoleType, RoleAsserter<RA>, RA> assignments() {
        AssignmentsAsserter<RoleType, RoleAsserter<RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public RoleAsserter<RA> assertDisplayName(String expectedOrig) {
        super.assertDisplayName(expectedOrig);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertLocality(String expectedOrig) {
        super.assertLocality(expectedOrig);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertIdentifier(String expectedOrig) {
        super.assertIdentifier(expectedOrig);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        super.assertHasProjectionOnResource(resourceOid);
        return this;
    }

    @Override
    public ShadowAsserter<RoleAsserter<RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        return (ShadowAsserter<RoleAsserter<RA>>) super.projectionOnResource(resourceOid);
    }

    @Override
    public RoleAsserter<RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
        super.displayWithProjections();
        return this;
    }

    @Override
    public ShadowReferenceAsserter<RoleAsserter<RA>> singleLink() {
        return (ShadowReferenceAsserter<RoleAsserter<RA>>) super.singleLink();
    }

    @Override
    public RoleAsserter<RA> assertAssignments(int expected) {
        super.assertAssignments(expected);
        return this;
    }

    @Override
    public ParentOrgRefsAsserter<RoleType, RoleAsserter<RA>, RA> parentOrgRefs() {
        ParentOrgRefsAsserter<RoleType, RoleAsserter<RA>, RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public RoleAsserter<RA> assertParentOrgRefs(String... expectedOids) {
        super.assertParentOrgRefs(expectedOids);
        return this;
    }

    @Override
    public RoleMembershipRefsAsserter<RoleType, ? extends RoleAsserter<RA>, RA> roleMembershipRefs() {
        RoleMembershipRefsAsserter<RoleType,RoleAsserter<RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public RoleAsserter<RA> assertRoleMembershipRefs(int expected) {
        super.assertRoleMembershipRefs(expected);
        return this;
    }

    @Override
    public ArchetypeRefsAsserter<RoleType, ? extends RoleAsserter<RA>, RA> archetypesRefs() {
        ArchetypeRefsAsserter<RoleType,RoleAsserter<RA>,RA> asserter = new ArchetypeRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public RoleAsserter<RA> assertArchetypeRefs(int expected) {
        archetypesRefs().assertArchetypeRefs(expected);
        return this;
    }

    @Override
    public ExtensionAsserter<RoleType, ? extends RoleAsserter<RA>> extension() {
        ExtensionAsserter<RoleType, RoleAsserter<RA>> asserter = new ExtensionAsserter<>(getObjectable(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public TriggersAsserter<RoleType, ? extends RoleAsserter<RA>, RA> triggers() {
        TriggersAsserter<RoleType, ? extends RoleAsserter<RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public RoleAsserter<RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertArchetypeRef(String expectedArchetypeOid) {
        super.assertArchetypeRef(expectedArchetypeOid);
        return this;
    }

    @Override
    public RoleAsserter<RA> assertNoArchetypeRef() {
        super.assertNoArchetypeRef();
        return this;
    }

    @Override
    public RoleAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }
}
