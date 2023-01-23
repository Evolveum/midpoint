/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author honchar
 */
public class BasicSearchFilter<C extends Containerable> extends SearchFilter<C> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(BasicSearchFilter.class);

    private LogicalFilterValue logicalFilterValue = LogicalFilterValue.AND;

    public BasicSearchFilter(PageBase pageBase, ObjectFilter baseFilter, Class<C> type) {
        super(pageBase, baseFilter, type);
    }

    @Override
    public void addSearchFilterItem(ValueSearchFilterItem valueSearchFilterItem) {
        getValueSearchFilterItems().add(valueSearchFilterItem);
    }

    public void deleteSearchFilterItem(ItemDefinition itemToDelete) {
        if (itemToDelete == null){
            return;
        }
        Iterator<ValueSearchFilterItem> it = getValueSearchFilterItems().iterator();
        while (it.hasNext()){
            if (QNameUtil.match(itemToDelete.getItemName(), it.next().getPropertyDef().getItemName())){
                it.remove();
                return;
            }
        }
    }

    protected void initSearchFilterItems(ObjectFilter baseFilter) {
        if (baseFilter == null) {
            return;
        }
        if (baseFilter instanceof AndFilter) {
            logicalFilterValue = LogicalFilterValue.AND;

            AndFilter andFilter = (AndFilter) baseFilter;
            addValueFilters(andFilter.getConditions());
        } else if (baseFilter instanceof OrFilter) {
            logicalFilterValue = LogicalFilterValue.OR;

            OrFilter orFilter = (OrFilter) baseFilter;
            addValueFilters(orFilter.getConditions());
        } else if (baseFilter instanceof ValueFilter) {
            addValueFilters(Arrays.asList(baseFilter));
        }
    }

    @Override
    public ObjectFilter buildObjectFilter(){
        if (logicalFilterValue.equals(LogicalFilterValue.OR)){
            return getPrismContext().queryFactory().createOr(getObjectFilterList());
        } else {
            return getPrismContext().queryFactory().createAnd(getObjectFilterList());
        }
    }

    public void addValueFilters(List<ObjectFilter> objectFilters) {
        objectFilters.forEach(filter -> {
            boolean applyNegation = false;
            if (filter instanceof NotFilter){
                applyNegation = true;
            }
            ObjectFilter realFilter = applyNegation ? ((NotFilter) filter).getFilter() : filter;
            if (realFilter instanceof ValueFilter) {
                if (((ValueFilter) realFilter).getDefinition() != null) {
                    ValueSearchFilterItem filterItem = new ValueSearchFilterItem((ValueFilter)realFilter, applyNegation);
                    addSearchFilterItem(filterItem);
                }
            }
        });
    }

}
