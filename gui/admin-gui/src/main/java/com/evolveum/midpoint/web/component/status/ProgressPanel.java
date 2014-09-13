/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component.status;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.List;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;

/**
 * @author mederly
 */
public class ProgressPanel extends SimplePanel<ProgressDto> {

    private static final String ID_CONTENTS_PANEL = "contents";
    private static final String ID_ACTIVITIES = "progressReportActivities";
    private static final String ID_STATUS_ITEM_DESCRIPTION = "description";
    private static final String ID_STATUS_ITEM_STATE = "status";
    private static final String ID_LOG_ITEMS = "logItems";
    private static final String ID_LOG_ITEM = "logItem";

    private WebMarkupContainer contentsPanel;

    public ProgressPanel(String id) {
        super(id);
    }

    public ProgressPanel(String id, IModel<ProgressDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        contentsPanel = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        add(contentsPanel);

        ListView statusItemsListView = new ListView<ProgressReportActivityDto>(ID_ACTIVITIES, new AbstractReadOnlyModel<List<ProgressReportActivityDto>>() {
            @Override
            public List<ProgressReportActivityDto> getObject() {
                ProgressDto progressDto = ProgressPanel.this.getModelObject();
                return progressDto.getProgressReportActivities();
            }
        }) {
            protected void populateItem(final ListItem<ProgressReportActivityDto> item) {
                item.add(new Label(ID_STATUS_ITEM_DESCRIPTION, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        ProgressReportActivityDto si = item.getModelObject();
                        if (si.getActivityType() == RESOURCE_OBJECT_OPERATION && si.getResourceShadowDiscriminator() != null) {
                            ResourceShadowDiscriminator rsd = si.getResourceShadowDiscriminator();
                            return createStringResource(rsd.getKind()).getString()
                                    + " (" + rsd.getIntent() + ") on " + si.getResourceName();             // TODO correct i18n
                        } else {
                            return createStringResource(si.getActivityType()).getString();
                        }
                    }
                }));
                item.add(createImageLabel(ID_STATUS_ITEM_STATE,
                        new AbstractReadOnlyModel<String>() {

                            @Override
                            public String getObject() {
                                OperationResultStatusType statusType = item.getModelObject().getStatus();
                                if (statusType == null) {
                                    return null;
                                } else {
                                    return OperationResultStatusIcon.parseOperationalResultStatus(statusType).getIcon();
                                }
                            }
                        },
                        new AbstractReadOnlyModel<String>() {

                            @Override
                            public String getObject() {     // TODO why this does not work???
                                OperationResultStatusType statusType = item.getModelObject().getStatus();       // TODO i18n
                                if (statusType == null) {
                                    return null;
                                } else {
                                    return statusType.toString();
                                }
                            }
                        }

                ));
            }
            private Label createImageLabel(String id, IModel<String> cssClass, IModel<String> title) {
                Label label = new Label(id);
                label.add(AttributeModifier.replace("class", cssClass));
                label.add(AttributeModifier.replace("title", title));           // does not work, currently

                return label;
            }

        };
        contentsPanel.add(statusItemsListView);

        ListView logItemsListView = new ListView(ID_LOG_ITEMS, new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                ProgressDto progressDto = ProgressPanel.this.getModelObject();
                return progressDto.getLogItems();
            }
        }) {
            protected void populateItem(ListItem item) {
                item.add(new Label(ID_LOG_ITEM, item.getModel()));
            }
        };
        contentsPanel.add(logItemsListView);
    }

    public void show() {
        contentsPanel.setVisible(true);
    }

    public void hide() {
        contentsPanel.setVisible(false);
    }
}
