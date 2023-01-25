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

public class BasicQueryWrapper<C extends Containerable> extends AbstractQueryWrapper {

    private List<FilterableSearchItemWrapper> itemsList = new ArrayList<>();

    private boolean allowToConfigureSearchItems;

   public BasicQueryWrapper(SearchBoxConfigurationType searchBoxConfig) {
        if (searchBoxConfig.isAllowToConfigureSearchItems() != null) {
            allowToConfigureSearchItems = searchBoxConfig.isAllowToConfigureSearchItems();
        }
    }

    public BasicQueryWrapper() {
     }

    public List<FilterableSearchItemWrapper> getItemsList() {
        return itemsList;
    }

    public boolean isAllowToConfigureSearchItems() {
        return allowToConfigureSearchItems;
    }

    public void setAllowToConfigureSearchItems(boolean allowToConfigureSearchItems) {
        this.allowToConfigureSearchItems = allowToConfigureSearchItems;
    }

    public BasicQueryWrapper<C> removePropertySearchItem(ItemPath path) {
        if (path == null) {
            return this;
        }
        Iterator<FilterableSearchItemWrapper> it = getItemsList().iterator();
        while (it.hasNext()) {
            FilterableSearchItemWrapper item = it.next();
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
    public ObjectQuery createQuery(Class<? extends Containerable> typeClass, PageBase pageBase, VariablesMap variablesMap) throws SchemaException {
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

    private List<ObjectFilter> getSearchItemFilterList(PageBase pageBase, Class<? extends Containerable> typeClass, VariablesMap defaultVariables) {
        List<ObjectFilter> conditions = new ArrayList<>();
//        if (!SearchBoxModeType.BASIC.equals(getSearchMode())) {
//            return conditions;
//        }
        for (FilterableSearchItemWrapper item : itemsList) {

            ObjectFilter filter = item.createFilter(typeClass, pageBase, defaultVariables);
            if (filter != null) {
                conditions.add(filter);
            }
        }
        return conditions;
    }
}
