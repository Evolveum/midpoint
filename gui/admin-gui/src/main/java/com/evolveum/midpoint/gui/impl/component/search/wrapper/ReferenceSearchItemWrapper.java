/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.ReferenceSearchItemPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

public class ReferenceSearchItemWrapper extends PropertySearchItemWrapper<ObjectReferenceType> {

    PrismReferenceDefinition def;
    Class<?> searchType;
    private QName targetType;
//    List<T> availableValues = new ArrayList<>();

    public ReferenceSearchItemWrapper(PrismReferenceDefinition def, QName targetType, Class<?> searchType) {
        super(def.getItemName());
        this.def = def;
        this.targetType = targetType;
        this.searchType = searchType;
    }

//    public ReferenceSearchItemWrapper(PrismReferenceDefinition def, Class<? extends Containerable> searchType) {
//        super(def.getItemName());
//        this.def = def;
//        this.searchType = searchType;
////        this.availableValues.addAll(availableValues);
//    }

    public PrismReferenceDefinition getDef() {
        return def;
    }

    public Class<?> getSearchType() {
        return searchType;
    }

    @Override
    public Class<ReferenceSearchItemPanel> getSearchItemPanelClass() {
        return ReferenceSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getDefaultValue() {
        ObjectReferenceType ref = new ObjectReferenceType();
        List<QName> supportedTargets = getSupportedTargetTypes();
        if (supportedTargets.size() == 1) {
            ref.setType(supportedTargets.iterator().next());
        }
        return new SearchValue<>(ref);
    }

    private List<QName> getSupportedTargetTypes() {
        return targetType != null ? Arrays.asList(targetType) : WebComponentUtil.createSupportedTargetTypeList(def.getTargetTypeName());
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (getPredefinedFilter() != null) {
            return evaluatePredefinedFilter(type, variables, pageBase);
        }
        PrismReferenceValue refValue = getValue().getValue().asReferenceValue();
        if (refValue.isEmpty()) {
            return null;
        }
        List<QName> supportedTargets = getSupportedTargetTypes();
        if (supportedTargets.size() == 1 && QNameUtil.match(supportedTargets.iterator().next(), refValue.getTargetType())  && refValue.getOid() == null
                && refValue.getObject() == null && refValue.getRelation() == null && refValue.getFilter() == null) {
            return null;
        }
        RefFilter refFilter = (RefFilter) PrismContext.get().queryFor(type)
                .item(getPath()).ref(refValue.clone())
                .buildFilter();
        refFilter.setOidNullAsAny(true);
        refFilter.setTargetTypeNullAsAny(true);
        return refFilter;
    }

//    public List<T> getAvailableValues() {
//        return availableValues;
//    }

    public QName getTargetType() {
        return targetType;
    }
}
