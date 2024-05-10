/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.merger.threeway.item.ObjectTreeDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ThreeWayMergeTest extends AbstractSchemaTest {

    @Test
    public void testInitialObjectsConflicts() {
        File objects44Dir = new File("src/test/resources/objects/44");
    }

    @Test
    public void testSingleValuePropertyNoConflict() throws SchemaException {
        final String oid = UUID.randomUUID().toString();
        PrismContext ctx = PrismTestUtil.getPrismContext();

        // GIVEN
        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).add(List.of("My new description"))
                .asObjectDelta(oid);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).add(List.of("My new description"))
                .asObjectDelta(oid);

        // WHEN

        ObjectTreeDelta<UserType> leftTreeDelta = ObjectTreeDelta.fromItemDelta(leftToBase);
        ObjectTreeDelta<UserType> rightTreeDelta = ObjectTreeDelta.fromItemDelta(rightToBase);

        AssertJUnit.assertFalse(leftTreeDelta.hasConflictWith(rightTreeDelta));
    }

    @Test
    public void testSingleValuePropertyNoConflictAddReplace() throws SchemaException {
        final String oid = UUID.randomUUID().toString();
        PrismContext ctx = PrismTestUtil.getPrismContext();

        // GIVEN
        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).add(List.of("My new description"))
                .asObjectDelta(oid);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(List.of("My new description"))
                .asObjectDelta(oid);

        // WHEN

        ObjectTreeDelta<UserType> leftTreeDelta = ObjectTreeDelta.fromItemDelta(leftToBase);
        ObjectTreeDelta<UserType> rightTreeDelta = ObjectTreeDelta.fromItemDelta(rightToBase);

        AssertJUnit.assertFalse(leftTreeDelta.hasConflictWith(rightTreeDelta));
    }

    @Test
    public void testSingleValuePropertyConflict() throws SchemaException {
        final String oid = UUID.randomUUID().toString();
        PrismContext ctx = PrismTestUtil.getPrismContext();

        // GIVEN
        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).deleteRealValues(List.of("My old description"))
                .asObjectDelta(oid);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(List.of("My new description"))
                .asObjectDelta(oid);

        // WHEN

        ObjectTreeDelta<UserType> leftTreeDelta = ObjectTreeDelta.fromItemDelta(leftToBase);
        ObjectTreeDelta<UserType> rightTreeDelta = ObjectTreeDelta.fromItemDelta(rightToBase);

//        System.out.println("Left tree delta:\n" + leftTreeDelta.debugDump());
//        System.out.println("Right tree delta:\n" + rightTreeDelta.debugDump());

        System.out.println(leftTreeDelta.hasConflictWith(rightTreeDelta));

        AssertJUnit.assertTrue(leftTreeDelta.hasConflictWith(rightTreeDelta));
    }

    @Test
    public void testConflictAssignments() throws SchemaException {
        final String oid = UUID.randomUUID().toString();
        PrismContext ctx = PrismTestUtil.getPrismContext();

        // GIVEN

        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1L, AssignmentType.F_DESCRIPTION).addRealValues(List.of("new assignment 1 description"))
                .asObjectDelta(oid);

        AssignmentType a1 = new AssignmentType()
                .id(1L)
                .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(a1.asPrismContainerValue())
                .asObjectDelta(oid);

        UserType u1 = new UserType();
        u1.setName(new PolyStringType("jdoe"));
        u1.setOid(leftToBase.getOid());
        u1.getOrganization().addAll(List.of(new PolyStringType("org1"), new PolyStringType("org2")));
        u1.getAssignment().add(a1.clone());

        System.out.println("User:\n" + u1.asPrismObject().debugDump());

        // WHEN

        ObjectTreeDelta<UserType> leftTreeDelta = ObjectTreeDelta.fromItemDelta(leftToBase);
        ObjectTreeDelta<UserType> rightTreeDelta = ObjectTreeDelta.fromItemDelta(rightToBase);

        System.out.println("Left tree delta:\n" + leftToBase.debugDump());
        System.out.println("Right tree delta:\n" + rightToBase.debugDump());

        System.out.println("Left tree delta:\n" + leftTreeDelta.debugDump());
        System.out.println("Right tree delta:\n" + rightTreeDelta.debugDump());
    }

    // todo this fails because we would have to use base object to figure out whether these two deltas will do the same thing
    @Test
    public void conflictAssignmentAndPropertyAdd() throws SchemaException {
        final String oid = UUID.randomUUID().toString();
        PrismContext ctx = PrismTestUtil.getPrismContext();

        // GIVEN
        AssignmentType a1 = new AssignmentType()
                .id(1L)
                .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE);

        AssignmentType a1new = new AssignmentType()
                .id(1L)
                .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE)
                .description("New description");

        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                    .delete(a1.asPrismContainerValue())
                    .add(a1new.asPrismContainerValue())
                .asObjectDelta(oid);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1L, AssignmentType.F_DESCRIPTION).replace(List.of("New other description"))
                .asObjectDelta(oid);

        // WHEN
        ObjectTreeDelta<UserType> leftTreeDelta = ObjectTreeDelta.fromItemDelta(leftToBase);
        ObjectTreeDelta<UserType> rightTreeDelta = ObjectTreeDelta.fromItemDelta(rightToBase);

        // THEN
        AssertJUnit.assertTrue(leftTreeDelta.hasConflictWith(rightTreeDelta));
    }


    // todo this doesn't fail only by accident, because current algorithm
    //  doesn't check for modification type in other subtree, just on the same level...
    //  FIX ME
    @Test
    public void noConflictAssignmentAndPropertyAdd() throws SchemaException {
        final String oid = UUID.randomUUID().toString();
        PrismContext ctx = PrismTestUtil.getPrismContext();

        // GIVEN
        AssignmentType a1 = new AssignmentType()
                .id(1L)
                .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE);

        AssignmentType a1new = new AssignmentType()
                .id(1L)
                .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE)
                .description("New description");

        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(a1.asPrismContainerValue())
                .add(a1new.asPrismContainerValue())
                .asObjectDelta(oid);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1L, AssignmentType.F_DESCRIPTION).replace(List.of("New description"))
                .asObjectDelta(oid);

        // WHEN
        ObjectTreeDelta<UserType> leftTreeDelta = ObjectTreeDelta.fromItemDelta(leftToBase);
        ObjectTreeDelta<UserType> rightTreeDelta = ObjectTreeDelta.fromItemDelta(rightToBase);

        // THEN
        AssertJUnit.assertFalse(leftTreeDelta.hasConflictWith(rightTreeDelta));
    }
}
