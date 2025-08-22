/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.jetbrains.annotations.Nullable;

public class ItemPanelSettings implements Serializable {

    private ItemVisibilityHandler visibilityHandler;
    private ItemEditabilityHandler editabilityHandler = wrapper -> true;
    private boolean headerVisible = true;
    private Boolean removeButtonVisible = null;

    private boolean displayedInColumn = false;
    private ItemMandatoryHandler mandatoryHandler;
    private ContainerPanelConfigurationType config;

    ItemPanelSettings() {

    }

    public ItemVisibilityHandler getVisibilityHandler() {
        return visibilityHandler;
    }

    void setVisibilityHandler(ItemVisibilityHandler visibilityHandler) {
        this.visibilityHandler = visibilityHandler;
    }

    public ItemEditabilityHandler getEditabilityHandler() {
        return editabilityHandler;
    }

    public void setEditabilityHandler(ItemEditabilityHandler editabilityHandler) {
        this.editabilityHandler = editabilityHandler;
    }

    public boolean isHeaderVisible() {
        return headerVisible;
    }

    void setHeaderVisible(boolean headerVisible) {
        this.headerVisible = headerVisible;
    }

    public ItemMandatoryHandler getMandatoryHandler() {
        return mandatoryHandler;
    }

    public void setMandatoryHandler(ItemMandatoryHandler mandatoryHandler) {
        this.mandatoryHandler = mandatoryHandler;
    }

    public ContainerPanelConfigurationType getConfig() {
        return config;
    }

    public void setConfig(ContainerPanelConfigurationType config) {
        this.config = config;
    }

    public boolean isDisplayedInColumn() {
        return displayedInColumn;
    }

    public void setDisplayedInColumn(boolean displayedInColumn) {
        this.displayedInColumn = displayedInColumn;
    }

    void setRemoveButtonVisible(@Nullable Boolean removeButtonVisible) {
        this.removeButtonVisible = removeButtonVisible;
    }

    public Boolean isRemoveButtonVisible() {
        return removeButtonVisible;
    }

    public ItemPanelSettings copy() {
        return new ItemPanelSettingsBuilder()
                .editabilityHandler(editabilityHandler)
                .visibilityHandler(visibilityHandler)
                .mandatoryHandler(mandatoryHandler)
                .displayedInColumn(displayedInColumn)
                .isRemoveButtonVisible(removeButtonVisible)
                .build();
    }
}
