/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class PropertySearchItemWrapper<T extends Serializable> extends AbstractSearchItemWrapper<T> {

    private SearchItemType searchItem;
    private ItemDefinition<?> itemDef;

    public PropertySearchItemWrapper (SearchItemType searchItem) {
        this.searchItem = searchItem;
    }

    @Override
    public Class<? extends AbstractSearchItemPanel> getSearchItemPanelClass() {
        return null;
    }

    @Override
    public DisplayableValue<T> getDefaultValue() {
        return new SearchValue<>();
    }

    @Override
    public boolean canRemoveSearchItem() {
        return !isResourceRefSearchItem();
    }

    private boolean isObjectClassSearchItem() {
        return ShadowType.F_OBJECT_CLASS.equivalent(searchItem.getPath().getItemPath());
    }

    private boolean isResourceRefSearchItem() {
        return ShadowType.F_RESOURCE_REF.equivalent(searchItem.getPath().getItemPath());
    }

    @Override
    public String getName() {
        if (searchItem.getDisplayName() != null){
            return WebComponentUtil.getTranslatedPolyString(searchItem.getDisplayName());
        }
        return "";
//        String key = getDefinition().getDef().getDisplayName();
//        if (StringUtils.isEmpty(key)) {
//            key = getSearch().getTypeClass().getSimpleName() + '.' + getDefinition().getDef().getItemName().getLocalPart();
//        }
//
//        StringResourceModel nameModel = PageBase.createStringResourceStatic(null, key);
//        if (nameModel != null) {
//            if (StringUtils.isNotEmpty(nameModel.getString())) {
//                return nameModel.getString();
//            }
//        }
//        String name = getDefinition().getDef().getDisplayName();
//        if (StringUtils.isNotEmpty(name)) {
//            return name;
//        }
//
//        return getDefinition().getDef().getItemName().getLocalPart();
//        if (getDisplayName() != null){
//            return WebComponentUtil.getTranslatedPolyString(getDisplayName());
//        }

//        if (getDef() != null && StringUtils.isNotEmpty(getDef().getDisplayName())) {
//            return PageBase.createStringResourceStatic(null, getDef().getDisplayName()).getString();
//        }
//        return WebComponentUtil.getItemDefinitionDisplayNameOrName(getDef(), null);
    }

    @Override
    public String getHelp() {
        return ""; //todo
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    public SearchItemType getSearchItem() {
        return searchItem;
    }

    public void setSearchItem(SearchItemType searchItem) {
        this.searchItem = searchItem;
    }

    public ItemDefinition<?> getItemDef() {
        return itemDef;
    }

    public void setItemDef(ItemDefinition<?> itemDef) {
        this.itemDef = itemDef;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        if (getValue().getValue() == null) {
            return null;
        }
        PrismContext ctx = PrismContext.get();
        QName typeName = itemDef.getTypeName();
//        if ((propDef.getAllowedValues() != null && !propDef.getAllowedValues().isEmpty())
//                || DOMUtil.XSD_BOOLEAN.equals(propDef.getTypeName())) {
//            we're looking for enum value, therefore equals filter is ok
//            or if it's boolean value
//            Object value = searchValue.getValue();
//            return ctx.queryFor(searchType)
//                    .item(path, propDef).eq(value).buildFilter();
//        } else
            if (DOMUtil.XSD_INT.equals(typeName)
                || DOMUtil.XSD_INTEGER.equals(typeName)
                || DOMUtil.XSD_LONG.equals(typeName)
                || DOMUtil.XSD_SHORT.equals(typeName)) {

            String text = (String) getValue().getValue();
            if (!StringUtils.isNumeric(text) && (getValue() instanceof SearchValue)) {
                ((SearchValue) getValue()).clear();
                return null;
            }
            Object parsedValue = Long.parseLong((String) getValue().getValue());
            return ctx.queryFor(ObjectType.class)
                    .item(getSearchItem().getPath().getItemPath(), itemDef).eq(parsedValue).buildFilter();
        } else if (DOMUtil.XSD_STRING.equals(typeName)) {
            String text = (String) getValue().getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(getSearchItem().getPath().getItemPath(), itemDef).contains(text).matchingCaseIgnore().buildFilter();
        } else if (DOMUtil.XSD_QNAME.equals(typeName)) {
            Object qnameValue = getValue().getValue();
            QName qName;
            if (qnameValue instanceof QName) {
                qName = (QName) qnameValue;
            } else {
                qName = new QName((String) qnameValue);
            }
            return ctx.queryFor(ObjectType.class)
                    .item(getSearchItem().getPath().getItemPath(), itemDef).eq(qName).buildFilter();
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(typeName)) {
                //we're looking for string value, therefore substring filter should be used
                String text = (String) getValue().getValue();
                return ctx.queryFor(ObjectType.class)
                        .item(getSearchItem().getPath().getItemPath(), itemDef).contains(text).matchingNorm().buildFilter();
            }
//            else if (propDef.getValueEnumerationRef() != null) {
//                String value = (String) searchValue.getValue();
//                return ctx.queryFor(searchType)
//                        .item(path, propDef).contains(value).matchingCaseIgnore().buildFilter();
//            }
        return null;
    }
}
