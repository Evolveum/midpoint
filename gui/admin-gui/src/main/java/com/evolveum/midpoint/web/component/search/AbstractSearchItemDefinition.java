/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;

public abstract class AbstractSearchItemDefinition implements Serializable, Comparable<AbstractSearchItemDefinition> {

    protected boolean displayed;
    protected boolean visibleByDefault = true;
    private PolyStringType displayName;
    private String description;
    private boolean fixed;
    private boolean isSelected = false;

    public abstract String getName();

    public abstract String getHelp();

    public abstract <SI extends SearchItem> SI createSearchItem();

    public boolean isVisibleByDefault() {
        return visibleByDefault;
    }

    public void setVisibleByDefault(boolean visibleByDefault) {
        this.visibleByDefault = visibleByDefault;
    }

    public PolyStringType getDisplayName() {
        return displayName;
    }

    public void setDisplayName(PolyStringType displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDisplayed() {
        return displayed;
    }

    public void setDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public int compareTo(AbstractSearchItemDefinition def) {
        String n1 = getName();
        String n2 = def.getName();

        if (n1 == null || n2 == null) {
            return 0;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
    }
}
