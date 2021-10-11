/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItem<T extends Serializable> implements Serializable {

    public static final String F_VALUES = "values";

    public enum Type {
        TEXT, BOOLEAN, ENUM, BROWSER, REFERENCE
    }

    private Search search;

    private ItemPath path;
    private ItemDefinition definition;
    private List<DisplayableValue<T>> values;

    //TODO: think about dividing searchItem to searchProperty, searchReference?
    private List<QName> allowedRelations;

    private boolean fixed;
    private boolean editWhenVisible;

    public SearchItem(Search search, ItemPath path, ItemDefinition definition, List<QName> allowedRelations) {
        Validate.notNull(path, "Item path must not be null.");
        Validate.notNull(definition, "Item definition must not be null.");

        if (!(definition instanceof PrismPropertyDefinition)
                && !(definition instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition type '" + definition + "'");
        }

        this.search = search;
        this.path = path;
        this.definition = definition;
        this.allowedRelations = allowedRelations;
    }

    public ItemPath getPath() {
        return path;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        String key = definition.getDisplayName();
        if (StringUtils.isEmpty(key)) {
            StringBuilder sb = new StringBuilder();
            sb.append(search.getType().getSimpleName()).append('.').append(definition.getItemName().getLocalPart());
            key =  sb.toString();
        }

        StringResourceModel nameModel = PageBase.createStringResourceStatic(null, key);
        if (nameModel != null){
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

    public List<DisplayableValue<T>> getValues() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }

    public void setValues(List<DisplayableValue<T>> values) {
        this.values = values;
    }

    public Search getSearch() {
        return search;
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

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public boolean isEditWhenVisible() {
        return editWhenVisible;
    }

    public void setEditWhenVisible(boolean editWhenVisible) {
        this.editWhenVisible = editWhenVisible;
    }

    public List<QName> getAllowedRelations() {
        return allowedRelations;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("definition", definition)
                .append("search", search)
                .append("path", path)
                .append("values", values)
                .toString();
    }
}
