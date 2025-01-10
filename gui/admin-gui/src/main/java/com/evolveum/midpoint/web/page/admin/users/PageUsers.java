/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageMergeObjects;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/users", matchUrlForSecurity = "/admin/users")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_URL,
                        label = "PageUsers.auth.users.label",
                        description = "PageUsers.auth.users.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL,
                        label = "PageUsers.auth.users.view.label",
                        description = "PageUsers.auth.users.view.description")
        })
@CollectionInstance(identifier = "allUsers", applicableForType = UserType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.users.list", singularLabel = "ObjectType.user", icon = GuiStyleConstants.CLASS_OBJECT_USER_ICON))
public class PageUsers extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageUsers.class);

    private static final String DOT_CLASS = PageUsers.class.getName() + ".";

    private static final String OPERATION_DISABLE_USERS = DOT_CLASS + "disableUsers";
    private static final String OPERATION_DISABLE_USER = DOT_CLASS + "disableUser";
    private static final String OPERATION_ENABLE_USERS = DOT_CLASS + "enableUsers";
    private static final String OPERATION_ENABLE_USER = DOT_CLASS + "enableUser";
    private static final String OPERATION_RECONCILE_USERS = DOT_CLASS + "reconcileUsers";
    private static final String OPERATION_RECONCILE_USER = DOT_CLASS + "reconcileUser";
    private static final String OPERATION_UNLOCK_USERS = DOT_CLASS + "unlockUsers";
    private static final String OPERATION_UNLOCK_USER = DOT_CLASS + "unlockUser";
    private static final String OPERATION_LOAD_MERGE_CONFIGURATION = DOT_CLASS + "loadMergeConfiguration";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageUsers() {
        this(null);
    }

    public PageUsers(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<UserType> table = new MainObjectListPanel<>(ID_TABLE, UserType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return TableId.TABLE_USERS;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper helper = new FocusListInlineMenuHelper(UserType.class, PageUsers.this, this) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isShowConfirmationDialog(ColumnMenuAction action) {
                        return PageUsers.this.isShowConfirmationDialog(action);
                    }

                    @Override
                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName) {
                        return PageUsers.this.getTable().getConfirmationMessageModel(action, actionName);
                    }
                };

                List<InlineMenuItem> items = helper.createRowActions(UserType.class);
                items.addAll(createRowActions());

                return items;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageUsers.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageUsers.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageUsers.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        menu.add(new InlineMenuItem(createStringResource("pageUsers.menu.unlock")) {

            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<UserType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        unlockPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public boolean showConfirmationDialog() {
                return PageUsers.this.isShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageUsers.message.unlockAction").getString();
                return getTable().getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        //TODO: shouldn't  be visible only if supported?
        menu.add(new InlineMenuItem(createStringResource("pageUsers.menu.merge")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<UserType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        OperationResult result = new OperationResult(OPERATION_LOAD_MERGE_CONFIGURATION);
                        List<MergeConfigurationType> mergeConfiguration;
                        try {
                            mergeConfiguration = getModelInteractionService().getMergeConfiguration(result);
                        } catch (ObjectNotFoundException | SchemaException ex){
                            LOGGER.error("Couldn't load merge configuration: {}", ex.getLocalizedMessage());
                            result.recomputeStatus();
                            getFeedbackMessages().error(PageUsers.this, ex.getLocalizedMessage());
                            target.add(getFeedbackPanel());
                            return;
                        }

                        if (mergeConfiguration == null || mergeConfiguration.size() == 0){
                            getFeedbackMessages().warn(PageUsers.this, createStringResource("PageUsers.noMergeConfigurationMessage").getString());
                            target.add(getFeedbackPanel());
                            return;
                        }
                        if (getRowModel() == null) {
                            mergePerformed(target, null);
                        } else {
                            SelectableBean<UserType> rowDto = getRowModel().getObject();
                            mergePerformed(target, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }
        });
        return menu;
    }

    private boolean isShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                getTable().getSelectedObjectsCount() > 0;
    }

    private MainObjectListPanel<UserType> getTable() {
        return (MainObjectListPanel<UserType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private void mergePerformed(AjaxRequestTarget target, final UserType selectedUser) {
        List<QName> supportedTypes = new ArrayList<>();
        supportedTypes.add(UserType.COMPLEX_TYPE);
        ObjectFilter filter = getPrismContext().queryFactory().createInOid(selectedUser.getOid());
        ObjectFilter notFilter = getPrismContext().queryFactory().createNot(filter);
        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<>(
                getMainPopupBodyId(), UserType.class,
                supportedTypes, false, PageUsers.this, notFilter) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                hideMainPopup(target);
                mergeConfirmedPerformed(selectedUser, user, target);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("ObjectBrowserPanel.chooseObject.merge.with", WebComponentUtil.getName(selectedUser)) ;
            }

        };
        panel.setOutputMarkupId(true);
        showMainPopup(panel, target);
    }

    private void mergeConfirmedPerformed(UserType mergeObject, UserType mergeWithObject, AjaxRequestTarget target) {
        setResponsePage(new PageMergeObjects(mergeObject, mergeWithObject, UserType.class));
    }

    private void unlockPerformed(AjaxRequestTarget target, IModel<SelectableBean<UserType>> selectedUser) {
        List<SelectableBean<UserType>> users = getTable().isAnythingSelected(selectedUser);
        if (users.isEmpty()) {
            warn(getString("PageUsers.message.nothingSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_UNLOCK_USERS);
        for (SelectableBean<UserType> user : users) {
            OperationResult opResult = result.createSubresult(getString(OPERATION_UNLOCK_USER, user));
            try {
                Task task = createSimpleTask(OPERATION_UNLOCK_USER + user);
                // TODO skip the operation if the user has no password
                // credentials specified (otherwise this would create
                // almost-empty password container)
                ObjectDelta delta = getPrismContext().deltaFactory().object().createModificationReplaceProperty(
                        UserType.class, user.getValue().getOid(), ItemPath.create(UserType.F_ACTIVATION,
                                ActivationType.F_LOCKOUT_STATUS),
                        LockoutStatusType.NORMAL);
                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(delta);
                getModelService().executeChanges(deltas, null, task, opResult);
                opResult.computeStatusIfUnknown();
            } catch (Exception ex) {
                opResult.recomputeStatus();
                opResult.recordFatalError(getString("PageUsers.message.unlock.fatalError", WebComponentUtil.getName(user.getValue())), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't unlock user " + user.getValue() + ".", ex);
            }
        }

        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        getTable().refreshTable(target);
        getTable().clearCache();
    }

    private void reconcilePerformed(AjaxRequestTarget target, IModel<SelectableBean<UserType>> selectedUser) {
        List<SelectableBean<UserType>> users = getTable().isAnythingSelected(selectedUser);
        if (users.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECONCILE_USERS);
        for (SelectableBean<UserType> user : users) {
            OperationResult opResult = result.createSubresult(getString(OPERATION_RECONCILE_USER, user));
            UserType userType = user.getValue();
            try {
                Task task = createSimpleTask(OPERATION_RECONCILE_USER + userType);
                ObjectDelta delta = getPrismContext().deltaFactory().object().createEmptyModifyDelta(UserType.class, user.getValue().getOid()
                );
                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(delta);
                getModelService().executeChanges(deltas, executeOptions().reconcile(), task,
                        opResult);
                opResult.computeStatusIfUnknown();
            } catch (Exception ex) {
                opResult.recomputeStatus();
                opResult.recordFatalError(getString("PageUsers.message.reconcile.fatalError", userType), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reconcile user " + userType + ".", ex);
            }
        }

        result.setSummarizeSuccesses(true);
        result.summarize();
        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        getTable().refreshTable(target);
        getTable().clearCache();
    }

    /**
     * This method updates user activation. If userOid parameter is not null,
     * than it updates only that user, otherwise it checks table for selected
     * users.
     */
    private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling,
            IModel<SelectableBean<UserType>> selectedUser) {
        List<SelectableBean<UserType>> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }
//        List<SelectableObjectModel<UserType>> users = (List<SelectableObjectModel<UserType>>) TableUtil.getSelectedModels(getTable().getTable().getDataTable());

        String operation = enabling ? OPERATION_ENABLE_USERS : OPERATION_DISABLE_USERS;
        OperationResult result = new OperationResult(operation);
        for (SelectableBean<UserType> user : users) {
            operation = enabling ? OPERATION_ENABLE_USER : OPERATION_DISABLE_USER;
            OperationResult subResult = result.createSubresult(operation);
            try {
                Task task = createSimpleTask(operation);

                ObjectDelta objectDelta = WebModelServiceUtils.createActivationAdminStatusDelta(
                        UserType.class, user.getValue().getOid(), enabling, getPrismContext());

                ExecuteChangeOptionsDto executeOptions = getTable().getExecuteOptions();
                ModelExecuteOptions options = executeOptions.createOptions(getPrismContext());
                LOGGER.debug("Using options {}.", executeOptions);
                getModelService().executeChanges(MiscUtil.createCollection(objectDelta), options,
                        task, subResult);
                subResult.recordSuccess();
            } catch (Exception ex) {
                subResult.recomputeStatus();
                if (enabling) {
                    subResult.recordFatalError(getString("PageUsers.message.enable.fatalError"), ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't enable user", ex);
                } else {
                    subResult.recordFatalError(getString("PageUsers.message.disable.fatalError"), ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't disable user", ex);
                }
            }
        }
        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        getTable().clearCache();
        getTable().refreshTable(target);
    }

    /**
     * This method check selection in table. If selectedObject != null than it
     * returns only this object.
     */
    public List<SelectableBean<UserType>> isAnythingSelected(AjaxRequestTarget target, IModel<SelectableBean<UserType>> selectedObject) {
        List<SelectableBean<UserType>>  users;
        if (selectedObject != null) {
            users = new ArrayList<>();
            users.add(selectedObject.getObject());
        } else {
            users = TableUtil.getSelectedModels(getTable().getTable().getDataTable());
//            if (users.isEmpty() && StringUtils.isNotEmpty(getNothingSelectedMessage())) {
//                warn(getNothingSelectedMessage());
//                target.add(getFeedbackPanel());
//            }
        }

        return users;
    }

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }
}
