package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;

public class AxiomQueryWrapper extends AbstractQueryWrapper {

    public static final String F_DSL_QUERY = "dslQuery";
    private String dslQuery;
    //TODO
    private ItemDefinition<?> containerDefinitionOverride;

    public <T> ObjectQuery createQuery(Class<T> typeClass, PageBase pageBase, VariablesMap variablesMap) throws SchemaException {
        if (StringUtils.isEmpty(dslQuery)) {
            return null;
        }
        PrismContext ctx = PrismContext.get();
        var parser = ctx.createQueryParser(ctx.getSchemaRegistry().staticNamespaceContext().allPrefixes());
        if (containerDefinitionOverride  == null) {
            ObjectFilter filter = parser.parseFilter(typeClass, dslQuery);
            return ctx.queryFactory().createQuery(filter);
        }
        ObjectFilter filter = parser.parseFilter(containerDefinitionOverride, dslQuery);
        return ctx.queryFactory().createQuery(filter);
    }

    public void setDslQuery(String dslQuery) {
        this.dslQuery = dslQuery;
    }

    public String getDslQuery() {
        return dslQuery;
    }
}
