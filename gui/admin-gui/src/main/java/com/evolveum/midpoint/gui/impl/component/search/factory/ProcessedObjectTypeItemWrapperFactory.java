/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ProcessedObjectTypeSearchItemWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProcessedObjectTypeItemWrapperFactory extends AbstractSearchItemWrapperFactory<QName, ProcessedObjectTypeSearchItemWrapper> {

    @Override
    protected ProcessedObjectTypeSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        return new ProcessedObjectTypeSearchItemWrapper();
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return SimulationResultProcessedObjectType.F_TYPE.equivalent(ctx.getPath());
    }
}
