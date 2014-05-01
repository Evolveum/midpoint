/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

public class ResourcesPopup extends Panel {

    public ResourcesPopup(String id, SimpleUserResourceProvider provider) {
        super(id);

        initLayout(provider);
    }

    private void initLayout(SimpleUserResourceProvider provider) {
        TablePanel resources = new TablePanel<ResourceDto>("table",
                provider, initResourceColumns());
        resources.setOutputMarkupId(true);
        add(resources);

        AjaxButton addButton = new AjaxButton("add",
                new StringResourceModel("resourcePopup.button.add", this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, getSelectedResources());
            }
        };
        add(addButton);
    }

    private List<IColumn<ResourceDto, String>> initResourceColumns() {
        List<IColumn<ResourceDto, String>> columns = new ArrayList<IColumn<ResourceDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<ResourceDto>();
        columns.add(column);

        columns.add(new PropertyColumn(new StringResourceModel("resourcePopup.name", this, null), "value.name"));

        return columns;
    }

    private List<ResourceType> getSelectedResources() {
        List<ResourceType> list = new ArrayList<ResourceType>();

        TablePanel table = (TablePanel) get("table");
        SimpleUserResourceProvider provider = (SimpleUserResourceProvider) table.getDataTable().getDataProvider();
        for (SelectableBean<ResourceType> bean : provider.getAvailableData()) {
            if (!bean.isSelected()) {
                continue;
            }

            list.add(bean.getValue());
        }

        return list;
    }

    protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {

    }
}
