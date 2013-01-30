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

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.component.ContentPanel;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentEntitlements;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceController;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDtoProvider;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceState;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageResources extends PageAdminResources {

    private static final Trace LOGGER = TraceManager.getTrace(PageResources.class);
    private static final String DOT_CLASS = PageResources.class.getName() + ".";
    private static final String TEST_RESOURCE = DOT_CLASS + "testResource";
    private static final String SYNC_STATUS = DOT_CLASS + "syncStatus";
    private static final String OPERATION_DELETE_RESOURCES = DOT_CLASS + "deleteResources";
    private static final String OPERATION_CONNECTOR_DISCOVERY = DOT_CLASS + "connectorDiscovery";

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

        add(new ConfirmationDialog("confirmDeletePopup", createStringResource("pageResources.dialog.title.confirmDelete"),
                createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteConfirmedPerformed(target);
            }
        });
    }

    private void initButtons(Form mainForm) {
        AjaxLinkButton deleteResource = new AjaxLinkButton("deleteResource", ButtonType.NEGATIVE,
                createStringResource("pageResources.button.deleteResource")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteResourcePerformed(target);
            }
        };
        mainForm.add(deleteResource);

        AjaxLinkButton discoveryRemote = new AjaxLinkButton("discoveryRemote",
                createStringResource("pageResources.button.discoveryRemote")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                discoveryRemotePerformed(target);
            }
        };
        mainForm.add(discoveryRemote);
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

        columns.add(new AbstractColumn<ResourceDto>(createStringResource("pageResources.content")) {

            @Override
            public void populateItem(Item<ICellPopulator<ResourceDto>> cellItem,
                                     String componentId, final IModel<ResourceDto> model) {
                cellItem.add(new ContentPanel(componentId) {

                    @Override
                    public void accountsPerformed(AjaxRequestTarget target) {
                        ResourceDto dto = model.getObject();

                        PageParameters parameters = new PageParameters();
                        parameters.add(PageContentAccounts.PARAM_RESOURCE_ID, dto.getOid());
                        setResponsePage(PageContentAccounts.class, parameters);
                    }

                    @Override
                    public void entitlementsPerformed(AjaxRequestTarget target) {
                        ResourceDto dto = model.getObject();

                        PageParameters parameters = new PageParameters();
                        parameters.add(PageContentEntitlements.PARAM_RESOURCE_ID, dto.getOid());
                        setResponsePage(PageContentEntitlements.class, parameters);
                    }
                });
            }
        });
   
        column = new LinkIconColumn<ResourceDto>(createStringResource("pageResources.status")) {

            @Override
            protected IModel<ResourceReference> createIconModel(final IModel<ResourceDto> rowModel) {
                return new AbstractReadOnlyModel<ResourceReference>() {

                    @Override
                    public ResourceReference getObject() {   
                    	ResourceDto dto = rowModel.getObject();
                        ResourceController.updateLastAvailabilityState(dto.getState(),
        						dto.getLastAvailabilityStatus());
                        ResourceState state = dto.getState();
                        return new PackageResourceReference(PageResources.class, state
								.getLastAvailability().getIcon());
                    }
                };
            }

            @Override
            protected IModel<String> createTitleModel(final IModel<ResourceDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                    	ResourceState state = rowModel.getObject().getState();
                        return PageResources.this.getString(ResourceStatus.class.getSimpleName() + "." + state
								.getLastAvailability().name());
                    }
                };
            }
            
			@Override
			protected void onClickPerformed(AjaxRequestTarget target, IModel<ResourceDto> rowModel,
					AjaxLink link) {
				testResourcePerformed(target, rowModel);
				target.add(link);
			}
        };
        columns.add(column);

        /*column = new LinkIconColumn<ResourceDto>(createStringResource("pageResources.sync")) {

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
        columns.add(column);*/

        //todo import
