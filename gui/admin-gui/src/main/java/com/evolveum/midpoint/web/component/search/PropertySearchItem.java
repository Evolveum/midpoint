/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

/**
 * @author honchar
 */
public class PropertySearchItem<T extends Serializable> extends SearchItem {

    private static final long serialVersionUID = 1L;

    private DisplayableValue<T> value;

    @Experimental
    private boolean visible = true;

    public PropertySearchItem(Search search, @NotNull SearchItemDefinition definition) {
        this(search, definition, null);
    }

    public PropertySearchItem(Search search, @NotNull SearchItemDefinition definition, DisplayableValue<T> defaultValue) {
        super(search);
        Validate.notNull(definition.getPath(), "Item definition.getPath() must not be null.");
        Validate.notNull(definition.getDef(), "Item definition.getDef() must not be null.");

        if (!(definition.getDef() instanceof PrismPropertyDefinition)
                && !(definition.getDef() instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition.getDef() type '" + definition.getDef() + "'");
        }

        setDefinition(definition);
        this.value = defaultValue;
    }

        public DisplayableValue<T> getValue() {
        return value;
    }

    public void setValue(DisplayableValue<T> value) {
        this.value = value;
    }

    public List<DisplayableValue<T>> getAllowedValues(PageBase pageBase) {
        List<DisplayableValue<T>> list = new ArrayList<>();
        if (!(getDefinition().getDef() instanceof PrismPropertyDefinition)) {
            return list;
        }

        PrismPropertyDefinition<T> def = (PrismPropertyDefinition<T>) getDefinition().getDef();
        if (def.getAllowedValues() != null) {
            list.addAll(def.getAllowedValues());
        }

        if (list.isEmpty() && getDefinition().getAllowedValues() != null && !getDefinition().getAllowedValues().isEmpty()
                && getDefinition().getAllowedValues().iterator().next() instanceof DisplayableValue) {
            return getDefinition().getAllowedValues();
        }

        return list;
    }

    public ItemPath getPath() {
        return getDefinition().getPath();
    }

    @Override
    public String getName() {
        if (getDefinition() == null) {
            return "";
        }
        if (getDefinition().getDisplayName() != null){
            return WebComponentUtil.getTranslatedPolyString(getDefinition().getDisplayName());
        }
        if (getDefinition().getDef() == null) {
            return "";
        }
        String key = getDefinition().getDef().getDisplayName();
        if (StringUtils.isEmpty(key)) {
            key = getSearch().getTypeClass().getSimpleName() + '.' + getDefinition().getDef().getItemName().getLocalPart();
        }

        StringResourceModel nameModel = PageBase.createStringResourceStatic(key);
        if (nameModel != null) {
            if (StringUtils.isNotEmpty(nameModel.getString())) {
                return nameModel.getString();
            }
        }
        String name = getDefinition().getDef().getDisplayName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }

        return getDefinition().getDef().getItemName().getLocalPart();
    }

    @Override
    public Type getSearchItemType() {
        if (getDefinition().getDef() instanceof PrismReferenceDefinition) {
            return Type.REFERENCE;
        }

        PrismPropertyDefinition def = (PrismPropertyDefinition) getDefinition().getDef();
        if (!getAllowedValues(null).isEmpty()) {
            return Type.ENUM;
        }

        if (DOMUtil.XSD_BOOLEAN.equals(def.getTypeName())) {
            return Type.BOOLEAN;
        }

        if (QNameUtil.match(ItemPathType.COMPLEX_TYPE, def.getTypeName())) {
            return Type.ITEM_PATH;
        }

        return Type.TEXT;
    }

    public PolyStringType getDisplayName() {
        return getDefinition().getDisplayName();
    }

    public void setDisplayName(PolyStringType displayName) {
        this.getDefinition().setDisplayName(displayName);
    }

    @Override
    public String getHelp(PageBase pageBase) {
        return getDefinition().getHelp();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    @Experimental
    public ObjectFilter transformToFilter() {
        return null;
    }

    @Override
    public String toString() {
        return "PropertySearchItem{" +
                "path=" + getDefinition().getPath() +
                ", definition=" + getDefinition() +
                ", allowedRelations=" + getDefinition().getAllowedValues() +
                ", value=" + value +
                '}';
    }
}
