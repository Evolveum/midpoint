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
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

import static com.evolveum.midpoint.model.api.OperationStatus.EventType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.web.component.status.StatusDto.StatusItem;
import static com.evolveum.midpoint.web.page.PageBase.createStringResourceStatic;

/**
 * @author mederly
 */
public class StatusPanel extends SimplePanel<StatusDto> {

    private static final String ID_CONTENTS_PANEL = "contents";
    private static final String ID_STATUS_ITEM = "statusItems";
    private static final String ID_STATUS_ITEM_DESCRIPTION = "description";
    private static final String ID_STATUS_ITEM_STATE = "state";
    private static final String ID_LOG_ITEMS = "logItems";
    private static final String ID_LOG_ITEM = "logItem";


    private WebMarkupContainer contentsPanel;

    public StatusPanel(String id) {
        super(id);
    }

    public StatusPanel(String id, IModel<StatusDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        contentsPanel = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        add(contentsPanel);

        ListView statusItemsListView = new ListView<StatusItem>(ID_STATUS_ITEM, new AbstractReadOnlyModel<List<StatusItem>>() {
            @Override
            public List<StatusItem> getObject() {
                StatusDto statusDto = StatusPanel.this.getModelObject();
                return statusDto.getStatusItems();
            }
        }) {
            protected void populateItem(final ListItem<StatusItem> item) {
                item.add(new Label(ID_STATUS_ITEM_DESCRIPTION, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        StatusItem si = item.getModelObject();
                        if (si.getEventType() == RESOURCE_OBJECT_OPERATION && si.getResourceShadowDiscriminator() != null) {
                            ResourceShadowDiscriminator rsd = si.getResourceShadowDiscriminator();
                            return createStringResource(rsd.getKind()).getString()
                                    + " (" + rsd.getIntent() + ") on " + si.getResourceName();             // TODO correct i18n
                        } else {
                            return createStringResource(si.getEventType()).getString();
                        }
                    }
                }));
                item.add(createImageLabel(ID_STATUS_ITEM_STATE,
                        new AbstractReadOnlyModel<String>() {

                            @Override
                            public String getObject() {
                                OperationResultStatusType statusType = item.getModelObject().getState();
                                if (statusType == null) {
                                    return null;
                                } else {
                                    return OperationResultStatusIcon.parseOperationalResultStatus(statusType).getIcon();
                                }
                            }
                        },
                        new AbstractReadOnlyModel<String>() {

                            @Override
                            public String getObject() {
                                OperationResultStatusType statusType = item.getModelObject().getState();
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
                label.add(AttributeModifier.replace("title", title));

                return label;
            }

        };
        contentsPanel.add(statusItemsListView);

        ListView logItemsListView = new ListView(ID_LOG_ITEMS, new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                StatusDto statusDto = StatusPanel.this.getModelObject();
                return statusDto.getLogItems();
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
