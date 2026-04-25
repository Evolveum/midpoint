/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.table;

import static com.evolveum.midpoint.web.component.menu.cog.MenuDividerPanel.createSectionDivider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * Assembles inline menu items for {@link SmartMappingTable}.
 *
 * <p>Combines menu sections (suggestions, lifecycle, simulation, statistics, etc.)
 * using actions provided by {@link SmartMappingActions}.
 *
 * <p>Contains no business logic – only menu composition.
 */
record SmartMappingMenus<P extends Containerable>(SmartMappingTable<P> table, SmartMappingActions<P> actions) implements Serializable {

    @NotNull List<InlineMenuItem> getInlineMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();

        addSuggestionHeaderMenus(items);
        addMappingManagementMenus(items);
        addSimulationMenu(items);
        addStatisticsMenus(items);
        addFinalMenus(items);
        addSuggestionButtons(items);

        return items;
    }

    private void addSuggestionHeaderMenus(@NotNull List<InlineMenuItem> items) {
        InlineMenuItem accept = actions.createAcceptSuggestionInlineMenu();
        InlineMenuItem discard = actions.createDiscardSuggestionInlineMenu();

        items.add(accept);
        items.add(discard);
        items.add(createSectionDivider(
                accept.getVisibilityChecker(),
                discard.getVisibilityChecker()));
    }

    private void addMappingManagementMenus(@NotNull List<InlineMenuItem> items) {
        InlineMenuItem changeLifecycle = actions.createChangeLifecycleButtonInlineMenu();
        InlineMenuItem edit = actions.createEditInlineMenu();
        InlineMenuItem duplicate = actions.createDuplicateInlineMenu();
        InlineMenuItem rename = actions.createChangeMappingNameInlineMenu();

        items.add(changeLifecycle);
        items.add(edit);
        items.add(duplicate);
        items.add(rename);
        items.add(createSectionDivider(
                changeLifecycle.getVisibilityChecker(),
                edit.getVisibilityChecker(),
                duplicate.getVisibilityChecker(),
                rename.getVisibilityChecker()));
    }

    private void addSimulationMenu(List<InlineMenuItem> items) {
        if (!table.isSimulationSupported()) {
            return;
        }

        InlineMenuItem simulation = actions.createSimulationInlineMenu();
        items.add(simulation);
        items.add(createSectionDivider(simulation.getVisibilityChecker()));
    }

    private void addStatisticsMenus(List<InlineMenuItem> items) {
        var definitionWrapper = table.findResourceObjectTypeDefinition();
        if (definitionWrapper == null || definitionWrapper.getRealValue() == null) {
            return;
        }

        ResourceObjectTypeDefinitionType definition = definitionWrapper.getRealValue();
        items.add(actions.createResourceAttributeStatisticsMenu(definition));
        items.add(actions.createFocusAttributeStatisticsMenu(definition));
        items.add(createSectionDivider());
    }

    private void addFinalMenus(List<InlineMenuItem> items) {
        items.add(actions.createDeleteItemMenu());
        items.add(actions.createPreviewInlineMenu());
    }

    private void addSuggestionButtons(List<InlineMenuItem> items) {
        items.add(actions.createSuggestionOperationInlineMenu());
        items.add(actions.createSuggestionDetailsInlineMenu());
        items.add(actions.createAcceptItemMenu());
        items.add(actions.createDiscardItemMenu());
    }
}
