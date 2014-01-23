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

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.roles.dto.RolesSearchDto;
import com.evolveum.midpoint.web.session.RolesStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/roles", action = {
        PageAdminRoles.AUTHORIZATION_ROLE_ALL,
        AuthorizationConstants.NS_AUTHORIZATION + "#roles"})
public class PageRoles extends PageAdminRoles {

    private static final Trace LOGGER = TraceManager.getTrace(PageRoles.class);
    private static final String DOT_CLASS = PageRoles.class.getName() + ".";
    private static final String OPERATION_DELETE_ROLES = DOT_CLASS + "deleteRoles";

    private static final String DIALOG_CONFIRM_DELETE = "confirmDeletePopup";
    private static final String ID_TABLE = "table";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH_REQUESTABLE = "choice";
    private static final String ID_SEARCH_BUTTON = "searchButton";

    private IModel<RolesSearchDto> searchModel;

    public PageRoles() {

        searchModel = new LoadableModel<RolesSearchDto>() {

            @Override
            protected RolesSearchDto load() {
                RolesStorage storage = getSessionStorage().getRoles();
                RolesSearchDto dto = storage.getRolesSearch();

                if(dto == null){
                    dto = new RolesSearchDto();
                }

                return dto;
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        initSearchForm(searchForm);

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<IColumn<RoleType, String>> columns = initColumns();
        TablePanel table = new TablePanel<>(ID_TABLE, new ObjectDataProvider(PageRoles.this, RoleType.class), columns);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        add(new ConfirmationDialog(DIALOG_CONFIRM_DELETE, createStringResource("pageRoles.dialog.title.confirmDelete"),
                createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                deleteConfirmedPerformed(target);
            }
        });
    }

    private void initSearchForm(Form searchForm){
        TextField text = new TextField<String>(ID_SEARCH_TEXT, new PropertyModel<String>(searchModel, RolesSearchDto.F_SEARCH_TEXT));
        searchForm.add(text);

        IChoiceRenderer<RolesSearchDto.Requestable> renderer = new IChoiceRenderer<RolesSearchDto.Requestable>() {

            @Override
            public Object getDisplayValue(RolesSearchDto.Requestable requestable) {
                return requestable.getKey();
            }

            @Override
            public String getIdValue(RolesSearchDto.Requestable requestable, int i) {
                return requestable.getKey();
            }
        };

        DropDownChoice requestable = new DropDownChoice(ID_SEARCH_REQUESTABLE, new PropertyModel(searchModel, RolesSearchDto.F_REQUESTABLE),
                createChoiceModel(renderer), renderer);
        requestable.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                listRolesPerformed(target);
            }
        });

        searchForm.add(requestable);

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON,
                createStringResource("pageRoles.button.search")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                listRolesPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(getFeedbackPanel());
            }

        };
       searchForm.add(searchButton);
    }

    private IModel<List<RolesSearchDto.Requestable>> createChoiceModel(final IChoiceRenderer<RolesSearchDto.Requestable> renderer){
        return new LoadableModel<List<RolesSearchDto.Requestable>>(false) {

            @Override
            protected List<RolesSearchDto.Requestable> load() {
                List<RolesSearchDto.Requestable> choices = new ArrayList<RolesSearchDto.Requestable>();

                Collections.addAll(choices, RolesSearchDto.Requestable.values());

                return choices;
            }
        };
    }

    private List<IColumn<RoleType, String>> initColumns() {
        List<IColumn<RoleType, String>> columns = new ArrayList<IColumn<RoleType, String>>();

        IColumn column = new CheckBoxHeaderColumn<RoleType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<RoleType>>(createStringResource("pageRoles.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                RoleType role = rowModel.getObject().getValue();
                roleDetailsPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageRoles.description"), "value.description");
        columns.add(column);

        column = new InlineMenuHeaderColumn(initInlineMenu());
        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("pageRoles.button.delete"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deletePerformed(target);
                    }
                }));

        return headerMenuItems;
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("pageRoles.message.deleteRoleConfirm",
                        getSelectedRoles().size()).getString();
            }
        };
    }

    private TablePanel getRoleTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private ObjectDataProvider<SelectableBean<RoleType>, RoleType> getRoleDataProvider() {
        DataTable table = getRoleTable().getDataTable();
        return (ObjectDataProvider<SelectableBean<RoleType>, RoleType>) table.getDataProvider();
    }

    private List<RoleType> getSelectedRoles() {
        ObjectDataProvider<SelectableBean<RoleType>, RoleType> provider = getRoleDataProvider();

        List<SelectableBean<RoleType>> rows = provider.getAvailableData();
        List<RoleType> selected = new ArrayList<RoleType>();
        for (SelectableBean<RoleType> row : rows) {
            if (row.isSelected()) {
                selected.add(row.getValue());
            }
        }

        return selected;
    }

    private void deletePerformed(AjaxRequestTarget target) {
        List<RoleType> selected = getSelectedRoles();
        if (selected.isEmpty()) {
            warn(getString("pageRoles.message.nothingSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<RoleType> selected = getSelectedRoles();

        OperationResult result = new OperationResult(OPERATION_DELETE_ROLES);
        for (RoleType role : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_ROLES);

                ObjectDelta delta = ObjectDelta.createDeleteDelta(RoleType.class, role.getOid(), getPrismContext());
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            } catch (Exception ex) {
                result.recordPartialError("Couldn't delete role.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete role", ex);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus("Error occurred during role deleting.");
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The role(s) have been successfully deleted.");
        }

        ObjectDataProvider provider = getRoleDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getRoleTable());
    }

    private void roleDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        setResponsePage(PageRole.class, parameters);
    }

    private void listRolesPerformed(AjaxRequestTarget target){
        RolesSearchDto dto = searchModel.getObject();
        String text = dto.getText();
        Boolean requestable = dto.getRequestableValue();
        ObjectQuery query = new ObjectQuery();

        ObjectDataProvider provider = getRoleDataProvider();
        if(StringUtils.isNotEmpty(text)){
            try{
                PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
                String normalizedText = normalizer.normalize(text);

                ObjectFilter substring = SubstringFilter.createSubstring(RoleType.F_NAME, RoleType.class, getPrismContext(),
                        PolyStringNormMatchingRule.NAME, normalizedText);

                if(requestable == null){
                    query.setFilter(substring);
                } else {
                    EqualsFilter boolFilter = EqualsFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
                            null, requestable);

                    if (requestable == true){
                        query.setFilter(AndFilter.createAnd(substring, boolFilter));

                    } else {
                        boolFilter = EqualsFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
                                null, false);
                        EqualsFilter nullFilter = EqualsFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
                                null, null);
                        OrFilter or = OrFilter.createOr(boolFilter, nullFilter);
                        query.setFilter(AndFilter.createAnd(substring, or));
                    }
                }

                provider.setQuery(query);

            }catch (Exception e){
                LoggingUtils.logException(LOGGER, "Couldn't create filter", e);
                error(getString("pageRoles.message.queryError", e.getMessage()));
                target.add(getFeedbackPanel());
            }
        }else{
            if(requestable == null){
                query.setFilter(null);
            } else {
                EqualsFilter boolFilter = EqualsFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
                        null, requestable);

                if (requestable == true){
                    query.setFilter(boolFilter);
                }
                else {
                    boolFilter = EqualsFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
                            null, false);
                    EqualsFilter nullFilter = EqualsFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
                            null, null);

                    OrFilter or = OrFilter.createOr(boolFilter, nullFilter);
                    query.setFilter(or);
                }
            }
            provider.setQuery(query);
        }

        RolesStorage storage = getSessionStorage().getRoles();
        storage.setRolesSearch(dto);

        TablePanel table = getRoleTable();
        target.add(table);
        target.add(getFeedbackPanel());
    }
}
