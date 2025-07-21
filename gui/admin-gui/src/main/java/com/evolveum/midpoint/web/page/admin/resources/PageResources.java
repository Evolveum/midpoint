/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateBadgeColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart.PageSmartIntegrationDefiningTypes;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.prism.PrismObjectDefinition;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

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
@CollectionInstance(identifier = "allResources", applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.resources.list", singularLabel = "ObjectType.resource", icon = GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON))
public class PageResources extends PageAdmin {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageResources.class);
    private static final String DOT_CLASS = PageResources.class.getName() + ".";
    private static final String OPERATION_TEST_RESOURCE = DOT_CLASS + "testResource";
    private static final String OPERATION_DELETE_RESOURCES = DOT_CLASS + "deleteResources";
    private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";
    private static final String OPERATION_SET_MAINTENANCE = DOT_CLASS + "setMaintenance";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private ResourceType singleDelete;

    public PageResources() {
        this(null);
    }

    public PageResources(PageParameters params) {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ResourceType> table = new MainObjectListPanel<>(ID_TABLE, ResourceType.class) {

            @Override
            protected void objectDetailsPerformed(ResourceType object) {
                clearSessionStorageForResourcePage();
                super.objectDetailsPerformed(object);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ResourceType>> createProvider() {
                if (isNativeRepo() && getPageParameters().isEmpty()) {
                    SelectableBeanObjectDataProvider<ResourceType> provider = createSelectableBeanObjectDataProvider(() ->
                            getCustomizeContentQuery(), null, getQueryOptions());
                    provider.setEmptyListOnNullQuery(true);
                    provider.setSort(ResourceType.F_NAME.getLocalPart(), SortOrder.ASCENDING);
                    provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                    return provider;
                } else {
                    return createSelectableBeanObjectDataProvider(null, null, getQueryOptions());
                }
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_RESOURCES;
            }

            @Override
            protected List<IColumn<SelectableBean<ResourceType>, String>> createDefaultColumns() {
                return PageResources.this.initResourceColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return PageResources.this.createRowMenuItems();
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

        table.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES);

    }

    protected ObjectQuery getCustomizeContentQuery() {
        return getPrismContext().queryFor(ResourceType.class)
                .not().item(ResourceType.F_TEMPLATE).eq(true)
                .and()
                .not().item(ResourceType.F_ABSTRACT).eq(true)
                .build();
    }

    private Collection<SelectorOptions<GetOperationOptions>> getQueryOptions() {
        return getOperationOptionsBuilder()
                .noFetch()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();
    }

    private ButtonInlineMenuItem.VisibilityChecker isInlineButtonVisible() {
        return (rowModel, isHeader) -> {
            boolean templateCategory = false;
            SelectableBean<ResourceType> object = (SelectableBean<ResourceType>) rowModel.getObject();
            if (object != null && object.getValue() != null) {
                templateCategory = !WebComponentUtil.isTemplateCategory(object.getValue());
            }
            return templateCategory;
        };
    }

    private List<InlineMenuItem> createRowMenuItems() {

        List<InlineMenuItem> menuItems = new ArrayList<>();

        ButtonInlineMenuItem buttonInlineMenuItem = new ButtonInlineMenuItem(createStringResource("PageResources.inlineMenuItem.test")) {
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
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_TEST_CONNECTION_MENU_ITEM);
            }
        };

        buttonInlineMenuItem.setVisibilityChecker(isInlineButtonVisible());
        menuItems.add(buttonInlineMenuItem);

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
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }
        });

        InlineMenuItem inlineMenuItem = new InlineMenuItem(createStringResource("pageResource.button.refreshSchema")) {
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
            public boolean isHeaderMenuItem() {
                return false;
            }
        };

        inlineMenuItem.setVisibilityChecker(isInlineButtonVisible());
        menuItems.add(inlineMenuItem);

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
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
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
            public boolean isHeaderMenuItem() {
                return false;
            }

        });

        menuItems.add(new InlineMenuItem(createStringResource("pageResources.inlineMenuItem.toggleMaintenance")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ResourceType> rowDto = getRowModel().getObject();
                        ProvisioningObjectsUtil.toggleResourceMaintenance(rowDto.getValue().asPrismContainer(),
                                OPERATION_SET_MAINTENANCE, target, PageResources.this);
                        target.add(getResourceTable());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });

        menuItems.add(new InlineMenuItem(createStringResource("pageResources.inlineMenuItem.smartIntegration")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ResourceType>>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageSmartIntegrationDefiningTypes.navigateTo(
                                PageResources.this, getRowModel().getObject().getValue().getOid());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });

        return menuItems;
    }

    private List<IColumn<SelectableBean<ResourceType>, String>> initResourceColumns() {
        List<IColumn<SelectableBean<ResourceType>, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(createStringResource("pageResources.connectorType"),
                SelectableBeanImpl.F_VALUE + ".connectorRef.objectable.connectorType"));
        columns.add(new PropertyColumn<>(createStringResource("pageResources.version"),
                SelectableBeanImpl.F_VALUE + ".connectorRef.objectable.connectorVersion"));

        IModel<PrismContainerDefinition<ResourceType>> def =
                () -> PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
        columns.add(new LifecycleStateBadgeColumn<>(def, PageResources.this));

        return columns;
    }

    private List<ResourceType> isAnyResourceSelected(ResourceType single) {
        return single != null
                ? Collections.singletonList(single)
                : getResourceTable().getSelectedRealObjects();

    }

    private void refreshSchemaPerformed(ResourceType resource, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                createStringResource("pageResources.message.refreshResourceSchemaConfirm")) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                ProvisioningObjectsUtil.refreshResourceSchema(resource.asPrismObject(),
                        OPERATION_REFRESH_SCHEMA, target, PageResources.this);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);
    }

    private void deleteResourcePerformed(AjaxRequestTarget target, ResourceType single) {
        List<ResourceType> selected = isAnyResourceSelected(single);
        if (selected.size() < 1) {
            warn(createStringResource("pageResources.message.noResourceSelected").getString());
            target.add(getFeedbackPanel());
            return;
        }

        singleDelete = single;

        if (selected.isEmpty()) {
            return;
        }

        ConfirmationPanel dialog = new DeleteConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                createDeleteConfirmString("pageResources.message.deleteResourceConfirm",
                        "pageResources.message.deleteResourcesConfirm")) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteResourceConfirmedPerformed(target);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);
    }

    private MainObjectListPanel<ResourceType> getResourceTable() {
        return (MainObjectListPanel<ResourceType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    /**
     * @param oneDeleteKey message if deleting one item
     * @param moreDeleteKey message if deleting more items
     */
    private IModel<String> createDeleteConfirmString(
            final String oneDeleteKey, final String moreDeleteKey) {
        return new IModel<String>() {

            @Override
            public String getObject() {
                List<ResourceType> selected = new ArrayList<>();
                if (singleDelete != null) {
                    selected.add(singleDelete);
                } else {
                    selected = getResourceTable().getSelectedRealObjects();
                }

                if (selected.size() == 1) {
                    String name = WebComponentUtil.getName(selected.get(0));
                    return createStringResource(oneDeleteKey, name).getString();
                }
                return createStringResource(moreDeleteKey, selected.size()).getString();
            }
        };
    }

    private void deleteResourceConfirmedPerformed(AjaxRequestTarget target) {
        List<ResourceType> selected = new ArrayList<>();

        if (singleDelete != null) {
            selected.add(singleDelete);
        } else {
            selected = getResourceTable().getSelectedRealObjects();
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
                result.recordPartialError(createStringResource(
                        "PageResources.message.deleteResourceConfirmedPerformed.partialError").getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete resource", ex);
            }
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, createStringResource(
                    "PageResources.message.deleteResourceConfirmedPerformed.success").getString());
        }

        getResourceTable().clearCache();

        showResult(result);
        target.add(getFeedbackPanel(), getResourceTable());
    }

    private void testResourcePerformed(AjaxRequestTarget target, ResourceType resourceType) {

        OperationResult result = new OperationResult(OPERATION_TEST_RESOURCE);

        if (StringUtils.isEmpty(resourceType.getOid())) {
            result.recordFatalError(createStringResource(
                    "PageResources.message.testResourcePerformed.partialError").getString());
        }

        Task task = createSimpleTask(OPERATION_TEST_RESOURCE);
        try {
            getModelService().testResource(resourceType.getOid(), task, result);
            // todo de-duplicate code (see the same operation in PageResource)
            // this provides some additional tests, namely a test for schema
            // handling section
            getModelService().getObject(ResourceType.class, resourceType.getOid(), null, task, result);
        } catch (Exception ex) {
            result.recordFatalError(createStringResource(
                    "PageResources.message.testResourcePerformed.fatalError").getString(), ex);
        }

        result.close();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getResourceTable());
    }

    private void deleteResourceSyncTokenPerformed(AjaxRequestTarget target, ResourceType resourceType) {
        WebComponentUtil.deleteSyncTokenPerformed(target, resourceType, PageResources.this);
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

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }

}
