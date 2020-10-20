/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.wicket.model.StringResourceModel;

/**
 * @author Viliam Repan (lazyman)
 */
public class Property implements Serializable, Comparable<Property> {

    public static final String F_SELECTED = "selected";
    public static final String F_NAME = "name";

    private final ItemDefinition definition;
    private boolean selected;
    private ItemPath fullPath;

    public Property(ItemDefinition definition, ItemPath fullPath) {
        Validate.notNull(definition, "Property name must no be null");

        this.definition = definition;
        this.fullPath = fullPath;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        if (definition != null && StringUtils.isNotEmpty(definition.getDisplayName())) {
            return PageBase.createStringResourceStatic(null, definition.getDisplayName()).getString();
        }
        return WebComponentUtil.getItemDefinitionDisplayNameOrName(definition, null);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public ItemPath getFullPath() {
        return fullPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Property property = (Property) o;

        if (selected != property.selected) { return false; }
        return !(definition != null ? !definition.equals(property.definition) : property.definition != null);

    }

    @Override
    public int hashCode() {
        int result = (selected ? 1 : 0);
        result = 31 * result + (definition != null ? definition.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Property o) {
        String n1 = WebComponentUtil.getItemDefinitionDisplayNameOrName(definition, null);
        String n2 = WebComponentUtil.getItemDefinitionDisplayNameOrName(o.definition, null);

        if (n1 == null || n2 == null) {
            return 0;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
    }


    @Override
    public String toString() {
        return "Property{" +
                "definition=" + definition +
                ", selected=" + selected +
                '}';
    }
}
