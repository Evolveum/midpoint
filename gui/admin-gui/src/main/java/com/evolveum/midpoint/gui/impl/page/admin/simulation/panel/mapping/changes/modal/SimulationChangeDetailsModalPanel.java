package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.modal;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.custom.DropDownModalContentPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.model.SimulationChangeSummaryDto;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Dropdown panel showing full removed, added, and unchanged values
 * for a multi-value simulation change.
 */
public class SimulationChangeDetailsModalPanel extends DropDownModalContentPanel {

    private static final String ID_HEADER_FRAGMENT = "titleFragment";

    private static final String ID_REMOVED_VALUES = "removedValues";
    private static final String ID_ADDED_VALUES = "addedValues";
    private static final String ID_UNCHANGED_VALUES = "unchangedValues";
    private static final String ID_VALUE = "value";

    private static final String ID_COUNT_FRAGMENT = "countFragment";
    private static final String ID_COUNT = "count";

    private final IModel<SimulationChangeSummaryDto> summaryModel;

    public SimulationChangeDetailsModalPanel(String id, IModel<SimulationChangeSummaryDto> summaryModel) {
        super(id);
        this.summaryModel = summaryModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        SimulationChangeSummaryDto dto = summaryModel.getObject();
        WebMarkupContainer contentContainer = getContentContainer();

        contentContainer.add(createValuesList(ID_REMOVED_VALUES, dto.getRemovedValues()));
        contentContainer.add(createValuesList(ID_ADDED_VALUES, dto.getAddedValues()));
        contentContainer.add(createValuesList(ID_UNCHANGED_VALUES, dto.getUnchangedValues()));
    }

    @Override
    protected IModel<String> getLabelModel() {
        return createStringResource("SimulationChangeValuesPanel.seeDetails");
    }

    @Override
    protected Component createTitleComponent(String id) {
        return new Fragment(id, ID_HEADER_FRAGMENT, this);
    }

    @Override
    protected Component createFooterComponent(String id) {
        SimulationChangeSummaryDto dto = summaryModel.getObject();

        Fragment footer = new Fragment(id, ID_COUNT_FRAGMENT, this);
        footer.add(new Label(
                ID_COUNT,
                createStringResource("SimulationChangeDetailsPopupPanel.count", getTotalValueCount(dto))));
        return footer;
    }

    @Override
    protected String getWidthCssStyle() {
        return "min-width: 50vw;";
    }

    @Override
    protected String getHeightCssStyle() {
        return "";
    }

    private ListView<String> createValuesList(String id, List<String> values) {
        return new ListView<>(id, Model.ofList(values)) {
            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_VALUE, item.getModel()));
            }
        };
    }

    private int getTotalValueCount(SimulationChangeSummaryDto dto) {
        return dto.getRemovedValues().size()
                + dto.getAddedValues().size()
                + dto.getUnchangedValues().size();
    }
}
