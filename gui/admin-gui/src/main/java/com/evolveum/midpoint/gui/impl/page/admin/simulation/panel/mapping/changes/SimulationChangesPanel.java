/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes;

import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.model.SimulationChangeSummaryDto;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Panel displaying a list of summarized mapping simulation changes.
 */
public class SimulationChangesPanel extends BasePanel<List<SimulationChangeSummaryDto>> {

    private static final String ID_EMPTY = "empty";
    private static final String ID_ROWS = "rows";
    private static final String ID_ROW = "row";

    public SimulationChangesPanel(String id, IModel<List<SimulationChangeSummaryDto>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        List<SimulationChangeSummaryDto> changes = getModelObject();
        boolean hasChanges = changes != null && !changes.isEmpty();

        Label emptyLabel = new Label(ID_EMPTY, createStringResource("SimulationChangesPanel.noChanges"));
        emptyLabel.setVisible(!hasChanges);
        emptyLabel.setOutputMarkupPlaceholderTag(true);
        add(emptyLabel);

        ListView<SimulationChangeSummaryDto> rows = new ListView<>(ID_ROWS, getModel()) {
            @Override
            protected void populateItem(@NotNull ListItem<SimulationChangeSummaryDto> item) {
                item.add(new SimulationChangeValuesPanel(ID_ROW, item.getModel()));
            }
        };
        rows.setVisible(hasChanges);
        rows.setOutputMarkupPlaceholderTag(true);
        add(rows);
    }
}
