/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

/**
 * TEMPORARY, UNFINISHED, just a placeholder
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/connector")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTORS_ALL_URL, label = "PageAdminResources.auth.connectorsAll.label", description = "PageAdminResources.auth.connectorsAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTORS_ALL_URL, label = "PageResources.auth.connectors.label", description = "PageResources.auth.connectors.description") })
public class PageConnectors extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageConnectors.class);
    private static final String DOT_CLASS = PageConnectors.class.getName() + ".";

    private static final String OPERATION_DELETE_CONNECTORS = DOT_CLASS + "deleteConnectors";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private IModel<Search> searchModel;

    public PageConnectors() {
        this(true);
    }

    public PageConnectors(boolean clearSessionPaging) {
        this(clearSessionPaging, "");
    }

    public PageConnectors(String searchText) {
        this(true, searchText);
    }

    public PageConnectors(boolean clearSessionPaging, String searchText) {

        searchModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Search load() {
                return new SearchBuilder(ConnectorType.class).modelServiceLocator(PageConnectors.this).build();
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

        MainObjectListPanel<ConnectorType> table = new MainObjectListPanel<>(ID_TABLE, ConnectorType.class) {
            @Override
            protected void objectDetailsPerformed(ConnectorType host) {
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_RESOURCES_CONNECTOR_HOSTS; // FIXME
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected List<IColumn<SelectableBean<ConnectorType>, String>> createDefaultColumns() {
                return PageConnectors.this.initColumns();
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<ConnectorType>> rowModel) {
                return false;
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ConnectorType>> createProvider() {
                return createSelectableBeanObjectDataProvider(null, null, getQueryOptions());
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private List<IColumn<SelectableBean<ConnectorType>, String>> initColumns() {
        List<IColumn<SelectableBean<ConnectorType>, String>> columns = new ArrayList<>();

        // TODO i18n
        columns.add(new PropertyColumn(createStringResource("Display name"), "value.displayName"));
        columns.add(
                new PropertyColumn(createStringResource("Type"), "value.connectorType"));
        columns.add(new PropertyColumn(createStringResource("Version"), "value.connectorVersion"));
        //columns.add(new PropertyColumn(createStringResource("Bundle"), "value.connectorBundle"));

        return columns;
    }

    private List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageConnectors.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteConnectorPerformed(target);
                    }
                };
            }
        });
        return headerMenuItems;
    }

    private void deleteConnectorPerformed(AjaxRequestTarget target) {
        List<ConnectorType> selected = getObjectListPanel().getSelectedRealObjects();
        if (selected.isEmpty()) {
            warn(getString("pageResources.message.noHostSelected")); // FIXME
            target.add(getFeedbackPanel());
            return;
        }

        ConfirmationPanel dialog = new DeleteConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                createDeleteConfirmString("pageResources.message.deleteHostConfirm", // FIXME
                        "pageResources.message.deleteHostsConfirm", false)) { // FIXME
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteConnectorConfirmedPerformed(target);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);

    }

    private MainObjectListPanel<ConnectorType> getObjectListPanel() {
        return (MainObjectListPanel<ConnectorType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
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
                List<ConnectorType> selected = getObjectListPanel().getSelectedRealObjects();

                switch (selected.size()) {
                    case 1:
                        ConnectorType first = selected.get(0);
                        String name = WebComponentUtil
                                .getName(first);
                        return createStringResource(oneDeleteKey, name).getString();
                    default:
                        return createStringResource(moreDeleteKey, selected.size()).getString();
                }
            }
        };
    }

    private void deleteConnectorConfirmedPerformed(AjaxRequestTarget target) {
        List<ConnectorType> selected = getObjectListPanel().getSelectedRealObjects();

        OperationResult result = new OperationResult(OPERATION_DELETE_CONNECTORS);
        for (ConnectorType selectable : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_CONNECTORS);

                if (selectable != null) {
                    ObjectDelta<ConnectorType> delta = getPrismContext().deltaFactory().object().createDeleteDelta(ConnectorType.class,
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

    private Collection<SelectorOptions<GetOperationOptions>> getQueryOptions(){
        return SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
    }
}
