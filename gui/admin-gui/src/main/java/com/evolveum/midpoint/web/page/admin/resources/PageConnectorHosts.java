/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.impl.component.search.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
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
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/connectorHosts")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_HOSTS_ALL_URL, label = "PageAdminResources.auth.connectorHostsAll.label", description = "PageAdminResources.auth.connectorHostsAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_HOSTS_ALL_URL, label = "PageResources.auth.connectorHosts.label", description = "PageResources.auth.connectorHosts.description") })
public class PageConnectorHosts extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageConnectorHosts.class);
    private static final String DOT_CLASS = PageConnectorHosts.class.getName() + ".";
    private static final String OPERATION_DELETE_HOSTS = DOT_CLASS + "deleteHosts";
    private static final String OPERATION_CONNECTOR_DISCOVERY = DOT_CLASS + "connectorDiscovery";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private IModel<Search> searchModel;

    public PageConnectorHosts() {
        this(true);
    }

    public PageConnectorHosts(boolean clearSessionPaging) {
        this(clearSessionPaging, "");
    }

    public PageConnectorHosts(String searchText) {
        this(true, searchText);
    }

    public PageConnectorHosts(boolean clearSessionPaging, String searchText) {

        searchModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Search load() {
                return new SearchBuilder(ConnectorHostType.class).modelServiceLocator(PageConnectorHosts.this).build();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ConnectorHostType> table = new MainObjectListPanel<ConnectorHostType>(ID_TABLE, ConnectorHostType.class, getQueryOptions()) {
            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ConnectorHostType host) {
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_RESOURCES_CONNECTOR_HOSTS;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected List<IColumn<SelectableBean<ConnectorHostType>, String>> createDefaultColumns() {
                return PageConnectorHosts.this.initColumns();
            }

            @Override
            protected boolean isCreateNewObjectEnabled() {
                return false;
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<ConnectorHostType>> rowModel) {
                return false;
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private List<IColumn<SelectableBean<ConnectorHostType>, String>> initColumns() {
        List<IColumn<SelectableBean<ConnectorHostType>, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn(createStringResource("pageResources.connector.hostname"),
                "value.hostname"));
        columns.add(new PropertyColumn(createStringResource("pageResources.connector.port"), "value.port"));
        columns.add(
                new PropertyColumn(createStringResource("pageResources.connector.timeout"), "value.timeout"));
        columns.add(new CheckBoxColumn(createStringResource("pageResources.connector.protectConnection"),
                "value.protectConnection"));

        return columns;
    }

    private List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageConnectorHosts.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteHostPerformed(target);
                    }
                };
            }
        });
        headerMenuItems.add(new InlineMenuItem(createStringResource("pageResources.button.discoveryRemote")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageConnectorHosts.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        discoveryRemotePerformed(target);
                    }
                };
            }
        });

        return headerMenuItems;
    }

    private void deleteHostPerformed(AjaxRequestTarget target) {
        List<ConnectorHostType> selected = getObjectListPanel().getSelectedRealObjects();
        if (selected.isEmpty()) {
            warn(getString("pageResources.message.noHostSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        ConfirmationPanel dialog = new DeleteConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                createDeleteConfirmString("pageResources.message.deleteHostConfirm",
                        "pageResources.message.deleteHostsConfirm", false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteHostConfirmedPerformed(target);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);

    }

    private MainObjectListPanel<ConnectorHostType> getObjectListPanel() {
        return (MainObjectListPanel<ConnectorHostType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
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
            private static final long serialVersionUID = 1L;
            @Override
            public String getObject() {
                List<ConnectorHostType> selected = getObjectListPanel().getSelectedRealObjects();

                switch (selected.size()) {
                    case 1:
                        ConnectorHostType first = selected.get(0);
                        String name = WebComponentUtil
                                .getName(first);
                        return createStringResource(oneDeleteKey, name).getString();
                    default:
                        return createStringResource(moreDeleteKey, selected.size()).getString();
                }
            }
        };
    }

    private void deleteHostConfirmedPerformed(AjaxRequestTarget target) {
        List<ConnectorHostType> selected = getObjectListPanel().getSelectedRealObjects();

        OperationResult result = new OperationResult(OPERATION_DELETE_HOSTS);
        for (ConnectorHostType selectable : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_HOSTS);

                if (selectable != null) {
                    ObjectDelta<ConnectorHostType> delta = getPrismContext().deltaFactory().object().createDeleteDelta(ConnectorHostType.class,
                            selectable.getOid());
                    getModelService().executeChanges(MiscUtil.createCollection(delta), null, task,
                            result);
                }
            } catch (Exception ex) {
                result.recordPartialError(createStringResource("PageConnectorHosts.message.deleteHostConfirmedPerformed.partialError").getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete host", ex);
            }
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("PageConnectorHosts.message.deleteHostConfirmedPerformed.success").getString());
        }

        getObjectListPanel().clearCache();

        showResult(result);
        target.add(getFeedbackPanel(), getObjectListPanel());
    }

    private void discoveryRemotePerformed(AjaxRequestTarget target) {
        target.add(getFeedbackPanel());

        PageBase page = (PageBase) getPage();
        Task task = page.createSimpleTask(OPERATION_CONNECTOR_DISCOVERY);
        OperationResult result = task.getResult();
        List<ConnectorHostType> selected = getObjectListPanel().getSelectedRealObjects();
        if (selected.isEmpty()) {
            warn(getString("pageResources.message.noHostSelected"));
            return;
        }

        for (ConnectorHostType host : selected) {
            try {
                getModelService().discoverConnectors(host, task, result);
            } catch (Exception ex) {
                result.recordFatalError(createStringResource("PageConnectorHosts.message.discoveryRemotePerformed.fatalError", host.getHostname(), host.getPort()).getString(), ex);
            }
        }

        result.recomputeStatus();
        showResult(result);
    }

    private Collection<SelectorOptions<GetOperationOptions>> getQueryOptions(){
        return SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
    }
}
