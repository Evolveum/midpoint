/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.component;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismContainerDefinition;

import javax.xml.namespace.QName;

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

        return getItemName();
    }

    public String getItemName() {
        return def.getItemName().getLocalPart();
    }

    public QName getTypeName() {
        return def.getTypeName();
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
