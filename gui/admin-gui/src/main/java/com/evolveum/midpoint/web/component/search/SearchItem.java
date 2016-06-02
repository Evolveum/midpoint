/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItem<T extends Serializable> implements Serializable {

    public static final String F_VALUES = "values";

    public enum Type {
        TEXT, BOOLEAN, ENUM, BROWSER
    }

    private Search search;

    private ItemPath path;
    private ItemDefinition definition;
    private List<DisplayableValue<T>> values;

    private boolean fixed;
    private boolean editWhenVisible;

    public SearchItem(Search search, ItemPath path, ItemDefinition definition) {
        Validate.notNull(path, "Item path must not be null.");
        Validate.notNull(definition, "Item definition must not be null.");

        if (!(definition instanceof PrismPropertyDefinition)
                && !(definition instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition type '" + definition + "'");
        }

        this.search = search;
        this.path = path;
        this.definition = definition;
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
            sb.append(search.getType().getSimpleName()).append('.').append(definition.getName().getLocalPart());
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

        return definition.getName().getLocalPart();
    }

    public Type getType() {
        if (definition instanceof PrismReferenceDefinition) {
            return Type.BROWSER;
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

    public List<DisplayableValue> getAllowedValues() {
        List<DisplayableValue> list = new ArrayList();
        if (!(definition instanceof PrismPropertyDefinition)) {
            return list;
        }

        PrismPropertyDefinition def = (PrismPropertyDefinition) definition;
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
