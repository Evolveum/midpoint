/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String DIALOG_CONFIRM_DELETE = "confirmDeletePopup";
    private LoadableModel<UsersDto> model;

    public PageUsers() {
        model = new LoadableModel<UsersDto>(false) {

            @Override
            protected UsersDto load() {
                return new UsersDto();
            }
        };
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        OptionPanel option = new OptionPanel("option", createStringResource("pageUsers.optionsTitle"),
        		getPage(), false);
        option.setOutputMarkupId(true);
        mainForm.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageUsers.search"));
        option.getBodyContainer().add(item);
        initSearch(item);

        OptionContent content = new OptionContent("optionContent");
        mainForm.add(content);
        initTable(content);

        add(new ConfirmationDialog(DIALOG_CONFIRM_DELETE, createStringResource("pageUsers.dialog.title.confirmDelete"),
                createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteConfirmedPerformed(target);
            }
        });

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
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("pageUsers.message.deleteUserConfirm",
                        getSelectedUsers().size()).getString();
            }
        };
    }

    private List<IColumn<SelectableBean<UserType>>> initColumns() {
        List<IColumn<SelectableBean<UserType>>> columns = new ArrayList<IColumn<SelectableBean<UserType>>>();

        IColumn column = new CheckBoxHeaderColumn<UserType>();
        columns.add(column);

        columns.add(new IconColumn<SelectableBean<UserType>>(createStringResource("pageUsers.type")) {

            @Override
            protected IModel<ResourceReference> createIconModel(final IModel<SelectableBean<UserType>> rowModel) {
                return new AbstractReadOnlyModel<ResourceReference>() {

                    @Override
                    public ResourceReference getObject() {
                        UserType user = rowModel.getObject().getValue();
                        CredentialsType credentials = user.getCredentials();

                        if (credentials != null) {
                            Boolean allowedAdmin = credentials.isAllowedIdmAdminGuiAccess();
                            if (allowedAdmin != null && allowedAdmin) {
                                return new SharedResourceReference(ImgResources.class, "user_red.png");
                            }
                        }

                        return new SharedResourceReference(ImgResources.class, "user.png");
                    }
                };
            }
        });

        column = new LinkColumn<SelectableBean<UserType>>(createStringResource("pageUsers.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                UserType user = rowModel.getObject().getValue();
                userDetailsPerformed(target, user.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.givenName"), "givenName", "value.givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.familyName"), "familyName", "value.familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.fullName"), "fullName", "value.fullName.orig");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<UserType>>(createStringResource("pageUsers.email")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId,
                    IModel<SelectableBean<UserType>> rowModel) {

                String email = rowModel.getObject().getValue().getEmailAddress();
                cellItem.add(new Label(componentId, new Model<String>(email)));
            }
        };
        columns.add(column);

        return columns;
    }

    private void initTable(OptionContent content) {
        List<IColumn<SelectableBean<UserType>>> columns = initColumns();
        TablePanel table = new TablePanel<SelectableBean<UserType>>("table",
                new ObjectDataProvider(PageUsers.this, UserType.class), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);
    }

    private void initSearch(OptionItem item) {
        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
        item.add(search);

        CheckBox nameCheck = new CheckBox("nameCheck", new PropertyModel<Boolean>(model, "name"));
        item.add(nameCheck);
        CheckBox fullNameCheck = new CheckBox("fullNameCheck", new PropertyModel<Boolean>(model, "fullName"));
        item.add(fullNameCheck);
        CheckBox givenNameCheck = new CheckBox("givenNameCheck", new PropertyModel<Boolean>(model, "givenName"));
        item.add(givenNameCheck);
        CheckBox familyNameCheck = new CheckBox("familyNameCheck", new PropertyModel<Boolean>(model, "familyName"));
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
        QueryType query = createQuery();
        target.add(getFeedbackPanel());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        table.setCurrentPage(0);

        target.add(panel);
    }

    private QueryType createQuery() {
        UsersDto dto = model.getObject();
        QueryType query = null;
        if (StringUtils.isEmpty(dto.getSearchText())) {
            return null;
        }

        try {
            Document document = DOMUtil.getDocument();
            List<Element> elements = new ArrayList<Element>();
            if (dto.isName()) {
                elements.add(QueryUtil.createSubstringFilter(document, null, ObjectType.F_NAME, dto.getSearchText()));
            }
            if (dto.isFamilyName()) {
                elements.add(QueryUtil.createSubstringFilter(document, null, UserType.F_FAMILY_NAME, dto.getSearchText()));
            }
            if (dto.isFullName()) {
                elements.add(QueryUtil.createSubstringFilter(document, null, UserType.F_FULL_NAME, dto.getSearchText()));
            }
            if (dto.isGivenName()) {
                elements.add(QueryUtil.createSubstringFilter(document, null, UserType.F_GIVEN_NAME, dto.getSearchText()));
            }

            if (!elements.isEmpty()) {
                query = new QueryType();
                query.setFilter(QueryUtil.createOrFilter(document, elements.toArray(new Element[elements.size()])));
            }
        } catch (Exception ex) {
            error(getString("pageUsers.message.queryError") + " " + ex.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
        }

        return query;
    }

    private void clearButtonPerformed(AjaxRequestTarget target) {
        model.reset();

        target.add(get("mainForm:option"));
        searchPerformed(target);
    }

    private List<SelectableBean<UserType>> getSelectedUsers() {
        DataTable table = getTable().getDataTable();
        ObjectDataProvider<UserType> provider = (ObjectDataProvider) table.getDataProvider();

        List<SelectableBean<UserType>> selected = new ArrayList<SelectableBean<UserType>>();
        for (SelectableBean<UserType> row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
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
        if (!isAnythingSelected(getSelectedUsers(), target)) {
            return;
        }

        ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<SelectableBean<UserType>> users = getSelectedUsers();
        if (!isAnythingSelected(users, target)) {
            return;
        }
        OperationResult result = new OperationResult(OPERATION_DELETE_USERS);
        for (SelectableBean<UserType> bean : users) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_USER);
            try {
                Task task = createSimpleTask(OPERATION_DELETE_USER);
                UserType user = bean.getValue();
                getModelService().deleteObject(UserType.class, user.getOid(), task, subResult);
                subResult.recordSuccess();
            } catch (Exception ex) {
                subResult.recomputeStatus();
                subResult.recordFatalError("Couldn't delete user.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete user", ex);
            }
        }
        result.recomputeStatus();

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

    private void updateActivationPerformed(AjaxRequestTarget target, boolean enabling) {
        List<SelectableBean<UserType>> users = getSelectedUsers();
        if (!isAnythingSelected(users, target)) {
            return;
        }
        OperationResult result = enabling ? new OperationResult(OPERATION_ENABLE_USERS) :
                new OperationResult(OPERATION_DISABLE_USERS);
        for (SelectableBean<UserType> bean : users) {
            String operation = enabling ? OPERATION_ENABLE_USER : OPERATION_DISABLE_USER;
            OperationResult subResult = result.createSubresult(operation);
            try {
                Task task = createSimpleTask(operation);
                UserType user = bean.getValue();
                getPrismContext().adopt(user);

                PrismObject<UserType> object = user.asPrismObject();
                PropertyPath path = new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED);
                PrismProperty property = object.findOrCreateProperty(path);
                PropertyDelta delta = new PropertyDelta(path, property.getDefinition());
                delta.setValuesToReplace(Arrays.asList(new PrismPropertyValue(enabling, SourceType.USER_ACTION, null)));

                Collection<PropertyDelta> deltas = new ArrayList<PropertyDelta>();
                deltas.add(delta);

                getModelService().modifyObject(UserType.class, user.getOid(), deltas, task, subResult);

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
