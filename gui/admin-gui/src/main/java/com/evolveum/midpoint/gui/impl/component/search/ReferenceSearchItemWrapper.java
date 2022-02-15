/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
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

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        PrismReferenceValue refValue = ((ObjectReferenceType) getValue().getValue()).asReferenceValue();
        if (refValue.isEmpty()) {
            return null;
        }
        List<QName> supportedTargets = WebComponentUtil.createSupportedTargetTypeList(def.getTargetTypeName());
        if (supportedTargets.size() == 1 && QNameUtil.match(supportedTargets.iterator().next(), refValue.getTargetType())  && refValue.getOid() == null
                && refValue.getObject() == null && refValue.getRelation() == null && refValue.getFilter() == null) {
            return null;
        }
        RefFilter refFilter = (RefFilter) PrismContext.get().queryFor(type)
                .item(getSearchItem().getPath().getItemPath()).ref(refValue.clone())
                .buildFilter();
        refFilter.setOidNullAsAny(true);
        refFilter.setTargetTypeNullAsAny(true);
        return refFilter;
    }
}
