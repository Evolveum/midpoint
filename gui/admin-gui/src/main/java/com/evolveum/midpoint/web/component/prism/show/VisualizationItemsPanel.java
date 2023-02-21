/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class VisualizationItemsPanel extends BasePanel<VisualizationDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_OLD_VALUE_LABEL = "oldValueLabel";
    private static final String ID_NEW_VALUE_LABEL = "newValueLabel";
    private static final String ID_VALUE_LABEL = "valueLabel";
    private static final String ID_SORT_PROPERTIES = "sortProperties";

    private boolean operationalItemsVisible;

    public VisualizationItemsPanel(String id, IModel<VisualizationDto> model) {
        super(id, model);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "table");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "table table-striped table-hover table-bordered table-word-break"));
        setOutputMarkupId(true);

        ToggleIconButton<String> sortPropertiesButton = new ToggleIconButton<>(ID_SORT_PROPERTIES,
                GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSortClicked(target, VisualizationItemsPanel.this.getModelObject());
            }

            @Override
            public boolean isOn() {
                return VisualizationItemsPanel.this.getModelObject().isSorted();
            }
        };
        sortPropertiesButton.setOutputMarkupId(true);
        sortPropertiesButton.setOutputMarkupPlaceholderTag(true);
        add(sortPropertiesButton);

        WebMarkupContainer oldValueLabel = new WebMarkupContainer(ID_OLD_VALUE_LABEL);
        oldValueLabel.add(new VisibleBehaviour(() -> VisualizationItemsPanel.this.getModelObject().containsDeltaItems()));
        add(oldValueLabel);

        WebMarkupContainer newValueLabel = new WebMarkupContainer(ID_NEW_VALUE_LABEL);
        newValueLabel.add(new VisibleBehaviour(() -> VisualizationItemsPanel.this.getModelObject().containsDeltaItems()));
        add(newValueLabel);

        WebMarkupContainer valueLabel = new WebMarkupContainer(ID_VALUE_LABEL);
        valueLabel.add(new VisibleBehaviour(() -> !VisualizationItemsPanel.this.getModelObject().containsDeltaItems()));
        add(valueLabel);

        ListView<VisualizationItemDto> items = new ListView<>(ID_ITEMS, () -> getModelObject().getItems()) {

            @Override
            protected void populateItem(ListItem<VisualizationItemDto> item) {
                VisualizationItemPanel panel = new VisualizationItemPanel(ID_ITEM, item.getModel());
                panel.add(new VisibleBehaviour(() -> !isOperationalItem(item.getModel()) || isOperationalItemsVisible()));
                panel.setRenderBodyOnly(true);
                item.add(panel);
            }
        };
        items.setReuseItems(true);
        add(items);
    }

    private boolean isOperationalItem(IModel<VisualizationItemDto> visualizationDtoModel) {
        if (visualizationDtoModel == null || visualizationDtoModel.getObject() == null) {
            return false;
        }
        return visualizationDtoModel.getObject().isOperational();
    }

    protected void onSortClicked(AjaxRequestTarget target, VisualizationDto visualization) {
        visualization.setSorted(!visualization.isSorted());

        target.add(this);
    }

    protected boolean isOperationalItemsVisible() {
        return operationalItemsVisible;
    }
}
