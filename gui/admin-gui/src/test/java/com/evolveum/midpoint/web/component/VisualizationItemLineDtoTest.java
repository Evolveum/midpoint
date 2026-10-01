/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.visualizer.output.NameImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemValueImpl;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationItemLineDto;

/**
 * Tests icon suppression for visualization item lines.
 *
 * Verifies that items inside added or deleted containers can remain
 * non-descriptive while still suppressing redundant change icons in the GUI,
 * and that unrelated visualization cases keep their existing behavior.
 */
public class VisualizationItemLineDtoTest {

    @Test
    public void testSuppressesIconForAddedContainerChildInModifyVisualization() {
        VisualizationItemLineDto line = createLine(ChangeType.MODIFY, ChangeType.ADD, false);

        assertFalse(line.isDescriptive());
        assertTrue(line.shouldSuppressNewValueIcon());
    }

    @Test
    public void testSuppressesIconForDeletedContainerChildInModifyVisualization() {
        VisualizationItemLineDto line = createLine(ChangeType.MODIFY, ChangeType.DELETE, false);

        assertFalse(line.isDescriptive());
        assertTrue(line.shouldSuppressNewValueIcon());
    }

    @Test
    public void testSuppressesIconForNestedAddedContainerChildInModifyVisualization() {
        VisualizationImpl modifyVisualization = new VisualizationImpl(null);
        modifyVisualization.setChangeType(ChangeType.MODIFY);
        VisualizationImpl addedVisualization = new VisualizationImpl(modifyVisualization);
        addedVisualization.setChangeType(ChangeType.ADD);

        VisualizationItemLineDto line = createLineWithOwner(addedVisualization, ChangeType.ADD, false);

        assertFalse(line.isDescriptive());
        assertTrue(line.shouldSuppressNewValueIcon());
    }

    @Test
    public void testKeepsTopLevelAddAndDeleteIconBehavior() {
        assertFalse(createLine(null, ChangeType.ADD, false).shouldSuppressNewValueIcon());
        assertFalse(createLine(null, ChangeType.DELETE, false).shouldSuppressNewValueIcon());
    }

    @Test
    public void testDoesNotSuppressNestedAddOutsideModifyVisualization() {
        assertFalse(createLine(ChangeType.ADD, ChangeType.ADD, false).shouldSuppressNewValueIcon());
    }

    @Test
    public void testSuppressesIconForDescriptiveItem() {
        assertTrue(createLine(ChangeType.MODIFY, ChangeType.MODIFY, true).shouldSuppressNewValueIcon());
    }

    private VisualizationItemLineDto createLine(
            ChangeType ownerChangeType, ChangeType visualizationChangeType, boolean descriptive) {
        VisualizationImpl owner = null;
        if (ownerChangeType != null) {
            owner = new VisualizationImpl(null);
            owner.setChangeType(ownerChangeType);
        }

        return createLineWithOwner(owner, visualizationChangeType, descriptive);
    }

    private VisualizationItemLineDto createLineWithOwner(
            VisualizationImpl owner, ChangeType visualizationChangeType, boolean descriptive) {
        VisualizationImpl visualization = new VisualizationImpl(owner);
        visualization.setChangeType(visualizationChangeType);

        VisualizationItemImpl item = new VisualizationItemImpl(new NameImpl("item"));
        item.setDescriptive(descriptive);
        item.setNewValues(List.of(new VisualizationItemValueImpl("value")));
        visualization.addItem(item);

        return new VisualizationDto(visualization)
                .getItems().get(0)
                .getLines().get(0);
    }
}
