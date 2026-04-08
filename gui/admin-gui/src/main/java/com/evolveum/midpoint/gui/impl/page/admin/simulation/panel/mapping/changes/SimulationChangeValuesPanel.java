/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.modal.SimulationChangeDetailsModalPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.modal.SimulationMoreValuesModalPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.model.SimulationChangeSummaryDto;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.model.SimulationChangeSummaryDto.ChangeViewType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimulationChangeValuesPanel extends BasePanel<SimulationChangeSummaryDto> {

    private static final String ID_EMPTY = "empty";
    private static final String ID_EMPTY_LABEL = "emptyLabel";

    private static final String ID_SIMPLE = "simple";
    private static final String ID_SIMPLE_OLD = "simpleOld";
    private static final String ID_SIMPLE_NEW = "simpleNew";

    private static final String ID_ADD_ONLY = "addOnly";
    private static final String ID_ADD_ONLY_OLD = "addOnlyOld";
    private static final String ID_ADD_ONLY_VALUES = "addOnlyValues";
    private static final String ID_ADD_ONLY_MORE = "addOnlyMore";

    private static final String ID_DELETE_ONLY = "deleteOnly";
    private static final String ID_DELETE_ONLY_VALUES = "deleteOnlyValues";
    private static final String ID_DELETE_ONLY_NEW = "deleteOnlyNew";
    private static final String ID_DELETE_ONLY_MORE = "deleteOnlyMore";

    private static final String ID_MULTI = "multi";
    private static final String ID_MULTI_REMOVED = "multiRemoved";
    private static final String ID_MULTI_REMOVED_VALUE = "multiRemovedValue";
    private static final String ID_MULTI_REMOVED_MORE = "multiRemovedMore";

    private static final String ID_MULTI_ADDED = "multiAdded";
    private static final String ID_MULTI_ADDED_VALUE = "multiAddedValue";
    private static final String ID_MULTI_ADDED_MORE = "multiAddedMore";

    private static final String ID_MULTI_UNCHANGED_COUNT = "multiUnchangedCount";
    private static final String ID_SEE_DETAILS = "seeDetails";

    private static final String ID_VALUE = "value";

    public enum ChangeListType {
        REMOVED, ADDED, UNCHANGED
    }

    /**
     * Panel responsible for rendering value changes in simulation results.
     *
     * <p>The panel displays differences between old and new values for a single
     * {@link SimulationChangeSummaryDto}. Depending on the detected change type,
     * it renders one of several views:</p>
     *
     * <ul>
     *     <li><b>EMPTY</b> – no value change.</li>
     *     <li><b>SIMPLE</b> – single value replaced (old → new).</li>
     *     <li><b>ADD_ONLY</b> – values were only added.</li>
     *     <li><b>DELETE_ONLY</b> – values were only removed.</li>
     *     <li><b>MULTI_VALUE</b> – combination of added, removed, and unchanged values.</li>
     * </ul>
     *
     * <p>For multi-value changes the panel shows a preview of values and provides
     * links to open modal panels with the full list of changes.</p>
     */
    public SimulationChangeValuesPanel(String id, IModel<SimulationChangeSummaryDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        SimulationChangeSummaryDto dto = getModelObject();
        ChangeViewType viewType = dto.getViewType();

        add(createEmptyContainer(viewType));
        add(createSimpleContainer(dto, viewType));
        add(createAddOnlyContainer(dto, viewType));
        add(createDeleteOnlyContainer(dto, viewType));
        add(createMultiContainer(dto, viewType));
    }

    private @NotNull WebMarkupContainer createEmptyContainer(ChangeViewType viewType) {
        WebMarkupContainer empty = new WebMarkupContainer(ID_EMPTY);
        empty.setVisible(viewType == ChangeViewType.EMPTY);
        empty.add(new Label(ID_EMPTY_LABEL, createStringResource("SimulationChangeValuesPanel.empty")));
        return empty;
    }

    private @NotNull WebMarkupContainer createSimpleContainer(@NotNull SimulationChangeSummaryDto dto, ChangeViewType viewType) {
        WebMarkupContainer simple = new WebMarkupContainer(ID_SIMPLE);
        simple.setVisible(viewType == ChangeViewType.SIMPLE);

        simple.add(new Label(ID_SIMPLE_OLD, Model.of(displayEmpty(dto.getOldValue()))));

        Label newValue = new Label(ID_SIMPLE_NEW, Model.of(displayEmpty(dto.getNewValue())));
        newValue.add(AttributeModifier.replace("class", resolveValueCss(dto.getNewValue())));
        simple.add(newValue);

        return simple;
    }

    private @NotNull WebMarkupContainer createAddOnlyContainer(@NotNull SimulationChangeSummaryDto dto, ChangeViewType viewType) {
        WebMarkupContainer container = new WebMarkupContainer(ID_ADD_ONLY);
        container.setVisible(viewType == ChangeViewType.ADD_ONLY);

        container.add(new Label(ID_ADD_ONLY_OLD, getString("SimulationChangeValuesPanel.emptyValue")));

        container.add(createValuesList(ID_ADD_ONLY_VALUES, dto.getTopAddedValues()));

        container.add(createMorePanel(
                ID_ADD_ONLY_MORE,
                dto,
                ChangeListType.ADDED,
                dto.getMoreAddedCount()
        ));

        return container;
    }

    private @NotNull WebMarkupContainer createDeleteOnlyContainer(@NotNull SimulationChangeSummaryDto dto, ChangeViewType viewType) {
        WebMarkupContainer container = new WebMarkupContainer(ID_DELETE_ONLY);
        container.setVisible(viewType == ChangeViewType.DELETE_ONLY);

        container.add(createValuesList(ID_DELETE_ONLY_VALUES, dto.getTopRemovedValues()));

        container.add(new Label(ID_DELETE_ONLY_NEW, getString("SimulationChangeValuesPanel.emptyValue")));

        container.add(createMorePanel(
                ID_DELETE_ONLY_MORE,
                dto,
                ChangeListType.REMOVED,
                dto.getMoreRemovedCount()
        ));

        return container;
    }

    private @NotNull WebMarkupContainer createMultiContainer(@NotNull SimulationChangeSummaryDto dto, ChangeViewType viewType) {
        WebMarkupContainer multi = new WebMarkupContainer(ID_MULTI);
        multi.setVisible(viewType == ChangeViewType.MULTI_VALUE);

        multi.add(createValuesList(ID_MULTI_REMOVED, dto.getTopRemovedValues(), ID_MULTI_REMOVED_VALUE));
        multi.add(createMorePanel(ID_MULTI_REMOVED_MORE, dto, ChangeListType.REMOVED, dto.getMoreRemovedCount()));

        multi.add(createValuesList(ID_MULTI_ADDED, dto.getTopAddedValues(), ID_MULTI_ADDED_VALUE));
        multi.add(createMorePanel(ID_MULTI_ADDED_MORE, dto, ChangeListType.ADDED, dto.getMoreAddedCount()));

        multi.add(new Label(
                ID_MULTI_UNCHANGED_COUNT,
                createStringResource("SimulationChangeValuesPanel.unmodifiedCount", dto.getUnchangedCount())
        ));

        SimulationChangeDetailsModalPanel seeDetails =
                new SimulationChangeDetailsModalPanel(ID_SEE_DETAILS, Model.of(dto));

        seeDetails.setVisible(true);
        multi.add(seeDetails);

        return multi;
    }

    private @NotNull ListView<String> createValuesList(String id, List<String> values) {
        return createValuesList(id, values, ID_VALUE);
    }

    private @NotNull ListView<String> createValuesList(String id, List<String> values, String valueId) {
        return new ListView<>(id, Model.ofList(values)) {
            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(valueId, item.getModel()));
            }
        };
    }

    private @NotNull SimulationMoreValuesModalPanel createMorePanel(
            String id,
            SimulationChangeSummaryDto dto,
            ChangeListType type,
            int moreCount) {

        SimulationMoreValuesModalPanel panel =
                new SimulationMoreValuesModalPanel(id, Model.of(dto), Model.of(type)) {
                    @Override
                    protected IModel<String> getLabelModel() {
                        return createStringResource("SimulationChangeValuesPanel.more", moreCount);
                    }
                };

        panel.setVisible(moreCount > 0);
        return panel;
    }

    private @NotNull String resolveValueCss(String value) {
        return value == null ? "simulation-empty-value" : "simulation-value-pill";
    }

    private @NotNull String displayEmpty(String value) {
        return value != null ? value : getString("SimulationChangeValuesPanel.emptyValue");
    }
}
