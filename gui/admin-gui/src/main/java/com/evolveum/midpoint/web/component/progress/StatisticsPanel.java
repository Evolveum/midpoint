/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author mederly
 */
public class StatisticsPanel extends BasePanel<StatisticsDto> {

    private static final String ID_CONTENTS_PANEL = "contents";
    private static final String ID_PROVISIONING_STATISTICS_LINES = "provisioningStatisticsLines";
    private static final String ID_PROVISIONING_RESOURCE = "Provisioning.Resource";
    private static final String ID_PROVISIONING_OBJECT_CLASS = "Provisioning.ObjectClass";
    private static final String ID_PROVISIONING_GET_SUCCESS = "Provisioning.GetSuccess";
    private static final String ID_PROVISIONING_GET_FAILURE = "Provisioning.GetFailure";
    private static final String ID_PROVISIONING_SEARCH_SUCCESS = "Provisioning.SearchSuccess";
    private static final String ID_PROVISIONING_SEARCH_FAILURE = "Provisioning.SearchFailure";
    private static final String ID_PROVISIONING_CREATE_SUCCESS = "Provisioning.CreateSuccess";
    private static final String ID_PROVISIONING_CREATE_FAILURE = "Provisioning.CreateFailure";
    private static final String ID_PROVISIONING_UPDATE_SUCCESS = "Provisioning.UpdateSuccess";
    private static final String ID_PROVISIONING_UPDATE_FAILURE = "Provisioning.UpdateFailure";
    private static final String ID_PROVISIONING_DELETE_SUCCESS = "Provisioning.DeleteSuccess";
    private static final String ID_PROVISIONING_DELETE_FAILURE = "Provisioning.DeleteFailure";
    private static final String ID_PROVISIONING_SYNC_SUCCESS = "Provisioning.SyncSuccess";
    private static final String ID_PROVISIONING_SYNC_FAILURE = "Provisioning.SyncFailure";
    private static final String ID_PROVISIONING_SCRIPT_SUCCESS = "Provisioning.ScriptSuccess";
    private static final String ID_PROVISIONING_SCRIPT_FAILURE = "Provisioning.ScriptFailure";
    private static final String ID_PROVISIONING_OTHER_SUCCESS = "Provisioning.OtherSuccess";
    private static final String ID_PROVISIONING_OTHER_FAILURE = "Provisioning.OtherFailure";
    private static final String ID_PROVISIONING_TOTAL_OPERATIONS_COUNT = "Provisioning.TotalOperationsCount";
    private static final String ID_PROVISIONING_AVERAGE_TIME = "Provisioning.AverageTime";
    private static final String ID_PROVISIONING_MIN_TIME = "Provisioning.MinTime";
    private static final String ID_PROVISIONING_MAX_TIME = "Provisioning.MaxTime";
    private static final String ID_PROVISIONING_TOTAL_TIME = "Provisioning.TotalTime";

    private static final String ID_MAPPINGS_STATISTICS_LINES = "mappingsStatisticsLines";
    private static final String ID_MAPPINGS_OBJECT = "Mappings.Object";
    private static final String ID_MAPPINGS_COUNT = "Mappings.Count";
    private static final String ID_MAPPINGS_AVERAGE_TIME = "Mappings.AverageTime";
    private static final String ID_MAPPINGS_MIN_TIME = "Mappings.MinTime";
    private static final String ID_MAPPINGS_MAX_TIME = "Mappings.MaxTime";
    private static final String ID_MAPPINGS_TOTAL_TIME = "Mappings.TotalTime";

