/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProcessedObjectsProvider extends SelectableBeanContainerDataProvider<SimulationResultProcessedObjectType> {

    public static final String SORT_BY_NAME = "name";

    public ProcessedObjectsProvider(Component component, @NotNull IModel<Search<SimulationResultProcessedObjectType>> search) {
        super(component, search, null, false);
    }

    @Override
    protected ObjectQuery getCustomizeContentQuery() {
        String resultOid = getSimulationResultOid();

        S_FilterExit builder = getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                .id(resultOid);

        String tagOid = getTagOid();
        if (StringUtils.isNotEmpty(tagOid)) {
            builder = builder
                    .and()
                    .ref(SimulationResultProcessedObjectType.F_EVENT_MARK_REF, MarkType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT, tagOid).all();
        }

        return builder.build();
    }

    @NotNull
    protected String getSimulationResultOid() {
        return null;
    }

    protected String getTagOid() {
        return null;
    }
}
