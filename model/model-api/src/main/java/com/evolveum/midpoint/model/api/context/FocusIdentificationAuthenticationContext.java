/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.query.OrFilterImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author skublik
 */

public class FocusIdentificationAuthenticationContext extends AbstractAuthenticationContext {

    private Map<ItemPath, String> values;

    public FocusIdentificationAuthenticationContext(
            Map<ItemPath, String> values, Class<? extends FocusType> principalType, List<ObjectReferenceType> requireAssignment) {
        super(null, principalType, requireAssignment);
        this.values = values;
    }

    @Override
    public Object getEnteredCredential() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectQuery createFocusQuery() {
        List<ObjectFilter> filters = new ArrayList<>();
        for (Map.Entry<ItemPath, String> entry : values.entrySet()) {
            ObjectFilter objectFilter = PrismContext.get().queryFor(getPrincipalType())
            .item(entry.getKey()).eq(entry.getValue()).buildFilter();
            filters.add(objectFilter);
        }

        ObjectQuery query = PrismContext.get().queryFor(getPrincipalType()).build();
        OrFilter orFilter = new OrFilterImpl(filters);
        query.addFilter(orFilter);
        return query;
    }
}
