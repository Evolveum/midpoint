package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;

public class AssignmentFulltextQueryWrapper extends FulltextQueryWrapper {

    public AssignmentFulltextQueryWrapper(String fullText) {
        super(fullText);
    }

    public <T> ObjectQuery createQuery(Class<T> typeClass, PageBase pageBase, VariablesMap variablesMap) throws SchemaException {
        ObjectQuery orig = super.createQuery(typeClass, pageBase, variablesMap);
        if (orig == null) {
            return null;
        }
        return PrismContext.get().queryFor(AssignmentType.class)
                .exists(ItemPath.create(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE))
                .filter(orig.getFilter())
                .build();
    }
}
