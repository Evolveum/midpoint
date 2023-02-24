/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimpleVisualizationPanel extends BasePanel<VisualizationDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS_TABLE = "itemsTable";
    private static final String ID_PARTIAL_VISUALIZATIONS = "partialVisualizations";
    private static final String ID_PARTIAL_VISUALIZATION = "partialVisualization";
    private static final String ID_SHOW_OPERATIONAL_ITEMS_LINK = "showOperationalItemsLink";

    private final boolean advanced;
    private final boolean showOperationalItems;
    private boolean operationalItemsVisible;

    public SimpleVisualizationPanel(String id, @NotNull IModel<VisualizationDto> model) {
        this(id, model, false, true);
    }

    public SimpleVisualizationPanel(String id, @NotNull IModel<VisualizationDto> model, boolean showOperationalItems, boolean advanced) {
        super(id, model);

        this.advanced = advanced;
        this.showOperationalItems = showOperationalItems;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        initLayout();
    }

    private void initLayout() {
        final IModel<VisualizationDto> model = getModel();

        VisualizationItemsPanel itemsTable = new VisualizationItemsPanel(ID_ITEMS_TABLE, getModel()) {

            @Override
            protected boolean isOperationalItemsVisible() {
                return operationalItemsVisible;
            }
        };
        itemsTable.add(new VisibleBehaviour(() -> !model.getObject().getItems().isEmpty()));
        itemsTable.setOutputMarkupId(true);
        add(itemsTable);

        ListView<VisualizationDto> partialVisualizations = new ListView<>(ID_PARTIAL_VISUALIZATIONS,
                () -> getModelObject().getPartialVisualizations()) {

            @Override
            protected void populateItem(ListItem<VisualizationDto> item) {
                VisualizationPanel panel = new VisualizationPanel(ID_PARTIAL_VISUALIZATION, item.getModel(), showOperationalItems, advanced) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isOperationalItemsVisible() {
                        VisualizationPanel parent = findParent(VisualizationPanel.class);
                        if (parent != null) {
                            return parent.isOperationalItemsVisible();
                        } else {
                            return SimpleVisualizationPanel.this.operationalItemsVisible;
                        }
                    }
                };
                panel.add(new VisibleBehaviour(() -> !isOperationalPartialVisualization(item.getModel()) || operationalItemsVisible));
                panel.setOutputMarkupPlaceholderTag(true);
                item.add(panel);
            }
        };
        partialVisualizations.setReuseItems(true);
        add(partialVisualizations);

        AjaxButton showOperationalItemsLink = new AjaxButton(ID_SHOW_OPERATIONAL_ITEMS_LINK, getShowOperationalItemsLinkLabel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setOperationalItemsVisible(!operationalItemsVisible);
                target.add(SimpleVisualizationPanel.this);
            }
        };
        showOperationalItemsLink.setOutputMarkupId(true);
        showOperationalItemsLink.add(new VisibleBehaviour(() -> showOperationalItems && getModelObject().hasOperationalItems()));
        add(showOperationalItemsLink);
    }

    private void setOperationalItemsVisible(boolean operationalItemsVisible) {
        this.operationalItemsVisible = operationalItemsVisible;
    }

    private IModel<String> getShowOperationalItemsLinkLabel() {
        return () -> operationalItemsVisible ? getString("ScenePanel.hideOperationalItemsLink") : getString("ScenePanel.showOperationalItemsLink");
    }

    private boolean isOperationalPartialVisualization(IModel<VisualizationDto> visualizationDtoModel) {
        if (visualizationDtoModel == null || visualizationDtoModel.getObject() == null) {
            return false;
        }
        return visualizationDtoModel.getObject().getVisualization().isOperational();
    }
}
