package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class AdvancedQueryWrapper implements Serializable {

    private String advancedQuery;
    private Class<? extends Containerable> typeClass;

    public AdvancedQueryWrapper() {

    }

    public ObjectQuery createQuery(PrismContext ctx) throws SchemaException {
        if (StringUtils.isEmpty(advancedQuery)) {
            return null;
        }
        SearchFilterType search = ctx.parserFor(advancedQuery).type(SearchFilterType.COMPLEX_TYPE).parseRealValue();
        ObjectFilter filter = ctx.getQueryConverter().parseFilter(search, typeClass);
        return ctx.queryFactory().createQuery(filter);
    }

    public void setAdvancedQuery(String advancedQuery) {
        this.advancedQuery = advancedQuery;
    }
}
