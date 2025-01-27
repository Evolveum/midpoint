/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.*;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MembersDetailsPopupPanel extends BasePanel<String> implements Popupable {

    List<String> elements;
    RoleAnalysisProcessModeType processModeType;
    Map<String, RoleAnalysisAttributeStatistics> map;

    public MembersDetailsPopupPanel(String id, IModel<String> messageModel, List<String> members,
            RoleAnalysisProcessModeType processModeType) {
        super(id, messageModel);
        this.elements = members == null ? new ArrayList<>() : members;
        this.processModeType = processModeType;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();

    }

    private void initLayout() {
        Class<?> roleTypeClass = processModeType.equals(RoleAnalysisProcessModeType.ROLE) ? RoleType.class : UserType.class;

        MainObjectListPanel<FocusType> table = new MainObjectListPanel<>("table", FocusType.class, null) {

            @Override
            protected IColumn<SelectableBean<FocusType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected boolean showTableAsCard() {
                return MembersDetailsPopupPanel.this.showTableAsCard();
            }

            @Override
            protected List<IColumn<SelectableBean<FocusType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<FocusType>, String>> defaultColumns = super.createDefaultColumns();

                if (map == null) {
                    return defaultColumns;
                }

                defaultColumns.add(new AbstractColumn<>(createStringResource("MembersDetailsPopupPanel.inGroup")) {
                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<FocusType>>> cellItem, String componentId,
                            IModel<SelectableBean<FocusType>> rowModel) {
                        String object = rowModel.getObject().getValue().getOid();
                        RoleAnalysisAttributeStatistics roleAnalysisAttributeStatistics = map.get(object);
                        Integer inGroup;
                        if (roleAnalysisAttributeStatistics == null) {
                            inGroup = 0;
                        } else {
                            inGroup = roleAnalysisAttributeStatistics.getInGroup();
                            if (inGroup == null) {
                                inGroup = 0;
                            }
                        }
                        cellItem.add(new Label(componentId, inGroup));
                    }
                });

                defaultColumns.add(new AbstractColumn<>(createStringResource("MembersDetailsPopupPanel.inRepo")) {
                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<FocusType>>> cellItem, String componentId,
                            IModel<SelectableBean<FocusType>> rowModel) {
                        String object = rowModel.getObject().getValue().getOid();
                        RoleAnalysisAttributeStatistics roleAnalysisAttributeStatistics = map.get(object);
                        Integer inRepo;
                        if (roleAnalysisAttributeStatistics == null) {
                            inRepo = 0;
                        } else {
                            inRepo = roleAnalysisAttributeStatistics.getInRepo();
                            if (inRepo == null) {
                                inRepo = 0;
                            }
                        }
                        cellItem.add(new Label(componentId, inRepo));
                    }
                });

                return defaultColumns;
            }

            @Override
            protected Class<FocusType> getDefaultType() {
                return (Class<FocusType>) roleTypeClass;
            }

            @Override
            protected boolean isHeaderVisible() {
                return true;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return List.of();
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<FocusType>> createProvider() {
                SelectableBeanObjectDataProvider<FocusType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(elements), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(ObjectType.F_NAME.getLocalPart(), SortOrder.ASCENDING);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected boolean isDuplicationSupported() {
                return false;
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return null;
    }

    public Map<String, RoleAnalysisAttributeStatistics> getMap() {
        return map;
    }

    public void setMap(@Nullable Map<String, RoleAnalysisAttributeStatistics> map) {
        this.map = map;
    }

    protected boolean showTableAsCard() {
        return true;
    }

    protected ObjectQuery getCustomizeContentQuery(@NotNull List<String> elements) {
        return PrismContext.get().queryFor(FocusType.class)
                .id(elements.toArray(new String[0]))
                .build();
    }
}
