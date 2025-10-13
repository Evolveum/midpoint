/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProcessedObjectTypeSearchItemWrapper extends ChoicesSearchItemWrapper<QName> {

    public ProcessedObjectTypeSearchItemWrapper() {
        super(SimulationResultProcessedObjectType.F_TYPE,
                ObjectTypeListUtil.createObjectTypesList().stream()
                        .map(o -> new DisplayableValueImpl<>(o.getTypeQName(), WebComponentUtil.createEnumResourceKey(o)))
                        .collect(Collectors.toList()));
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (getValue().getValue() == null) {
            return null;
        }

        return PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .item(getPath()).eq(getValue().getValue()).buildFilter();
    }
}
