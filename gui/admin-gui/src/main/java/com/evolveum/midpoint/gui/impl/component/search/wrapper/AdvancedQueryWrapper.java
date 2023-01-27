package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class AdvancedQueryWrapper extends AbstractQueryWrapper {

    public static final String F_ADVANCED_QUERY = "advancedQuery";
    private String advancedQuery;

    public AdvancedQueryWrapper(String advancedQuery) {
        this.advancedQuery = advancedQuery;
    }

    public <T> ObjectQuery createQuery(Class<T> typeClass, PageBase pageBase, VariablesMap variablesMap) throws SchemaException {
        if (StringUtils.isEmpty(advancedQuery)) {
            return null;
        }
        PrismContext ctx = PrismContext.get();
        SearchFilterType search = ctx.parserFor(advancedQuery).type(SearchFilterType.COMPLEX_TYPE).parseRealValue();
        ObjectFilter filter = ctx.getQueryConverter().parseFilter(search, typeClass);
        return ctx.queryFactory().createQuery(filter);
    }

    public String getAdvancedQuery() {
        return advancedQuery;
    }
}
