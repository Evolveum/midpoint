/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.computeCorrelationStrategyMethod;

public class CorrelationItemTypePanel extends BasePanel<List<CorrelationItemType>> implements Popupable {

    private static final String ID_CORRELATION_ITEMS_PANEL = "correlationItemsPanel";
    private static final String ID_CORRELATION_ITEMS_PANEL_VALUE = "correlationItemsPanelValue";
    private static final String ID_CORRELATION_ITEMS_PANEL_LABEL = "correlationItemsPanelLabel";

    Integer maxItems;
    List<CorrelationItemRecord> correlationItemTypes;

    private record CorrelationItemRecord(
            @Nullable String path, @Nullable String strategy, boolean isHover) implements Serializable {

        public String getPath() {
            return path;
        }

        public String getStrategy() {
            return strategy;
        }

        public boolean isHover() {
            return isHover;
        }
    }

    public CorrelationItemTypePanel(String id, IModel<List<CorrelationItemType>> model, Integer maxItems) {
        super(id, model);
        this.maxItems = maxItems;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        IModel<List<CorrelationItemRecord>> recordsModel =
                LoadableDetachableModel.of(
                        () -> buildCorrelationItemRecordList(getModelObject()));

        List<CorrelationItemType> modelObject = getModelObject();
        ListView<CorrelationItemRecord> correlationItemlistView =
                new ListView<>(ID_CORRELATION_ITEMS_PANEL, recordsModel) {
                    @Override
                    protected void populateItem(@NotNull ListItem<CorrelationItemRecord> listItem) {

                        CorrelationItemRecord correlationItemRecord = listItem.getModelObject();
                        Label stateValue = new Label(ID_CORRELATION_ITEMS_PANEL_VALUE, correlationItemRecord.getPath());
                        stateValue.setOutputMarkupId(true);

                        if (correlationItemRecord.isHover()) {
                            stateValue.add(new AjaxEventBehavior("click") {
                                @Override
                                protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                                    CorrelationItemTypePanel fullList = new CorrelationItemTypePanel(
                                            getPageBase().getMainPopupBodyId(),
                                            Model.ofList(modelObject),
                                            null);
                                    getPageBase().showMainPopup(fullList, ajaxRequestTarget);
                                }
                            });
                        }
                        listItem.add(stateValue);

                        Label stateLabel = new Label(ID_CORRELATION_ITEMS_PANEL_LABEL, correlationItemRecord.getStrategy());
                        stateLabel.setOutputMarkupId(true);
                        stateLabel.add(new VisibleBehaviour(() -> !correlationItemRecord.isHover()));
                        listItem.add(stateLabel);
                    }
                };
        correlationItemlistView.setOutputMarkupId(true);
        add(correlationItemlistView);
    }

    private List<CorrelationItemRecord> buildCorrelationItemRecordList(List<CorrelationItemType> items) {
        this.correlationItemTypes = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            return correlationItemTypes;
        }

        int count = 0;

        for (CorrelationItemType correlationItemType : items) {
            if (maxItems != null && count >= maxItems) {
                int missingCount = items.size() - count;
                correlationItemTypes.add(new CorrelationItemRecord("+" + missingCount, "", true));
                break;
            }

            String path = correlationItemType.getRef() != null
                    ? correlationItemType.getRef().toString()
                    : "(no path)";

            String strategy = computeCorrelationStrategyMethod(correlationItemType);

            correlationItemTypes.add(new CorrelationItemRecord(path, strategy, false));
            count++;
        }

        return correlationItemTypes;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public String getWidthUnit() {
        return "";
    }

    @Override
    public String getHeightUnit() {
        return "";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public String getCssClassForDialog() {
        return Popupable.super.getCssClassForDialog() + " modal-dialog-centered";
    }

    @Override
    public @NotNull Component getFooter() {
        Component footer = Popupable.super.getFooter();
        footer.setVisible(false); // Hide footer as it is not needed
        return footer;
    }
}
