/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AvailableMarkSearchItemWrapper;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AvailableMarkItemWrapperFactory extends AbstractSearchItemWrapperFactory<String, AvailableMarkSearchItemWrapper> {

    @Override
    protected AvailableMarkSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        List<DisplayableValue<String>> availableEventMarks = ctx.getAvailableEventMarks();

        DisplayableValue<String> selected = availableEventMarks.stream()
                .filter(d -> Objects.equals(d.getValue(), ctx.getSelectedEventMark()))
                .findFirst().orElse(null);

        AvailableMarkSearchItemWrapper wrapper = new AvailableMarkSearchItemWrapper(availableEventMarks);
        wrapper.setCanConfigure(false);
        wrapper.setValue(selected);

        return wrapper;
    }

    @Override
    public AvailableMarkSearchItemWrapper create(SearchItemContext ctx) {
        AvailableMarkSearchItemWrapper wrapper = super.create(ctx);
        wrapper.setVisible(true);

        return wrapper;
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return SimulationResultProcessedObjectType.F_EVENT_MARK_REF.equivalent(ctx.getPath());
    }
}
