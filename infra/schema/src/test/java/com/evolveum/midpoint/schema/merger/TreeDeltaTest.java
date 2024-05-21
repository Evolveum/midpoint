/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
import com.evolveum.midpoint.schema.delta.Conflict;
import com.evolveum.midpoint.schema.delta.Direction;
import com.evolveum.midpoint.schema.delta.ThreeWayMergeOperation;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class TreeDeltaTest extends AbstractSchemaTest {

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
        ThreeWayMergeOperation<UserType> operation = new ThreeWayMergeOperation<>(
                leftToBase, rightToBase, user.asPrismObject(), EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS_NATURAL_KEYS);

        // THEN
        AssertJUnit.assertTrue(operation.hasConflicts());

    }

    private <O extends ObjectType> ThreeWayMergeOperation<O> createThreeWayMerge(ObjectDelta<O> left, ObjectDelta<O> right, PrismObject<O> base, ParameterizedEquivalenceStrategy strategy) throws SchemaException {
        ThreeWayMergeOperation<O> merge = new ThreeWayMergeOperation<>(left, right, base, strategy);

        AssertJUnit.assertTrue(left.equivalent(merge.getLeftDelta().toObjectDelta()));
        AssertJUnit.assertTrue(right.equivalent(merge.getRightDelta().toObjectDelta()));

        Collection<? extends ItemDelta<?, ?>> leftToRight = merge.getNonConflictingModifications(Direction.LEFT_TO_RIGHT);
        Collection<? extends ItemDelta<?, ?>> rightToLeft = merge.getNonConflictingModifications(Direction.RIGHT_TO_LEFT);
        Collection<Conflict> conflicting = merge.getConflictingModifications();

        // todo assert that these are not overlapping

        return merge;
    }
}
