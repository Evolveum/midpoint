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

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.resources.component.ContentPanel;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentEntitlements;
import com.evolveum.midpoint.web.page.admin.resources.dto.*;
import com.evolveum.midpoint.web.page.admin.users.dto.UserListItemDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class PageResources extends PageAdminResources {

    private static final Trace LOGGER = TraceManager.getTrace(PageResources.class);
    private static final String DOT_CLASS = PageResources.class.getName() + ".";
    private static final String OPERATION_TEST_RESOURCE = DOT_CLASS + "testResource";
    private static final String OPERATION_SYNC_STATUS = DOT_CLASS + "syncStatus";
    private static final String OPERATION_DELETE_RESOURCES = DOT_CLASS + "deleteResources";
    private static final String OPERATION_DELETE_HOSTS = DOT_CLASS + "deleteHosts";
    private static final String OPERATION_CONNECTOR_DISCOVERY = DOT_CLASS + "connectorDiscovery";

    private static final String ID_DELETE_HOST = "deleteHost";
    private static final String ID_DELETE_RESOURCES_POPUP = "deleteResourcesPopup";
    private static final String ID_DELETE_HOSTS_POPUP = "deleteHostsPopup";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";
    private static final String ID_CONNECTOR_TABLE = "connectorTable";

    public PageResources() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        TablePanel resources = new TablePanel<ResourceDto>(ID_TABLE, initResourceDataProvider(), initResourceColumns());
        resources.setOutputMarkupId(true);
        mainForm.add(resources);

        TablePanel connectorHosts = new TablePanel<ConnectorHostType>(ID_CONNECTOR_TABLE,
                new ObjectDataProvider(PageResources.this, ConnectorHostType.class), initConnectorHostsColumns());
        connectorHosts.setShowPaging(false);
        connectorHosts.setOutputMarkupId(true);
        mainForm.add(connectorHosts);

        initButtons(mainForm);

        add(new ConfirmationDialog(ID_DELETE_RESOURCES_POPUP,
                createStringResource("pageResources.dialog.title.confirmDeleteResource"),
                createDeleteConfirmString("pageResources.message.deleteResourceConfirm",
                        "pageResources.message.deleteResourcesConfirm", true)) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteResourceConfirmedPerformed(target);
            }
        });

        add(new ConfirmationDialog(ID_DELETE_HOSTS_POPUP,
                createStringResource("pageResources.dialog.title.confirmDelete"),
                createDeleteConfirmString("pageResources.message.deleteHostConfirm",
                        "pageResources.message.deleteHostsConfirm", false)) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteHostConfirmedPerformed(target);
            }
        });
    }

    private BaseSortableDataProvider initResourceDataProvider() {
        ObjectDataProvider provider = new ObjectDataProvider<ResourceDto, ResourceType>(this, ResourceType.class) {

            @Override
            public ResourceDto createDataObjectWrapper(PrismObject<ResourceType> obj) {
                return createRowDto(obj);
            }
        };

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(ResourceType.F_CONNECTOR, GetOperationOptions.createResolve());
        provider.setOptions(options);

        return provider;
    }

    private ResourceDto createRowDto(PrismObject<ResourceType> object) {
        ResourceDto dto =  new ResourceDto(object);
        dto.getMenuItems().add(new InlineMenuItem(createStringResource("PageBase.button.delete"),
                new ColumnMenuAction<ResourceDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ResourceDto rowDto = getRowModel().getObject();
                        deleteResourcePerformed(target, rowDto);
                    }
                }));

        return dto;
    }

    private void initButtons(Form mainForm) {
        AjaxLinkButton deleteHost = new AjaxLinkButton(ID_DELETE_HOST, ButtonType.NEGATIVE,
                createStringResource("PageBase.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteHostPerformed(target);
            }
        };
        mainForm.add(deleteHost);

        AjaxLinkButton discoveryRemote = new AjaxLinkButton("discoveryRemote",
                createStringResource("pageResources.button.discoveryRemote")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                discoveryRemotePerformed(target);
            }
        };
        mainForm.add(discoveryRemote);
    }

    private List<IColumn<ResourceDto, String>> initResourceColumns() {
        List<IColumn<ResourceDto, String>> columns = new ArrayList<IColumn<ResourceDto, String>>();

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

        columns.add(new AbstractColumn<ResourceDto, String>(createStringResource("pageResources.content")) {

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

        InlineMenuHeaderColumn menu = new InlineMenuHeaderColumn(initInlineMenu());
        columns.add(menu);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete"),
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteResourcePerformed(target, null);
                    }
                }));

        return headerMenuItems;
    }

    private List<IColumn<ConnectorHostType, String>> initConnectorHostsColumns() {
        List<IColumn<ConnectorHostType, String>> columns = new ArrayList<IColumn<ConnectorHostType, String>>();

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

    private void deleteHostPerformed(AjaxRequestTarget target) {
        List<SelectableBean<ConnectorHostType>> selected = WebMiscUtil.getSelectedData(getConnectorHostTable());
        if (selected.isEmpty()) {
            warn(getString("pageResources.message.noHostSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        ModalWindow dialog = (ModalWindow) get(ID_DELETE_HOSTS_POPUP);
        dialog.show(target);
    }

    private List<ResourceDto> isAnyResourceSelected(AjaxRequestTarget target, ResourceDto single) {
        return WebMiscUtil.isAnythingSelected(target, single, getResourceTable(), this,
                "pageResources.message.noResourceSelected");
    }

    private void deleteResourcePerformed(AjaxRequestTarget target, ResourceDto single) {
        List<ResourceDto> selected = isAnyResourceSelected(target, single);
        if (selected.isEmpty()) {
            return;
        }

        ModalWindow dialog = (ModalWindow) get(ID_DELETE_RESOURCES_POPUP);
        dialog.show(target);
    }

    private TablePanel getResourceTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private TablePanel getConnectorHostTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_CONNECTOR_TABLE));
    }

    /**
     * @param oneDeleteKey  message if deleting one item
     * @param moreDeleteKey message if deleting more items
     * @param resources     if true selecting resources if false selecting from hosts
     */
    private IModel<String> createDeleteConfirmString(final String oneDeleteKey, final String moreDeleteKey,
                                                     final boolean resources) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                TablePanel table = resources ? getResourceTable() : getConnectorHostTable();
                List selected = WebMiscUtil.getSelectedData(table);
                switch (selected.size()) {
                    case 1:
                        Object first = selected.get(0);
                        String name = resources ? ((ResourceDto) first).getName() :
                                WebMiscUtil.getName(((SelectableBean<ConnectorHostType>) first).getValue());
                        return createStringResource(oneDeleteKey, name).getString();
                    default:
                        return createStringResource(moreDeleteKey, selected.size()).getString();
                }
            }
        };
    }

    private void deleteHostConfirmedPerformed(AjaxRequestTarget target) {
        TablePanel hostTable = getConnectorHostTable();
        List<SelectableBean<ConnectorHostType>> selected = WebMiscUtil.getSelectedData(hostTable);

        OperationResult result = new OperationResult(OPERATION_DELETE_HOSTS);
        for (SelectableBean<ConnectorHostType> selectable : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_HOSTS);

                ObjectDelta<ConnectorHostType> delta = ObjectDelta.createDeleteDelta(ConnectorHostType.class,
                        selectable.getValue().getOid(), getPrismContext());
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            } catch (Exception ex) {
                result.recordPartialError("Couldn't delete host.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete host", ex);
            }
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The resource(s) have been successfully deleted.");
        }

        BaseSortableDataProvider provider = (BaseSortableDataProvider) hostTable.getDataTable().getDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getFeedbackPanel(), hostTable);
    }

    private void deleteResourceConfirmedPerformed(AjaxRequestTarget target) {
        List<ResourceDto> selected = isAnyResourceSelected(target, null);

        OperationResult result = new OperationResult(OPERATION_DELETE_RESOURCES);
        for (ResourceDto resource : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_RESOURCES);

                ObjectDelta<ResourceType> delta = ObjectDelta.createDeleteDelta(ResourceType.class, resource.getOid(),
                        getPrismContext());
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            } catch (Exception ex) {
                result.recordPartialError("Couldn't delete resource.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete resource", ex);
            }
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The resource(s) have been successfully deleted.");
        }

        TablePanel resourceTable = getResourceTable();
        ResourceDtoProvider provider = (ResourceDtoProvider) resourceTable.getDataTable().getDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getFeedbackPanel(), resourceTable);
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
                result.recordFatalError("Fail to discover connectors on host '" + host.getHostname()
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
            result = getModelService().testResource(dto.getOid(), createSimpleTask(OPERATION_TEST_RESOURCE));
            ResourceController.updateResourceState(dto.getState(), result);
        } catch (ObjectNotFoundException ex) {
            result.recordFatalError("Fail to test resource connection", ex);
        }

        if (result == null) {
            result = new OperationResult(OPERATION_TEST_RESOURCE);
        }

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        }

    }
}
