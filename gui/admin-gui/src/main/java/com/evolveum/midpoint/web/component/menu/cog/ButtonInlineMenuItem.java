/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public abstract class ButtonInlineMenuItem extends InlineMenuItem {

    boolean isLabelVisible = false;

    public ButtonInlineMenuItem(IModel<String> labelModel) {
        super(labelModel);
    }

    public ButtonInlineMenuItem(IModel<String> labelModel, boolean isSubmit) {
        super(labelModel, isSubmit);
    }

    protected boolean isBadgeVisible() {
        return false;
    }

    public abstract CompositedIconBuilder getIconCompositedBuilder();

    public boolean isLabelVisible() {
        return isLabelVisible;
    }

    public void setLabelVisible(boolean visible) {
        this.isLabelVisible = visible;
    }

    public IModel<String> getButtonLabelModel() {
        return getLabel();
    }
}
