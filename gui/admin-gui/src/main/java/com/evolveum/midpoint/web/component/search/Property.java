/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class Property implements Serializable, Comparable<Property> {

    public static final String F_SELECTED = "selected";
    public static final String F_NAME = "name";

    private boolean selected;
    private ItemDefinition definition;

    public Property(ItemDefinition definition) {
        Validate.notNull(definition, "Property name must no be null");

        this.definition=definition;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        return getItemDefinitionName(definition);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property property = (Property) o;

        if (selected != property.selected) return false;
        return !(definition != null ? !definition.equals(property.definition) : property.definition != null);

    }

    @Override
    public int hashCode() {
        int result = (selected ? 1 : 0);
        result = 31 * result + (definition != null ? definition.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("definition", definition)
                .append("selected", selected)
                .toString();
    }

    @Override
    public int compareTo(Property o) {
        String n1 = getItemDefinitionName(definition);
        String n2 = getItemDefinitionName(o.definition);

        if (n1 == null || n2 == null) {
            return 0;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
    }

    private String getItemDefinitionName(ItemDefinition def) {
        if (def == null) {
            return null;
        }

        if (def.getDisplayName() != null) {
            StringResourceModel nameModel = PageBase.createStringResourceStatic(null, def.getDisplayName());
            if (StringUtils.isNotEmpty(nameModel.getString())) {
                return nameModel.getString();
            }
        }
        String name = def.getDisplayName();        // TODO this is always null here, isn't it?
        if (StringUtils.isEmpty(name)) {
            name = def.getItemName().getLocalPart();
        }
        return name;
    }
}
