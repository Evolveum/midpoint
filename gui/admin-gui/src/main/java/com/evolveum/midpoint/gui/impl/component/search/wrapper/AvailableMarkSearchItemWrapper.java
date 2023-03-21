/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AvailableMarkSearchItemWrapper extends ChoicesSearchItemWrapper<String> {

    public AvailableMarkSearchItemWrapper(List<DisplayableValue<String>> availableValues) {
        super(SimulationResultProcessedObjectType.F_EVENT_MARK_REF, availableValues);

        setApplyFilter(true);
        setVisible(true);
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (getValue().getValue() == null) {
            return null;
        }

        return PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .item(getPath()).ref(getValue().getValue(), MarkType.COMPLEX_TYPE).buildFilter();
    }

    public void setValue(String oid) {
        DisplayableValue<String> newValue = getAvailableValues().stream()
                .filter(d -> Objects.equals(d.getValue(), oid))
                .findFirst().orElse(null);
        setValue(newValue);
    }
}
