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
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author semancik
 */
public class OrgAsserter<RA> extends AbstractRoleAsserter<OrgType,RA> {

    public OrgAsserter(PrismObject<OrgType> focus) {
        super(focus);
    }

    public OrgAsserter(PrismObject<OrgType> focus, String details) {
        super(focus, details);
    }

    public OrgAsserter(PrismObject<OrgType> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static OrgAsserter<Void> forOrg(PrismObject<OrgType> focus) {
        return new OrgAsserter<>(focus);
    }

    public static OrgAsserter<Void> forOrg(PrismObject<OrgType> focus, String details) {
        return new OrgAsserter<>(focus, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public OrgAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public OrgAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public OrgAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    @Override
    public OrgAsserter<RA> assertAdministrativeStatus(ActivationStatusType expected) {
        super.assertAdministrativeStatus(expected);
        return this;
    }

    @Override
    public OrgAsserter<RA> display() {
        super.display();
        return this;
    }

    @Override
    public OrgAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public ActivationAsserter<OrgAsserter<RA>> activation() {
        ActivationAsserter<OrgAsserter<RA>> asserter = new ActivationAsserter<>(getObject().asObjectable().getActivation(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public LinksAsserter<OrgType, OrgAsserter<RA>, RA> links() {
        LinksAsserter<OrgType, OrgAsserter<RA>, RA> asserter = new LinksAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public OrgAsserter<RA> assertLiveLinks(int expected) {
        super.assertLiveLinks(expected);
        return this;
    }

    @Override
    public AssignmentsAsserter<OrgType, OrgAsserter<RA>, RA> assignments() {
        AssignmentsAsserter<OrgType, OrgAsserter<RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public OrgAsserter<RA> assertDisplayName(String expectedOrig) {
        super.assertDisplayName(expectedOrig);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertLocality(String expectedOrig) {
        super.assertLocality(expectedOrig);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertIdentifier(String expectedOrig) {
        super.assertIdentifier(expectedOrig);
        return this;
    }

    public OrgAsserter<RA> assertTenant(Boolean expected) {
        assertPropertyEquals(OrgType.F_TENANT, expected);
        return this;
    }

    public OrgAsserter<RA> assertIsTenant() {
        assertPropertyEquals(OrgType.F_TENANT, true);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertRiskLevel(String expected) {
        super.assertRiskLevel(expected);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        super.assertHasProjectionOnResource(resourceOid);
        return this;
    }

    @Override
    public ShadowAsserter<OrgAsserter<RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        return (ShadowAsserter<OrgAsserter<RA>>) super.projectionOnResource(resourceOid);
    }

    @Override
    public OrgAsserter<RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
        super.displayWithProjections();
        return this;
    }

    @Override
    public ShadowReferenceAsserter<OrgAsserter<RA>> singleLink() {
        return (ShadowReferenceAsserter<OrgAsserter<RA>>) super.singleLink();
    }

    @Override
    public OrgAsserter<RA> assertAssignments(int expected) {
        super.assertAssignments(expected);
        return this;
    }

    @Override
    public ParentOrgRefsAsserter<OrgType, OrgAsserter<RA>, RA> parentOrgRefs() {
        ParentOrgRefsAsserter<OrgType, OrgAsserter<RA>, RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public OrgAsserter<RA> assertParentOrgRefs(String... expectedOids) {
        super.assertParentOrgRefs(expectedOids);
        return this;
    }

    @Override
    public RoleMembershipRefsAsserter<OrgType, ? extends OrgAsserter<RA>, RA> roleMembershipRefs() {
        RoleMembershipRefsAsserter<OrgType,OrgAsserter<RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public OrgAsserter<RA> assertRoleMembershipRefs(int expected) {
        super.assertRoleMembershipRefs(expected);
        return this;
    }

    @Override
    public ExtensionAsserter<OrgType, ? extends OrgAsserter<RA>> extension() {
        ExtensionAsserter<OrgType, OrgAsserter<RA>> asserter = new ExtensionAsserter<>(getObjectable(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public TriggersAsserter<OrgType, ? extends OrgAsserter<RA>, RA> triggers() {
        TriggersAsserter<OrgType, ? extends OrgAsserter<RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public OrgAsserter<RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertArchetypeRef(String expectedArchetypeOid) {
        super.assertArchetypeRef(expectedArchetypeOid);
        return this;
    }

    @Override
    public OrgAsserter<RA> assertNoArchetypeRef() {
        super.assertNoArchetypeRef();
        return this;
    }

    @Override
    public OrgAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }
}
