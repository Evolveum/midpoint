package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization.ObjectVisualization;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization.ObjectVisualizationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization.VisualizationFactory;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class ChangesPanel extends BasePanel<List<ObjectDeltaType>> {

    public enum ChangesView {

        SIMPLE,
        ADVANCED
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_TOGGLE = "toggle";
    private static final String ID_BODY = "body";
    private static final String ID_CHANGES = "changes";
    private static final String ID_CHANGE = "change";
    private static final String ID_ADVANCED = "advanced";

    private IModel<ChangesView> changesViewModel;

    private IModel<List<ObjectVisualization>> changesNewModel;

    private IModel<VisualizationDto> changesModel;

    public ChangesPanel(String id, IModel<List<ObjectDeltaType>> model) {
        super(id, model);

        initModels();
        initLayout();
    }

    private void initModels() {
        changesViewModel = Model.of(ChangesView.SIMPLE);

        changesModel = new LoadableDetachableModel<>() {

            @Override
            protected VisualizationDto load() {
                ObjectDeltaType objectDelta = getModelObject().get(0);  // todo improve
                return SimulationsGuiUtil.createDeltaVisualization(objectDelta, getPageBase());
            }
        };

        changesNewModel = new LoadableDetachableModel<>() {
            @Override
            protected List<ObjectVisualization> load() {
                ObjectDeltaType delta = getModelObject().get(0);  // todo improve
                if (delta == null) {
                    return Collections.emptyList();
                }

                try {
                    ObjectVisualization visualization = VisualizationFactory.createObjectVisualization(delta);

                    return Collections.singletonList(visualization);
                } catch (Exception ex) {
                    // todo handle exception
                    ex.printStackTrace();
                }

                return Collections.emptyList();
            }
        };
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card"));

        IModel<List<Toggle<ChangesView>>> toggleModel = new LoadableModel<>(false) {

            @Override
            protected List<Toggle<ChangesView>> load() {
                List<Toggle<ChangesView>> toggles = new ArrayList<>();

                Toggle<ChangesView> simple = new Toggle<>("fa-solid fa-magnifying-glass mr-1", "ChangesView.SIMPLE");
                simple.setValue(ChangesView.SIMPLE);
                simple.setActive(changesViewModel.getObject() == simple.getValue());
                toggles.add(simple);

                Toggle<ChangesView> advanced = new Toggle<>("fa-solid fa-microscope mr-1", "ChangesView.ADVANCED");
                advanced.setValue(ChangesView.ADVANCED);
                advanced.setActive(changesViewModel.getObject() == advanced.getValue());
                toggles.add(advanced);

                return toggles;
            }
        };

        TogglePanel<ChangesView> toggle = new TogglePanel<>(ID_TOGGLE, toggleModel) {

            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ChangesView>> item) {
                super.itemSelected(target, item);

                onChangesViewClicked(target, item.getObject());
            }
        };
        add(toggle);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.setOutputMarkupId(true);
        add(body);

        ListView<ObjectVisualization> changes = new ListView<>(ID_CHANGES, changesNewModel) {

            @Override
            protected void populateItem(ListItem<ObjectVisualization> item) {
                ObjectVisualizationPanel change = new ObjectVisualizationPanel(ID_CHANGE, item.getModel(), true);
                item.add(change);
            }
        };
        changes.add(new VisibleBehaviour(() -> changesViewModel.getObject() == ChangesView.SIMPLE));
        body.add(changes);

        VisualizationPanel advanced = new VisualizationPanel(ID_ADVANCED, changesModel);
        advanced.add(new VisibleBehaviour(() -> changesViewModel.getObject() == ChangesView.ADVANCED && changesModel.getObject() != null));
        body.add(advanced);
    }

    private void onChangesViewClicked(AjaxRequestTarget target, Toggle<ChangesView> toggle) {
        changesViewModel.setObject(toggle.getValue());

        target.add(get(ID_BODY));
    }
}
