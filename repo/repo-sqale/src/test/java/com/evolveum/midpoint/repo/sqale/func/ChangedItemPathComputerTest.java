/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.func;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.audit.ChangedItemPathComputer;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ChangedItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ChangedItemPathComputerTest extends SqaleRepoBaseTest {

    private static final File TEST_DIR = new File("src/test/resources/changed-item-paths");

    private static final File USER = new File(TEST_DIR, "user.xml");

    private static final String NS_P = "https://example.com/p";

    private static final QName STRING = new QName(NS_P, "string");

    private static final QName STRING_2 = new QName(NS_P, "string2");

    private static final QName STRING_MV = new QName(NS_P, "string-mv");

    @BeforeClass
    public void init() throws Exception {
        PrismTestUtil.setPrismContext(prismContext);
    }

    @Test
    public void test000ItemPathVisitor() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER);

        assertItemPathVisitorResult(user, UserType.F_EXTENSION, 1);
        assertItemPathVisitorResult(user, UserType.F_ASSIGNMENT, 1);
        assertItemPathVisitorResult(user, ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION), 2);
        assertItemPathVisitorResult(user, ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION), 1);
        assertItemPathVisitorResult(user, ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), 2);
    }

    private void assertItemPathVisitorResult(PrismObject<?> object, ItemPath path, int expectedSize) {
        ChangedItemPathComputer.ItemPathVisitor visitor = new ChangedItemPathComputer.ItemPathVisitor(path);
        visitor.visit(object);

        List<Item<?, ?>> result = visitor.getResult();
        Assertions.assertThat(result).hasSize(expectedSize);
    }

    @Test
    public void test100CollectChangedItemPathsFromAddDelta() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER);
        ObjectDelta<UserType> delta = user.createAddDelta();

        assertCollectedPaths(
                delta,
                false,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, true)),
                Set.of());

        assertCollectedPaths(
                delta,
                true,
                Set.of(),
                Set.of(
                        // default because of add delta
                        UserType.F_NAME,
                        UserType.F_PARENT_ORG_REF,
                        UserType.F_ASSIGNMENT,
                        UserType.F_EXTENSION));

        assertCollectedPaths(
                delta,
                true,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, true)),
                Set.of(
                        // default because of add delta
                        UserType.F_NAME,
                        UserType.F_PARENT_ORG_REF,
                        UserType.F_ASSIGNMENT,
                        UserType.F_EXTENSION,
                        // additional
                        ItemPath.create(UserType.F_EXTENSION, STRING),
                        ItemPath.create(UserType.F_EXTENSION, STRING_2)));

        assertCollectedPaths(
                delta,
                true,
                Set.of(new ChangedItemPath(UserType.F_ASSIGNMENT, true)),
                Set.of(
                        // default because of add delta
                        UserType.F_NAME,
                        UserType.F_PARENT_ORG_REF,
                        UserType.F_ASSIGNMENT,
                        UserType.F_EXTENSION,
                        // additional
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION),
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_IDENTIFIER),
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF),
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION)));

        assertCollectedPaths(
                delta,
                true,
                Set.of(new ChangedItemPath(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION), false)),
                Set.of(
                        // default because of add delta
                        UserType.F_NAME,
                        UserType.F_PARENT_ORG_REF,
                        UserType.F_ASSIGNMENT,
                        UserType.F_EXTENSION,
                        // additional
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION)));
    }

    @Test
    public void test200CollectChangedItemPathsFromModifyDelta() throws Exception {
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_NAME).replace("jdoe")
                .item(UserType.F_FULL_NAME).add("John Doe")
                .item(ItemPath.create(UserType.F_EXTENSION, STRING)).replace("value1")
                .item(ItemPath.create(UserType.F_EXTENSION, STRING_2)).delete("value2")
                .asObjectDelta(UUID.randomUUID().toString());

        assertCollectedPaths(
                delta,
                false,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, false)),
                Set.of(
                        UserType.F_NAME,
                        UserType.F_FULL_NAME,
                        UserType.F_EXTENSION,
                        ItemPath.create(UserType.F_EXTENSION, STRING),
                        ItemPath.create(UserType.F_EXTENSION, STRING_2)));

        assertCollectedPaths(
                delta,
                false,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, true)),
                Set.of(
                        UserType.F_NAME,
                        UserType.F_FULL_NAME,
                        UserType.F_EXTENSION,
                        ItemPath.create(UserType.F_EXTENSION, STRING),
                        ItemPath.create(UserType.F_EXTENSION, STRING_2)));
    }

    @Test
    public void test210CollectChangedItemPathsFromModifyDelta() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER);

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_NAME).replace("jdoe")
                .item(UserType.F_EXTENSION).delete(user.findContainer(UserType.F_EXTENSION).getValue().clone())
                .asObjectDelta(UUID.randomUUID().toString());

        assertCollectedPaths(
                delta,
                false,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, false)),
                Set.of(
                        UserType.F_NAME,
                        UserType.F_EXTENSION));

        assertCollectedPaths(
                delta,
                false,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, true)),
                Set.of(
                        UserType.F_NAME,
                        UserType.F_EXTENSION,
                        ItemPath.create(UserType.F_EXTENSION, STRING),
                        ItemPath.create(UserType.F_EXTENSION, STRING_2)));
    }

    @Test
    public void test220CollectChangedItemPathsFromModifyDelta() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER);

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(
                        user.findContainer(UserType.F_ASSIGNMENT).getValue(1L).clone(),
                        user.findContainer(UserType.F_ASSIGNMENT).getValue(2L).clone())
                .asObjectDelta(UUID.randomUUID().toString());

        assertCollectedPaths(
                delta,
                false,
                Set.of(
                        new ChangedItemPath(UserType.F_EXTENSION, false),
                        new ChangedItemPath(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION), false)),
                Set.of(
                        UserType.F_ASSIGNMENT,
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION)));

        assertCollectedPaths(
                delta,
                false,
                Set.of(
                        new ChangedItemPath(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION), true),
                        new ChangedItemPath(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION), true)),
                Set.of(
                        UserType.F_ASSIGNMENT,
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION),
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION),
                        ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION, STRING_MV)));
    }

    private <O extends ObjectType> void assertCollectedPaths(
            ObjectDelta<O> delta,
            boolean indexAddObjectDeltaOperation,
            Set<ChangedItemPath> indexAdditionalItemPath,
            Set<ItemPath> expectedPaths) throws SchemaException {

        assertCollectedPathsForDelta(delta, indexAddObjectDeltaOperation, indexAdditionalItemPath, expectedPaths);

        assertCollectedPathsForDeltaType(delta, indexAddObjectDeltaOperation, indexAdditionalItemPath, expectedPaths);
    }

    private <O extends ObjectType> void assertCollectedPathsForDelta(ObjectDelta<O> delta,
            boolean indexAddObjectDeltaOperation,
            Set<ChangedItemPath> indexAdditionalItemPath,
            Set<ItemPath> expectedPaths) {

        QName type = ObjectTypes.getObjectType(delta.getObjectTypeClass()).getTypeQName();

        ChangedItemPathComputer computer = new ChangedItemPathComputer(
                indexAddObjectDeltaOperation, indexAdditionalItemPath, prismContext);

        Set<String> changedItemPaths = computer.collectChangedItemPaths(List.of(new ObjectDeltaOperation<>(delta)));
        Assertions.assertThat(changedItemPaths).hasSize(expectedPaths.size());

        Set<String> expected = expectedPaths.stream()
                .map(p -> prismContext.createCanonicalItemPath(p, type).asString())
                .collect(Collectors.toSet());
        Assertions.assertThat(changedItemPaths).containsAll(expected);
    }

    private <O extends ObjectType> void assertCollectedPathsForDeltaType(ObjectDelta<O> delta,
            boolean indexAddObjectDeltaOperation,
            Set<ChangedItemPath> indexAdditionalItemPath,
            Set<ItemPath> expectedPaths) throws SchemaException {

        ObjectDeltaOperationType odo = new ObjectDeltaOperationType();
        odo.setObjectDelta(DeltaConvertor.toObjectDeltaType(delta));

        ChangedItemPathComputer computer = new ChangedItemPathComputer(
                indexAddObjectDeltaOperation, indexAdditionalItemPath, prismContext);

        String[] changedItemPaths = computer.collectChangedItemPaths(List.of(odo));

        QName type = ObjectTypes.getObjectType(delta.getObjectTypeClass()).getTypeQName();

        Set<String> expected = expectedPaths.stream()
                .map(p -> prismContext.createCanonicalItemPath(p, type).asString())
                .collect(Collectors.toSet());

        if (expected.size() == 0) {
            Assertions.assertThat(changedItemPaths).isNull();
        } else {
            Assertions.assertThat(changedItemPaths).containsAll(expected);
        }
    }
}
