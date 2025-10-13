/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.model.IModel;

public abstract class ButtonInlineMenuItemWithCount extends ButtonInlineMenuItem {

    public ButtonInlineMenuItemWithCount(IModel<String> labelModel) {
        super(labelModel);
    }

    public ButtonInlineMenuItemWithCount(IModel<String> labelModel, boolean isSubmit) {
        super(labelModel, isSubmit);
    }

    protected abstract int getCount();

    @Override
    protected boolean isBadgeVisible() {
        return true;
    }
}
