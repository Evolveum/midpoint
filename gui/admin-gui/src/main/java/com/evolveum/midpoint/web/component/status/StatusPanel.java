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

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author mederly
 */
public class StatusPanel extends SimplePanel<Status> {

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

    public StatusPanel(String id, IModel<Status> model) {
        super(id, model);
    }

    protected void initLayout() {
        contentsPanel  = new WebMarkupContainer(ID_CONTENTS_PANEL);
        contentsPanel.setOutputMarkupId(true);
        add(contentsPanel);

        ListView statusItemsListView = new ListView(ID_STATUS_ITEM, new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                Status status = StatusPanel.this.getModelObject();
                return status.getStatusItems();
            }
        }) {
            protected void populateItem(ListItem item) {
                item.add(new Label(ID_STATUS_ITEM_DESCRIPTION, new PropertyModel<String>(item.getModelObject(), "description")));
                item.add(new Label(ID_STATUS_ITEM_STATE, new PropertyModel<String>(item.getModelObject(), "state")));
            }
        };
        contentsPanel.add(statusItemsListView);

        ListView logItemsListView = new ListView(ID_LOG_ITEMS, new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                Status status = StatusPanel.this.getModelObject();
                return status.getLogItems();
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
