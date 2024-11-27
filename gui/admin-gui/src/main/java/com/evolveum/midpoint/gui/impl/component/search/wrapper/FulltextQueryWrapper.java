package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;

public class FulltextQueryWrapper extends AbstractQueryWrapper {

    private final String fullText;

    public FulltextQueryWrapper(String fullText) {
        this.fullText = fullText;
    }

    public <T> ObjectQuery createQuery(Class<T> typeClass, PageBase pageBase, VariablesMap variablesMap) throws SchemaException {
        if (!Containerable.class.isAssignableFrom(typeClass)) {
            return null;
        }

        S_FilterEntryOrEmpty builder = PrismContext.get().queryFor((Class<? extends Containerable>) typeClass);
        if (StringUtils.isEmpty(fullText)) {
            return builder.build();
        }

        return builder.fullText(fullText).build();
    }

    public String getFullText() {
        return fullText;
    }
}
