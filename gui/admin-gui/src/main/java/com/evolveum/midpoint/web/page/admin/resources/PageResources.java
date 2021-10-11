/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/resources", matchUrlForSecurity = "/admin/resources")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                        label = "PageAdminResources.auth.resourcesAll.label",
                        description = "PageAdminResources.auth.resourcesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                        label = "PageResources.auth.resources.label",
                        description = "PageResources.auth.resources.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_VIEW_URL,
                        label = "PageResources.auth.resources.view.label",
                        description = "PageResources.auth.resources.view.description")
        })
public class PageResources extends PageAdminObjectList<ResourceType> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageResources.class);
    private static final String DOT_CLASS = PageResources.class.getName() + ".";
    private static final String OPERATION_TEST_RESOURCE = DOT_CLASS + "testResource";
    private static final String OPERATION_DELETE_RESOURCES = DOT_CLASS + "deleteResources";
    private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private ResourceType singleDelete;

    public PageResources() {
        this(null);
    }

    public PageResources(PageParameters params) {
        super();
    }

    protected void initLayout(){
        super.initLayout();
        getObjectListPanel().setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES);

    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        return PageResources.this.createRowMenuItems();
    }

    @Override
    protected List<IColumn<SelectableBean<ResourceType>, String>> initColumns() {
        return PageResources.this.initResourceColumns();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, ResourceType object) {
        PageResources.this.resourceDetailsPerformed(target, object.getOid());

    }

    @Override
    protected Class<ResourceType> getType(){
        return ResourceType.class;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getQueryOptions() {
        return getOperationOptionsBuilder()
                .noFetch()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_RESOURCES;
    }

    private List<InlineMenuItem> createRowMenuItems() {

        List<InlineMenuItem> menuItems = new ArrayList<>();

        menuItems.add(new ButtonInlineMenuItem(createStringResource("PageResources.inlineMenuItem.test")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ResourceType> rowDto = getRowModel().getObject();
                        testResourcePerformed(target, rowDto.getValue());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_TEST_CONNECTION_MENU_ITEM;
            }
        });

        menuItems.add(new ButtonInlineMenuItem(createStringResource("pageResources.button.editAsXml")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ResourceType> rowDto = getRowModel().getObject();
                        editAsXmlPerformed(rowDto.getValue());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_EDIT_MENU_ITEM;
            }
        });

        menuItems.add(new InlineMenuItem(createStringResource("pageResources.inlineMenuItem.editResource")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ResourceType> rowDto = getRowModel().getObject();
                        editResourcePerformed(rowDto.getValue());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }
        });

        menuItems.add(new InlineMenuItem(createStringResource("pageResource.button.refreshSchema")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ResourceType> rowDto = getRowModel().getObject();
                        refreshSchemaPerformed(rowDto.getValue(), target);
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }
        });

        menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            deleteResourcePerformed(target, null);
                        } else {
                            SelectableBeanImpl<ResourceType> rowDto = getRowModel().getObject();
                            deleteResourcePerformed(target, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
            }
        });

        menuItems.add(new InlineMenuItem(createStringResource("pageResources.inlineMenuItem.deleteSyncToken")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ResourceType> rowDto = getRowModel().getObject();
                        deleteResourceSyncTokenPerformed(target, rowDto.getValue());
                    }

                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }

        });

        return menuItems;
    }

    private List<IColumn<SelectableBean<ResourceType>, String>> initResourceColumns() {
        List<IColumn<SelectableBean<ResourceType>, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn(createStringResource("pageResources.connectorType"),
                SelectableBeanImpl.F_VALUE + ".connectorRef.objectable.connectorType"));
        columns.add(new PropertyColumn(createStringResource("pageResources.version"),
                SelectableBeanImpl.F_VALUE + ".connectorRef.objectable.connectorVersion"));

        return columns;
    }

    private void resourceDetailsPerformed(AjaxRequestTarget target, String oid) {
        clearSessionStorageForResourcePage();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageResource.class, parameters);
    }

    private List<ResourceType> isAnyResourceSelected(AjaxRequestTarget target, ResourceType single) {
        List<ResourceType> selected = null;
        if (single != null) {
            selected = new ArrayList<>(1);
            selected.add(single);
            return selected;
        }
        selected = getResourceTable().getSelectedObjects();
        return selected;

    }

    private void refreshSchemaPerformed(ResourceType resource, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(((PageBase)getPage()).getMainPopupBodyId(),
                createStringResource("pageResources.message.refreshResourceSchemaConfirm")){
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                WebComponentUtil.refreshResourceSchema(resource.asPrismObject(), OPERATION_REFRESH_SCHEMA, target, PageResources.this);
            }
        };
        ((PageBase)getPage()).showMainPopup(dialog, target);
    }

    private void deleteResourcePerformed(AjaxRequestTarget target, ResourceType single) {
        List<ResourceType> selected = isAnyResourceSelected(target, single);
        if (selected.size() < 1) {
            warn(createStringResource("pageResources.message.noResourceSelected").getString());
            target.add(getFeedbackPanel());
            return;
        }

        singleDelete = single;

        if (selected.isEmpty()) {
            return;
        }

        ConfirmationPanel dialog = new ConfirmationPanel(((PageBase)getPage()).getMainPopupBodyId(),
                createDeleteConfirmString("pageResources.message.deleteResourceConfirm",
                        "pageResources.message.deleteResourcesConfirm", true)){
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteResourceConfirmedPerformed(target);
            }
        };
        ((PageBase)getPage()).showMainPopup(dialog, target);
    }

    private MainObjectListPanel<ResourceType> getResourceTable() {
        return (MainObjectListPanel<ResourceType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    /**
     * @param oneDeleteKey
     *            message if deleting one item
     * @param moreDeleteKey
     *            message if deleting more items
     * @param resources
     *            if true selecting resources if false selecting from hosts
     */
    private IModel<String> createDeleteConfirmString(final String oneDeleteKey, final String moreDeleteKey,
            final boolean resources) {
        return new IModel<String>() {

            @Override
            public String getObject() {
                List selected = new ArrayList();
                    if (singleDelete != null) {
                        selected.add(singleDelete);
                    } else {
                        selected = getResourceTable().getSelectedObjects();
                    }

                switch (selected.size()) {
                    case 1:
                        Object first = selected.get(0);
                        String name = WebComponentUtil.getName(((ResourceType) first));
                        return createStringResource(oneDeleteKey, name).getString();
                    default:
                        return createStringResource(moreDeleteKey, selected.size()).getString();
                }
            }
        };
    }

    private void deleteResourceConfirmedPerformed(AjaxRequestTarget target) {
        List<ResourceType> selected = new ArrayList<>();

        if (singleDelete != null) {
            selected.add(singleDelete);
        } else {
            selected = getResourceTable().getSelectedObjects();
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_RESOURCES);
        for (ResourceType resource : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_RESOURCES);

                ObjectDelta<ResourceType> delta = getPrismContext().deltaFactory().object().createDeleteDelta(ResourceType.class,
                        resource.getOid());
                getModelService().executeChanges(MiscUtil.createCollection(delta), null, task,
                        result);
            } catch (Exception ex) {
                result.recordPartialError(createStringResource("PageResources.message.deleteResourceConfirmedPerformed.partialError").getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete resource", ex);
            }
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("PageResources.message.deleteResourceConfirmedPerformed.success").getString());
        }

        getResourceTable().clearCache();

        showResult(result);
        target.add(getFeedbackPanel(), (Component) getResourceTable());
    }

    private void testResourcePerformed(AjaxRequestTarget target, ResourceType resourceType) {

        OperationResult result = new OperationResult(OPERATION_TEST_RESOURCE);

        if (StringUtils.isEmpty(resourceType.getOid())) {
            result.recordFatalError(createStringResource("PageResources.message.testResourcePerformed.partialError").getString());
        }

        Task task = createSimpleTask(OPERATION_TEST_RESOURCE);
        try {
            result = getModelService().testResource(resourceType.getOid(), task);
            // todo de-duplicate code (see the same operation in PageResource)
            // this provides some additional tests, namely a test for schema
            // handling section
            getModelService().getObject(ResourceType.class, resourceType.getOid(), null, task, result);
        } catch (Exception ex) {
            result.recordFatalError(createStringResource("PageResources.message.testResourcePerformed.fatalError").getString(), ex);
        }

        // a bit of hack: result of TestConnection contains a result of
        // getObject as a subresult
        // so in case of TestConnection succeeding we recompute the result to
        // show any (potential) getObject problems
        if (result.isSuccess()) {
            result.recomputeStatus();
        }

        // if (!result.isSuccess()) {
        showResult(result);
        target.add(getFeedbackPanel());
        // }
        target.add(getResourceTable());
    }


    private void deleteResourceSyncTokenPerformed(AjaxRequestTarget target, ResourceType resourceType) {
        WebComponentUtil.deleteSyncTokenPerformed(target, resourceType, PageResources.this);
    }

    private void editResourcePerformed(ResourceType resourceType) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, resourceType.getOid());
        navigateToNext(new PageResourceWizard(parameters));
    }

    private void editAsXmlPerformed(ResourceType resourceType) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, resourceType.getOid());
        parameters.add(PageDebugView.PARAM_OBJECT_TYPE, ResourceType.class.getSimpleName());
        navigateToNext(PageDebugView.class, parameters);
    }

    private void clearSessionStorageForResourcePage() {
        ((PageBase) getPage()).getSessionStorage().clearResourceContentStorage();
    }

}
