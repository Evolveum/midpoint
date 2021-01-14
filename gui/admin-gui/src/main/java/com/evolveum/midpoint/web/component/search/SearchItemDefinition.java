/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;

public class SearchItemDefinition implements Serializable, Comparable<SearchItemDefinition> {

    public static final String F_SELECTED = "selected";
    public static final String F_NAME = "name";
    public static final String F_HELP = "help";

    private ItemPath path;
    private ItemDefinition def;
    private SearchItemType predefinedFilter;
    private PolyStringType displayName;
    private List<QName> allowedValues;
    private String description;
    private boolean isSelected = false;
    private boolean visibleByDefault = true;

    public SearchItemDefinition(ItemPath path, ItemDefinition def, List<QName> allowedValues) {
        this.path = path;
        this.def = def;
        this.allowedValues = allowedValues;
    }

    public SearchItemDefinition(SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
        this.description = predefinedFilter != null ? predefinedFilter.getDescription() : null;
        this.visibleByDefault = !Boolean.FALSE.equals(predefinedFilter.isVisibleByDefault());
        this.displayName = predefinedFilter.getDisplayName();
    }

    public ItemPath getPath() {
        return path;
    }

    public ItemDefinition getDef() {
        return def;
    }

    public List<QName> getAllowedValues() {
        return allowedValues;
    }

    public SearchItemType getPredefinedFilter() {
        return predefinedFilter;
    }

    public void setPredefinedFilter(SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
    }

    public PolyStringType getDisplayName() {
        return displayName;
    }

    public void setDisplayName(PolyStringType displayName) {
        this.displayName = displayName;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isVisibleByDefault() {
        return visibleByDefault;
    }

    public String getName() {
        if (getDisplayName() != null){
            return WebComponentUtil.getTranslatedPolyString(getDisplayName());
        }

        if (getDef() != null && StringUtils.isNotEmpty(getDef().getDisplayName())) {
            return PageBase.createStringResourceStatic(null, getDef().getDisplayName()).getString();
        }
        return WebComponentUtil.getItemDefinitionDisplayNameOrName(getDef(), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SearchItemDefinition property = (SearchItemDefinition) o;

        if (isSelected != property.isSelected()) { return false; }
        return !(getDef() != null ? !getDef().equals(property.getDef()) : property.getDef() != null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, def, predefinedFilter, displayName, allowedValues, description);
    }

    @Override
    public int compareTo(SearchItemDefinition o) {
        String n1 = getName();
        String n2 = o.getName();

        if (n1 == null || n2 == null) {
            return 0;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
    }

    public String getHelp(){
        if (StringUtils.isNotBlank(description)) {
            return description;
        }
        String help = "";
        if (def != null) {
            if (StringUtils.isNotEmpty(def.getHelp())) {
                help = def.getHelp();
            } else {
                help = def.getDocumentation();
            }
        }
        if (StringUtils.isNotBlank(help)) {
            help = help.replace("\n", "").replace("\r", "").replaceAll("^ +| +$|( )+", "$1");
        }
        return help;
    }
}
