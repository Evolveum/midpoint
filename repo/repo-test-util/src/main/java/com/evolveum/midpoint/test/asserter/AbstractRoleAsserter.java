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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;

/**
 * @author semancik
 *
 */
public class AbstractRoleAsserter<F extends AbstractRoleType, RA> extends FocusAsserter<F,RA> {

    public AbstractRoleAsserter(PrismObject<F> focus) {
        super(focus);
    }

    public AbstractRoleAsserter(PrismObject<F> focus, String details) {
        super(focus, details);
    }

    public AbstractRoleAsserter(PrismObject<F> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static <F extends AbstractRoleType> AbstractRoleAsserter<F,Void> forAbstractRole(PrismObject<F> focus) {
        return new AbstractRoleAsserter<>(focus);
    }

    public static <F extends AbstractRoleType> AbstractRoleAsserter<F,Void> forAbstractRole(PrismObject<F> focus, String details) {
        return new AbstractRoleAsserter<>(focus, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public AbstractRoleAsserter<F,RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertAdministrativeStatus(ActivationStatusType expected) {
        super.assertAdministrativeStatus(expected);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> display() {
        super.display();
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public ActivationAsserter<? extends AbstractRoleAsserter<F,RA>> activation() {
        ActivationAsserter<AbstractRoleAsserter<F,RA>> asserter = new ActivationAsserter<>(getObject().asObjectable().getActivation(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public LinksAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> links() {
        LinksAsserter<F, AbstractRoleAsserter<F,RA>, RA> asserter = new LinksAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertLiveLinks(int expected) {
        super.assertLiveLinks(expected);
        return this;
    }

    @Override
    public AssignmentsAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> assignments() {
        AssignmentsAsserter<F, AbstractRoleAsserter<F,RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertLocality(String expectedOrig) {
        super.assertLocality(expectedOrig);
        return this;
    }

    public AbstractRoleAsserter<F,RA> assertDisplayName(String expectedOrig) {
        assertPolyStringProperty(AbstractRoleType.F_DISPLAY_NAME, expectedOrig);
        return this;
    }

    public AbstractRoleAsserter<F,RA> assertIdentifier(String expectedOrig) {
        assertPropertyEquals(AbstractRoleType.F_IDENTIFIER, expectedOrig);
        return this;
    }

    public AbstractRoleAsserter<F,RA> assertRiskLevel(String expected) {
        assertPropertyEquals(AbstractRoleType.F_RISK_LEVEL, expected);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        super.assertHasProjectionOnResource(resourceOid);
        return this;
    }

    @Override
    public ShadowAsserter<? extends AbstractRoleAsserter<F,RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        return super.projectionOnResource(resourceOid);
    }

    @Override
    public AbstractRoleAsserter<F,RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
        super.displayWithProjections();
        return this;
    }

    @Override
    public ShadowReferenceAsserter<? extends AbstractRoleAsserter<F,RA>> singleLink() {
        return (ShadowReferenceAsserter<AbstractRoleAsserter<F,RA>>) super.singleLink();
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertAssignments(int expected) {
        super.assertAssignments(expected);
        return this;
    }

    @Override
    public ParentOrgRefsAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> parentOrgRefs() {
        ParentOrgRefsAsserter<F, AbstractRoleAsserter<F,RA>, RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertParentOrgRefs(String... expectedOids) {
        super.assertParentOrgRefs(expectedOids);
        return this;
    }

    @Override
    public RoleMembershipRefsAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> roleMembershipRefs() {
        RoleMembershipRefsAsserter<F,AbstractRoleAsserter<F,RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertRoleMembershipRefs(int expected) {
        super.assertRoleMembershipRefs(expected);
        return this;
    }

    @Override
    public ExtensionAsserter<F, ? extends AbstractRoleAsserter<F, RA>> extension() {
        ExtensionAsserter<F, AbstractRoleAsserter<F, RA>> asserter = new ExtensionAsserter<>(getObjectable(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public TriggersAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> triggers() {
        TriggersAsserter<F, ? extends AbstractRoleAsserter<F,RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertArchetypeRef(String expectedArchetypeOid) {
        super.assertArchetypeRef(expectedArchetypeOid);
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertNoArchetypeRef() {
        super.assertNoArchetypeRef();
        return this;
    }

    @Override
    public AbstractRoleAsserter<F,RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }
}
