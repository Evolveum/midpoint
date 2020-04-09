/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.io.Serializable;

public class ItemPanelSettings implements Serializable {

    private ItemVisibilityHandler visibilityHandler;
    private ItemEditabilityHandler editabilityHandler = wrapper -> true;
    private boolean showOnTopLevel;
    private boolean headerVisible = true;
    private ItemMandatoryHandler mandatoryHandler;


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

    public boolean isShowOnTopLevel() {
        return showOnTopLevel;
    }

    void setShowOnTopLevel(boolean showOnTopLevel) {
        this.showOnTopLevel = showOnTopLevel;
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
}

