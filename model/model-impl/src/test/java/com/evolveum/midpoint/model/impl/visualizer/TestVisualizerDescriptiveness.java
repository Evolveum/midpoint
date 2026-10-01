/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Tests correct handling of the {@code descriptive} flag in visualizations.
 *
 * In particular, verifies that children of added or deleted containers are treated
 * as actual changed items, while unchanged items included only for identification
 * remain descriptive.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestVisualizerDescriptiveness extends AbstractInternalModelIntegrationTest {

    @Autowired
    private Visualizer visualizer;

    @Test
    public void testChildrenOfAddedContainerAreNotDescriptive() throws Exception {
        UserType oldUser = createUser();
        UserType modifiedUser = oldUser.clone();
        modifiedUser.getAssignment().add(
                new AssignmentType().id(1L).description("added assignment"));

        Visualization visualization = visualize(oldUser.asPrismObject(),
                oldUser.asPrismObject().diff(modifiedUser.asPrismObject()));

        assertThat(getItemsRecursively(findPartialVisualization(visualization, ChangeType.ADD)))
                .isNotEmpty()
                .allMatch(item -> !item.isDescriptive());
    }

    @Test
    public void testChildrenOfDeletedContainerAreNotDescriptive() throws Exception {
        UserType oldUser = createUser();
        oldUser.getAssignment().add(
                new AssignmentType().id(1L).description("deleted assignment"));
        UserType modifiedUser = oldUser.clone();
        modifiedUser.getAssignment().clear();

        Visualization visualization = visualize(oldUser.asPrismObject(),
                oldUser.asPrismObject().diff(modifiedUser.asPrismObject()));

        assertThat(getItemsRecursively(findPartialVisualization(visualization, ChangeType.DELETE)))
                .isNotEmpty()
                .allMatch(item -> !item.isDescriptive());
    }

    @Test
    public void testIdentificationItemsRemainDescriptiveAndChangedItemDoesNot() throws Exception {
        ShadowType oldShadow = new ShadowType()
                .oid(UUID.randomUUID().toString())
                .name(new PolyStringType("shadow"))
                .resourceRef(new ObjectReferenceType()
                        .oid(RESOURCE_DUMMY_OID)
                        .targetName(new PolyStringType("Dummy Resource")))
                .kind(ShadowKindType.ACCOUNT)
                .intent("default");
        ShadowType modifiedShadow = oldShadow.clone().description("changed description");

        Visualization visualization = visualize(oldShadow.asPrismObject(),
                oldShadow.asPrismObject().diff(modifiedShadow.asPrismObject()));

        assertThat(findItem(visualization, ShadowType.F_RESOURCE_REF).isDescriptive()).isTrue();
        assertThat(findItem(visualization, ShadowType.F_KIND).isDescriptive()).isTrue();
        assertThat(findItem(visualization, ShadowType.F_INTENT).isDescriptive()).isTrue();

        VisualizationItem changedItem = findItem(visualization, ObjectType.F_DESCRIPTION);
        assertThat(changedItem).isInstanceOf(VisualizationDeltaItem.class);
        assertThat(changedItem.isDescriptive()).isFalse();
    }

    private UserType createUser() {
        return new UserType()
                .oid(UUID.randomUUID().toString())
                .name(new PolyStringType("user"));
    }

    private Visualization visualize(PrismObject<? extends ObjectType> oldObject, ObjectDelta<? extends ObjectType> delta) throws Exception {
        VisualizationContext context = new VisualizationContext();
        context.getOldObjects().put(oldObject.getOid(), oldObject);
        var task = getTestTask();
        return visualizer.visualizeDelta(delta, null, context, false, task, task.getResult());
    }

    private static Visualization findPartialVisualization(Visualization visualization, ChangeType changeType) {
        Visualization matchingVisualization = findPartialVisualizationOrNull(visualization, changeType);
        if (matchingVisualization != null) {
            return matchingVisualization;
        }
        throw new AssertionError("No partial visualization with change type " + changeType);
    }

    private static Visualization findPartialVisualizationOrNull(Visualization visualization, ChangeType changeType) {
        for (Visualization partial : visualization.getPartialVisualizations()) {
            if (partial.getChangeType() == changeType) {
                return partial;
            }
            Visualization matchingDescendant = findPartialVisualizationOrNull(partial, changeType);
            if (matchingDescendant != null) {
                return matchingDescendant;
            }
        }
        return null;
    }

    private static VisualizationItem findItem(Visualization visualization, ItemPath path) {
        return getItemsRecursively(visualization).stream()
                .filter(item -> item.getSourceRelPath() != null && item.getSourceRelPath().equivalent(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No visualization item for path " + path));
    }

    private static List<VisualizationItem> getItemsRecursively(Visualization visualization) {
        List<VisualizationItem> items = new ArrayList<>(visualization.getItems());
        for (Visualization partial : visualization.getPartialVisualizations()) {
            items.addAll(getItemsRecursively(partial));
        }
        return items;
    }
}
