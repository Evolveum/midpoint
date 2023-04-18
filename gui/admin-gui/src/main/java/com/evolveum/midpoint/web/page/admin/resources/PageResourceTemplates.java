/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.impl.error.ErrorPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
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

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/resourceTemplates", matchUrlForSecurity = "/admin/resourceTemplates")
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
public class PageResourceTemplates extends PageAdmin {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageResourceTemplates.class);
    private static final String DOT_CLASS = PageResourceTemplates.class.getName() + ".";
    private static final String OPERATION_DELETE_RESOURCES = DOT_CLASS + "deleteResources";
    private static final String OPERATION_SET_MAINTENANCE = DOT_CLASS + "setMaintenance";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private ResourceType singleDelete;

    public PageResourceTemplates() {
        this(null);
    }

    public PageResourceTemplates(PageParameters params) {
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

        if (!isNativeRepo()) {
            mainForm.add(new ErrorPanel(ID_TABLE,
                    () -> getString("PageAdmin.menu.top.resources.templates.list.nonNativeRepositoryWarning")));
            return;
        }
        MainObjectListPanel<ResourceType> table = new MainObjectListPanel<>(ID_TABLE, ResourceType.class, getQueryOptions()) {

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ResourceType object) {
                clearSessionStorageForResourcePage();
                super.objectDetailsPerformed(target, object);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ResourceType>> createProvider() {
                SelectableBeanObjectDataProvider<ResourceType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_RESOURCE_TEMPLATES;
            }

            @Override
            protected List<IColumn<SelectableBean<ResourceType>, String>> createDefaultColumns() {
                return PageResourceTemplates.this.initResourceColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return PageResourceTemplates.this.createRowMenuItems();
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

        table.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES);

    }

    private ObjectQuery getCustomizeContentQuery() {
        return getPrismContext().queryFor(ResourceType.class)
                .item(ResourceType.F_TEMPLATE).eq(true).or()
                .item(ResourceType.F_ABSTRACT).eq(true)
                .build();
    }

    private Collection<SelectorOptions<GetOperationOptions>> getQueryOptions() {
        return getOperationOptionsBuilder()
                .noFetch()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();
    }

    private List<InlineMenuItem> createRowMenuItems() {

        List<InlineMenuItem> menuItems = new ArrayList<>();

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
                        WebComponentUtil.toggleResourceMaintenance(rowDto.getValue().asPrismContainer(),
                                OPERATION_SET_MAINTENANCE, target, PageResourceTemplates.this);
                        target.add(getResourceTable());
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

        columns.add(new PropertyColumn<>(createStringResource("pageResources.template"),
                SelectableBeanImpl.F_VALUE + "." + ResourceType.F_TEMPLATE.getLocalPart()));
        columns.add(new PropertyColumn<>(createStringResource("pageResources.abstract"),
                SelectableBeanImpl.F_VALUE + "." + ResourceType.F_ABSTRACT.getLocalPart()) {
        });
        columns.add(new PropertyColumn<>(createStringResource("pageResources.connectorType"),
                SelectableBeanImpl.F_VALUE + ".connectorRef.objectable.connectorType"));
        columns.add(new PropertyColumn<>(createStringResource("pageResources.version"),
                SelectableBeanImpl.F_VALUE + ".connectorRef.objectable.connectorVersion"));
        return columns;
    }

    private List<ResourceType> isAnyResourceSelected(ResourceType single) {
        return single != null
                ? Collections.singletonList(single)
                : getResourceTable().getSelectedRealObjects();
    }

    private void deleteResourcePerformed(AjaxRequestTarget target, ResourceType single) {
        List<ResourceType> selected = isAnyResourceSelected(single);
        if (selected.size() < 1) {
            warn(createStringResource("pageResources.message.noResourceSelected").getString());
            target.add(getFeedbackPanel());
            return;
        }

        singleDelete = single;

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
        return () -> {
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

    private void deleteResourceSyncTokenPerformed(AjaxRequestTarget target, ResourceType resourceType) {
        WebComponentUtil.deleteSyncTokenPerformed(target, resourceType, PageResourceTemplates.this);
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
