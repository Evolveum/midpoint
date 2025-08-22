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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

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

    public ItemPanelSettingsBuilder panelConfiguration(ContainerPanelConfigurationType config) {
        settings.setConfig(config);
        return this;
    }

    public ItemPanelSettingsBuilder displayedInColumn(boolean displayedInColumn) {
        settings.setDisplayedInColumn(displayedInColumn);
        return this;
    }

    public ItemPanelSettingsBuilder isRemoveButtonVisible(Boolean isRemoveButtonVisible) {
        settings.setRemoveButtonVisible(isRemoveButtonVisible);
        return this;
    }

    public ItemPanelSettings build() {
        return settings;
    }
}
