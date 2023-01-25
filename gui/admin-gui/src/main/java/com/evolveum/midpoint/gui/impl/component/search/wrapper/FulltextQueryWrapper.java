package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;

public class FulltextQueryWrapper extends AbstractQueryWrapper {

    private String fullText;

    public FulltextQueryWrapper(String fullText) {
        this.fullText = fullText;
    }

    public ObjectQuery createQuery(Class<? extends Containerable> typeClass, PageBase pageBase, VariablesMap variablesMap) throws SchemaException {
        if (StringUtils.isEmpty(fullText)) {
            return null;
        }
        return PrismContext.get().queryFor(typeClass)
                .fullText(fullText).build();

    }

    public String getFullText() {
        return fullText;
    }
}
