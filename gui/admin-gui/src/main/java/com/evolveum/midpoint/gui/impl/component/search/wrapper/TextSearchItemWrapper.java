/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.impl.component.search.panel.TextSearchItemPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;

import javax.xml.namespace.QName;

public class TextSearchItemWrapper extends PropertySearchItemWrapper<String> {

    private String valueEnumerationRefOid;
    private QName valueEnumerationRefType;

    public TextSearchItemWrapper() {
        super();
    }

    public TextSearchItemWrapper(ItemPath path, ItemDefinition<?> itemDef) {
        super(path, itemDef);
    }

    public TextSearchItemWrapper(
            ItemPath path, ItemDefinition<?> itemDef, String valueEnumerationRefOid, QName valueEnumerationRefType) {
        super(path, itemDef);
        this.valueEnumerationRefOid = valueEnumerationRefOid;
        this.valueEnumerationRefType = valueEnumerationRefType;
    }

    @Override
    public Class<TextSearchItemPanel> getSearchItemPanelClass() {
        return TextSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }

    public String getValueEnumerationRefOid() {
        return valueEnumerationRefOid;
    }

    public void setValueEnumerationRefOid(String valueEnumerationRefOid) {
        this.valueEnumerationRefOid = valueEnumerationRefOid;
    }

    public QName getValueEnumerationRefType() {
        return valueEnumerationRefType;
    }

    public void setValueEnumerationRefType(QName valueEnumerationRefType) {
        this.valueEnumerationRefType = valueEnumerationRefType;
    }
}
