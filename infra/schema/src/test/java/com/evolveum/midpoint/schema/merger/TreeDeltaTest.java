/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.delta.Conflict;
import com.evolveum.midpoint.schema.delta.Direction;
import com.evolveum.midpoint.schema.delta.ObjectTreeDelta;
import com.evolveum.midpoint.schema.delta.ThreeWayMergeOperation;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class TreeDeltaTest extends AbstractSchemaTest {

    private static final File TEST_DIRECTORY = new File("./src/test/resources/delta");

    @Test(enabled = false)
    public void test10RoleChanges() throws Exception {
        ThreeWayMergeOperation<RoleType> operation = createMergeOperation("role");
        AssertJUnit.assertTrue(operation.hasConflicts());

        AssertJUnit.assertEquals(3, operation.getConflictingModifications().size());
    }

    @Test
    public void testRoleChangesDifferentPcvIds() throws Exception {
        TestData<RoleType> data = loadTestData("role");

        data.base.asObjectable().getAuthorization().get(0).setId(123L);
        data.left.asObjectable().getAuthorization().get(0).setId(456L);
        data.right.asObjectable().getAuthorization().get(0).setId(789L);

        ThreeWayMergeOperation<RoleType> operation = createMergeOperation(data);
        AssertJUnit.assertTrue(operation.hasConflicts());

        PrismObject<RoleType> base = data.base.clone();

        ObjectDelta<RoleType> fullLeft = operation.getLeftDelta().toObjectDelta();
        ObjectDelta<RoleType> fullRight = operation.getRightDelta().toObjectDelta();

        ObjectDelta<RoleType> left = operation.getNonConflictingDelta(Direction.FROM_LEFT);
        ObjectDelta<RoleType> right = operation.getNonConflictingDelta(Direction.FROM_RIGHT);

        // todo non-conflicting modifications are not computed correctly
        System.out.println();
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
        ThreeWayMergeOperation<UserType> operation = new ThreeWayMergeOperation<>(
                leftToBase, rightToBase, null, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        // THEN
        AssertJUnit.assertFalse(operation.hasConflicts());
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
        ThreeWayMergeOperation<UserType> operation = new ThreeWayMergeOperation<>(
                leftToBase, rightToBase, null, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        // THEN
        AssertJUnit.assertFalse(operation.hasConflicts());
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
        ThreeWayMergeOperation<UserType> operation = new ThreeWayMergeOperation<>(
                leftToBase, rightToBase, null, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        // THEN
        AssertJUnit.assertTrue(operation.hasConflicts());
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
        ThreeWayMergeOperation<UserType> operation = new ThreeWayMergeOperation<>(
                leftToBase, rightToBase, u1.asPrismObject(), EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        // THEN
        AssertJUnit.assertTrue(operation.hasConflicts());
    }

    // todo this fails because we would have to use base object to figure out whether these two deltas will do the same thing
    @Test
    public void conflictAssignmentAndPropertyAdd() throws SchemaException {
        final String oid = UUID.randomUUID().toString();
        PrismContext ctx = PrismTestUtil.getPrismContext();

        // GIVEN
        AssignmentType a1 = new AssignmentType()
                .id(1L)
                .identifier("my-assignment")
                .targetRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE);

        AssignmentType a1new = new AssignmentType()
                .id(null)
                .identifier("my-assignment")
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
        ThreeWayMergeOperation<UserType> operation = new ThreeWayMergeOperation<>(
                leftToBase, rightToBase, null, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        // THEN
        AssertJUnit.assertTrue(operation.hasConflicts());
    }

    // todo this doesn't fail only by accident, because current algorithm
    //  doesn't check for modification type in other subtree, just on the same level...
    //  FIX ME
    @Test
    public void conflictActivation() throws SchemaException {
        PrismContext ctx = PrismTestUtil.getPrismContext();

        final String oid = UUID.randomUUID().toString();

        UserType user = new UserType();
        user.setName(new PolyStringType("user"));
        ActivationType a = new ActivationType();
        a.setAdministrativeStatus(ActivationStatusType.DISABLED);
        user.setActivation(a);

        // GIVEN
        ActivationType a1 = new ActivationType()
                .administrativeStatus(ActivationStatusType.DISABLED)
                .disableReason("New reason");

        ObjectDelta<UserType> leftToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ACTIVATION)
                .replace(a1.asPrismContainerValue())
                .asObjectDelta(oid);

        ObjectDelta<UserType> rightToBase = ctx.deltaFor(UserType.class)
                .item(UserType.F_ACTIVATION, ActivationType.F_DISABLE_REASON).add(List.of("New reason"))
                .asObjectDelta(oid);

        // WHEN
        ThreeWayMergeOperation<UserType> operation = createMergeOperation(
                leftToBase, rightToBase, user.asPrismObject(), EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        // THEN
        AssertJUnit.assertTrue(operation.hasConflicts());

    }

    private <O extends ObjectType> void assertOperationState(
            ThreeWayMergeOperation<O> operation, ObjectDelta<O> left, ObjectDelta<O> right) throws SchemaException {

        AssertJUnit.assertTrue(left.equivalent(operation.getLeftDelta().toObjectDelta()));
        AssertJUnit.assertTrue(right.equivalent(operation.getRightDelta().toObjectDelta()));

        Collection<? extends ItemDelta<?, ?>> leftToRight = operation.getNonConflictingModifications(Direction.FROM_LEFT);
        Collection<? extends ItemDelta<?, ?>> rightToLeft = operation.getNonConflictingModifications(Direction.FROM_RIGHT);
        Collection<Conflict> conflicting = operation.getConflictingModifications();

        // todo assert that these are not overlapping
    }

    private <O extends ObjectType> ThreeWayMergeOperation<O> createMergeOperation(
            ObjectDelta<O> left, ObjectDelta<O> right, PrismObject<O> base, ParameterizedEquivalenceStrategy strategy)
            throws SchemaException {

        ThreeWayMergeOperation<O> operation = new ThreeWayMergeOperation<>(left, right, base, strategy);
        assertOperationState(operation, left, right);

        return operation;
    }

    private <O extends ObjectType> ThreeWayMergeOperation<O> createMergeOperation(String filename)
            throws SchemaException, IOException {

        TestData<O> data = loadTestData(filename);

        return createMergeOperation(data);
    }

    private <O extends ObjectType> ThreeWayMergeOperation<O> createMergeOperation(TestData<O> data)
            throws SchemaException, IOException {
        ParameterizedEquivalenceStrategy strategy = EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS;

        ThreeWayMergeOperation<O> operation = new ThreeWayMergeOperation<>(data.left, data.right, data.base, strategy);

        ObjectDelta<O> leftToBase = data.base.diff(data.left, strategy);
        ObjectDelta<O> rightToBase = data.base.diff(data.right, strategy);

        assertOperationState(operation, leftToBase, rightToBase);

        return operation;
    }

    private <O extends ObjectType> TestData<O> loadTestData(String filename) throws SchemaException, IOException {
        File baseFile = new File(TEST_DIRECTORY, filename + "-base.xml");
        File leftFile = new File(TEST_DIRECTORY, filename + "-left.xml");
        File rightFile = new File(TEST_DIRECTORY, filename + "-right.xml");

        PrismContext ctx = PrismTestUtil.getPrismContext();

        PrismObject<O> base = ctx.parseObject(baseFile);
        PrismObject<O> left = ctx.parseObject(leftFile);
        PrismObject<O> right = ctx.parseObject(rightFile);

        return new TestData<>(baseFile, leftFile, rightFile, base, left, right);
    }

    private record TestData<O extends ObjectType>(
            File baseFile, File leftFile, File rightFile, PrismObject<O> base, PrismObject<O> left, PrismObject<O> right) {
    }

    @Test
    public void testTreeDeltaWithMetadata() throws Exception {
        ObjectDeltaType deltaType = PrismContext.get()
                .parserFor(new File(TEST_DIRECTORY, "delta-metadata.xml"))
                .type(ObjectDeltaType.COMPLEX_TYPE)
                .parseRealValue();
        ObjectDelta<? extends ObjectType> delta = DeltaConvertor.createObjectDelta(deltaType);
        ObjectTreeDelta<? extends ObjectType> treeDelta = ObjectTreeDelta.fromItemDelta(delta);

        Assertions.assertThat(treeDelta).isNotNull();
        Assertions.assertThat(treeDelta.getSingleValue().getSize()).isEqualTo(3);
    }
}
