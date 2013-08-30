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
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.session.UsersStorage;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
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
        Form mainForm = new Form("mainForm");
        add(mainForm);

        OptionPanel option = new OptionPanel("option", createStringResource("pageUsers.optionsTitle"), false);
        option.setOutputMarkupId(true);
        mainForm.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageUsers.search"));
        option.getBodyContainer().add(item);
        initSearch(item);

        OptionContent content = new OptionContent("optionContent");
        mainForm.add(content);
        initTable(content);

        add(new ConfirmationDialog(DIALOG_CONFIRM_DELETE,
                createStringResource("pageUsers.dialog.title.confirmDelete"), createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteConfirmedPerformed(target);
            }
        });

        mainForm.add(new ExecuteChangeOptionsPanel(false, ID_EXECUTE_OPTIONS, executeOptionsModel));
        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitLinkButton enable = new AjaxSubmitLinkButton("enable",
                createStringResource("pageUsers.button.enable")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                enablePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(enable);

        AjaxSubmitLinkButton disable = new AjaxSubmitLinkButton("disable",
                createStringResource("pageUsers.button.disable")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                disablePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(disable);

        AjaxSubmitLinkButton delete = new AjaxSubmitLinkButton("delete", ButtonType.NEGATIVE,
                createStringResource("pageUsers.button.delete")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                deletePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(delete);
        
        AjaxSubmitLinkButton reconcile = new AjaxSubmitLinkButton("reconcile",
                createStringResource("pageUsers.button.reconcile")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                reconcilePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(reconcile);
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("pageUsers.message.deleteUserConfirm",
                        WebMiscUtil.getSelectedData(getTable()).size()).getString();
            }
        };
    }

    private List<IColumn<SelectableBean<UserType>, String>> initColumns() {
        List<IColumn<SelectableBean<UserType>, String>> columns =
                new ArrayList<IColumn<SelectableBean<UserType>, String>>();

        IColumn column = new CheckBoxHeaderColumn<UserType>();
        columns.add(column);

        columns.add(new IconColumn<SelectableBean<UserType>>(createStringResource("pageUsers.type")) {

            @Override
            protected IModel<ResourceReference> createIconModel(
                    final IModel<SelectableBean<UserType>> rowModel) {
                return new AbstractReadOnlyModel<ResourceReference>() {

                    @Override
                    public ResourceReference getObject() {
                        UserType user = rowModel.getObject().getValue();
                        CredentialsType credentials = user.getCredentials();

                        //if allowedIdmAdminGuiAccess is true, it's superuser
                        if (credentials != null) {
                            Boolean allowedAdmin = credentials.isAllowedIdmAdminGuiAccess();
                            if (allowedAdmin != null && allowedAdmin) {
                                return new SharedResourceReference(ImgResources.class, "user_red.png");
                            }
                        }

                        //if user has superuser role assigned, it's superuser
                        for (AssignmentType assignment : user.getAssignment()) {
                            ObjectReferenceType targetRef = assignment.getTargetRef();
                            if (targetRef == null) {
                                continue;
                            }
                            if (StringUtils.equals(targetRef.getOid(), SystemObjectsType.ROLE_SUPERUSER.value())) {
                                return new SharedResourceReference(ImgResources.class, "user_red.png");
                            }
                        }

                        return new SharedResourceReference(ImgResources.class, "user.png");
                    }
                };
            }
        });

        column = new LinkColumn<SelectableBean<UserType>>(createStringResource("pageUsers.name"), "name",
                "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                UserType user = rowModel.getObject().getValue();
                userDetailsPerformed(target, user.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.givenName"), "givenName",
                "value.givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.familyName"), "familyName",
                "value.familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.fullName"), "fullName",
                "value.fullName.orig");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<UserType>, String>(createStringResource("pageUsers.email")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                                     String componentId, IModel<SelectableBean<UserType>> rowModel) {

                String email = rowModel.getObject().getValue().getEmailAddress();
                cellItem.add(new Label(componentId, new Model<String>(email)));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<UserType>, String>(createStringResource("pageUsers.accounts")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                                     String componentId, IModel<SelectableBean<UserType>> rowModel) {

                UserType user = rowModel.getObject().getValue();

                PrismObject<UserType> object = user.asPrismObject();
                PrismReference accountRef = object.findReference(UserType.F_LINK_REF);
                int count = accountRef != null ? accountRef.size() : 0;

                cellItem.add(new Label(componentId, new Model<Integer>(count)));
            }
        };
        columns.add(column);

        return columns;
    }

    private void initTable(OptionContent content) {
        List<IColumn<SelectableBean<UserType>, String>> columns = initColumns();

        ObjectDataProvider<UserType> provider = new ObjectDataProvider(PageUsers.this, UserType.class) {

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                UsersStorage storage = getSessionStorage().getUsers();
                storage.setUsersPaging(paging);
            }
        };
        provider.setQuery(createQuery());

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(UserType.F_LINK_REF,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        options.add(SelectorOptions.create(UserType.F_ASSIGNMENT,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        provider.setOptions(options);

        TablePanel table = new TablePanel<SelectableBean<UserType>>("table", provider, columns);
        table.setOutputMarkupId(true);

        UsersStorage storage = getSessionStorage().getUsers();
        table.setCurrentPage(storage.getUsersPaging());

        content.getBodyContainer().add(table);
    }

    private void initSearch(OptionItem item) {
        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model,
                "searchText"));
        item.add(search);

        CheckBox nameCheck = new CheckBox("nameCheck", new PropertyModel<Boolean>(model, "name"));
        item.add(nameCheck);
        CheckBox fullNameCheck = new CheckBox("fullNameCheck", new PropertyModel<Boolean>(model, "fullName"));
        item.add(fullNameCheck);
        CheckBox givenNameCheck = new CheckBox("givenNameCheck", new PropertyModel<Boolean>(model,
                "givenName"));
        item.add(givenNameCheck);
        CheckBox familyNameCheck = new CheckBox("familyNameCheck", new PropertyModel<Boolean>(model,
                "familyName"));
        item.add(familyNameCheck);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                createStringResource("pageUsers.button.clearButton")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                clearButtonPerformed(target);
            }
        };
        item.add(clearButton);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                createStringResource("pageUsers.button.searchButton")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        item.add(searchButton);
    }

    private void userDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageUser.PARAM_USER_ID, oid);
        setResponsePage(PageUser.class, parameters);
    }

    private TablePanel getTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("table");
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
        panel.setCurrentPage(storage.getUsersPaging());

        target.add(panel);
    }

    private ObjectQuery createQuery() {
        UsersDto dto = model.getObject();
        ObjectQuery query = null;
        if (StringUtils.isEmpty(dto.getSearchText())) {
            return null;
        }

        try {
            List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedString = normalizer.normalize(dto.getSearchText());

            if (dto.isName()) {
                filters.add(SubstringFilter.createSubstring(UserType.class, getPrismContext(),
                        UserType.F_NAME, PolyStringNormMatchingRule.NAME.getLocalPart(), normalizedString));
            }

            if (dto.isFamilyName()) {
                filters.add(SubstringFilter.createSubstring(UserType.class, getPrismContext(),
                        UserType.F_FAMILY_NAME, PolyStringNormMatchingRule.NAME.getLocalPart(), normalizedString));
            }
            if (dto.isFullName()) {
                filters.add(SubstringFilter.createSubstring(UserType.class, getPrismContext(),
                        UserType.F_FULL_NAME, PolyStringNormMatchingRule.NAME.getLocalPart(), normalizedString));
            }
            if (dto.isGivenName()) {
                filters.add(SubstringFilter.createSubstring(UserType.class, getPrismContext(),
                        UserType.F_GIVEN_NAME, PolyStringNormMatchingRule.NAME.getLocalPart(), normalizedString));
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

    private void clearButtonPerformed(AjaxRequestTarget target) {
        UsersStorage storage = getSessionStorage().getUsers();
        storage.setUsersSearch(null);
        storage.setUsersPaging(null);

        model.reset();

        target.appendJavaScript("init()");
        target.add(get("mainForm:option"));
        searchPerformed(target);
    }

    private boolean isAnythingSelected(List<SelectableBean<UserType>> users, AjaxRequestTarget target) {
        if (!users.isEmpty()) {
            return true;
        }

        warn(getString("pageUsers.message.nothingSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private void deletePerformed(AjaxRequestTarget target) {
        List<SelectableBean<UserType>> users = WebMiscUtil.getSelectedData(getTable());
        if (!isAnythingSelected(users, target)) {
            return;
        }
// disabling user preview MID-1300
//        // When delete one user -> submit page
//        if (users.size() == 1) {
//            Task task = createSimpleTask(PageUsers.class.getName() + "sendToSubmit");
//            OperationResult result = task.getResult();
//            SelectableBean<UserType> bean = users.get(0);
//
//            UserType user = bean.getValue();
//            ModelContext changes = null;
//            ObjectDelta delta = null;
//            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//
//            ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
//            try {
//                delta = ObjectDelta.createDeleteDelta(UserType.class, user.getOid(), getPrismContext());
//                deltas.add(delta);
//                ModelExecuteOptions options = executeOptions.createOptions();
//                changes = getModelInteractionService().previewChanges(deltas, options, task, result);
//            } catch (Exception ex) {
//                result.recordFatalError("Couldn't send user to submit.", ex);
//                LoggingUtils.logException(LOGGER, "Couldn't submit user", ex);
//            }
//
//            if (result.isError()) {
//                showResult(result);
//                target.add(getFeedbackPanel());
//            } else {
//                PageUserPreview pageUserPreview = new PageUserPreview(changes, deltas, delta,
//                        null, executeOptions);
//                setResponsePage(pageUserPreview);
//            }
//
//        } else {
            ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE);
            dialog.show(target);
//        }
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<SelectableBean<UserType>> users = WebMiscUtil.getSelectedData(getTable());
        if (!isAnythingSelected(users, target)) {
            return;
        }
        OperationResult result = new OperationResult(OPERATION_DELETE_USERS);
        for (SelectableBean<UserType> bean : users) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_USER);
            try {
                Task task = createSimpleTask(OPERATION_DELETE_USER);
                UserType user = bean.getValue();

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

        ObjectDataProvider<UserType> provider = (ObjectDataProvider) getTable().getDataTable()
                .getDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    private void enablePerformed(AjaxRequestTarget target) {
        updateActivationPerformed(target, true);
    }

    private void disablePerformed(AjaxRequestTarget target) {
        updateActivationPerformed(target, false);
    }
    
    private void reconcilePerformed(AjaxRequestTarget target){
    	 List<SelectableBean<UserType>> users = WebMiscUtil.getSelectedData(getTable());
         if (!isAnythingSelected(users, target)) {
             return;
         }
         OperationResult result = new OperationResult(OPERATION_RECONCILE_USERS);
         for (SelectableBean<UserType> user : users){
        	 OperationResult opResult = result.createSubresult(getString(OPERATION_RECONCILE_USER, ObjectTypeUtil.toShortString(user.getValue())));
        	 try{
        		 Task task = createSimpleTask(OPERATION_RECONCILE_USER + ObjectTypeUtil.toShortString(user.getValue()));
        		 ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, user.getValue().getOid(), getPrismContext());
        		 Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
        		 getModelService().executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, opResult);
        		 opResult.recordSuccess();
        	 } catch (Exception ex) {
        		 opResult.recomputeStatus();
                	 opResult.recordFatalError("Couldn't reconcile user " + ObjectTypeUtil.toShortString(user.getValue()) +"." , ex);
                     LoggingUtils.logException(LOGGER, "Couldn't reconcile user " + ObjectTypeUtil.toShortString(user.getValue()) +".", ex);
             }
         }
         
         result.recomputeStatus();

         showResult(result);
         target.add(getFeedbackPanel());
         target.add(getTable());
    }

    private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling) {
        List<SelectableBean<UserType>> users = WebMiscUtil.getSelectedData(getTable());
        if (!isAnythingSelected(users, target)) {
            return;
        }
        OperationResult result = enabling ? new OperationResult(OPERATION_ENABLE_USERS)
                : new OperationResult(OPERATION_DISABLE_USERS);
        for (SelectableBean<UserType> bean : users) {
            String operation = enabling ? OPERATION_ENABLE_USER : OPERATION_DISABLE_USER;
            OperationResult subResult = result.createSubresult(operation);
            try {
                Task task = createSimpleTask(operation);
                UserType user = bean.getValue();
                getPrismContext().adopt(user);

//				PrismObject<UserType> object = user.asPrismObject();
                ItemPath path = new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
//				PrismProperty property = object.findOrCreateProperty(path);
//				PropertyDelta delta = new PropertyDelta(path, property.getDefinition());
//				delta.setValuesToReplace(Arrays.asList(new PrismPropertyValue(enabling,
//						OriginType.USER_ACTION, null)));

                ActivationStatusType status = enabling ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
                ObjectDelta objectDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, user.getOid(),
                        path, getPrismContext(), status);
//				ObjectDelta objectDelta = new ObjectDelta(UserType.class, ChangeType.MODIFY, getPrismContext());
//				objectDelta.addModification(delta);

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

}
