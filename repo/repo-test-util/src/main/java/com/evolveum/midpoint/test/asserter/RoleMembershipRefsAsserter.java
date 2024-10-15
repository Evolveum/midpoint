/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author semancik
 */
public class RoleMembershipRefsAsserter<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> extends AbstractAsserter<FA> {

    private FA focusAsserter;
    private List<PrismReferenceValue> roleMembershipRefs;

    public RoleMembershipRefsAsserter(FA focusAsserter) {
        super();
        this.focusAsserter = focusAsserter;
    }

    public RoleMembershipRefsAsserter(FA focusAsserter, String details) {
        super(details);
        this.focusAsserter = focusAsserter;
    }

    public static <F extends FocusType> RoleMembershipRefsAsserter<F,FocusAsserter<F,Void>,Void> forFocus(PrismObject<F> focus) {
        return new RoleMembershipRefsAsserter<>(FocusAsserter.forFocus(focus));
    }

    List<PrismReferenceValue> getRoleMembershipRefs() {
        if (roleMembershipRefs == null) {
            PrismReference linkRef = getFocus().findReference(FocusType.F_ROLE_MEMBERSHIP_REF);
            if (linkRef == null) {
                roleMembershipRefs = new ArrayList<>();
            } else {
                roleMembershipRefs = linkRef.getValues();
            }
        }
        return roleMembershipRefs;
    }

    public RoleMembershipRefsAsserter<F, FA, RA> assertRoleMemberhipRefs(int expected) {
        assertEquals("Wrong number of roleMembershipRefs in " + desc(), expected, getRoleMembershipRefs().size());
        return this;
    }

    public RoleMembershipRefsAsserter<F, FA, RA> assertNone() {
        assertRoleMemberhipRefs(0);
        return this;
    }

    RoleMembershipRefAsserter<RoleMembershipRefsAsserter<F, FA, RA>> forRef(PrismReferenceValue refVal, PrismObject<? extends FocusType> target) {
        RoleMembershipRefAsserter<RoleMembershipRefsAsserter<F, FA, RA>> asserter = new RoleMembershipRefAsserter<>(refVal, target, this, "roleMemberhipRef in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public RoleMembershipRefAsserter<RoleMembershipRefsAsserter<F, FA, RA>> single() {
        assertRoleMemberhipRefs(1);
        return forRef(getRoleMembershipRefs().get(0), null);
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
        return descWithDetails("roleMembershipRefs of "+getFocus());
    }

    public RoleMembershipRefFinder<F,FA,RA> by() {
        return new RoleMembershipRefFinder<>(this);
    }


    public RoleMembershipRefsAsserter<F,FA,RA> assertRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(roleOid)
            .targetType(RoleType.COMPLEX_TYPE)
            .find();
        return this;
    }

    public RoleMembershipRefsAsserter<F,FA,RA> assertRole(String roleOid, QName relation) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(roleOid)
            .targetType(RoleType.COMPLEX_TYPE)
            .relation(relation)
            .find();
        return this;
    }

    public RoleMembershipRefsAsserter<F,FA,RA> assertNoRole(String roleOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(roleOid)
            .targetType(RoleType.COMPLEX_TYPE)
            .assertNone();
        return this;
    }

    public RoleMembershipRefsAsserter<F,FA,RA> assertNoRole() throws ObjectNotFoundException, SchemaException {
        by()
            .targetType(RoleType.COMPLEX_TYPE)
            .assertNone();
        return this;
    }

    public RoleMembershipRefsAsserter<F,FA,RA> assertOrg(String orgOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(orgOid)
            .targetType(OrgType.COMPLEX_TYPE)
            .find();
        return this;
    }

    public RoleMembershipRefsAsserter<F,FA,RA> assertArchetype(String archetypeOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(archetypeOid)
            .targetType(ArchetypeType.COMPLEX_TYPE)
            .find();
        return this;
    }

    public RoleMembershipRefsAsserter<F,FA,RA> assertNoArchetype(String archetypeOid) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(archetypeOid)
                .targetType(ArchetypeType.COMPLEX_TYPE)
                .assertNone();
        return this;
    }

}
