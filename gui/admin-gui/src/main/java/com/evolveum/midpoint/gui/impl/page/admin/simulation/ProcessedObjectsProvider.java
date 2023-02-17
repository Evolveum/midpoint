/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProcessedObjectsProvider extends SelectableBeanContainerDataProvider<SimulationResultProcessedObjectType> {

    public static final String SORT_BY_NAME = "name";

    public ProcessedObjectsProvider(Component component, @NotNull IModel<Search<SimulationResultProcessedObjectType>> search) {
        super(component, search, null, true);
    }

    @Override
    protected ObjectQuery getCustomizeContentQuery() {
        String resultOid = getSimulationResultOid();

        return getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                .id(resultOid).build();
    }

    @NotNull
    protected String getSimulationResultOid() {
        return null;
    }
}
