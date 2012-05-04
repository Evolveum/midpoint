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

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDtoProvider;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;

public class ResourcesPopup extends Panel {

	
	
	public ResourcesPopup(final PageReference responsePage ,String id, final ModalWindow window, PageBase page) {
		super(id);
		createRolesTable(page, window);
		//((PageUser)responsePage.getPage()).createAssignmentsList()
	}

	public void createRolesTable(PageBase page, final ModalWindow window){
		Form mainForm = new Form("resourceForm");
        add(mainForm);

        TablePanel resources = new TablePanel<ResourceDto>("table",
                new ResourceDtoProvider(page), initResourceColumns());
        resources.setOutputMarkupId(true);
        mainForm.add(resources);
        
        initButtons(window, mainForm);
    }
    
    private void resourceDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        //parameters.add(PageRole.PARAM_ROLE_ID, oid);
        //setResponsePage(PageRole.class, parameters);
    }
    
    private void initButtons(final ModalWindow window, Form form) {
    	AjaxLinkButton addButton = new AjaxLinkButton("add", new StringResourceModel("resourcePopup.button.add", this, null)) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				window.close(target);
			}
		};
    	form.add(addButton);
    }
    
    private List<IColumn<ResourceDto>> initResourceColumns() {
        List<IColumn<ResourceDto>> columns = new ArrayList<IColumn<ResourceDto>>();

        IColumn column = new CheckBoxHeaderColumn<ResourceDto>();
        columns.add(column);

        column = new LinkColumn<ResourceDto>(new StringResourceModel("resourcePopup.name", this, null),
                "name", "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ResourceDto> rowModel) {
                ResourceDto resource = rowModel.getObject();
                resourceDetailsPerformed(target, resource.getOid());
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(new StringResourceModel("resourcePopup.bundle", this, null), "bundle"));
        columns.add(new PropertyColumn(new StringResourceModel("resourcePopup.version", this, null), "version"));

        //todo sync import progress
//        column = new PropertyColumn(createStringResource("pageResources.sync"), "value.connector.connectorVersion");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.import"), "value.connector.connectorVersion");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.progress"), "value.connector.connectorVersion");
//        columns.add(column);

        return columns;
    }
}