    private static final String ID_NOTIFICATIONS_STATISTICS_LINES = "notificationsStatisticsLines";
    private static final String ID_NOTIFICATIONS_TRANSPORT = "Notifications.Transport";
    private static final String ID_NOTIFICATIONS_COUNT_SUCCESS = "Notifications.CountSuccess";
    private static final String ID_NOTIFICATIONS_COUNT_FAILURE = "Notifications.CountFailure";
    private static final String ID_NOTIFICATIONS_AVERAGE_TIME = "Notifications.AverageTime";
    private static final String ID_NOTIFICATIONS_MIN_TIME = "Notifications.MinTime";
    private static final String ID_NOTIFICATIONS_MAX_TIME = "Notifications.MaxTime";
    private static final String ID_NOTIFICATIONS_TOTAL_TIME = "Notifications.TotalTime";

    private static final String ID_LAST_MESSAGE = "lastMessage";

    private static final String ID_SOURCE = "source";

    private WebMarkupContainer contentsPanel;

    public StatisticsPanel(String id) {
        super(id);
		initLayout();
    }

    public StatisticsPanel(String id, IModel<StatisticsDto> model) {
        super(id, model);
		initLayout();
	}

    protected void initLayout() {
        contentsPanel = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        add(contentsPanel);

        ListView provisioningLines = new ListView<ProvisioningStatisticsLineDto>(ID_PROVISIONING_STATISTICS_LINES, new PropertyModel<>(getModel(), StatisticsDto.F_PROVISIONING_LINES)) {
            protected void populateItem(final ListItem<ProvisioningStatisticsLineDto> item) {
                item.add(new Label(ID_PROVISIONING_RESOURCE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_RESOURCE)));
                item.add(new Label(ID_PROVISIONING_OBJECT_CLASS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_OBJECT_CLASS)));
                item.add(new Label(ID_PROVISIONING_GET_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_GET_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_GET_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_GET_FAILURE)));
                item.add(new Label(ID_PROVISIONING_SEARCH_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_SEARCH_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_SEARCH_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_SEARCH_FAILURE)));
                item.add(new Label(ID_PROVISIONING_CREATE_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_CREATE_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_CREATE_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_CREATE_FAILURE)));
                item.add(new Label(ID_PROVISIONING_UPDATE_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_UPDATE_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_UPDATE_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_UPDATE_FAILURE)));
                item.add(new Label(ID_PROVISIONING_DELETE_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_DELETE_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_DELETE_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_DELETE_FAILURE)));
                item.add(new Label(ID_PROVISIONING_SYNC_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_SYNC_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_SYNC_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_SYNC_FAILURE)));
                item.add(new Label(ID_PROVISIONING_SCRIPT_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_SCRIPT_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_SCRIPT_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_SCRIPT_FAILURE)));
                item.add(new Label(ID_PROVISIONING_OTHER_SUCCESS, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_OTHER_SUCCESS)));
                item.add(new Label(ID_PROVISIONING_OTHER_FAILURE, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_OTHER_FAILURE)));
                item.add(new Label(ID_PROVISIONING_TOTAL_OPERATIONS_COUNT, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_TOTAL_OPERATIONS_COUNT)));
                item.add(new Label(ID_PROVISIONING_AVERAGE_TIME, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_AVERAGE_TIME)));
                item.add(new Label(ID_PROVISIONING_MIN_TIME, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_MIN_TIME)));
                item.add(new Label(ID_PROVISIONING_MAX_TIME, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_MAX_TIME)));
                item.add(new Label(ID_PROVISIONING_TOTAL_TIME, new PropertyModel<String>(item.getModel(), ProvisioningStatisticsLineDto.F_TOTAL_TIME)));
            }
        };
        contentsPanel.add(provisioningLines);

        ListView mappingsLines = new ListView<MappingsLineDto>(ID_MAPPINGS_STATISTICS_LINES, new PropertyModel<>(getModel(), StatisticsDto.F_MAPPINGS_LINES)) {
            protected void populateItem(final ListItem<MappingsLineDto> item) {
                item.add(new Label(ID_MAPPINGS_OBJECT, new PropertyModel<String>(item.getModel(), MappingsLineDto.F_OBJECT)));
                item.add(new Label(ID_MAPPINGS_COUNT, new PropertyModel<String>(item.getModel(), MappingsLineDto.F_COUNT)));
                item.add(new Label(ID_MAPPINGS_AVERAGE_TIME, new PropertyModel<String>(item.getModel(), MappingsLineDto.F_AVERAGE_TIME)));
                item.add(new Label(ID_MAPPINGS_MIN_TIME, new PropertyModel<String>(item.getModel(), MappingsLineDto.F_MIN_TIME)));
                item.add(new Label(ID_MAPPINGS_MAX_TIME, new PropertyModel<String>(item.getModel(), MappingsLineDto.F_MAX_TIME)));
                item.add(new Label(ID_MAPPINGS_TOTAL_TIME, new PropertyModel<String>(item.getModel(), MappingsLineDto.F_TOTAL_TIME)));
            }
        };
        contentsPanel.add(mappingsLines);

        ListView notificationsLines = new ListView<NotificationsLineDto>(ID_NOTIFICATIONS_STATISTICS_LINES, new PropertyModel<>(getModel(), StatisticsDto.F_NOTIFICATIONS_LINES)) {
            protected void populateItem(final ListItem<NotificationsLineDto> item) {
                item.add(new Label(ID_NOTIFICATIONS_TRANSPORT, new PropertyModel<String>(item.getModel(), NotificationsLineDto.F_TRANSPORT)));
                item.add(new Label(ID_NOTIFICATIONS_COUNT_SUCCESS, new PropertyModel<String>(item.getModel(), NotificationsLineDto.F_COUNT_SUCCESS)));
                item.add(new Label(ID_NOTIFICATIONS_COUNT_FAILURE, new PropertyModel<String>(item.getModel(), NotificationsLineDto.F_COUNT_FAILURE)));
                item.add(new Label(ID_NOTIFICATIONS_AVERAGE_TIME, new PropertyModel<String>(item.getModel(), NotificationsLineDto.F_AVERAGE_TIME)));
                item.add(new Label(ID_NOTIFICATIONS_MIN_TIME, new PropertyModel<String>(item.getModel(), NotificationsLineDto.F_MIN_TIME)));
                item.add(new Label(ID_NOTIFICATIONS_MAX_TIME, new PropertyModel<String>(item.getModel(), NotificationsLineDto.F_MAX_TIME)));
                item.add(new Label(ID_NOTIFICATIONS_TOTAL_TIME, new PropertyModel<String>(item.getModel(), NotificationsLineDto.F_TOTAL_TIME)));
            }
        };
        contentsPanel.add(notificationsLines);

        Label lastMessage = new Label(ID_LAST_MESSAGE, new PropertyModel<>(getModel(), StatisticsDto.F_LAST_MESSAGE));
        contentsPanel.add(lastMessage);

//        Label source = new Label(ID_SOURCE, new AbstractReadOnlyModel<String>() {
//            @Override
//            public String getObject() {
//                StatisticsDto dto = getModelObject();
//                if (dto == null) {
//                    return null;
//                }
//                EnvironmentalPerformanceInformationType info = dto.getEnvironmentalPerformanceInformationType();
//                if (info == null) {
//                    return null;
//                }
//                if (Boolean.TRUE.equals(info.isFromMemory())) {
//                    return getString("Message.SourceMemory",
//                            WebMiscUtil.formatDate(info.getTimestamp()));
//                } else {
//                    return getString("Message.SourceRepository",
//                            WebMiscUtil.formatDate(info.getTimestamp()));
//                }
//            }
//        });
//        contentsPanel.add(source);
    }

    // Note: do not setVisible(false) on the progress panel itself - it will disable AJAX refresh functionality attached to it.
    // Use the following two methods instead.

    public void show() {
        contentsPanel.setVisible(true);
    }

    public void hide() {
        contentsPanel.setVisible(false);
    }

}
