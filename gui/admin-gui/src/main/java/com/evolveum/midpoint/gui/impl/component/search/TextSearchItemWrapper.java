/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

public class TextSearchItemWrapper extends PropertySearchItemWrapper<String> {

    private PrismReferenceValue valueEnumerationRef;

    public TextSearchItemWrapper(SearchItemType searchItem) {
        this(searchItem, null);
    }

    public TextSearchItemWrapper(SearchItemType searchItem, PrismReferenceValue valueEnumerationRef) {
        super(searchItem);
        this.valueEnumerationRef = valueEnumerationRef;
    }

    @Override
    public Class<TextSearchItemPanel> getSearchItemPanelClass() {
        return TextSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }

    public PrismReferenceValue getValueEnumerationRef() {
        return valueEnumerationRef;
    }

    public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
        this.valueEnumerationRef = valueEnumerationRef;
    }
}
