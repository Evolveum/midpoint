/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.func;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;

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
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ChangedItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ChangedItemPathComputerTest extends SqaleRepoBaseTest {

    private static final File TEST_DIR = new File("src/test/resources/changed-item-paths");

    private static final File USER = new File(TEST_DIR, "user.xml");

    private static final String NS_P = "https://example.com/p";

    @BeforeClass
    public void init() throws Exception {
        PrismTestUtil.setPrismContext(prismContext);
    }

    @Test
    public void test100ItemPathVisitor() throws Exception {
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
    public void test110CollectChangedItemPathsFromAddDelta() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER);

        assertCollectedPaths(
                user.createAddDelta(),
                false,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, true)),
                Set.of());

        assertCollectedPaths(
                user.createAddDelta(),
                true,
                Set.of(),
                Set.of(
                        // default because of add delta
                        UserType.F_NAME,
                        UserType.F_PARENT_ORG_REF,
                        UserType.F_ASSIGNMENT,
                        UserType.F_EXTENSION));

        assertCollectedPaths(
                user.createAddDelta(),
                true,
                Set.of(new ChangedItemPath(UserType.F_EXTENSION, true)),
                Set.of(
                        // default because of add delta
                        UserType.F_NAME,
                        UserType.F_PARENT_ORG_REF,
                        UserType.F_ASSIGNMENT,
                        UserType.F_EXTENSION,
                        // additional
                        ItemPath.create(UserType.F_EXTENSION, new QName(NS_P, "string")),
                        ItemPath.create(UserType.F_EXTENSION, new QName(NS_P, "string2"))));

        assertCollectedPaths(
                user.createAddDelta(),
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
                user.createAddDelta(),
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

    public void test120CollectChangedItemPathsFromModifyDelta() throws Exception {
        // todo implement
    }

    public void test130CollectChangedItemPathsFromAddDeltaType() throws Exception {
        // todo implement
    }

    public void test140CollectChangedItemPathsFromModifyDeltaType() throws Exception {
        // todo implement
    }

    private <O extends ObjectType> void assertCollectedPaths(
            ObjectDelta<O> delta,
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
}
