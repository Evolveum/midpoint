/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.modal;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.custom.DropDownModalContentPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.model.SimulationChangeSummaryDto;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.SimulationChangeValuesPanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Dropdown panel showing full values for a single change category
 * (removed, added, or unchanged).
 */
public class SimulationMoreValuesModalPanel extends DropDownModalContentPanel {

    private static final String ID_VALUES = "values";
    private static final String ID_VALUE = "value";

    private static final String ID_COUNT_FRAGMENT = "countFragment";
    private static final String ID_COUNT = "count";

    private final IModel<SimulationChangeSummaryDto> summaryModel;
    private final IModel<SimulationChangeValuesPanel.ChangeListType> typeModel;

    public SimulationMoreValuesModalPanel(
            String id,
            IModel<SimulationChangeSummaryDto> summaryModel,
            IModel<SimulationChangeValuesPanel.ChangeListType> typeModel) {
        super(id);
        this.summaryModel = summaryModel;
        this.typeModel = typeModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer contentContainer = getContentContainer();
        contentContainer.add(createValuesList(getValues()));
    }

    @Override
    protected IModel<String> getLabelModel() {
        return createStringResource("SimulationChangeValuesPanel.more", getValueCount());
    }

    @Override
    protected Component createTitleComponent(String id) {
        Label label = new Label(id, createStringResource("SimulationMoreValuesPopupPanel." + typeModel.getObject().name()));
        label.add(AttributeModifier.append("class", "text-uppercase small font-weight-semibold"));
        return label;
    }

    @Override
    protected Component createFooterComponent(String id) {
        Fragment footer = new Fragment(id, ID_COUNT_FRAGMENT, this);
        footer.add(new Label(
                ID_COUNT,
                createStringResource("SimulationMoreValuesPopupPanel.count", getValueCount())));
        return footer;
    }

    @Override
    protected String getWidthCssStyle() {
        return "max-width: 30vw;";
    }

    @Override
    protected String getHeightCssStyle() {
        return "";
    }

    private ListView<String> createValuesList(List<String> values) {
        return new ListView<>(SimulationMoreValuesModalPanel.ID_VALUES, Model.ofList(values)) {
            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_VALUE, item.getModel()));
            }
        };
    }

    private int getValueCount() {
        return getValues().size();
    }

    private List<String> getValues() {
        SimulationChangeSummaryDto dto = summaryModel.getObject();

        return switch (typeModel.getObject()) {
            case REMOVED -> dto.getRemovedValues();
            case ADDED -> dto.getAddedValues();
            case UNCHANGED -> dto.getUnchangedValues();
        };
    }
}
