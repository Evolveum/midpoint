/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class InternalsLoggedInUsersPanel<F extends FocusType> extends BasePanel<F> {

    private static final String ID_TABLE = "table";

    private static final String DOT_CLASS = InternalsLoggedInUsersPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_LOGGED_IN_PRINCIPALS = DOT_CLASS + "loadLoggedInPrincipals";
    private static final String OPERATION_TERMINATE_SESSIONS = DOT_CLASS + "terminateSessions";

    public InternalsLoggedInUsersPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MainObjectListPanel<F> table = new MainObjectListPanel(ID_TABLE, FocusType.class, null) {

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
                if (WebComponentUtil.hasDetailsPage(object.getClass())) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
                    getPageBase().navigateToNext(WebComponentUtil.getObjectDetailsPage(object.getClass()), parameters);
                }
            }

            @Override
            protected List<IColumn<SelectableBean<F>, String>> createDefaultColumns() {
                return InternalsLoggedInUsersPanel.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
               return initInlineMenu();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<F>> createProvider() {
                LoadableModel<List<UserSessionManagementType>> principals = new LoadableModel<List<UserSessionManagementType>>(true) {

                    @Override
                    protected List<UserSessionManagementType> load() {
                        return loadLoggedInPrincipals();
                    }
                };

                return new SelectableListDataProvider<>(InternalsLoggedInUsersPanel.this, principals) {

                    @Override
                    protected SelectableBean<F> createObjectWrapper(UserSessionManagementType principal) {
                        SelectableBeanImpl<F> user = new SelectableBeanImpl<>(Model.of((F) principal.getFocus()));
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

            @Override
            protected boolean enableSavePageSize() {
                return false;
            }
        };

        add(table);
    }

    private List<UserSessionManagementType> loadLoggedInPrincipals() {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_LOGGED_IN_PRINCIPALS);
        OperationResult result = task.getResult();
        return getPageBase().getModelInteractionService().getLoggedInPrincipals(task, result);
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
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SIGN_OUT);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            terminateSessions(target, null);
                        } else {
                            SelectableBean<F> rowDto = getRowModel().getObject();
                            terminateSessions(target, rowDto.getValue());
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

    private void terminateSessions(AjaxRequestTarget target, F selectedObject) {
        List<String> selected = getSelectedObjects(selectedObject);
        if (CollectionUtils.isEmpty(selected)) {
            getSession().warn(getString("InternalsLoggedInUsersPanel.expireSession.warn"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        Task task = getPageBase().createSimpleTask(OPERATION_TERMINATE_SESSIONS);

        TerminateSessionEvent details = new TerminateSessionEvent();
        details.setPrincipalOids(selected);

        getPageBase().getModelInteractionService().terminateSessions(details, task, task.getResult());

        getSession().success(getString("InternalsLoggedInUsersPanel.expireSession.success"));
        target.add(getPageBase().getFeedbackPanel());
    }

    private List<String> getSelectedObjects(F selectedObject) {
        MainObjectListPanel<F> table = getTable();

        if (selectedObject != null) {
            return Arrays.asList(selectedObject.getOid());
        }

        List<F> selectedObjects = table.getSelectedRealObjects();
        if (selectedObject != null || !selectedObjects.isEmpty()) {
            return selectedObjects.stream().map(o -> o.getOid()).collect(Collectors.toList());
        }

        return null;
    }

    private MainObjectListPanel<F> getTable() {
        return (MainObjectListPanel) get(ID_TABLE);
    }
}
