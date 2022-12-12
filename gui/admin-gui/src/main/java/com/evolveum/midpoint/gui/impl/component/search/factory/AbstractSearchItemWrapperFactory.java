/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.PrismContext;

import javax.xml.namespace.QName;

public abstract class AbstractSearchItemWrapperFactory<T, PSW extends PropertySearchItemWrapper<T>> {

    protected abstract PSW createSearchWrapper(SearchItemContext ctx);

    public PSW create(SearchItemContext ctx) {
        PSW searchItem = createSearchWrapper(ctx);

        searchItem.setVisible(ctx.isVisible());
        searchItem.setValueTypeName(ctx.getValueTypeName());

        searchItem.setName(ctx.getDisplayName()); //getSearchItemName(item, itemDef)
        searchItem.setHelp(ctx.getHelp()); //getSearchItemHelp(item, itemDef)

        setupParameterOptions(ctx, searchItem);

        if (ctx.hasPredefinedFilter()) {
            searchItem.setPredefinedFilter(ctx.getPredefinedFilter());
            searchItem.setVisible(true);
            searchItem.setApplyFilter(true);
            searchItem.setFilterExpression(ctx.getFilterExpression());
        }
        return searchItem;
    }

    protected void setupParameterOptions(SearchItemContext ctx, PSW searchItem) {
        if (ctx.hasParameter()) {
            searchItem.setParameterName(ctx.getParameterName());
            QName parameterType = ctx.getParameterType();
            if (parameterType != null) {
                searchItem.setParameterValueType(PrismContext.get().getSchemaRegistry().determineClassForType(parameterType));
            }
        }
    }

    public abstract boolean match(SearchItemContext ctx);

}
