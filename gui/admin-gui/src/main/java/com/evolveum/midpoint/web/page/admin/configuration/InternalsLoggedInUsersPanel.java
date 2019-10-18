/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.authentication.UserProfileService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.ListDataProvider2;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

public class InternalsLoggedInUsersPanel<F extends FocusType> extends BasePanel<F> {

    private static final String ID_TABLE = "table";

    private static final String DOT_CLASS = InternalsLoggedInUsersPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_PRINCIPALS_REMOte_NODES = DOT_CLASS + "loadPrincipalsFromRemoteNodes";

    public InternalsLoggedInUsersPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MainObjectListPanel<F, String> table = new MainObjectListPanel(ID_TABLE, FocusType.class, null, null, getPageBase()) {

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {

            }

            @Override
            protected List<IColumn<SelectableBean<F>, String>> createColumns() {
                return InternalsLoggedInUsersPanel.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
               return initInlineMenu();
            }

            @Override
            protected BaseSortableDataProvider<SelectableBean<F>> initProvider() {
                LoadableModel<List<UserSessionManagementType>> principals = new LoadableModel<List<UserSessionManagementType>>(true) {

                    @Override
                    protected List<UserSessionManagementType> load() {
                        return loadLoggedIUsersFromAllNodes();
                    }
                };

                return new ListDataProvider2<SelectableBean<F>, UserSessionManagementType>(InternalsLoggedInUsersPanel.this, principals) {

                    @Override
                    protected SelectableBean<F> createObjectWrapper(UserSessionManagementType principal) {
                        SelectableBean<F> user = new SelectableBean<F>((F) principal.getUser());
                        user.setActiveSessions(principal.getActiveSessions());
                        user.setNodes(principal.getNode());
                        return user;
                    }
                };
            }

            @Override
            protected boolean isCreateNewObjectEnabled() {
                return false;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return new ArrayList<>();
            }
        };

        add(table);
    }

    private List<UserSessionManagementType> loadLoggedIUsersFromAllNodes() {
        OperationResult result = new OperationResult(OPERATION_LOAD_PRINCIPALS_REMOte_NODES);

        List<UserSessionManagementType> loggedUsers = getPageBase().getModelInteractionService().getLoggedInUsers();

        Map<String, UserSessionManagementType> usersMap = loggedUsers.stream().collect(Collectors.toMap(key -> key.getUser().getOid(), value -> value));

        getPageBase().getClusterExecutionHelper().execute((client, operationResult) -> {

            client.path(UserProfileService.EVENT_LIST_USER_SESSION);
            Response response = client.get();
            LOGGER.info("Cluster-wide cache clearance finished with status {}, {}", response.getStatusInfo().getStatusCode(),
                    response.getStatusInfo().getReasonPhrase());

            if (!response.hasEntity()) {
                return;
            }

            UserSessionManagementListType sessionUsers = response.readEntity(UserSessionManagementListType.class);
            response.close();

            List<UserSessionManagementType> nodeSpecificSessions = sessionUsers.getSession();

            if (sessionUsers == null || nodeSpecificSessions == null || nodeSpecificSessions.isEmpty()) {
                return;
            }

            for (UserSessionManagementType user : nodeSpecificSessions) {
                UserSessionManagementType existingUser = usersMap.get(user.getUser().getOid());
                if (existingUser != null) {
                    existingUser.setActiveSessions(existingUser.getActiveSessions() + user.getActiveSessions());
                    existingUser.getNode().addAll(user.getNode());
                    continue;
                }

                usersMap.put(user.getUser().getOid(), user);
            }

            }, " list principals from remote nodes ", result);


        return new ArrayList<>(usersMap.values());
    }

    private List<IColumn<SelectableBean<F>, String>> initColumns() {
        List<IColumn<SelectableBean<F>, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("ActivationType.effectiveStatus"), "value.activation.effectiveStatus"));
        columns.add(new PropertyColumn<>(createStringResource("InternalsLoggedInUsers.activeSessions"), "activeSessions"));
        columns.add(new PropertyColumn<>(createStringResource("InternalsLoggedInUsers.nodes"), "nodes"));
        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new ButtonInlineMenuItem(createStringResource("InternalsLoggedInUsers.refresh")) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_SIGN_OUT;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            expireSessionsFor(target, null);
                        } else {
                            SelectableBean<F> rowDto = getRowModel().getObject();
                            expireSessionsFor(target, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return true;
            }
        });
        return menuItems;

    }

    private void expireSessionsFor(AjaxRequestTarget target, F selectedObject) {
        List<String> selected = getSelectedObjects(selectedObject);
        if (CollectionUtils.isEmpty(selected)) {
            getSession().warn(getString("InternalsLoggedInUsersPanel.expireSession.warn"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        TerminateSessionEvent details = new TerminateSessionEvent();
        details.setPrincipalOids(selected);
        CacheInvalidationContext ctx = new CacheInvalidationContext(false, details);
        ctx.setTerminateSession(true);
        getPageBase().getCacheDispatcher().dispatchInvalidation(null, null, true, ctx);

        //getPageBase().getModelInteractionService().expirePrincipals(selected);
        getSession().success(getString("InternalsLoggedInUsersPanel.expireSession.success"));
        target.add(getPageBase().getFeedbackPanel());
    }

    private List<String> getSelectedObjects(F selectedObject) {
        MainObjectListPanel<F, String> table = getTable();

        if (selectedObject != null) {
            return Arrays.asList(selectedObject.getOid());
        }

        List<F> selectedObjects = table.getSelectedObjects();
        if (selectedObject != null || !selectedObjects.isEmpty()) {
            return selectedObjects.stream().map(o -> o.getOid()).collect(Collectors.toList());
        }

        return null;
    }

    private MainObjectListPanel<F, String> getTable() {
        return (MainObjectListPanel) get(ID_TABLE);
    }
}
