/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import com.evolveum.midpoint.web.component.data.BoxedTablePanel;

import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsOperationEntryType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class StatisticsPanel extends BasePanel<StatisticsDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_CONTENTS_PANEL = "contents";

    private static final String ID_PROVISIONING_TITLE = "provisioningTitle";
    private static final String ID_PROVISIONING_STATISTICS_LINES = "provisioningStatisticsLines";
    private static final String ID_PROVISIONING_RESOURCE = "Provisioning.Resource";
    private static final String ID_PROVISIONING_OBJECT_CLASS = "Provisioning.ObjectClass";


    private static final String ID_MAPPINGS_TITLE = "mappingsTitle";
    private static final String ID_MAPPINGS_STATISTICS_LINES = "mappingsStatisticsLines";

    private static final String ID_NOTIFICATIONS_TITLE = "notificationsTitle";
    private static final String ID_NOTIFICATIONS_STATISTICS_LINES = "notificationsStatisticsLines";

    private static final String ID_LAST_MESSAGE = "lastMessage";


    private static final String ID_PROVISIONING_OPERATIONS = "provisioningOperations";

    private WebMarkupContainer contentsPanel;

    public StatisticsPanel(String id, IModel<StatisticsDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        contentsPanel = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        add(contentsPanel);

        ListView<ProvisioningStatisticsLineDto> provisioningLines = new ListView<>(ID_PROVISIONING_STATISTICS_LINES, new PropertyModel<>(getModel(), StatisticsDto.F_PROVISIONING_LINES)) {
            protected void populateItem(final ListItem<ProvisioningStatisticsLineDto> item) {

                ListDataProvider<ProvisioningStatisticsOperationDto> provider = new ListDataProvider<>(StatisticsPanel.this, new PropertyModel<>(item.getModel(), ProvisioningStatisticsLineDto.F_OPERATIONS));
                BoxedTablePanel<ProvisioningStatisticsOperationDto> provisioningTable = new BoxedTablePanel<>(ID_PROVISIONING_OPERATIONS, provider, createProvisioningStatisticsColumns()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected WebMarkupContainer createHeader(String headerId) {
                        return new ProvisioningStatisticsHeaderFragment(headerId, ID_PROVISIONING_TITLE, StatisticsPanel.this, item.getModel());
                    }

                    @Override
                    protected Item<ProvisioningStatisticsOperationDto> customizeNewRowItem(Item<ProvisioningStatisticsOperationDto> item, IModel<ProvisioningStatisticsOperationDto> model) {
                        item.add(AttributeModifier.append("class", new ReadOnlyModel<>(() -> {
                            if (model.getObject() != null && OperationResultStatusType.FATAL_ERROR == model.getObject().getStatus()) {
                                return "bg-red disabled color-palette";
                            }

                            return null;
                        })));
                        return item;
                    }

                    @Override
                    protected boolean hideFooterIfSinglePage() {
                        return true;
                    }
                };
                provisioningTable.setOutputMarkupId(true);
                provisioningTable.add(new VisibleBehaviour(() -> hasAnyOperation(item.getModelObject())));
                item.add(provisioningTable);
            }
        };
        contentsPanel.add(provisioningLines);

        ListDataProvider<MappingsLineDto> mappingsProvider = new ListDataProvider<>(this, new PropertyModel<>(getModel(), StatisticsDto.F_MAPPINGS_LINES));
        BoxedTablePanel<MappingsLineDto> mappingsLines = new BoxedTablePanel<>(ID_MAPPINGS_STATISTICS_LINES, mappingsProvider, createMappingsColumn()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new TableHeaderFragment(headerId, ID_MAPPINGS_TITLE, StatisticsPanel.this,
                        createStringResource("Title.MappingsStatistics"));
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };

        mappingsLines.setOutputMarkupId(true);
        contentsPanel.add(mappingsLines);

        ListDataProvider<NotificationsLineDto> notificationsProvider = new ListDataProvider<>(this, new PropertyModel<>(getModel(), StatisticsDto.F_NOTIFICATIONS_LINES));
        BoxedTablePanel<NotificationsLineDto> notificationsLines = new BoxedTablePanel<>(ID_NOTIFICATIONS_STATISTICS_LINES, notificationsProvider, createNotificationColumns()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new TableHeaderFragment(headerId, ID_NOTIFICATIONS_TITLE, StatisticsPanel.this,
                        createStringResource("Title.NotificationsStatistics"));
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

        };
        notificationsLines.setOutputMarkupId(true);
        contentsPanel.add(notificationsLines);

        Label lastMessage = new Label(ID_LAST_MESSAGE, new PropertyModel<>(getModel(), StatisticsDto.F_LAST_MESSAGE));
        contentsPanel.add(lastMessage);
    }

    private boolean hasAnyOperation(ProvisioningStatisticsLineDto provisioningStatisticsLineDto) {
        if (provisioningStatisticsLineDto == null) {
            return false;
        }

        return CollectionUtils.isNotEmpty(provisioningStatisticsLineDto.getOperations());
    }

    private List<IColumn<ProvisioningStatisticsOperationDto, String>> createProvisioningStatisticsColumns() {
        List<IColumn<ProvisioningStatisticsOperationDto, String>> columns = new ArrayList<>();
        columns.add(createProvisioningStatisticsPropertyColumn(ProvisioningStatisticsOperationEntryType.F_OPERATION));
        columns.add(createProvisioningStatisticsPropertyEnumColumn(ProvisioningStatisticsOperationEntryType.F_STATUS));
        columns.add(createProvisioningStatisticsPropertyColumn(ProvisioningStatisticsOperationEntryType.F_COUNT));
        columns.add(new PropertyColumn<>(createStringResource("StatisticsPanel.provisioningStatistics.averageTime"), ProvisioningStatisticsOperationDto.F_AVG_TIME));
        columns.add(createProvisioningStatisticsPropertyColumn(ProvisioningStatisticsOperationEntryType.F_MIN_TIME));
        columns.add(createProvisioningStatisticsPropertyColumn(ProvisioningStatisticsOperationEntryType.F_MAX_TIME));
        columns.add(createProvisioningStatisticsPropertyColumn(ProvisioningStatisticsOperationEntryType.F_TOTAL_TIME));
        return columns;
    }

    private IColumn<ProvisioningStatisticsOperationDto, String> createProvisioningStatisticsPropertyColumn(QName column) {
        String columnName = column.getLocalPart();
        return new PropertyColumn<>(createStringResource("ProvisioningStatisticsOperationEntryType." + columnName), columnName);
    }

    private IColumn<ProvisioningStatisticsOperationDto, String> createProvisioningStatisticsPropertyEnumColumn(QName column) {
        String columnName = column.getLocalPart();
        return new EnumPropertyColumn<>(createStringResource("ProvisioningStatisticsOperationEntryType." + columnName), columnName);
    }

    private List<IColumn<MappingsLineDto, String>> createMappingsColumn() {
        List<IColumn<MappingsLineDto,String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("MappingsStatistics.Object"), MappingsLineDto.F_OBJECT));
        columns.add(new PropertyColumn<>(createStringResource("MappingsStatistics.Count"), MappingsLineDto.F_COUNT));
        columns.add(new PropertyColumn<>(createStringResource("MappingsStatistics.AverageTime"), MappingsLineDto.F_AVERAGE_TIME));
        columns.add(new PropertyColumn<>(createStringResource("MappingsStatistics.MinTime"), MappingsLineDto.F_MIN_TIME));
        columns.add(new PropertyColumn<>(createStringResource("MappingsStatistics.MaxTime"), MappingsLineDto.F_MAX_TIME));
        columns.add(new PropertyColumn<>(createStringResource("MappingsStatistics.TotalTime"), MappingsLineDto.F_TOTAL_TIME));
        return columns;
    }

    private List<IColumn<NotificationsLineDto, String>> createNotificationColumns() {
        List<IColumn<NotificationsLineDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("NotificationsStatistics.Transport"), NotificationsLineDto.F_TRANSPORT));
        columns.add(new PropertyColumn<>(createStringResource("NotificationsStatistics.CountSuccess"), NotificationsLineDto.F_COUNT_SUCCESS));
        columns.add(new PropertyColumn<>(createStringResource("NotificationsStatistics.CountFailure"), NotificationsLineDto.F_COUNT_FAILURE));
        columns.add(new PropertyColumn<>(createStringResource("NotificationsStatistics.AverageTime"), NotificationsLineDto.F_AVERAGE_TIME));
        columns.add(new PropertyColumn<>(createStringResource("NotificationsStatistics.MinTime"), NotificationsLineDto.F_MIN_TIME));
        columns.add(new PropertyColumn<>(createStringResource("NotificationsStatistics.MaxTime"), NotificationsLineDto.F_MAX_TIME));
        columns.add(new PropertyColumn<>(createStringResource("NotificationsStatistics.TotalTime"), NotificationsLineDto.F_TOTAL_TIME));
        return columns;
    }

    // Note: do not setVisible(false) on the progress panel itself - it will disable AJAX refresh functionality attached to it.
    // Use the following two methods instead.

    public void show() {
        contentsPanel.setVisible(true);
    }

    public void hide() {
        contentsPanel.setVisible(false);
    }

    class TableHeaderFragment extends Fragment {

        private static final long serialVersionUID = 1L;
        private static final String ID_LABEL = "headerLabel";

        public TableHeaderFragment(String id, String markupId, MarkupContainer markupProvider, IModel<String> model) {
            super(id, markupId, markupProvider, model);
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            initLayout();
        }

        private IModel<String> getModel() {
            return (IModel<String>) getDefaultModel();
        }

        private void initLayout() {
            Label label = new Label(ID_LABEL, getModel());
            label.setOutputMarkupId(true);
            add(label);
        }
    }

    class ProvisioningStatisticsHeaderFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public ProvisioningStatisticsHeaderFragment(String id, String markupId, MarkupContainer markupProvider, IModel<ProvisioningStatisticsLineDto> model) {
            super(id, markupId, markupProvider, model);
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            initLayout();
        }

        private IModel<ProvisioningStatisticsLineDto> getModel() {
            return (IModel<ProvisioningStatisticsLineDto>) getDefaultModel();
        }

        private ProvisioningStatisticsLineDto getModelObject() {
            return getModel() == null ? null : getModel().getObject();
        }

        private void initLayout() {
            add(new Label(ID_PROVISIONING_RESOURCE, new ReadOnlyModel<>(() -> WebModelServiceUtils.resolveReferenceName(getModelObject().getResourceRef(), getPageBase()))));
            add(new Label(ID_PROVISIONING_OBJECT_CLASS, new ReadOnlyModel<>(() -> getModelObject().getObjectClass() != null ? getModelObject().getObjectClass().getLocalPart() : "")));
        }
    }

}
