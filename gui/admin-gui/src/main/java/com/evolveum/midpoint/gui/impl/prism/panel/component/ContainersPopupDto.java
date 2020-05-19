/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.component;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismContainerDefinition;

/**
 * @author katka
 *
 */
public class ContainersPopupDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean selected;
    private PrismContainerDefinition<?> def;

    public ContainersPopupDto(boolean selected, PrismContainerDefinition<?> def) {
        this.selected = selected;
        this.def = def;
    }

    public String getDisplayName() {
        if (def.getDisplayName() != null) {
            return def.getDisplayName();
        }

        return def.getItemName().getLocalPart();
    }

    public PrismContainerDefinition<?> getDef() {
        return def;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
