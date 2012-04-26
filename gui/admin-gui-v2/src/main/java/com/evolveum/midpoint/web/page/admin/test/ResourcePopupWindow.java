/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * @author mserbak
 */

public class ResourcePopupWindow extends Panel {

	public ResourcePopupWindow(String id, final ModalWindow window) {
		super(id);
		initLayout(window);
		//
		// add(new AjaxLinkButton("aaa", new Model<String>("Close")) {
		//
		// @Override
		// public void onClick(AjaxRequestTarget target) {
		// window.close(target);
		// }
		// });
	}

	public void initLayout(final ModalWindow window) {
		List<IColumn<ResourceType>> columns = new ArrayList<IColumn<ResourceType>>();

		IColumn column = new LinkColumn<SelectableBean<ResourceType>>(
				createStringResource("resourcePopupWindow.name"), "value.name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ResourceType>> rowModel) {
				ResourceType resource = rowModel.getObject().getValue();
				resourceAcceptPerformed(target, resource.getOid());
				window.close(target);
			}
		};
		columns.add(column);

		// todo fix connector resolving...
		column = new PropertyColumn(createStringResource("resourcePopupWindow.bundle"),
				"value.connector.connectorBundle");
		columns.add(column);
		
		column = new PropertyColumn(createStringResource("resourcePopupWindow.version"),
				"value.connector.connectorVersion");
		columns.add(column);

		column = new IconColumn<ResourceType>(createStringResource("resourcePopupWindow.status"));
		columns.add(column);
		
		column = new PropertyColumn(createStringResource("resourcePopupWindow.sync"),
				"value.connector.connectorVersion");
		columns.add(column);

		column = new PropertyColumn(createStringResource("resourcePopupWindow.import"),
				"value.connector.connectorVersion");
		columns.add(column);
		
		column = new PropertyColumn(createStringResource("resourcePopupWindow.progress"),
				"value.connector.connectorVersion");
		columns.add(column);

		add(new TablePanel<ResourceType>("resourceTable", new ObjectDataProvider(ResourceType.class), columns));
	}

	public void resourceAcceptPerformed(AjaxRequestTarget target, String oid) {
		// TODO: accept selected resource
	}

	protected StringResourceModel createStringResource(String resourceKey) {
		return new StringResourceModel(resourceKey, this, null);
	}
}
