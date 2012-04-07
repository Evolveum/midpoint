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

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageResources extends PageAdminResources {

    public PageResources() {
        initLayout();
    }

    private void initLayout() {
        List<IColumn<ResourceType>> columns = new ArrayList<IColumn<ResourceType>>();

        IColumn column = new CheckBoxColumn<ResourceType>();
        columns.add(column);

        column = new LinkColumn<Selectable<ResourceType>>(createStringResource("pageResources.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<ResourceType>> rowModel) {
                ResourceType resource = rowModel.getObject().getValue();
                resourceDetailsPerformed(target, resource.getOid());
            }
        };
        columns.add(column);

        //todo fix connector resolving...
//        column = new PropertyColumn(createStringResource("pageResources.bundle"), "value.connector.connectorBundle");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.version"), "value.connector.connectorVersion");
//        columns.add(column);

//        column = new PropertyColumn(createStringResource("pageResources.status"), "value.connector.connectorVersion");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.sync"), "value.connector.connectorVersion");
//        columns.add(column);
//
//        column = new PropertyColumn(createStringResource("pageResources.import"), "value.connector.connectorVersion");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.progress"), "value.connector.connectorVersion");
//        columns.add(column);

        add(new TablePanel<ResourceType>("table", ResourceType.class, columns));
    }

    public void resourceDetailsPerformed(AjaxRequestTarget target, String oid) {

    }
}
