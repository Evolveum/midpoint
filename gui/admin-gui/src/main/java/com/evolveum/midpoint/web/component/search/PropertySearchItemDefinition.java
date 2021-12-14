/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class PropertySearchItemDefinition extends AbstractSearchItemDefinition {

    private ItemPath path;
    private ItemDefinition def;
    private List allowedValues;
    private PolyStringType displayName;

    public PropertySearchItemDefinition(@NotNull ItemPath path, @NotNull ItemDefinition def) {
        this(path, def, null);
    }

    public PropertySearchItemDefinition(@NotNull ItemPath path, @NotNull ItemDefinition def, List allowedValues) {
        this.path = path;
        this.def = def;
        this.allowedValues = allowedValues;
    }

    public ItemDefinition getDef() {
        return def;
    }

    public List getAllowedValues() {
        return allowedValues;
    }

    public PolyStringType getDisplayName() {
        return displayName;
    }

    public void setDisplayName(PolyStringType displayName) {
        this.displayName = displayName;
    }

    public ItemPath getPath() {
        return path;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    @Override
    public PropertySearchItem<PropertySearchItemDefinition> createSearchItem() {
        PropertySearchItem<PropertySearchItemDefinition> item = new PropertySearchItem<>(null, PropertySearchItemDefinition.this);
        item.getSearchItemDefinition().setSearchItemDisplayed(isVisibleByDefault());
        return item;
    }

    @Override
    public String getHelp(){
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

    @Override
    public String getName() {
        if (StringUtils.isNotEmpty(getDef().getDisplayName())) {
            return PageBase.createStringResourceStatic(null, getDef().getDisplayName()).getString();
        }
        return WebComponentUtil.getItemDefinitionDisplayNameOrName(getDef(), null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, def, allowedValues);
    }

}
