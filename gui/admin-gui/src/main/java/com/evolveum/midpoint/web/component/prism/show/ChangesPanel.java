/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class ChangesPanel extends BasePanel<List<VisualizationDto>> {

    public enum ChangesView {

        SIMPLE,
        ADVANCED
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_TOGGLE = "toggle";
    private static final String ID_BODY = "body";
    private static final String ID_NO_CHANGES = "noChanges";
    private static final String ID_VISUALIZATIONS = "visualizations";
    private static final String ID_VISUALIZATION = "visualization";

    private IModel<ChangesView> changesView = Model.of(ChangesView.SIMPLE);

    private VisibleEnableBehaviour changesViewVisible = VisibleBehaviour.ALWAYS_VISIBLE_ENABLED;

    private boolean showOperationalItems;

    public ChangesPanel(String id, IModel<List<VisualizationDto>> model) {
        super(id, model);

        initLayout();
    }

    protected WebMarkupContainer getBody() {
        return (WebMarkupContainer) get(ID_BODY);
    }

    public void setShowOperationalItems(boolean showOperationalItems) {
        this.showOperationalItems = showOperationalItems;
    }

    public void setChangesView(@NotNull IModel<ChangesView> changesView) {
        this.changesView = changesView;
    }

    public void setChangesViewVisible(@NotNull VisibleEnableBehaviour changesViewVisible) {
        this.changesViewVisible = changesViewVisible;
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

                Toggle<ChangesView> simple = new Toggle<>("fa-solid fa-magnifying-glass mr-1",
                        LocalizationUtil.createKeyForEnum(ChangesView.SIMPLE));
                simple.setValue(ChangesView.SIMPLE);
                simple.setActive(changesView.getObject() == simple.getValue());
                toggles.add(simple);

                Toggle<ChangesView> advanced = new Toggle<>("fa-solid fa-microscope mr-1",
                        LocalizationUtil.createKeyForEnum(ChangesView.ADVANCED));
                advanced.setValue(ChangesView.ADVANCED);
                advanced.setActive(changesView.getObject() == advanced.getValue());
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
        toggle.add(AttributeAppender.append("class","btn-group-sm"));
        toggle.add(changesViewVisible);
        add(toggle);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.setOutputMarkupId(true);
        add(body);

        WebMarkupContainer noChanges = new WebMarkupContainer(ID_NO_CHANGES);
        noChanges.add(new VisibleBehaviour(() -> getModelObject().isEmpty()));
        body.add(noChanges);

        Component visualizations = createVisualizations();
        body.add(visualizations);
    }

    private ListView<VisualizationDto> createVisualizations() {
        return new ListView<>(ID_VISUALIZATIONS, getModel()) {

            @Override
            protected void populateItem(ListItem<VisualizationDto> item) {
                IModel<VisualizationDto> model = item.getModel();

                boolean advanced = changesView.getObject() == ChangesView.ADVANCED;

                if (advanced) {
                    expandVisualization(model.getObject());
                }

                VisualizationPanel visualization = new VisualizationPanel(ID_VISUALIZATION, model, showOperationalItems, advanced);
                item.add(visualization);
            }
        };
    }

    private void onChangesViewClicked(AjaxRequestTarget target, Toggle<ChangesView> toggle) {
        changesView.setObject(toggle.getValue());

        Component newOne = createVisualizations();
        Component existing = get(createComponentPath(ID_BODY, ID_VISUALIZATIONS));
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
