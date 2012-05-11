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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceController;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDtoProvider;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceStatus;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceSyncStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;

/**
 * @author lazyman
 */
public class PageResources extends PageAdminResources {
	private static final String DOT_CLASS = PageResources.class.getName() + ".";
	private static final String TEST_RESOURCE = DOT_CLASS + "testResource";
	private static final String SYNC_STATUS = DOT_CLASS + "syncStatus";

    public PageResources() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        TablePanel resources = new TablePanel<ResourceDto>("table",
                new ResourceDtoProvider(this), initResourceColumns());
        resources.setOutputMarkupId(true);
        mainForm.add(resources);

        TablePanel connectorHosts = new TablePanel<ConnectorHostType>("connectorTable",
                new ObjectDataProvider(PageResources.this, ConnectorHostType.class), initConnectorHostsColumns());
        connectorHosts.setShowPaging(false);
        connectorHosts.setOutputMarkupId(true);
        mainForm.add(connectorHosts);

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxLinkButton deleteResource = new AjaxLinkButton("deleteResource",
                createStringResource("pageResources.button.deleteResource")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteResourcePerformed(target);
            }
        };
        mainForm.add(deleteResource);
    }

    private List<IColumn<ResourceDto>> initResourceColumns() {
        List<IColumn<ResourceDto>> columns = new ArrayList<IColumn<ResourceDto>>();

        IColumn column = new CheckBoxHeaderColumn<ResourceDto>();
        columns.add(column);

        column = new LinkColumn<ResourceDto>(createStringResource("pageResources.name"),
                "name", "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ResourceDto> rowModel) {
                ResourceDto resource = rowModel.getObject();
                resourceDetailsPerformed(target, resource.getOid());
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageResources.bundle"), "bundle"));
        columns.add(new PropertyColumn(createStringResource("pageResources.version"), "version"));

        column = new LinkIconColumn<ResourceDto>(createStringResource("pageResources.status")) {

            @Override
            protected IModel<ResourceReference> createIconModel(final IModel<ResourceDto> rowModel) {
                return new AbstractReadOnlyModel<ResourceReference>() {

                    @Override
                    public ResourceReference getObject() {
                        ResourceDto dto = rowModel.getObject();
                        ResourceStatus status = dto.getOverallStatus();
                        if (status == null) {
                            status = ResourceStatus.NOT_TESTED;
                        }
                        return new PackageResourceReference(PageResources.class, status.getIcon());
                    }
                };
            }

            @Override
            protected void onClickPerformed(AjaxRequestTarget target, IModel<ResourceDto> rowModel, AjaxLink link) {
                testResourcePerformed(target, rowModel);
                target.add(link);                
            }
        };
        columns.add(column);
        
        column = new LinkIconColumn<ResourceDto>(createStringResource("pageResources.sync")) {

            @Override
            protected IModel<ResourceReference> createIconModel(final IModel<ResourceDto> rowModel) {
                return new AbstractReadOnlyModel<ResourceReference>() {

                    @Override
                    public ResourceReference getObject() {
                        ResourceDto dto = rowModel.getObject();
                        ResourceSyncStatus status = dto.getSyncStatus();
                        if (status == null) {
                            status = ResourceSyncStatus.DISABLE;
                        }
                        return new PackageResourceReference(PageResources.class, status.getIcon());
                    }
                };
            }

            @Override
            protected void onClickPerformed(AjaxRequestTarget target, IModel<ResourceDto> rowModel, AjaxLink link) {
                showSyncStatus(target, rowModel);
                target.add(link);                
            }
        };
        columns.add(column);
        
        //todo sync import progress
//        column = new PropertyColumn(createStringResource("pageResources.sync"), "value.connector.connectorVersion");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.import"), "value.connector.connectorVersion");
//        columns.add(column);
//        column = new PropertyColumn(createStringResource("pageResources.progress"), "value.connector.connectorVersion");
//        columns.add(column);

        return columns;
    }

    private List<IColumn<ConnectorHostType>> initConnectorHostsColumns() {
        List<IColumn<ConnectorHostType>> columns = new ArrayList<IColumn<ConnectorHostType>>();

        IColumn column = new CheckBoxHeaderColumn<ConnectorHostType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<ConnectorHostType>>(createStringResource("pageResources.connector.name"),
                "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ConnectorHostType>> rowModel) {
                ConnectorHostType host = rowModel.getObject().getValue();
                //resourceDetailsPerformed(target, host.getOid());
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

        return columns;
    }

    private void resourceDetailsPerformed(AjaxRequestTarget target, String oid) {
    	PageParameters parameters = new PageParameters();
        parameters.add(PageResource.PARAM_RESOURCE_ID, oid);
        setResponsePage(PageResource.class, parameters);
    }

    private void deleteResourcePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void testResourcePerformed(AjaxRequestTarget target, IModel<ResourceDto> rowModel) {
        TaskManager taskManager = getTaskManager();
        OperationResult result = null;
    	ResourceDto dto = rowModel.getObject();
    	if (StringUtils.isEmpty(dto.getOid())) {
    		result.recordFatalError("Resource oid not defined in request");
		}
    	
    	try {
    		result = getModelService().testResource(dto.getOid(), taskManager.createTaskInstance(TEST_RESOURCE));
    		ResourceController.updateResourceState(dto.getState(), result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Fail to test resource connection", ex);
		}
    	
    	if(result == null) {
    		result = new OperationResult(TEST_RESOURCE);
    	}
    	
    	if(!result.isSuccess()){
    		showResult(result);
    		target.add(getFeedbackPanel());
    	}
    }
    
    private void showSyncStatus(AjaxRequestTarget target, IModel<ResourceDto> rowModel) {
    	OperationResult result = new OperationResult(SYNC_STATUS);
    	ResourceDto dto = rowModel.getObject();
		if (dto == null) {
			result.recordFatalError("Fail to synchronize resource");
		}
		//resourceSync.setResource(resourceItem);
	}
}
