/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Tests for mistakenly adding new empty items (PP, PR, PC) into PCVs.
 *
 * @author mederly
 */
public class TestEmptyItems extends AbstractSchemaTest {

    @Test
    public void testEmptyItemsOnGet() {
        // GIVEN
        UserType user = new UserType(getPrismContext());
        System.out.println("User before:\n" + user.asPrismObject().debugDump());
        assertEquals("Wrong # of user sub-items before 'get' operations", 0, user.asPrismContainerValue().size());

        // WHEN
        user.getAssignment();
        user.getLinkRef();
        user.getEmployeeType();

        // THEN
        System.out.println("User after:\n" + user.asPrismObject().debugDump());
        assertEquals("Wrong # of user sub-items after 'get' operations", 0, user.asPrismContainerValue().size());
    }

    @Test
    public void testEmptyItemsOnParse() throws Exception {
        // GIVEN
        UserType user = new UserType(getPrismContext());
        user.setName(PolyStringType.fromOrig("jack"));
        System.out.println("User before:\n" + user.asPrismObject().debugDump());
        assertEquals("Wrong # of user sub-items before serialization/reparsing", 1, user.asPrismContainerValue().size());

        // WHEN
        String xml = getPrismContext().xmlSerializer().serialize(user.asPrismObject());
        user = (UserType) getPrismContext().parserFor(xml).parse().asObjectable();

        // THEN
        System.out.println("User after:\n" + user.asPrismObject().debugDump());
        assertEquals("Wrong # of user sub-items after serialization/reparsing", 1, user.asPrismContainerValue().size());
    }

    @Test
    public void testEmptyItemsOnConstructed() {
        UserType jack = new UserType(getPrismContext())
                .oid("00000000-0000-0000-0000-000000000002").version("42")
                .name("jack")
                .givenName("Jack")
                .familyName("Sparrow")
                .honorificPrefix("Cpt.")
                .honorificSuffix("PhD.")
                .beginAssignment()
                .beginActivation()
                .administrativeStatus(ActivationStatusType.ENABLED)
                .enableTimestamp("2016-12-31T23:59:59+01:00")
                .<AssignmentType>end()
                .beginConstruction()
                .resourceRef("00000000-1233-4443-3123-943412324212", ResourceType.COMPLEX_TYPE)
                .<AssignmentType>end()
                .<UserType>end()
                .beginAssignment()
                .beginActivation()
                .validFrom("2017-01-01T12:00:00+01:00")
                .validTo("2017-03-31T00:00:00+01:00")
                .<AssignmentType>end()
                .targetRef("83138913-4329-4323-3432-432432143612", RoleType.COMPLEX_TYPE, SchemaConstants.ORG_APPROVER)
                .<UserType>end()
                .employeeType("pirate")
                .employeeType("captain")
                .organization("O123456");
        System.out.println("User:\n" + jack.asPrismObject().debugDump());

        assertEquals("Wrong # of user sub-items before 'get' operations", 8, jack.asPrismContainerValue().size());

        // WHEN
        jack.getAssignment();
        jack.getLinkRef();
        jack.getEmployeeType();

        // THEN
        System.out.println("User after:\n" + jack.asPrismObject().debugDump());
        assertEquals("Wrong # of user sub-items after 'get' operations", 8, jack.asPrismContainerValue().size());
    }
}
