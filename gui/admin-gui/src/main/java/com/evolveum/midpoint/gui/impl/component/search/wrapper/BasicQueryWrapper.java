/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BasicQueryWrapper extends AbstractQueryWrapper {

    private List<FilterableSearchItemWrapper<?>> itemsList = new ArrayList<>();

    private boolean allowToConfigureSearchItems;

   public BasicQueryWrapper(SearchBoxConfigurationType searchBoxConfig) {
        if (searchBoxConfig.isAllowToConfigureSearchItems() != null) {
            allowToConfigureSearchItems = searchBoxConfig.isAllowToConfigureSearchItems();
        }
    }

    public BasicQueryWrapper() {
     }

    public List<FilterableSearchItemWrapper<?>> getItemsList() {
        return itemsList;
    }

    public boolean isAllowToConfigureSearchItems() {
        return allowToConfigureSearchItems;
    }

    public void setAllowToConfigureSearchItems(boolean allowToConfigureSearchItems) {
        this.allowToConfigureSearchItems = allowToConfigureSearchItems;
    }

    public BasicQueryWrapper removePropertySearchItem(ItemPath path) {
        if (path == null) {
            return this;
        }
        Iterator<FilterableSearchItemWrapper<?>> it = getItemsList().iterator();
        while (it.hasNext()) {
            FilterableSearchItemWrapper<?> item = it.next();
            if (!(item instanceof PropertySearchItemWrapper)) {
                continue;
            }
            if (path.equivalent(((PropertySearchItemWrapper) item).getPath())) {
                it.remove();
                return this;
            }
        }
        return this;
    }

    @Override
    public <T> ObjectQuery createQuery(Class<T> typeClass, PageBase pageBase, VariablesMap variablesMap) throws SchemaException {
        if (itemsList.isEmpty()) {
            return null;
        }

        PrismContext ctx = PrismContext.get();
        ObjectQuery query = null;
        if (query == null) {
            query = ctx.queryFactory().createQuery();
        }
        List<ObjectFilter> filters = getSearchItemFilterList(pageBase, typeClass, variablesMap);
        if (filters != null) {
            query.addFilter(ctx.queryFactory().createAnd(filters));
        }
        return query;
    }

    private <T> List<ObjectFilter> getSearchItemFilterList(PageBase pageBase, Class<T> typeClass, VariablesMap defaultVariables) {
        List<ObjectFilter> conditions = new ArrayList<>();
        for (FilterableSearchItemWrapper item : itemsList) {
            // todo this should be uncommented after 4.7 release (release too close to "try" to fix something like this)
            // if (!item.isApplyFilter(SearchBoxModeType.BASIC)) {
            //     continue;
            // }

            ObjectFilter filter = item.createFilter(typeClass, pageBase, defaultVariables);
            if (filter != null) {
                conditions.add(filter);
            }
        }
        return conditions;
    }
}
