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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author honchar
 */
public class AttributeSearchItem<T extends Serializable> extends SearchItem<AttributeSearchItemDefinition> {

    private static final long serialVersionUID = 1L;

    private DisplayableValue<T> value;

    @Experimental
    private boolean visible = true;

    public AttributeSearchItem(Search search, @NotNull AttributeSearchItemDefinition definition) {
        this(search, definition, null);
    }

    public AttributeSearchItem(Search search, @NotNull AttributeSearchItemDefinition definition, DisplayableValue<T> defaultValue) {
        super(search, definition);
        Validate.notNull(definition.getPath(), "Item definition.getPath() must not be null.");
        Validate.notNull(definition.getDef(), "Item definition.getDef() must not be null.");

        if (!(definition.getDef() instanceof PrismPropertyDefinition)
                && !(definition.getDef() instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition.getDef() type '" + definition.getDef() + "'");
        }

        this.value = defaultValue;
    }

    @Override
    public Class<SearchPropertyPanel> getSearchItemPanelClass() {
        return SearchPropertyPanel.class;
    }

    public DisplayableValue<T> getValue() {
        return value;
    }

    public void setValue(DisplayableValue<T> value) {
        this.value = value;
    }

    public List<DisplayableValue<T>> getAllowedValues(PageBase pageBase) {
        List<DisplayableValue<T>> list = new ArrayList<>();
        if (!(getSearchItemDefinition().getDef() instanceof PrismPropertyDefinition)) {
            return list;
        }

        PrismPropertyDefinition<T> def = (PrismPropertyDefinition<T>) getSearchItemDefinition().getDef();
        if (def.getAllowedValues() != null) {
            list.addAll(def.getAllowedValues());
        }

        if (list.isEmpty() && getSearchItemDefinition().getAllowedValues() != null && !getSearchItemDefinition().getAllowedValues().isEmpty()
                && getSearchItemDefinition().getAllowedValues().iterator().next() instanceof DisplayableValue) {
            return getSearchItemDefinition().getAllowedValues();
        }

        return list;
    }

    public ItemPath getPath() {
        return getSearchItemDefinition().getPath();
    }

    @Override
    public String getName() {
        //commented because displayName can be used just in case of Filter search item definition
//        if (getDefinition().getDisplayName() != null){
//            return WebComponentUtil.getTranslatedPolyString(getDefinition().getDisplayName());
//        }
        String key = getSearchItemDefinition().getDef().getDisplayName();
        if (StringUtils.isEmpty(key)) {
            key = getSearch().getTypeClass().getSimpleName() + '.' + getSearchItemDefinition().getDef().getItemName().getLocalPart();
        }

        StringResourceModel nameModel = PageBase.createStringResourceStatic(null, key);
        if (nameModel != null) {
            if (StringUtils.isNotEmpty(nameModel.getString())) {
                return nameModel.getString();
            }
        }
        String name = getSearchItemDefinition().getDef().getDisplayName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }

        return getSearchItemDefinition().getDef().getItemName().getLocalPart();
    }

    @Override
    public Type getSearchItemType() {
        if (getSearchItemDefinition().getDef() instanceof PrismReferenceDefinition) {
            return Type.REFERENCE;
        }

        PrismPropertyDefinition def = (PrismPropertyDefinition) getSearchItemDefinition().getDef();
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

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables){
        return null;
    }


//    public PolyStringType getDisplayName() {
//        return getDefinition().getDisplayName();
//    }
//
//    public void setDisplayName(PolyStringType displayName) {
//        this.getDefinition().setDisplayName(displayName);
//    }

    @Override
    public String getHelp(PageBase pageBase) {
        return getSearchItemDefinition().getHelp();
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
                "path=" + getSearchItemDefinition().getPath() +
                ", definition=" + getSearchItemDefinition() +
                ", allowedRelations=" + getSearchItemDefinition().getAllowedValues() +
                ", value=" + value +
                '}';
    }
}
