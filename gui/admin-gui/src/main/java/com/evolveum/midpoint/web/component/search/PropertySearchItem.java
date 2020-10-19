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
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * @author honchar
 */
public class PropertySearchItem<T extends Serializable> extends SearchItem {

    private static final long serialVersionUID = 1L;

    private final ItemPath path;
    private final ItemDefinition definition;
    //TODO: think about dividing searchItem to searchProperty, searchReference?
    private final List<QName> allowedRelations;

    private DisplayableValue<T> value;
    private PolyStringType displayName;

    public PropertySearchItem(Search search, ItemPath path, ItemDefinition definition, List<QName> allowedRelations, PolyStringType displayName) {
        super(search);
        Validate.notNull(path, "Item path must not be null.");
        Validate.notNull(definition, "Item definition must not be null.");

        if (!(definition instanceof PrismPropertyDefinition)
                && !(definition instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition type '" + definition + "'");
        }

        this.path = path;
        this.definition = definition;
        this.allowedRelations = allowedRelations;
        this.displayName = displayName;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public DisplayableValue<T> getValue() {
        return value;
    }

    public void setValue(DisplayableValue<T> value) {
        this.value = value;
    }

    public List<DisplayableValue<T>> getAllowedValues() {
        List<DisplayableValue<T>> list = new ArrayList<>();
        if (!(definition instanceof PrismPropertyDefinition)) {
            return list;
        }

        PrismPropertyDefinition<T> def = (PrismPropertyDefinition<T>) definition;
        list.addAll(def.getAllowedValues());

        return list;
    }

    public List<QName> getAllowedRelations() {
        return allowedRelations;
    }

    public ItemPath getPath() {
        return path;
    }

    @Override
    public String getName() {
        if (displayName != null){
            return WebComponentUtil.getTranslatedPolyString(displayName);
        }
        String key = definition.getDisplayName();
        if (StringUtils.isEmpty(key)) {
            key = getSearch().getType().getSimpleName() + '.' + definition.getItemName().getLocalPart();
        }

        StringResourceModel nameModel = PageBase.createStringResourceStatic(null, key);
        if (nameModel != null) {
            if (StringUtils.isNotEmpty(nameModel.getString())) {
                return nameModel.getString();
            }
        }
        String name = definition.getDisplayName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }

        return definition.getItemName().getLocalPart();
    }

    @Override
    public Type getType() {
        if (definition instanceof PrismReferenceDefinition) {
            return Type.REFERENCE;
        }

        PrismPropertyDefinition def = (PrismPropertyDefinition) definition;
        if (def.getAllowedValues() != null && !def.getAllowedValues().isEmpty()) {
            return Type.ENUM;
        }

        if (DOMUtil.XSD_BOOLEAN.equals(def.getTypeName())) {
            return Type.BOOLEAN;
        }

        return Type.TEXT;
    }

    public PolyStringType getDisplayName() {
        return displayName;
    }

    public void setDisplayName(PolyStringType displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return "PropertySearchItem{" +
                "path=" + path +
                ", definition=" + definition +
                ", allowedRelations=" + allowedRelations +
                ", value=" + value +
                '}';
    }
}
