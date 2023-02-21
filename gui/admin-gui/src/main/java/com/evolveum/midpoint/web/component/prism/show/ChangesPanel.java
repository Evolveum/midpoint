/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class ChangesPanel extends BasePanel<Void> {

    public enum ChangesView {

        SIMPLE,
        ADVANCED
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_TOGGLE = "toggle";
    private static final String ID_BODY = "body";
    private static final String ID_VISUALIZATION = "visualization";

    private IModel<ChangesView> changesViewModel;

    private IModel<VisualizationDto> changesModel;

    public ChangesPanel(String id, IModel<List<ObjectDeltaType>> deltaModel, IModel<VisualizationDto> visualizationModel) {
        super(id);

        initModels(deltaModel, visualizationModel);
        initLayout();
    }

    private void initModels(IModel<List<ObjectDeltaType>> deltaModel, IModel<VisualizationDto> visualizationModel) {
        changesViewModel = Model.of(ChangesView.SIMPLE);

        changesModel = visualizationModel != null ? visualizationModel : new LoadableModel<>(false) {

            @Override
            protected VisualizationDto load() {
                ObjectDeltaType objectDelta = deltaModel.getObject().get(0);
                Visualization visualization = SimulationsGuiUtil.createVisualization(objectDelta, getPageBase());

                return new VisualizationDto(visualization);
            }
        };
    }

    protected IModel<String> createTitle() {
        return createStringResource("ChangesPanel.title");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card"));

        Label title = new Label(ID_TITLE, createTitle());
        add(title);

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

        Component visualization = new MainVisualizationPanel(ID_VISUALIZATION, changesModel, false, false);
        body.add(visualization);
    }

    private void onChangesViewClicked(AjaxRequestTarget target, Toggle<ChangesView> toggle) {
        changesViewModel.setObject(toggle.getValue());

        Component newOne = null;
        switch (changesViewModel.getObject()) {
            case SIMPLE:
                newOne = new MainVisualizationPanel(ID_VISUALIZATION, changesModel, false, false);
                break;
            case ADVANCED:
                expandVisualization(changesModel.getObject());
                newOne = new VisualizationPanel(ID_VISUALIZATION, changesModel, false, true);
        }

        Component existing = get(createComponentPath(ID_BODY, ID_VISUALIZATION));
        existing.replaceWith(newOne);

        target.add(get(ID_BODY));
    }

    private void expandVisualization(VisualizationDto dto) {
        dto.setMinimized(false);

        List<VisualizationDto> partials = dto.getPartialVisualizations();
        if (partials == null) {
            return;
        }

        for (VisualizationDto partial : partials) {
            expandVisualization(partial);
        }
    }
}