//        column = new LinkIconColumn<ResourceDto>(createStringResource("pageResources.import")) {
//
//            @Override
//            protected IModel<ResourceReference> createIconModel(final IModel<ResourceDto> rowModel) {
//                return new AbstractReadOnlyModel<ResourceReference>() {
//
//                    @Override
//                    public ResourceReference getObject() {
//                        ResourceDto dto = rowModel.getObject();
//                        ResourceImportStatus status = dto.getResImport();
//                        if (status == null) {
//                            status = ResourceImportStatus.DISABLE;
//                        }
//                        return new PackageResourceReference(PageResources.class, status.getIcon());
//                    }
//                };
//            }
//
//            @Override
//            protected void onClickPerformed(AjaxRequestTarget target, IModel<ResourceDto> rowModel, AjaxLink link) {
//                ResourceDto resource = rowModel.getObject();
//                resourceImportPerformed(target, resource.getOid());
//                target.add(link);
//            }
//        };
//        columns.add(column);

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

    private void resourceImportPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageResourceImport.PARAM_RESOURCE_IMPORT_ID, oid);
        setResponsePage(PageResourceImport.class, parameters);
    }

    private void deleteResourcePerformed(AjaxRequestTarget target) {
        List<ResourceDto> selected = WebMiscUtil.getSelectedData(getResourceTable());
        if (selected.isEmpty()) {
            warn(getString("pageResources.message.nothingSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        ModalWindow dialog = (ModalWindow) get("confirmDeletePopup");
        dialog.show(target);
    }

    private TablePanel getResourceTable() {
        return (TablePanel) get("mainForm:table");
    }

    private TablePanel getConnectorHostTable() {
        return (TablePanel) get("mainForm:connectorTable");
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
            	List<ResourceDto> selectedResources = WebMiscUtil.getSelectedData(getResourceTable());
            	if (selectedResources.size() == 1){
            		return createStringResource("pageResources.message.deleteResourceConfirm",
                            selectedResources.get(0).getName()).getString();
				} else {
					return createStringResource("pageResources.message.deleteResourcesConfirm",
							WebMiscUtil.getSelectedData(getResourceTable()).size()).getString();
				}
            }
        };
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<ResourceDto> selected = WebMiscUtil.getSelectedData(getResourceTable());

        OperationResult result = new OperationResult(OPERATION_DELETE_RESOURCES);
        for (ResourceDto resource : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_RESOURCES);

                ObjectDelta<ResourceType> delta = ObjectDelta.createDeleteDelta(ResourceType.class, resource.getOid(), getPrismContext()); 
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            } catch (Exception ex) {
                result.recordPartialError("Couldn't delete resource.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete resource", ex);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus("Error occurred during resource deleting.");
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The resource(s) have been successfully deleted.");
        }

        DataTable table = getResourceTable().getDataTable();
        ResourceDtoProvider provider = (ResourceDtoProvider) table.getDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getResourceTable());
    }

    private void showSyncStatus(AjaxRequestTarget target, IModel<ResourceDto> rowModel) {
        OperationResult result = new OperationResult(SYNC_STATUS);
        ResourceDto dto = rowModel.getObject();
        if (dto == null) {
            result.recordFatalError("Fail to synchronize resource");
        }
        //resourceSync.setResource(resourceItem);
    }

    private void discoveryRemotePerformed(AjaxRequestTarget target) {
        target.add(getFeedbackPanel());

        OperationResult result = new OperationResult(OPERATION_CONNECTOR_DISCOVERY);
        List<SelectableBean<ConnectorHostType>> selected = WebMiscUtil.getSelectedData(getConnectorHostTable());
        if (selected.isEmpty()) {
            warn(getString("pageResources.message.noHostSelected"));
            return;
        }

        for (SelectableBean<ConnectorHostType> bean : selected) {
            ConnectorHostType host = bean.getValue();
            try {
                getModelService().discoverConnectors(host, result);
            } catch (Exception ex) {
                result.recordFatalError("Fail to discover connectors on host '"+host.getHostname()
                        + ":" + host.getPort() + "'", ex);
            }
        }

        result.recomputeStatus();
        showResult(result);
    }
    
	private void testResourcePerformed(AjaxRequestTarget target, IModel<ResourceDto> rowModel) {

		OperationResult result = null;

		ResourceDto dto = rowModel.getObject();
		if (StringUtils.isEmpty(dto.getOid())) {
			result.recordFatalError("Resource oid not defined in request");
		}

		try {
			result = getModelService().testResource(dto.getOid(), createSimpleTask(TEST_RESOURCE));
			ResourceController.updateResourceState(dto.getState(), result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Fail to test resource connection", ex);
		}

		if (result == null) {
			result = new OperationResult(TEST_RESOURCE);
		}

		if (!result.isSuccess()) {
			showResult(result);
			target.add(getFeedbackPanel());
		}

	}
}
