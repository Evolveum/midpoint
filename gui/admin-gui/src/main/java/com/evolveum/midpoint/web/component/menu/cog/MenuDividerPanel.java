/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.markup.html.panel.Panel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author lazyman
 */
public class MenuDividerPanel extends Panel {
    public MenuDividerPanel(String id) {
        super(id);
    }

    public static @NotNull InlineMenuItem createSectionDivider(InlineMenuItem.VisibilityChecker... checkers) {

        return InlineMenuItemBuilder.create()
                .divider(true)
                .visibilityChecker(anyVisibleOrNull(checkers))
                .buildInlineMenu();
    }

    public static  @NotNull InlineMenuItem createSectionDivider() {

        return InlineMenuItemBuilder.create()
                .divider(true)
                .buildInlineMenu();
    }

    public static  @NotNull InlineMenuItem createSectionDividerNoHeader() {
        return InlineMenuItemBuilder.create()
                .divider(true)
                .headerMenuItem(false)
                .buildInlineMenu();
    }

    /**
     * Returns a visibility checker that checks if any of the provided checkers returns true.
     * If no checkers are provided, it returns a checker that always returns true.
     */
    public static  @NotNull InlineMenuItem.VisibilityChecker anyVisibleOrNull(
            InlineMenuItem.VisibilityChecker... checkers) {

        return (rowModel, isHeader) -> Arrays.stream(checkers)
                .filter(Objects::nonNull)
                .anyMatch(checker -> checker.isVisible(rowModel, isHeader));
    }
}
