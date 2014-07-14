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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.DropDownMultiChoice;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserListItemDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UsersStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/users", action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#users",
                label = "PageUsers.auth.users.label",
                description = "PageUsers.auth.users.description")})
public class PageUsers extends PageAdminUsers {

    private static final Trace LOGGER = TraceManager.getTrace(PageUsers.class);
    private static final String DOT_CLASS = PageUsers.class.getName() + ".";
    private static final String OPERATION_DELETE_USERS = DOT_CLASS + "deleteUsers";
    private static final String OPERATION_DELETE_USER = DOT_CLASS + "deleteUser";
    private static final String OPERATION_DISABLE_USERS = DOT_CLASS + "disableUsers";
    private static final String OPERATION_DISABLE_USER = DOT_CLASS + "disableUser";
    private static final String OPERATION_ENABLE_USERS = DOT_CLASS + "enableUsers";
    private static final String OPERATION_ENABLE_USER = DOT_CLASS + "enableUser";
    private static final String OPERATION_RECONCILE_USERS = DOT_CLASS + "reconcileUsers";
    private static final String OPERATION_RECONCILE_USER = DOT_CLASS + "reconcileUser";
    private static final String DIALOG_CONFIRM_DELETE = "confirmDeletePopup";

    private static final String ID_EXECUTE_OPTIONS = "executeOptions";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_SEARCH_TYPE = "searchType";

    private UserListItemDto singleDelete;
    private LoadableModel<UsersDto> model;
    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;

    public PageUsers() {
        model = new LoadableModel<UsersDto>(false) {

            @Override
            public UsersDto load() {
                UsersStorage storage = getSessionStorage().getUsers();
                UsersDto dto = storage.getUsersSearch();
                if (dto == null) {
                    dto = new UsersDto();
                }

                return dto;
            }
        };

        executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(false) {

            @Override
            protected ExecuteChangeOptionsDto load() {
                return new ExecuteChangeOptionsDto();
            }
        };
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        add(new ConfirmationDialog(DIALOG_CONFIRM_DELETE,
                createStringResource("pageUsers.dialog.title.confirmDelete"), createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteConfirmedPerformed(target);
            }
        });

        initSearch();
        initTable(mainForm);

