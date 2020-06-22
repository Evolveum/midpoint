/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.filter;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author honchar
 */
public abstract class SearchFilter<O extends ObjectType> implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<ValueSearchFilterItem> valueSearchFilterItems = new ArrayList<>();
//    private LogicalSearchFilterItem logicalSearchFilterItem;
    private PrismContext prismContext;
    private ObjectFilter baseFilter;
    Class<O> type;

    public enum LogicalFilterValue{
        AND,
        OR;
    }

    public SearchFilter(PrismContext prismContext, ObjectFilter baseFilter, Class<O> type){
        this.prismContext = prismContext;
        this.baseFilter = baseFilter;
        this.type = type;

        initSearchFilterItems(baseFilter);
    }

    public List<ValueSearchFilterItem> getValueSearchFilterItems() {
        return valueSearchFilterItems; //todo return unmodifiable list
    }

    public List<ObjectFilter> getObjectFilterList() {
        List<ObjectFilter> objectFilters = new ArrayList<>();
        valueSearchFilterItems.forEach(filterItem -> objectFilters.add(filterItem.getFilter()));
        return objectFilters;
    }

    public abstract void addSearchFilterItem(ValueSearchFilterItem valueSearchFilterItem);

    protected abstract void initSearchFilterItems(ObjectFilter baseFilter);

    public abstract ObjectFilter buildObjectFilter();

    public Class<O> getType(){
        return type;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }
}
