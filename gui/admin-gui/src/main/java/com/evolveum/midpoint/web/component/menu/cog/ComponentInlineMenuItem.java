/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * {@link InlineMenuItem} that renders a custom Wicket {@link Component}.
 *
 * <p>Unlike regular menu items, this item provides its own component for rendering.
 * Currently, this type is supported only by {@link com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel}.
 */
public abstract class ComponentInlineMenuItem extends InlineMenuItem {

    @Serial private static final long serialVersionUID = 1L;

    protected ComponentInlineMenuItem(IModel<String> label) {
        super(label);
    }

    public abstract @NotNull Component createComponent(
            @NotNull String componentId);

    @Override
    public InlineMenuItemAction initAction() {
        return null;
    }
}
