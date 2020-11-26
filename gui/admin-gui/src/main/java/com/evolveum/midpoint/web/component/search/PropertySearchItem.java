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
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
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
import org.jetbrains.annotations.NotNull;

/**
 * @author honchar
 */
public class PropertySearchItem<T extends Serializable> extends SearchItem {

    private static final long serialVersionUID = 1L;

    private final SearchItemDefinition definition;

    private DisplayableValue<T> value;

    public PropertySearchItem(Search search, @NotNull SearchItemDefinition definition) {
        super(search);
        Validate.notNull(definition.getPath(), "Item definition.getPath() must not be null.");
        Validate.notNull(definition.getDef(), "Item definition.getDef() must not be null.");

        if (!(definition.getDef() instanceof PrismPropertyDefinition)
                && !(definition.getDef() instanceof PrismReferenceDefinition)) {
            throw new IllegalArgumentException("Unknown item definition.getDef() type '" + definition.getDef() + "'");
        }

        this.definition = definition;
    }

    public SearchItemDefinition getDefinition() {
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
        if (!(definition.getDef() instanceof PrismPropertyDefinition)) {
            return list;
        }

        PrismPropertyDefinition<T> def = (PrismPropertyDefinition<T>) definition.getDef();
        list.addAll(def.getAllowedValues());

        return list;
    }

    public List<QName> getAllowedRelations() {
        return definition.getAllowedValues();
    }

    public ItemPath getPath() {
        return definition.getPath();
    }

    @Override
    public String getName() {
        if (definition.getDisplayName() != null){
            return WebComponentUtil.getTranslatedPolyString(definition.getDisplayName());
        }
        String key = definition.getDef().getDisplayName();
        if (StringUtils.isEmpty(key)) {
            key = getSearch().getType().getSimpleName() + '.' + definition.getDef().getItemName().getLocalPart();
        }

        StringResourceModel nameModel = PageBase.createStringResourceStatic(null, key);
        if (nameModel != null) {
            if (StringUtils.isNotEmpty(nameModel.getString())) {
                return nameModel.getString();
            }
        }
        String name = definition.getDef().getDisplayName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }

        return definition.getDef().getItemName().getLocalPart();
    }

    @Override
    public Type getType() {
        if (definition.getDef() instanceof PrismReferenceDefinition) {
            return Type.REFERENCE;
        }

        PrismPropertyDefinition def = (PrismPropertyDefinition) definition.getDef();
        if (def.getAllowedValues() != null && !def.getAllowedValues().isEmpty()) {
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
        return definition.getDisplayName();
    }

    public void setDisplayName(PolyStringType displayName) {
        this.definition.setDisplayName(displayName);
    }

    @Override
    public String getHelp(PageBase pageBase) {
        return definition.getHelp();
    }

    @Override
    public String toString() {
        return "PropertySearchItem{" +
                "path=" + definition.getPath() +
                ", definition=" + definition +
                ", allowedRelations=" + definition.getAllowedValues() +
                ", value=" + value +
                '}';
    }
}
