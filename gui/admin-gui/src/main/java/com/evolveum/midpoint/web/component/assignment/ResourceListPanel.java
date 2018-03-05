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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.BaseDeprecatedPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ResourceListPanel extends BaseDeprecatedPanel {

    public ResourceListPanel(String id) {
        super(id, null);
    }

    @Override
    protected void initLayout() {
        TablePanel resources = new TablePanel("table", new ObjectDataProvider((PageBase) getPage(),
                ResourceType.class), initColumns());
        resources.setOutputMarkupId(true);
        add(resources);
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new LinkColumn<SelectableBean<ResourceType>>(createStringResource("ObjectType.name"), "name",
                "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ResourceType>> rowModel) {
                ResourceType resource = rowModel.getObject().getValue();
                resourceSelectedPerformed(target, resource);
            }
        };
        columns.add(column);


        return columns;
    }

    public void resourceSelectedPerformed(AjaxRequestTarget target, ResourceType resource) {

    }
}