        mainForm.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, false));
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(singleDelete == null){
                    return createStringResource("pageUsers.message.deleteUserConfirm",
                        WebMiscUtil.getSelectedData(getTable()).size()).getString();
                } else {
                    return createStringResource("pageUsers.message.deleteUserConfirmSingle",
                            singleDelete.getName()).getString();
                }
            }
        };
    }

    private List<IColumn<UserListItemDto, String>> initColumns() {
        List<IColumn<UserListItemDto, String>> columns = new ArrayList<IColumn<UserListItemDto, String>>();

        columns.add(new CheckBoxHeaderColumn());
        columns.add(new IconColumn<UserListItemDto>(null) {

            @Override
            protected IModel<String> createIconModel(final IModel<UserListItemDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return rowModel.getObject().getIcon();
                    }
                };
            }

            @Override
            protected IModel<String> createTitleModel(final IModel<UserListItemDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        String key = rowModel.getObject().getIconTitle();
                        if (key == null) {
                            return null;
                        }
                        return createStringResource(key).getString();
                    }
                };
            }
        });

        IColumn column = new LinkColumn<UserListItemDto>(createStringResource("ObjectType.name"),
                UserType.F_NAME.getLocalPart(), UserListItemDto.F_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<UserListItemDto> rowModel) {
                userDetailsPerformed(target, rowModel.getObject().getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.givenName"),
                UserType.F_GIVEN_NAME.getLocalPart(), UserListItemDto.F_GIVEN_NAME);
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.familyName"),
                UserType.F_FAMILY_NAME.getLocalPart(), UserListItemDto.F_FAMILY_NAME);
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.fullName"),
                UserType.F_FULL_NAME.getLocalPart(), UserListItemDto.F_FULL_NAME);
        columns.add(column);

        column = new PropertyColumn(createStringResource("UserType.emailAddress"), null, UserListItemDto.F_EMAIL);
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.accounts"), null, UserListItemDto.F_ACCOUNT_COUNT);
        columns.add(column);

        column = new InlineMenuHeaderColumn(initInlineMenu());
        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.enable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, true, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.disable"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateActivationPerformed(target, false, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.reconcile"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        reconcilePerformed(target, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem());

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageUsers.menu.delete"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deletePerformed(target, null);
                    }
                }));

        return headerMenuItems;
    }

    private void initTable(Form mainForm) {
        List<IColumn<UserListItemDto, String>> columns = initColumns();

        ObjectDataProvider<UserListItemDto, UserType> provider =
                new ObjectDataProvider<UserListItemDto, UserType>(PageUsers.this, UserType.class) {

                    @Override
                    protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                        UsersStorage storage = getSessionStorage().getUsers();
                        storage.setUsersPaging(paging);
                    }

                    @Override
                    public UserListItemDto createDataObjectWrapper(PrismObject<UserType> obj) {
                        return createRowDto(obj);
                    }
                };
        provider.setQuery(createQuery());

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        options.add(SelectorOptions.create(UserType.F_ASSIGNMENT,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        provider.setOptions(options);

        TablePanel table = new TablePanel(ID_TABLE, provider, columns, UserProfileStorage.TableId.PAGE_USERS_PANEL);
        table.setOutputMarkupId(true);

        UsersStorage storage = getSessionStorage().getUsers();
        table.setCurrentPage(storage.getUsersPaging());

        mainForm.add(table);
    }

    private UserListItemDto createRowDto(PrismObject<UserType> obj) {
        UserType user = obj.asObjectable();

        UserListItemDto dto = new UserListItemDto(user.getOid(),
                WebMiscUtil.getOrigStringFromPoly(user.getName()),
                WebMiscUtil.getOrigStringFromPoly(user.getGivenName()),
                WebMiscUtil.getOrigStringFromPoly(user.getFamilyName()),
                WebMiscUtil.getOrigStringFromPoly(user.getFullName()),
                user.getEmailAddress());

        dto.setAccountCount(createAccountCount(obj));
        dto.setCredentials(obj.findContainer(UserType.F_CREDENTIALS));
        dto.setIcon(WebMiscUtil.createUserIcon(obj));
        dto.setIconTitle(WebMiscUtil.createUserIconTitle(obj));

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.enable"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        UserListItemDto rowDto = getRowModel().getObject();
                        updateActivationPerformed(target, true, rowDto);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.disable"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        UserListItemDto rowDto = getRowModel().getObject();
                        updateActivationPerformed(target, false, rowDto);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.reconcile"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        UserListItemDto rowDto = getRowModel().getObject();
                        reconcilePerformed(target, rowDto);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem());

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageUsers.menu.delete"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        UserListItemDto rowDto = getRowModel().getObject();
                        deletePerformed(target, rowDto);
                    }
                }));


        return dto;
    }

    private int createAccountCount(PrismObject<UserType> object) {
        PrismReference accountRef = object.findReference(UserType.F_LINK_REF);
        return accountRef != null ? accountRef.size() : 0;
    }

    private void initSearch() {
        final Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);

        IModel<Map<String, String>> options = new Model(null);
        DropDownMultiChoice searchType = new DropDownMultiChoice<UsersDto.SearchType>(ID_SEARCH_TYPE,
                new PropertyModel<List<UsersDto.SearchType>>(model, UsersDto.F_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(UsersDto.SearchType.class),
                new IChoiceRenderer<UsersDto.SearchType>() {

                    @Override
                    public Object getDisplayValue(UsersDto.SearchType object) {
                        return WebMiscUtil.createLocalizedModelForEnum(object, PageUsers.this).getObject();
                    }

                    @Override
                    public String getIdValue(UsersDto.SearchType object, int index) {
                        return Integer.toString(index);
                    }
                }, options);
        searchForm.add(searchType);

        BasicSearchPanel<UsersDto> basicSearch = new BasicSearchPanel<UsersDto>(ID_BASIC_SEARCH, model) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<String>(model, UsersDto.F_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                PageUsers.this.searchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                PageUsers.this.clearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);
    }

    private void userDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        setResponsePage(PageUser.class, parameters);
    }

    private TablePanel getTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private void searchPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createQuery();
        target.add(getFeedbackPanel());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        UsersStorage storage = getSessionStorage().getUsers();
        storage.setUsersSearch(model.getObject());
        storage.setUsersPaging(null);
        panel.setCurrentPage(null);

        target.add(panel);
    }

    private ObjectQuery createQuery() {
        UsersDto dto = model.getObject();
        ObjectQuery query = null;
        if (StringUtils.isEmpty(dto.getText())) {
            return null;
        }

        try {
            List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedString = normalizer.normalize(dto.getText());

            if (dto.hasType(UsersDto.SearchType.NAME)) {
                filters.add(SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, getPrismContext(),
                        PolyStringNormMatchingRule.NAME, normalizedString));
            }

            if (dto.hasType(UsersDto.SearchType.FAMILY_NAME)) {
                filters.add(SubstringFilter.createSubstring(UserType.F_FAMILY_NAME, UserType.class, getPrismContext(),
                        PolyStringNormMatchingRule.NAME, normalizedString));
            }
            if (dto.hasType(UsersDto.SearchType.FULL_NAME)) {
                filters.add(SubstringFilter.createSubstring(UserType.F_FULL_NAME, UserType.class, getPrismContext(),
                        PolyStringNormMatchingRule.NAME, normalizedString));
            }
            if (dto.hasType(UsersDto.SearchType.GIVEN_NAME)) {
                filters.add(SubstringFilter.createSubstring(UserType.F_GIVEN_NAME, UserType.class, getPrismContext(),
                       PolyStringNormMatchingRule.NAME, normalizedString));
            }

            if (filters.size() == 1) {
                query = ObjectQuery.createObjectQuery(filters.get(0));
            } else if (filters.size() > 1) {
                query = ObjectQuery.createObjectQuery(OrFilter.createOr(filters));
            }
        } catch (Exception ex) {
            error(getString("pageUsers.message.queryError") + " " + ex.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
        }

        return query;
    }

    private void deletePerformed(AjaxRequestTarget target, UserListItemDto selectedUser) {
        singleDelete = selectedUser;
        List<UserListItemDto> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }

        ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<UserListItemDto> users = new  ArrayList<UserListItemDto>();

        if(singleDelete == null){
            users = isAnythingSelected(target, null);
        } else {
            users.add(singleDelete);
        }

        if (users.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_USERS);
        for (UserListItemDto user : users) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_USER);
            try {
                Task task = createSimpleTask(OPERATION_DELETE_USER);

                ObjectDelta delta = new ObjectDelta(UserType.class, ChangeType.DELETE, getPrismContext());
                delta.setOid(user.getOid());

                ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
                ModelExecuteOptions options = executeOptions.createOptions();
                LOGGER.debug("Using options {}.", new Object[]{executeOptions});
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), options, task, subResult);
                subResult.computeStatus();
            } catch (Exception ex) {
                subResult.recomputeStatus();
                subResult.recordFatalError("Couldn't delete user.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete user", ex);
            }
        }
        result.computeStatusComposite();

        ObjectDataProvider<UserListItemDto, UserType> provider = (ObjectDataProvider) getTable().getDataTable()
                .getDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    public static String toShortString(UserListItemDto object) {
        if (object == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(ObjectTypeUtil.getShortTypeName(UserType.class));
        builder.append(": ");
        builder.append(object.getName());
        builder.append(" (OID:");
        builder.append(object.getOid());
        builder.append(")");

        return builder.toString();
    }

    private void reconcilePerformed(AjaxRequestTarget target, UserListItemDto selectedUser) {
        List<UserListItemDto> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECONCILE_USERS);
        for (UserListItemDto user : users) {
            String userShortString = toShortString(user);
            OperationResult opResult = result.createSubresult(getString(OPERATION_RECONCILE_USER, userShortString));
            try {
                Task task = createSimpleTask(OPERATION_RECONCILE_USER + userShortString);
                ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, user.getOid(), getPrismContext());
                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                getModelService().executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, opResult);
                opResult.recordSuccess();
            } catch (Exception ex) {
                opResult.recomputeStatus();
                opResult.recordFatalError("Couldn't reconcile user " + userShortString + ".", ex);
                LoggingUtils.logException(LOGGER, "Couldn't reconcile user " + userShortString + ".", ex);
            }
        }

        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    /**
     * This method check selection in table. If selectedUser != null than it returns only this user.
     */
    private List<UserListItemDto> isAnythingSelected(AjaxRequestTarget target, UserListItemDto selectedUser) {
        List<UserListItemDto> users;
        if (selectedUser != null) {
            users = new ArrayList<UserListItemDto>();
            users.add(selectedUser);
        } else {
            users = WebMiscUtil.getSelectedData(getTable());
            if (users.isEmpty()) {
                warn(getString("pageUsers.message.nothingSelected"));
                target.add(getFeedbackPanel());
            }
        }

        return users;
    }

    /**
     * This method updates user activation. If userOid parameter is not null, than it updates only that user,
     * otherwise it checks table for selected users.
     */
    private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling, UserListItemDto selectedUser) {
        List<UserListItemDto> users = isAnythingSelected(target, selectedUser);
        if (users.isEmpty()) {
            return;
        }

        String operation = enabling ? OPERATION_ENABLE_USERS : OPERATION_DISABLE_USERS;
        OperationResult result = new OperationResult(operation);
        for (UserListItemDto user : users) {
            operation = enabling ? OPERATION_ENABLE_USER : OPERATION_DISABLE_USER;
            OperationResult subResult = result.createSubresult(operation);
            try {
                Task task = createSimpleTask(operation);

                ObjectDelta objectDelta = WebModelUtils.createActivationAdminStatusDelta(UserType.class, user.getOid(),
                        enabling, getPrismContext());

                ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
                ModelExecuteOptions options = executeOptions.createOptions();
                LOGGER.debug("Using options {}.", new Object[]{executeOptions});
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(objectDelta), options, task,
                        subResult);
                subResult.recordSuccess();
            } catch (Exception ex) {
                subResult.recomputeStatus();
                if (enabling) {
                    subResult.recordFatalError("Couldn't enable user.", ex);
                    LoggingUtils.logException(LOGGER, "Couldn't enable user", ex);
                } else {
                    subResult.recordFatalError("Couldn't disable user.", ex);
                    LoggingUtils.logException(LOGGER, "Couldn't disable user", ex);
                }
            }
        }
        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    private void clearSearchPerformed(AjaxRequestTarget target){
        model.setObject(new UsersDto());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(null);

        UsersStorage storage = getSessionStorage().getUsers();
        storage.setUsersSearch(model.getObject());
        storage.setUsersPaging(null);
        panel.setCurrentPage(null);

        target.add(get(ID_SEARCH_FORM));
        target.add(panel);
    }
}
