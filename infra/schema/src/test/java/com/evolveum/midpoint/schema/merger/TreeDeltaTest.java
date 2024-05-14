/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.delta.Conflict;
import com.evolveum.midpoint.schema.delta.ObjectTreeDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class TreeDeltaTest extends AbstractSchemaTest {

    @Test(enabled = false)
    public void testInitialObjectsConflicts() {
        File objects44Dir = new File("../_mess/_init-objects-diff/initial-objects/4.4.8");
        File objectsCurrentDir = new File("../repo/system-init/src/main/resources/initial-objects");

        Map<String, PrismObject<?>> objects44 = loadObjects(objects44Dir);
        Map<String, PrismObject<?>> objectsCurrent = loadObjects(objectsCurrentDir);

        objects44.forEach((oid, object44) -> {
            PrismObject<?> objectCurrent = objectsCurrent.get(oid);
            if (objectCurrent == null) {
                return;
            }

        });
    }

    private Map<String, PrismObject<?>> loadObjects(File dir) {
        Map<String, PrismObject<?>> map = new HashMap<>();

        FileUtils.listFiles(dir, new String[] { "xml" }, true).forEach(file -> {
            try {
                PrismObject<?> object = PrismTestUtil.parseObject(file);
                map.put(object.getOid(), object);
            } catch (SchemaException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        return map;
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
    public void conflictActivation() throws SchemaException {
        PrismContext ctx = PrismTestUtil.getPrismContext();

        final String oid = UUID.randomUUID().toString();

        // GIVEN
        ActivationType a1 = new ActivationType()
                .administrativeStatus(ActivationStatusType.DISABLED)
                .disableReason("New reason");

        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ACTIVATION)
                .replace(a1.asPrismContainerValue())
                .asObjectDelta(oid);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ACTIVATION, ActivationType.F_DISABLE_REASON).add(List.of("New description"))
                .asObjectDelta(oid);

        // WHEN
        ObjectTreeDelta<UserType> leftTreeDelta = ObjectTreeDelta.fromItemDelta(leftToBase);
        ObjectTreeDelta<UserType> rightTreeDelta = ObjectTreeDelta.fromItemDelta(rightToBase);

        // THEN
        List<Conflict> conflicts = leftTreeDelta.getConflictsWith(rightTreeDelta);
        conflicts.forEach(System.out::println);

        AssertJUnit.assertFalse(conflicts.isEmpty());
    }
}
