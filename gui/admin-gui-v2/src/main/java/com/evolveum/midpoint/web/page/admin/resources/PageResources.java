/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.data.column.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * @author lazyman
 */
public class PageResources extends PageAdminResources {

    public PageResources() {
        initLayout();
    }

    private void initLayout() {
        List<IColumn<ResourceType>> columns = new ArrayList<IColumn<ResourceType>>();

        IColumn column = new CheckBoxHeaderColumn<ResourceType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<ResourceType>>(createStringResource("pageResources.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ResourceType>> rowModel) {
                ResourceType resource = rowModel.getObject().getValue();
                resourceDetailsPerformed(target, resource.getOid());
            }
        };
        columns.add(column);

        //todo fix connector resolving...
        column = new PropertyColumn(createStringResource("pageResources.bundle"), "connectorBundle", "value.connector.connectorBundle");
        columns.add(column);
        column = new PropertyColumn(createStringResource("pageResources.version"), "connectorVersion", "value.connector.connectorVersion");
        columns.add(column);

        column = new LinkIconColumn<ResourceType>(createStringResource("pageResources.status")) {

            @Override
            protected IModel<ResourceReference> createIconModel(IModel<ResourceType> rowModel) {
                return new AbstractReadOnlyModel<ResourceReference>() {

                    @Override
                    public ResourceReference getObject() {
                        return new PackageResourceReference(PageResources.class, "someicon.png");
                    }
                };
            }

            @Override
            protected void onClickPerformed(AjaxRequestTarget target) {
                System.out.println("aaa");
            }
        };
        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.sync"), "value.connector.connectorVersion");
//        columns.add(column);
//
//        column = new PropertyColumn(createStringResource("pageResources.import"), "value.connector.connectorVersion");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.progress"), "value.connector.connectorVersion");
//        columns.add(column);

        add(new TablePanel<ResourceType>("table", new ObjectDataProvider(PageResources.this, ResourceType.class), columns));

        initConnectorHostTable();
    }

    private void initConnectorHostTable() {
        List<IColumn<ConnectorHostType>> columns = new ArrayList<IColumn<ConnectorHostType>>();

        IColumn column = new CheckBoxHeaderColumn<ResourceType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<ResourceType>>(createStringResource("pageResources.connector.name"),
                "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ResourceType>> rowModel) {
                ResourceType resource = rowModel.getObject().getValue();
                resourceDetailsPerformed(target, resource.getOid());
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageResources.connector.hostname"),
                "value.hostname"));
        columns.add(new PropertyColumn(createStringResource("pageResources.connector.port"),
                "value.port"));
        columns.add(new PropertyColumn(createStringResource("pageResources.connector.timeout"),
                "value.timeout"));
        columns.add(new CheckBoxColumn(createStringResource("pageResources.connector.protectConnection"),
                "value.protectConnection"));

        TablePanel table = new TablePanel<ConnectorHostType>("connectorTable",
                new ObjectDataProvider(PageResources.this, ConnectorHostType.class), columns);
        table.setShowPaging(false);
        add(table);
    }

    public void resourceDetailsPerformed(AjaxRequestTarget target, String oid) {

    }
}
