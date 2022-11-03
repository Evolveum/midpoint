package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class FulltextQueryWrapper implements Serializable {

    private String fullText;

    private Class<? extends Containerable> typeClass;

    public ObjectQuery createQuery(PrismContext ctx) throws SchemaException {
        if (StringUtils.isEmpty(fullText)) {
            return null;
        }
        return ctx.queryFor(typeClass)
                .fullText(fullText).build();

    }
}
