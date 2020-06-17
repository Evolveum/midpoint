/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.filter;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;

import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;

/**
 * @author honchar
 */
public class ValueSearchFilterItem<V extends PrismValue, D extends ItemDefinition> implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String F_VALUE = "value";

    private boolean applyNegation;
    private ValueFilter<V, D> filter;

    public ValueSearchFilterItem(ValueFilter filter, boolean applyNegation){
        this.filter = filter;
        this.applyNegation = applyNegation;
    }

    public boolean isApplyNegation() {
        return applyNegation;
    }

    public void setApplyNegation(boolean applyNegation) {
        this.applyNegation = applyNegation;
    }

    public ValueFilter getFilter() {
        return filter;
    }

    public void setFilter(ValueFilter filter) {
        this.filter = filter;
    }

    public V getValue(){
        if (filter == null || CollectionUtils.isEmpty(filter.getValues())){
            return null;
        }
        return filter.getValues().get(0);
    }
}
