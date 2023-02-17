/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimpleVisualizationPanel extends BasePanel<VisualizationDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS_TABLE = "itemsTable";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_PARTIAL_VISUALIZATIONS = "partialVisualizations";
    private static final String ID_PARTIAL_VISUALIZATION = "partialVisualization";
    private static final String ID_SHOW_OPERATIONAL_ITEMS_LINK = "showOperationalItemsLink";
    private static final String ID_OLD_VALUE_LABEL = "oldValueLabel";
    private static final String ID_NEW_VALUE_LABEL = "newValueLabel";
    private static final String ID_VALUE_LABEL = "valueLabel";
    private static final String ID_SORT_PROPERTIES = "sortProperties";

    private final boolean showOperationalItems;
    private boolean operationalItemsVisible;

    public SimpleVisualizationPanel(String id, @NotNull IModel<VisualizationDto> model) {
        this(id, model, false);
    }

    public SimpleVisualizationPanel(String id, @NotNull IModel<VisualizationDto> model, boolean showOperationalItems) {
        super(id, model);

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

        WebMarkupContainer itemsTable = new WebMarkupContainer(ID_ITEMS_TABLE);
        itemsTable.add(AttributeAppender.append("class", () -> !getModelObject().getPartialVisualizations().isEmpty() ? "mb-2" : null));
        itemsTable.add(new VisibleBehaviour(() -> !model.getObject().getItems().isEmpty()));
        itemsTable.setOutputMarkupId(true);

        ToggleIconButton<String> sortPropertiesButton = new ToggleIconButton<>(ID_SORT_PROPERTIES,
                GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSortClicked(model, target);
            }

            @Override
            public boolean isOn() {
                return model.getObject().isSorted();
            }
        };
        sortPropertiesButton.setOutputMarkupId(true);
        sortPropertiesButton.setOutputMarkupPlaceholderTag(true);
        itemsTable.add(sortPropertiesButton);

        WebMarkupContainer oldValueLabel = new WebMarkupContainer(ID_OLD_VALUE_LABEL);
        oldValueLabel.add(new VisibleBehaviour(() -> model.getObject().containsDeltaItems()));
        itemsTable.add(oldValueLabel);

        WebMarkupContainer newValueLabel = new WebMarkupContainer(ID_NEW_VALUE_LABEL);
        newValueLabel.add(new VisibleBehaviour(() -> model.getObject().containsDeltaItems()));
        itemsTable.add(newValueLabel);

        WebMarkupContainer valueLabel = new WebMarkupContainer(ID_VALUE_LABEL);
        valueLabel.add(new VisibleBehaviour(() -> !model.getObject().containsDeltaItems()));
        itemsTable.add(valueLabel);

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
        itemsTable.add(items);
        add(itemsTable);

        ListView<VisualizationDto> partialVisualizations = new ListView<>(ID_PARTIAL_VISUALIZATIONS,
                () -> getModelObject().getPartialVisualizations()) {

            @Override
            protected void populateItem(ListItem<VisualizationDto> item) {
                VisualizationPanel panel = new VisualizationPanel(ID_PARTIAL_VISUALIZATION, item.getModel()) {
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

        AjaxButton showOperationalItemsLink = new AjaxButton(ID_SHOW_OPERATIONAL_ITEMS_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setOperationalItemsVisible(!operationalItemsVisible);
                target.add(SimpleVisualizationPanel.this);
            }

            @Override
            public IModel<?> getBody() {
                return getShowOperationalItemsLinkLabel();
            }
        };
        showOperationalItemsLink.setOutputMarkupId(true);
        showOperationalItemsLink.add(AttributeAppender.append("style", "cursor: pointer;"));
        showOperationalItemsLink.add(new VisibleBehaviour(() -> showOperationalItems));
        add(showOperationalItemsLink);
    }

    private void onSortClicked(IModel<VisualizationDto> model, AjaxRequestTarget target) {
        model.getObject().setSorted(!model.getObject().isSorted());
        target.add(get(getPageBase().createComponentPath(ID_ITEMS_TABLE)));
        target.add(get(getPageBase().createComponentPath(ID_ITEMS_TABLE, ID_SORT_PROPERTIES)));
    }

    private void setOperationalItemsVisible(boolean operationalItemsVisible) {
        this.operationalItemsVisible = operationalItemsVisible;
    }

    protected boolean isOperationalItemsVisible() {
        return operationalItemsVisible;
    }

    private IModel<?> getShowOperationalItemsLinkLabel() {
        return operationalItemsVisible ? PageBase.createStringResourceStatic("ScenePanel.hideOperationalItemsLink")
                : PageBase.createStringResourceStatic("ScenePanel.showOperationalItemsLink");
    }

    private boolean isOperationalPartialVisualization(IModel<VisualizationDto> visualizationDtoModel) {
        if (visualizationDtoModel == null || visualizationDtoModel.getObject() == null) {
            return false;
        }
        return visualizationDtoModel.getObject().getVisualization().isOperational();
    }

    private boolean isOperationalItem(IModel<VisualizationItemDto> visualizationDtoModel) {
        if (visualizationDtoModel == null || visualizationDtoModel.getObject() == null) {
            return false;
        }
        return visualizationDtoModel.getObject().isOperational();
    }
}
