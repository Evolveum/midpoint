/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;

public class ItemPanelSettingsBuilder {

    private ItemPanelSettings settings;

    public ItemPanelSettingsBuilder() {
        settings = new ItemPanelSettings();
    }

    public ItemPanelSettingsBuilder visibilityHandler(ItemVisibilityHandler handler) {
        settings.setVisibilityHandler(handler);
        return this;
    }

    public ItemPanelSettingsBuilder editabilityHandler(ItemEditabilityHandler handler) {
        settings.setEditabilityHandler(handler);
        return this;
    }

    public ItemPanelSettingsBuilder mandatoryHandler(ItemMandatoryHandler handler) {
        settings.setMandatoryHandler(handler);
        return this;
    }

    public ItemPanelSettingsBuilder headerVisibility(boolean headerVisibility) {
        settings.setHeaderVisible(headerVisibility);
        return this;
    }

    public ItemPanelSettings build() {
        return settings;
    }
}
