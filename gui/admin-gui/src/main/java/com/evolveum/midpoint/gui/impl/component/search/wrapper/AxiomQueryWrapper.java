package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class AxiomQueryWrapper<C extends Containerable> implements Serializable {

    private String dslQuery;
    private Class<C> typeClass;
    private PrismContainerDefinition<C> containerDefinitionOverride;

    public ObjectQuery createQuery(PrismContext ctx) throws SchemaException {
        if (StringUtils.isEmpty(dslQuery)) {
            return null;
        }
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
