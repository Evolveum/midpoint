/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.io.Serializable;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class PropertySearchItemWrapper<T extends Serializable> extends AbstractSearchItemWrapper<T> {

    private ItemPath path;
    private QName valueTypeName;
    private String name;
    private String help;

    public PropertySearchItemWrapper () {
    }

    public PropertySearchItemWrapper (ItemPath path) {
        this.path = path;
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
        return super.canRemoveSearchItem() && !isResourceRefSearchItem();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    private boolean isObjectClassSearchItem() {
        return ShadowType.F_OBJECT_CLASS.equivalent(path);
    }

    private boolean isResourceRefSearchItem() {
        return ShadowType.F_RESOURCE_REF.equivalent(path);
    }


    @Override
    public String getTitle() {
        return ""; //todo
    }

//    public ItemDefinition<?> getItemDef() {
//        return itemDef;
//    }
//
//    public void setItemDef(ItemDefinition<?> itemDef) {
//        this.itemDef = itemDef;
//    }

    public ItemPath getPath() {
        return path;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    public QName getValueTypeName() {
        return valueTypeName;
    }

    public void setValueTypeName(QName valueTypeName) {
        this.valueTypeName = valueTypeName;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (getValue().getValue() == null) {
            return null;
        }
        PrismContext ctx = PrismContext.get();
//        if ((propDef.getAllowedValues() != null && !propDef.getAllowedValues().isEmpty())
//                || DOMUtil.XSD_BOOLEAN.equals(propDef.getTypeName())) {
//            we're looking for enum value, therefore equals filter is ok
//            or if it's boolean value
//            Object value = searchValue.getValue();
//            return ctx.queryFor(searchType)
//                    .item(path, propDef).eq(value).buildFilter();
//        } else
        if (DOMUtil.XSD_INT.equals(valueTypeName)
                || DOMUtil.XSD_INTEGER.equals(valueTypeName)
                || DOMUtil.XSD_LONG.equals(valueTypeName)
                || DOMUtil.XSD_SHORT.equals(valueTypeName)) {

            String text = (String) getValue().getValue();
            if (!StringUtils.isNumeric(text) && (getValue() instanceof SearchValue)) {
                ((SearchValue) getValue()).clear();
                return null;
            }
            Object parsedValue = Long.parseLong((String) getValue().getValue());
            return ctx.queryFor(type)
                    .item(path).eq(parsedValue).buildFilter();
        } else if (DOMUtil.XSD_STRING.equals(valueTypeName)) {
            String text = (String) getValue().getValue();
            return ctx.queryFor(type)
                    .item(path).contains(text).matchingCaseIgnore().buildFilter();
        } else if (DOMUtil.XSD_QNAME.equals(valueTypeName)) {
            Object qnameValue = getValue().getValue();
            QName qName;
            if (qnameValue instanceof QName) {
                qName = (QName) qnameValue;
            } else {
                qName = new QName((String) qnameValue);
            }
            return ctx.queryFor(type)
                    .item(path).eq(qName).buildFilter();
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueTypeName)) {
                //we're looking for string value, therefore substring filter should be used
                String text = (String) getValue().getValue();
                return ctx.queryFor(type)
                        .item(path).contains(text).matchingNorm().buildFilter();
            }
//            else if (propDef.getValueEnumerationRef() != null) {
//                String value = (String) searchValue.getValue();
//                return ctx.queryFor(searchType)
//                        .item(path, propDef).contains(value).matchingCaseIgnore().buildFilter();
//            }
        return null;
    }
}
