package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization.ObjectVisualization;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization.ObjectVisualizationPanel;

public class ChangesPanel extends BasePanel<List<ObjectVisualization>> {

    public enum ChangesView {

        SIMPLE,
        ADVANCED
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_TOGGLE = "toggle";
    private static final String ID_CHANGES = "changes";
    private static final String ID_CHANGE = "change";

    public ChangesPanel(String id, IModel<List<ObjectVisualization>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card"));

        IModel<List<Toggle<ChangesView>>> toggleModel = new LoadableModel<>(false) {

            @Override
            protected List<Toggle<ChangesView>> load() {
                List<Toggle<ChangesView>> toggles = new ArrayList<>();

                Toggle<ChangesView> advanced = new Toggle<>("fa-solid fa-microscope mr-1", "ChangesView.ADVANCED");
                advanced.setValue(ChangesView.ADVANCED);
                toggles.add(advanced);

                Toggle<ChangesView> simple = new Toggle<>("fa-solid fa-magnifying-glass mr-1", "ChangesView.SIMPLE");
                simple.setValue(ChangesView.SIMPLE);
                toggles.add(simple);

                return toggles;
            }
        };

        TogglePanel toggle = new TogglePanel(ID_TOGGLE, toggleModel);
        add(toggle);

        ListView<ObjectVisualization> changes = new ListView<>(ID_CHANGES, getModel()) {

            @Override
            protected void populateItem(ListItem<ObjectVisualization> item) {
                ObjectVisualizationPanel change = new ObjectVisualizationPanel(ID_CHANGE, item.getModel(), true);
                item.add(change);
            }
        };
        add(changes);
    }
}
