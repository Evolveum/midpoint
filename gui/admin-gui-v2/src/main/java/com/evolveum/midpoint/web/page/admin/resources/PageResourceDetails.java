package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

public class PageResourceDetails extends PageAdminResources {

	public PageResourceDetails() {
		initLayout();
	}
	
	private void initLayout() {
        /*List<IColumn<ResourceType>> columns = new ArrayList<IColumn<ResourceType>>();

        IColumn column = new CheckBoxColumn<ResourceType>() {

            @Override
            public void onUpdateHeader(AjaxRequestTarget target) {
                //todo implement
            }

            @Override
            public void onUpdateRow(AjaxRequestTarget target, IModel<Selectable<ResourceType>> rowModel) {
                //todo implement
            }
        };
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

        TablePanel<ResourceType> table1 = new TablePanel<ResourceType>("table", ResourceType.class, columns);
        table1.add(new AttributeAppender("style", new Model<String>("width: 50%;"), " "));
        add(table1);
        
        TablePanel<ResourceType> table2 = new TablePanel<ResourceType>("table", ResourceType.class, columns);
        table2.add(new AttributeAppender("style", new Model<String>("width: 50%;"), " "));
        add(table2);*/
    }
	
	public void resourceDetailsPerformed(AjaxRequestTarget target, String oid) {

    }
}
