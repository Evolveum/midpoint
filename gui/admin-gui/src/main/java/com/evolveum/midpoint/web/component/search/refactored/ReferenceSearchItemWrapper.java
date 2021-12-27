/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReferenceSearchItemWrapper<T extends Serializable> extends PropertySearchItemWrapper {

    PrismReferenceDefinition def;
    Class<? extends Containerable> searchType;
    List<T> availableValues = new ArrayList<>();

    public ReferenceSearchItemWrapper(SearchItemType searchItem, PrismReferenceDefinition def, Class<? extends Containerable> searchType) {
        this(searchItem, def, new ArrayList<>(), searchType);
    }

    public ReferenceSearchItemWrapper(SearchItemType searchItem, PrismReferenceDefinition def,
            List<T> availableValues, Class<? extends Containerable> searchType) {
        super(searchItem);
        this.def = def;
        this.searchType = searchType;
        this.availableValues.addAll(availableValues);
    }

    public PrismReferenceDefinition getDef() {
        return def;
    }

    public Class<? extends Containerable> getSearchType() {
        return searchType;
    }

    @Override
    public Class<ReferenceSearchItemPanel> getSearchItemPanelClass() {
        return ReferenceSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getDefaultValue() {
        ObjectReferenceType ref = new ObjectReferenceType();
        List<QName> supportedTargets = WebComponentUtil.createSupportedTargetTypeList(def.getTargetTypeName());
        if (supportedTargets.size() == 1) {
            ref.setType(supportedTargets.iterator().next());
        }
        return new SearchValue<>(ref);
    }
}
