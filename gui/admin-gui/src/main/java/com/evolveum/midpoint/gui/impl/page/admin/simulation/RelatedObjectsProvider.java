package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

public class RelatedObjectsProvider extends SelectableBeanContainerDataProvider<SimulationResultProcessedObjectType> {

    public static final String SORT_BY_NAME = "name";

    public RelatedObjectsProvider(Component component, @NotNull IModel<Search<SimulationResultProcessedObjectType>> searchModel) {
        super(component, searchModel, null, true);
    }

    @Override
    protected ObjectQuery getCustomizeContentQuery() {
        String resultOid = getSimulationResultOid();
        Long objectId = getProcessedObjectId();

        return getPrismContext().queryFor(SimulationResultProcessedObjectType.class)
                .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                .id(resultOid)
                .and()
                .item(SimulationResultProcessedObjectType.F_FOCUS_RECORD_ID).eq(objectId)
                .build();
    }

    @NotNull
    protected String getSimulationResultOid() {
        return null;
    }

    @NotNull
    protected Long getProcessedObjectId() {
        return null;
    }
}
