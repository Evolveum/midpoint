/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.component.mining.*;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.RoleAnalyseHelper;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.CombinationHelperAlgorithm;
import com.evolveum.midpoint.gui.api.component.mining.structure.JaccardDataStructure;
import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMembersList;
import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMiningUserStructure;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleMining", matchUrlForSecurity = "/admin/roleMining")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, label = "PageAdminRoles.auth.roleAll.label", description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })

public class PageRoleMiningOld extends PageAdmin {
    private static final String DOT_CLASS = PageRoleMiningOld.class.getName() + ".";
    private static final String ID_MAIN_FORM = "main_form";
    private static final String ID_SECONDARY_FORM = "secondary_form";
    private static final String ID_TABLE_BASIC = "table_basic";
    private static final String ID_TABLE_MINING = "table_mining";
    private static final String ID_TABLE_PERM_CONFIDENCE = "table_perm_confidence";
    private static final String ID_TABLE_MEM_CONFIDENCE = "table_mem_confidence";
    private static final String ID_TABLE_MEM_SIMPLE_CONFIDENCE = "table_simple_mem_confidence";
    private static final String ID_TABLE_MEM_INTERSECTION_ROLE = "table_intersection_roles";
    private static final String ID_TABLE_MEM_INTERSECTION_USER = "table_intersection_users";
    private static final String ID_TABLE_MEM_JACQUARD_USER = "table_jaccard_users";
    private static final String ID_ROLE_SEARCH = "role_search";
    private static final String ID_USER_SEARCH = "user_search";
    private static final String ID_BUTTON_NEXT_RESULT = "button_next_result";
    private static final String ID_FORM_MIN_SIZE = "min_size_form";
    private static final String ID_MIN_SIZE = "input_min_size";
    private static final String ID_JACCARD_THRESHOLD_INPUT = "jaccard_threshold_input";
    private static final String ID_JACCARD_MIN_ROLES_COUNT_INPUT = "jaccard_min_roles_count_input";
    private static final String ID_FORM_JACCARD_THRESHOLD = "jaccard_threshold_form";
    private static final String ID_CALCULATOR = "calculator";
    private static final String ID_JACCARD_AJAX_LINK = "jaccard_execute_search";
    private static final String ID_LABEL_DUPLICATES_BASIC = "repeatingCountBasicTable";
    private static final String ID_LABEL_RESULT_COUNT = "resultCountLabel";
    private static final String ID_AJAX_CHECK_DUPLICATE_BASIC = "checkDuplicates";
    private static final String ID_BASIC_ROLE_TABLE_INFO = "basic_check_info";
    private static final String ID_JACCARD_THRESHOLD_INPUT_INFO = "jaccard_threshold_input_info";
    private static final String ID_JACCARD_MIN_ROLES_COUNT_INFO = "jaccard_min_roles_count_info";
    private static final String ID_JACCARD_EXECUTE_SEARCH_INFO = "jaccard_execute_search_info";
    private static final String ID_MIN_INTERSECTION_INPUT_INFO = "min_size_input_info";
    private static final String ID_EXECUTE_JACCARD_SETTING = "jaccard_execute_details";
    private static final String ID_HELPER_ALG_EXECUTE_INFO = "min_size_execute_info";

    double customSum = 0; //helper
    int basicCombMinIntersection = 4; //default 4 role;
    List<List<String>> basicCombResult;
    int basicCombDisplayResultIterator = 0;

    List<PrismObject<RoleType>> jaccardResultRoles;
    List<PrismObject<UserType>> jaccardUsersAnalysed;
    List<JaccardDataStructure> jaccardDataStructureList;
    double jaccardThreshold = 0.5; //default
    int jaccardMinRolesCount = 1; //default

    boolean searchMode = true;  //false: user   true: role

    public PageRoleMiningOld() {
        super();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(JavaScriptHeaderItem.forReference(
                new PackageResourceReference(PageRoleMiningOld.class, "js/jaccard_chart.js")));

        List<String> objectName = new ArrayList<>();
        List<Double> totalJaccard = new ArrayList<>();
        List<List<Double>> partialJaccardMap = new ArrayList<>();
        for (JaccardDataStructure jaccardDataStructure : jaccardDataStructureList) {
            objectName.add(jaccardDataStructure.getUserObject().getName().toString());
            totalJaccard.add(jaccardDataStructure.getObjectTotalResult());
            partialJaccardMap.add(jaccardDataStructure.getObjectPartialResult());
        }

        String javaScript = "jaccard_chart('"
                + objectName + "', '"
                + totalJaccard + "', '"
                + partialJaccardMap + "', '"
                + jaccardThreshold + "');";

        response.render(OnDomReadyHeaderItem.forScript(javaScript));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        List<PrismObject<RoleType>> roles;
        List<PrismObject<UserType>> users;
        try {
            roles = getRoles();
            users = getUsers();
        } catch (CommonException e) {
            throw new RuntimeException("Failed to load basic role mining list: " + e);
        }

        List<RoleMembersList> roleMembersListList;
        List<RoleMiningUserStructure> roleMiningData;
        try {
            roleMembersListList = getRolesAndMembers(roles);
            roleMiningData = getRoleMiningData(users);
        } catch (CommonException e) {
            throw new RuntimeException("Failed to load role mining list: " + e);
        }

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        basicOperationHelper(mainForm, roles, users);
        searchSelector(mainForm, roles, users);

        try {
            if (isSearchMode()) {
                mainForm.add(basicRoleHelperTable());
                mainForm.add(roleRoleMiningTable(users));
            } else {
                mainForm.add(basicUserHelperTable());
                mainForm.add(userRoleMiningTable(roles));
            }
        } catch (CommonException e) {
            throw new RuntimeException("Failed to load basic role mining table: " + e);
        }

        Form<?> secondaryForm = new MidpointForm<>(ID_SECONDARY_FORM);
        secondaryForm.setOutputMarkupId(true);
        add(secondaryForm);

        jaccardThresholdSubmit(secondaryForm, roleMiningData);

        executeJaccardRolesSearch(users, secondaryForm);
        fillJaccardData(roleMiningData, jaccardThreshold);

        try {
            secondaryForm.add(confidencePermRoleMiningTable(roleMembersListList));
            secondaryForm.add(confidenceMemRoleMiningTable(roleMembersListList));

            secondaryForm.add(calculatorReset());
            secondaryForm.add(intersectionMemRoleMiningTable(roleMembersListList));
            secondaryForm.add(intersectionMemUserMiningTable(roleMiningData));
            secondaryForm.add(jaccardIndexUserRoleMiningTable(roleMiningData));
            secondaryForm.add(confidenceMemSimpleRoleMiningTable(roleMembersListList));
        } catch (CommonException e) {
            throw new RuntimeException("Failed to load analyze role mining table: " + e);
        }

    }

    private void executeJaccardRolesSearch(List<PrismObject<UserType>> users, Form<?> secondForm) {

        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(ID_JACCARD_AJAX_LINK, Model.of("Execute intersection search")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                basicCombResult = null;
                getSelectionJaccardUserObjectId(users);
                getMiningTable().replaceWith(getMiningTable());
                target.add(getMiningTable());
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);
        secondForm.add(WebComponentUtil.createHelp(ID_JACCARD_EXECUTE_SEARCH_INFO));
        secondForm.add(ajaxLinkPanel);

    }

    private void getSelectionJaccardUserObjectId(List<PrismObject<UserType>> users) {
        String userObjectId = null;
        if (getJaccardTable().getSelectedObjects() != null && getJaccardTable().getSelectedObjects().size() == 1) {
            PrismObject<? extends ObjectType> userTypePrismObject = getJaccardTable().getSelectedObjects().get(0).getValue().asPrismObject();
            userObjectId = userTypePrismObject.getOid();
        }
        int rowPosition = 0;

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getOid().equals(userObjectId)) {
                rowPosition = i;
                break;
            }
        }

        jaccardUsersAnalysed = new ArrayList<>();
        for (int j = 0; j < jaccardDataStructureList.get(rowPosition).getObjectPartialResult().size(); j++) {
            if (jaccardDataStructureList.get(rowPosition).getObjectPartialResult().get(j) > jaccardThreshold) {
                jaccardUsersAnalysed.add(users.get(j));
            }
        }

        RoleAnalyseHelper roleAnalyseHelper = new RoleAnalyseHelper();
        List<ObjectReferenceType> rolesForCompare = getRoleObjectReferenceTypes(users.get(rowPosition).asObjectable());
        for (PrismObject<UserType> userTypePrismObject : jaccardUsersAnalysed) {
            rolesForCompare = roleAnalyseHelper.roleIntersected(rolesForCompare,
                    getRoleObjectReferenceTypes(userTypePrismObject.asObjectable()));
        }

        jaccardResultRoles = new ArrayList<>();
        for (ObjectReferenceType objectReferenceType : rolesForCompare) {
            try {
                jaccardResultRoles.add(getRoleByOid(objectReferenceType.getOid()));
            } catch (CommonException e) {
                e.printStackTrace();
            }

        }
    }

    private void printCounterResult(String resultCount) {
        //   System.out.println("Calculator sum: " + resultCount);
    }

    private void fillJaccardData(List<RoleMiningUserStructure> roleMiningData, double inputJaccardThreshold) {

        jaccardDataStructureList = new ArrayList<>();

        int dataCount = roleMiningData.size();
        for (int i = 0; i < dataCount; i++) {
            PrismObject<UserType> objectName = roleMiningData.get(i).getUserObject();
            double objectTotalResult = 0.0;
            ArrayList<Double> objectPartialResult = new ArrayList<>();
            for (RoleMiningUserStructure roleMiningDatum : roleMiningData) {
                double jaccardIndex = new RoleAnalyseHelper().jaccardIndex(
                        roleMiningData.get(i).getRoleObjectId(),
                        roleMiningDatum.getRoleObjectId(), jaccardMinRolesCount
                );

                if (jaccardIndex < inputJaccardThreshold) {
                    jaccardIndex = 0.0;
                }

                objectTotalResult = objectTotalResult + jaccardIndex;
                objectPartialResult.add(jaccardIndex);
            }
            objectTotalResult = objectTotalResult / dataCount;
            jaccardDataStructureList.add(new JaccardDataStructure(objectName, objectTotalResult, objectPartialResult));
        }

    }

    private void jaccardThresholdSubmit(Form<?> mainForm, List<RoleMiningUserStructure> roleMiningData) {
        final TextField<Double> inputThreshold = new TextField<>(ID_JACCARD_THRESHOLD_INPUT, Model.of(jaccardThreshold));
        inputThreshold.setOutputMarkupId(true);

        final TextField<Integer> inputMinRolesCount = new TextField<>(ID_JACCARD_MIN_ROLES_COUNT_INPUT, Model.of(jaccardMinRolesCount));
        inputMinRolesCount.setOutputMarkupId(true);

        Form<?> form = new Form<Void>(ID_FORM_JACCARD_THRESHOLD) {
            @Override
            protected void onSubmit() {
                if (inputThreshold.getModelObject() <= 1) {
                    jaccardThreshold = inputThreshold.getModelObject();
                } else {
                    jaccardThreshold = 1;
                }
                jaccardMinRolesCount = inputMinRolesCount.getModelObject();
                fillJaccardData(roleMiningData, jaccardThreshold);
            }

        };

        form.setOutputMarkupId(true);
        add(form);

        form.add(WebComponentUtil.createHelp(ID_EXECUTE_JACCARD_SETTING));
        form.add(WebComponentUtil.createHelp(ID_JACCARD_THRESHOLD_INPUT_INFO));
        form.add(WebComponentUtil.createHelp(ID_JACCARD_MIN_ROLES_COUNT_INFO));
        form.add(inputThreshold);
        form.add(inputMinRolesCount);

        mainForm.add(form);
    }

    private void executeBasicMining(int minSize, List<PrismObject<RoleType>> roles, List<PrismObject<UserType>> users) {
        List<String> rolesOid = new ArrayList<>();

        for (PrismObject<RoleType> role : roles) {
            rolesOid.add(role.getOid());
        }

        List<List<String>> allCombinations = new CombinationHelperAlgorithm().generateCombinations(rolesOid, minSize);

        List<List<String>> matrix = getMatrix(users);
        basicCombResult = new CombinationHelperAlgorithm().combinationsResult(allCombinations, matrix);
    }

    private AjaxLinkPanel calculatorReset() {
        AjaxLinkPanel calculator = new AjaxLinkPanel(ID_CALCULATOR, Model.of("Reset calculator")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                customSum = 0;
                printCounterResult(String.valueOf(customSum));
            }
        };
        calculator.setOutputMarkupId(true);
        return calculator;
    }

    private void basicOperationHelper(Form<?> mainForm, List<PrismObject<RoleType>> roles, List<PrismObject<UserType>> users) {

        Label repeatingCountBasicTable = new Label(ID_LABEL_DUPLICATES_BASIC, Model.of("0 duplicates"));
        repeatingCountBasicTable.setOutputMarkupId(true);
        repeatingCountBasicTable.setOutputMarkupPlaceholderTag(true);
        repeatingCountBasicTable.add(new VisibleBehaviour((this::isSearchMode)));
        mainForm.add(repeatingCountBasicTable);

        Label resultCountLabel = new Label(ID_LABEL_RESULT_COUNT, Model.of(createIterationResultString(0, 0)));
        resultCountLabel.setOutputMarkupId(true);

        mainForm.add(resultCountLabel);

        final TextField<Integer> inputMinIntersectionSize = new TextField<>(ID_MIN_SIZE, Model.of(basicCombMinIntersection));
        inputMinIntersectionSize.setOutputMarkupId(true);

        Form<?> form = new Form<Void>(ID_FORM_MIN_SIZE) {
            @Override
            protected void onSubmit() {
                basicCombDisplayResultIterator = 1;
                basicCombMinIntersection = inputMinIntersectionSize.getModelObject();
                executeBasicMining(basicCombMinIntersection, roles, users);

                if (basicCombResult != null) {
                    if (basicCombDisplayResultIterator == basicCombResult.size()) {
                        basicCombDisplayResultIterator = 0;
                    }
                    getResultCountLabel().setDefaultModel(Model.of(createIterationResultString(basicCombDisplayResultIterator, basicCombResult.size())));
                } else {
                    getResultCountLabel().setDefaultModel(Model.of(createIterationResultString(0, 0)));

                }
            }

        };

        form.setOutputMarkupId(true);
        form.add(WebComponentUtil.createHelp(ID_HELPER_ALG_EXECUTE_INFO));
        form.add(WebComponentUtil.createHelp(ID_MIN_INTERSECTION_INPUT_INFO));
        add(form);

        form.add(inputMinIntersectionSize);

        mainForm.add(form);

        AjaxLink<?> duplicateRolesComb = new AjaxLink<>(ID_AJAX_CHECK_DUPLICATE_BASIC) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (getBasicTable().getSelectedObjects() != null) {

                    List<String> combination = new ArrayList<>();
                    for (int i = 0; i < getBasicTable().getSelectedObjects().size(); i++) {
                        combination.add(getBasicTable().getSelectedObjects().get(i).getValue().getOid());
                    }

                    getRepeatingCountBasicTable().setDefaultModel(Model.of(
                            new CombinationHelperAlgorithm().findDuplicates(combination, getMatrix(users)) + " duplicates"));

                    ajaxRequestTarget.add(getRepeatingCountBasicTable());

                }
            }
        };

        duplicateRolesComb.setOutputMarkupId(true);
        duplicateRolesComb.add(new VisibleBehaviour((this::isSearchMode)));
        duplicateRolesComb.setOutputMarkupPlaceholderTag(true);
        duplicateRolesComb.add(WebComponentUtil.createHelp(ID_BASIC_ROLE_TABLE_INFO));

        mainForm.add(duplicateRolesComb);
    }

    private void searchSelector(Form<?> mainForm, List<PrismObject<RoleType>> roles, List<PrismObject<UserType>> users) {

        AjaxLink<Boolean> buttonNextResult = new AjaxLink<>(ID_BUTTON_NEXT_RESULT) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (basicCombResult != null) {
                    if (basicCombDisplayResultIterator != 0) {
                        if (basicCombDisplayResultIterator == basicCombResult.size()) {
                            basicCombDisplayResultIterator = 0;
                        }
                    }
                    basicCombDisplayResultIterator++;
                    ajaxRequestTarget.add(getMiningTable());
                    getResultCountLabel().setDefaultModel(Model.of(createIterationResultString(basicCombDisplayResultIterator, basicCombResult.size())));
                } else {
                    getResultCountLabel().setDefaultModel(Model.of(createIterationResultString(0, 0)));
                }

                ajaxRequestTarget.add(getResultCountLabel());
            }
        };

        mainForm.add(buttonNextResult);

        AjaxLink<Boolean> roleSearch = new AjaxLink<>(ID_ROLE_SEARCH) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setSearchMode(true);

                target.add(getRepeatingCountBasicTable());
                target.add(getAjaxCheckDuplicateBasic());
                getBasicTable().replaceWith(basicRoleHelperTable());
                target.add(getBasicTable());

                try {
                    getMiningTable().replaceWith(roleRoleMiningTable(users));
                    target.add(getMiningTable());
                } catch (CommonException e) {
                    throw new RuntimeException(e);
                }

                getUserSearchLink().add(new AttributeModifier("class", " btn btn-default"));
                target.add(getUserSearchLink());

                getRoleSearchLink().add(new AttributeModifier("class", " btn btn-secondary"));
                target.add(getRoleSearchLink());
            }
        };

        roleSearch.setOutputMarkupId(true);

        roleSearch.add(new AttributeModifier("class", " btn btn-secondary"));
        mainForm.add(roleSearch);

        AjaxLink<Boolean> userSearch = new AjaxLink<>(ID_USER_SEARCH) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setSearchMode(false);
                target.add(getAjaxCheckDuplicateBasic());
                target.add(getRepeatingCountBasicTable());
                getBasicTable().replaceWith(basicUserHelperTable());
                target.add(getBasicTable());

                try {
                    getMiningTable().replaceWith(userRoleMiningTable(roles));
                    target.add(getMiningTable());
                } catch (CommonException e) {
                    throw new RuntimeException(e);
                }

                getRoleSearchLink().add(new AttributeModifier("class", " btn btn-default"));
                target.add(getRoleSearchLink());

                getUserSearchLink().add(new AttributeModifier("class", " btn btn-secondary"));
                target.add(getUserSearchLink());
            }

        };
        userSearch.setOutputMarkupId(true);
        mainForm.add(userSearch);
    }

    protected AjaxLink<?> getAjaxCheckDuplicateBasic() {
        return (AjaxLink<?>) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_AJAX_CHECK_DUPLICATE_BASIC));
    }

    protected Label getResultCountLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_LABEL_RESULT_COUNT));
    }

    protected Label getRepeatingCountBasicTable() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_LABEL_DUPLICATES_BASIC));
    }

    protected AjaxLink<?> getUserSearchLink() {
        return (AjaxLink<?>) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_USER_SEARCH));
    }

    protected AjaxLink<?> getRoleSearchLink() {
        return (AjaxLink<?>) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_ROLE_SEARCH));
    }

    protected MainObjectListPanel<?> getBasicTable() {
        return (MainObjectListPanel<?>) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_TABLE_BASIC));
    }

    protected MainObjectListPanel<?> getMiningTable() {
        return (MainObjectListPanel<?>) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_TABLE_MINING));
    }

    protected MainObjectListPanel<?> getJaccardTable() {
        return (MainObjectListPanel<?>) get(((PageBase) getPage()).createComponentPath(ID_SECONDARY_FORM, ID_TABLE_MEM_JACQUARD_USER));
    }

    protected MainObjectListPanel<?> basicUserHelperTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_TABLE_BASIC, UserType.class) {

            @Override
            protected ISelectableDataProvider<SelectableBean<UserType>> createProvider() {
                return super.createProvider();
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<UserType>, String> column = new PropertyColumn<>(createStringResource("UserType.givenName"),
                        SelectableBeanImpl.F_VALUE + ".givenName");
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("UserType.assignments.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                            String componentId, IModel<SelectableBean<UserType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getAssignment() != null ?
                                        model.getObject().getValue().getAssignment().size() : null));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
                        return Model.of(rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getAssignment() != null ?
                                Integer.toString(rowModel.getObject().getValue().getAssignment().size()) : "");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("UserType.assignments.roles.count")) {
                    @Override
                    public String getSortProperty() {
                        return super.getSortProperty();
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                            String componentId, IModel<SelectableBean<UserType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getRoleMembershipRef() != null) {
                            AssignmentHolderType object = model.getObject().getValue();
                            cellItem.add(new Label(componentId,
                                    getRoleObjectReferenceTypes(object).size()));
                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
                        if (rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getAssignment() != null) {
                            AssignmentHolderType object = rowModel.getObject().getValue();
                            return Model.of(Integer.toString(getRoleObjectReferenceTypes(object).size()));
                        }
                        return Model.of("");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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
        basicTable.setOutputMarkupId(true);

        return basicTable;
    }

    protected MainObjectListPanel<?> basicRoleHelperTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_TABLE_BASIC, RoleType.class) {
            @Override
            public List<SelectableBean<RoleType>> isAnythingSelected(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> selectedObject) {
                return super.isAnythingSelected(target, selectedObject);
            }

            @Override
            public String getTb(String s) {
                return super.getTb(s);
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column = new PropertyColumn<>(createStringResource("RoleType.description"),
                        null,
                        SelectableBeanImpl.F_VALUE + ".description");
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleType.members.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {

                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getAssignment() != null ?
                                        getMembers(model.getObject().getValue().getOid()).size() : null));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                        return Model.of(rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getAssignment() != null ?
                                Integer.toString(rowModel.getObject().getValue().getAssignment().size()) : "");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("PageRoleEditor.label.riskLevel"), RoleType.F_RISK_LEVEL.getLocalPart()) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        try {
                            cellItem.add(new Label(componentId,
                                    model.getObject().getValue().getRiskLevel() != null ?
                                            model.getObject().getValue().getRiskLevel() : null));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                        return Model.of(rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getAssignment() != null ?
                                Integer.toString(rowModel.getObject().getValue().getAssignment().size()) : "");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };

                columns.add(column);
                return columns;

            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

        basicTable.setOutputMarkupId(true);
        return basicTable;
    }

    private void detailsPerformed(Class<? extends WebPage> pageClass, String objectOid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, objectOid);
        ((PageBase) getPage()).navigateToNext(pageClass, parameters);
    }

    protected MainObjectListPanel<?> userRoleMiningTable(List<PrismObject<RoleType>> roles) throws CommonException {

        MainObjectListPanel<?> miningTable = new MainObjectListPanel<>(ID_TABLE_MINING, UserType.class, true) {

            @Override
            protected ISelectableDataProvider<SelectableBean<UserType>> createProvider() {
                return super.createProvider();
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<UserType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return "col-sm-1 overflow-auto";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                        UserType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < roles.size(); i++) {

                    int finalI = i;

                    IColumn<SelectableBean<UserType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(roles.get(finalI).getName().toString())) {

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                                String componentId, IModel<SelectableBean<UserType>> model) {
                            tableUserTypeStyle(cellItem);
                            List<ObjectReferenceType> objectReferenceTypes = getRoleObjectReferenceTypes(model.getObject().getValue());

                            ArrayList<String> rolesObjectIds = new ArrayList<>();
                            for (ObjectReferenceType objectReferenceType : objectReferenceTypes) {
                                rolesObjectIds.add(objectReferenceType.getOid());
                            }

                            PrismObject<UserType> modelUser = model.getObject().getValue().asPrismObject();

                            if (basicCombResult != null) {
                                if (basicCombDisplayResultIterator == basicCombResult.size()) {
                                    basicCombDisplayResultIterator = 0;
                                }
                                ArrayList<String> rolesOid = new ArrayList<>(basicCombResult.get(basicCombDisplayResultIterator));

                                if (rolesObjectIds.containsAll(rolesOid)) {

                                    if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                        if (rolesOid.contains(roles.get(finalI).getOid())) {
                                            algMatchedUserTypeCell(cellItem, componentId);
                                        } else {
                                            filledUserTypeCell(cellItem, componentId);
                                        }
                                    } else {
                                        basicUserTypeCell(cellItem, componentId);

                                    }

                                } else {

                                    if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                        filledUserTypeCell(cellItem, componentId);
                                    } else {
                                        basicUserTypeCell(cellItem, componentId);

                                    }

                                }
                            } else if (jaccardUsersAnalysed != null && jaccardResultRoles != null) {

                                ArrayList<String> jaccardResultRolesOid = new ArrayList<>();
                                for (PrismObject<RoleType> jaccardResultRole : jaccardResultRoles) {
                                    jaccardResultRolesOid.add(jaccardResultRole.getOid());
                                }

                                if (jaccardUsersAnalysed.contains(modelUser) && rolesObjectIds.containsAll(jaccardResultRolesOid)) {

                                    if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                        if (jaccardResultRolesOid.contains(roles.get(finalI).getOid())) {
                                            algMatchedUserTypeCell(cellItem, componentId);
                                        } else {
                                            filledUserTypeCell(cellItem, componentId);
                                        }
                                    } else {
                                        basicUserTypeCell(cellItem, componentId);

                                    }

                                } else {

                                    if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                        filledUserTypeCell(cellItem, componentId);
                                    } else {
                                        basicUserTypeCell(cellItem, componentId);

                                    }

                                }

                            } else {

                                if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                    filledUserTypeCell(cellItem, componentId);
                                } else {
                                    basicUserTypeCell(cellItem, componentId);
                                }

                            }

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
                            AssignmentHolderType assignmentHolderType = rowModel.getObject().getValue();
                            List<ObjectReferenceType> objectReferenceTypes = getRoleObjectReferenceTypes(assignmentHolderType);

                            ArrayList<String> rolesObjectIds = new ArrayList<>();

                            for (ObjectReferenceType objectReferenceType : objectReferenceTypes) {
                                rolesObjectIds.add(objectReferenceType.getOid());
                            }

                            if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                return Model.of(roles.get(finalI).getOid());
                            }
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {
                            return new AjaxLinkPanel(componentId, createStringResource(roles.get(finalI).getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    RoleType object = roles.get(finalI).asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };
                    columns.add(column);
                }

                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

        miningTable.setOutputMarkupId(true);

        return miningTable;

    }

    protected MainObjectListPanel<?> roleRoleMiningTable(List<PrismObject<UserType>> users) throws CommonException {

        MainObjectListPanel<?> miningTable = new MainObjectListPanel<>(ID_TABLE_MINING, RoleType.class, true) {
            @Override
            protected ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                return super.createProvider();
            }

            @Override
            public LoadableDetachableModel<Search<RoleType>> getSearchModel() {
                return super.getSearchModel();
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-sm-1 overflow-auto";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                        RoleType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < users.size(); i++) {
                    int finalI = i;

                    IColumn<SelectableBean<RoleType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(users.get(finalI).getName().toString())) {

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleType>> model) {

                            tableRoleTypeStyle(cellItem);

                            PrismObject<UserType> userTypePrismObject = users.get(finalI);
                            AssignmentHolderType assignmentHolderType = userTypePrismObject.asObjectable();

                            String currentRoleOid = model.getObject().getValue().asPrismObject().getOid();
                            List<ObjectReferenceType> objectReferenceTypes = getRoleObjectReferenceTypes(assignmentHolderType);

                            ArrayList<String> userRolesObjectIds = new ArrayList<>();
                            for (ObjectReferenceType objectReferenceType : objectReferenceTypes) {
                                userRolesObjectIds.add(objectReferenceType.getOid());
                            }

                            if (basicCombResult != null) {
                                if (basicCombDisplayResultIterator == basicCombResult.size()) {
                                    basicCombDisplayResultIterator = 0;
                                }
                                ArrayList<String> algorithmRoleOid = new ArrayList<>(basicCombResult.get(basicCombDisplayResultIterator));

                                if (userRolesObjectIds.containsAll(algorithmRoleOid) && algorithmRoleOid.contains(currentRoleOid)) {
                                    algMatchedRoleTypeCell(cellItem, componentId);
                                } else if (userRolesObjectIds.contains(currentRoleOid)) {
                                    filledRoleTypeCell(cellItem, componentId);
                                } else {
                                    basicRoleTypeCell(cellItem, componentId);
                                }
                            } else if (jaccardUsersAnalysed != null && jaccardResultRoles != null) {
                                ArrayList<String> jaccardResultRolesOid = new ArrayList<>();
                                for (PrismObject<RoleType> jaccardResultRole : jaccardResultRoles) {
                                    jaccardResultRolesOid.add(jaccardResultRole.getOid());
                                }

                                if (userRolesObjectIds.containsAll(jaccardResultRolesOid) && jaccardResultRolesOid.contains(currentRoleOid)) {
                                    algMatchedRoleTypeCell(cellItem, componentId);
                                } else if (userRolesObjectIds.contains(currentRoleOid)) {
                                    filledRoleTypeCell(cellItem, componentId);
                                } else {
                                    basicRoleTypeCell(cellItem, componentId);
                                }

                            } else {
                                if (userRolesObjectIds.contains(currentRoleOid)) {
                                    filledRoleTypeCell(cellItem, componentId);
                                } else {
                                    basicRoleTypeCell(cellItem, componentId);
                                }
                            }

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                            String oid = rowModel.getObject().getValue().getOid();

                            AssignmentHolderType assignmentHolderType = users.get(finalI).asObjectable();
                            List<ObjectReferenceType> objectReferenceTypes = getRoleObjectReferenceTypes(assignmentHolderType);

                            ArrayList<String> rolesObjectIds = new ArrayList<>();

                            for (ObjectReferenceType objectReferenceType : objectReferenceTypes) {
                                rolesObjectIds.add(objectReferenceType.getOid());
                            }

                            if (rolesObjectIds.contains(oid)) {
                                return Model.of(users.get(finalI).getDisplayName());
                            }
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {

                            return new AjaxLinkPanel(componentId, createStringResource(users.get(finalI).getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    UserType object = users.get(finalI).asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageUser.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };

                    columns.add(column);
                }

                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

        miningTable.setOutputMarkupId(true);

        return miningTable;
    }

    protected MainObjectListPanel<?> confidencePermRoleMiningTable(List<RoleMembersList> roleMembersListList) throws CommonException {
        RoleAnalyseHelper roleAnalyseHelper = new RoleAnalyseHelper();

        MainObjectListPanel<?> confidenceTable = new MainObjectListPanel<>(ID_TABLE_PERM_CONFIDENCE, RoleType.class, true) {

            @Override
            public LoadableDetachableModel<Search<RoleType>> getSearchModel() {
                return super.getSearchModel();
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-sm-1";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                        RoleType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < roleMembersListList.size(); i++) {
                    int finalI = i;

                    IColumn<SelectableBean<RoleType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleType>> model) {

                            tableRoleTypeStyle(cellItem);

                            @NotNull RoleType roleA = roleMembersListList.get(finalI).getRole().asObjectable();

                            String roleObjectIdA = model.getObject().getValue().getOid();
                            String roleObjectIdB = roleA.getOid();

                            List<AuthorizationType> authorizationRoleA = roleA.getAuthorization();
                            List<AuthorizationType> authorizationRoleB = model.getObject().getValue().getAuthorization();

                            boolean sameObject = roleObjectIdA.equals(roleObjectIdB);
                            double confidence = roleAnalyseHelper.permissionSimilarity(authorizationRoleA, authorizationRoleB);
                            cellItem.add(new Label(componentId, confidence).add(new AttributeAppender("class", "row")));

                            if (sameObject) {
                                cellItem.add(new AttributeAppender("class", " table-warning"));
                            } else if (confidence > 0.5) {
                                cellItem.add(new AttributeAppender("class", " table-danger"));
                            }

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {

                            return new AjaxLinkPanel(componentId, createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    RoleType object = roleMembersListList.get(finalI).getRole().asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };

                    columns.add(column);
                }

                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

        confidenceTable.setOutputMarkupId(true);

        return confidenceTable;
    }

    protected MainObjectListPanel<?> confidenceMemRoleMiningTable(List<RoleMembersList> roleMembersListList) throws CommonException {
        RoleAnalyseHelper roleAnalyseHelper = new RoleAnalyseHelper();

        MainObjectListPanel<?> confidenceTable = new MainObjectListPanel<>(ID_TABLE_MEM_CONFIDENCE, RoleType.class, true) {
            @Override
            protected ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                return super.createProvider();
            }

            @Override
            public LoadableDetachableModel<Search<RoleType>> getSearchModel() {
                return super.getSearchModel();
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-sm-1";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                        RoleType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < roleMembersListList.size(); i++) {
                    int finalI = i;

                    IColumn<SelectableBean<RoleType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleType>> model) {

                            tableRoleTypeStyle(cellItem);

                            String roleObjectIdA = model.getObject().getValue().getOid();
                            List<PrismObject<UserType>> membersRoleA = null;

                            for (RoleMembersList roleAndMembers : roleMembersListList) {
                                if (roleAndMembers.getRole().getOid().equals(roleObjectIdA)) {
                                    membersRoleA = roleAndMembers.getMembers();
                                }
                            }

                            String roleObjectIdB = roleMembersListList.get(finalI).getRole().getOid();
                            List<PrismObject<UserType>> membersRoleB = roleMembersListList.get(finalI).getMembers();

                            boolean sameObject = roleObjectIdA.equals(roleObjectIdB);
                            double confidence = roleAnalyseHelper.membersSimilarity(membersRoleA, membersRoleB);
                            cellItem.add(new Label(componentId, confidence).add(new AttributeAppender("class", "row")));
                            if (sameObject) {
                                cellItem.add(new AttributeAppender("class", " table-warning"));
                            } else if (confidence > 0.5) {
                                cellItem.add(new AttributeAppender("class", " table-danger"));
                            }

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {

                            return new AjaxLinkPanel(componentId, createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    RoleType object = roleMembersListList.get(finalI).getRole().asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };

                    columns.add(column);
                }

                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

        confidenceTable.setOutputMarkupId(true);

        return confidenceTable;
    }

    protected MainObjectListPanel<?> confidenceMemSimpleRoleMiningTable(List<RoleMembersList> roleMembersListList) throws CommonException {
        RoleAnalyseHelper roleAnalyseHelper = new RoleAnalyseHelper();

        MainObjectListPanel<?> confidenceTable = new MainObjectListPanel<>(ID_TABLE_MEM_SIMPLE_CONFIDENCE, RoleType.class, true) {

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-sm-1";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                        RoleType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < roleMembersListList.size(); i++) {
                    int finalI = i;

                    IColumn<SelectableBean<RoleType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {
                        double confidence = 0;

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleType>> model) {

                            tableRoleTypeStyle(cellItem);

                            String roleObjectIdA = model.getObject().getValue().getOid();
                            List<PrismObject<UserType>> membersRoleA = null;

                            for (RoleMembersList roleAndMembers : roleMembersListList) {
                                if (roleAndMembers.getRole().getOid().equals(roleObjectIdA)) {
                                    membersRoleA = roleAndMembers.getMembers();
                                }
                            }

                            String roleObjectIdB = roleMembersListList.get(finalI).getRole().getOid();
                            List<PrismObject<UserType>> membersRoleB = roleMembersListList.get(finalI).getMembers();

                            boolean sameObject = roleObjectIdA.equals(roleObjectIdB);
                            confidence = roleAnalyseHelper.membersSimpleSimilarity(membersRoleA, membersRoleB);
                            cellItem.add(new Label(componentId, confidence).add(new AttributeAppender("class", "row")));

                            if (sameObject) {
                                cellItem.add(new AttributeAppender("class", " table-warning"));
                            } else if (confidence > 0.5) {
                                cellItem.add(new AttributeAppender("class", " table-danger"));
                            }

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                            return Model.of(String.valueOf(confidence));
                        }

                        @Override
                        public Component getHeader(String componentId) {

                            return new AjaxLinkPanel(componentId, createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    RoleType object = roleMembersListList.get(finalI).getRole().asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };

                    columns.add(column);
                }

                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

        confidenceTable.setOutputMarkupId(true);

        return confidenceTable;
    }

    protected MainObjectListPanel<?> intersectionMemRoleMiningTable(List<RoleMembersList> roleMembersListList) throws CommonException {
        RoleAnalyseHelper roleAnalyseHelper = new RoleAnalyseHelper();

        MainObjectListPanel<?> confidenceTable = new MainObjectListPanel<>(ID_TABLE_MEM_INTERSECTION_ROLE, RoleType.class, true) {
            @Override
            protected ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                return super.createProvider();
            }

            @Override
            public LoadableDetachableModel<Search<RoleType>> getSearchModel() {
                return super.getSearchModel();
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-sm-1 overflow-auto";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                        RoleType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < roleMembersListList.size(); i++) {
                    int finalI = i;

                    IColumn<SelectableBean<RoleType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                                String componentId, IModel<SelectableBean<RoleType>> model) {

                            tableRoleTypeStyle(cellItem);

                            String roleObjectIdA = model.getObject().getValue().getOid();
                            List<PrismObject<UserType>> membersRoleA = null;

                            for (RoleMembersList roleAndMembers : roleMembersListList) {
                                if (roleAndMembers.getRole().getOid().equals(roleObjectIdA)) {
                                    membersRoleA = roleAndMembers.getMembers();
                                }
                            }

                            String roleObjectIdB = roleMembersListList.get(finalI).getRole().getOid();
                            List<PrismObject<UserType>> membersRoleB = roleMembersListList.get(finalI).getMembers();

                            boolean sameObject = roleObjectIdA.equals(roleObjectIdB);
                            int confidence = roleAnalyseHelper.rolesMembersIntersection(membersRoleA, membersRoleB);
                            cellItem.add(new AjaxLinkPanel(componentId, Model.of(confidence)) {
                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    customSum = customSum + confidence;
                                    printCounterResult(String.valueOf(customSum));
                                }
                            }.add(new AttributeAppender("class", "row")));

                            if (sameObject) {
                                cellItem.add(new AttributeAppender("class", " table-warning"));
                            } else if (confidence > 5) {
                                cellItem.add(new AttributeAppender("class", " table-danger"));
                            } else if (confidence == 1) {
                                cellItem.add(new AttributeAppender("class", " table-active"));
                            } else if (confidence == 2) {
                                cellItem.add(new AttributeAppender("class", " table-primary"));
                            } else if (confidence == 3) {
                                cellItem.add(new AttributeAppender("class", " table-secondary"));
                            } else if (confidence == 4) {
                                cellItem.add(new AttributeAppender("class", " table-success"));
                            } else if (confidence == 5) {
                                cellItem.add(new AttributeAppender("class", " table-info"));
                            }

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                            String roleObjectIdA = rowModel.getObject().getValue().getOid();
                            String roleObjectIdA1 = null;
                            for (RoleMembersList roleAndMembers : roleMembersListList) {
                                if (roleAndMembers.getRole().getOid().equals(roleObjectIdA)) {
                                    roleObjectIdA1 = String.valueOf(roleAndMembers.getRole().getName());
                                }
                            }

                            String roleObjectIdB1 = String.valueOf(roleMembersListList.get(finalI).getRole().getName());

                            return Model.of(roleObjectIdA1 + " + " + roleObjectIdB1);
                        }

                        @Override
                        public Component getHeader(String componentId) {

                            return new AjaxLinkPanel(componentId, createStringResource(roleMembersListList.get(finalI).getRole().getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    RoleType object = roleMembersListList.get(finalI).getRole().asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageRole.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };

                    columns.add(column);
                }

                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
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

        confidenceTable.setOutputMarkupId(true);

        return confidenceTable;
    }

    protected MainObjectListPanel<?> intersectionMemUserMiningTable(List<RoleMiningUserStructure> roleMiningData) throws CommonException {
        RoleAnalyseHelper roleAnalyseHelper = new RoleAnalyseHelper();

        MainObjectListPanel<?> confidenceTable = new MainObjectListPanel<>(ID_TABLE_MEM_INTERSECTION_USER, UserType.class, true) {
            @Override
            protected ISelectableDataProvider<SelectableBean<UserType>> createProvider() {
                return super.createProvider();
            }

            @Override
            public LoadableDetachableModel<Search<UserType>> getSearchModel() {
                return super.getSearchModel();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<UserType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-sm-1 overflow-auto";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                        UserType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < roleMiningData.size(); i++) {
                    int finalI = i;

                    IColumn<SelectableBean<UserType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(roleMiningData.get(finalI).getUserObject().getName().toString())) {

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                                String componentId, IModel<SelectableBean<UserType>> model) {

                            tableUserTypeStyle(cellItem);

                            String userObjectIdA = model.getObject().getValue().getOid();
                            List<String> membersUserA = null;

                            for (RoleMiningUserStructure roleMiningDatum : roleMiningData) {
                                if (roleMiningDatum.getUserObject().getOid().equals(userObjectIdA)) {
                                    membersUserA = roleMiningDatum.getRoleObjectId();
                                }
                            }

                            String roleObjectIdB = roleMiningData.get(finalI).getUserObject().getOid();
                            List<String> membersRoleB = roleMiningData.get(finalI).getRoleObjectId();

                            boolean sameObject = userObjectIdA.equals(roleObjectIdB);
                            int confidence = roleAnalyseHelper.usersMembersIntersection(membersUserA, membersRoleB);
                            cellItem.add(new AjaxLinkPanel(componentId, Model.of(confidence)) {
                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    customSum = customSum + confidence;
                                    printCounterResult(String.valueOf(customSum));
                                }
                            }.add(new AttributeAppender("class", "row")));

                            setColor(sameObject, confidence, cellItem);

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {

                            return new AjaxLinkPanel(componentId, createStringResource(roleMiningData.get(finalI).getUserObject().getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    UserType object = roleMiningData.get(finalI).getUserObject().asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageUser.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };

                    columns.add(column);
                }

                return columns;
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

        confidenceTable.setOutputMarkupId(true);

        return confidenceTable;
    }

    public void setColor(boolean sameObject, double confidence, Item<ICellPopulator<SelectableBean<UserType>>> cellItem) {
        if (sameObject) {
            cellItem.add(new AttributeAppender("class", " table-warning"));
        } else if (confidence > 5) {
            cellItem.add(new AttributeAppender("class", " table-danger"));
        } else if (confidence == 1) {
            cellItem.add(new AttributeAppender("class", " table-active"));
        } else if (confidence == 2) {
            cellItem.add(new AttributeAppender("class", " table-primary"));
        } else if (confidence == 3) {
            cellItem.add(new AttributeAppender("class", " table-secondary"));
        } else if (confidence == 4) {
            cellItem.add(new AttributeAppender("class", " table-success"));
        } else if (confidence == 5) {
            cellItem.add(new AttributeAppender("class", " table-info"));
        }
    }

    protected MainObjectListPanel<?> jaccardIndexUserRoleMiningTable(List<RoleMiningUserStructure> roleMiningData) throws CommonException {
        RoleAnalyseHelper roleAnalyseHelper = new RoleAnalyseHelper();

        MainObjectListPanel<?> confidenceTable = new MainObjectListPanel<>(ID_TABLE_MEM_JACQUARD_USER, UserType.class, true) {
            @Override
            public List<SelectableBean<UserType>> isAnythingSelected(AjaxRequestTarget target, IModel<SelectableBean<UserType>> selectedObject) {
                return super.isAnythingSelected(target, selectedObject);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<UserType>> createProvider() {
                return super.createProvider();
            }

            @Override
            public LoadableDetachableModel<Search<UserType>> getSearchModel() {
                return super.getSearchModel();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<UserType>, String> column1 = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " col-sm-1 overflow-auto";
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                        UserType object = rowModel.getObject().getValue();
                        PageRoleMiningOld.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                };

                columns.add(column1);

                for (int i = 0; i < roleMiningData.size(); i++) {
                    int finalI = i;

                    IColumn<SelectableBean<UserType>, String> column = new AbstractExportableColumn<>(
                            createStringResource(roleMiningData.get(finalI).getUserObject().getName().toString())) {

                        @Override
                        public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                                String componentId, IModel<SelectableBean<UserType>> model) {

                            tableUserTypeStyle(cellItem);

                            String userObjectIdA = model.getObject().getValue().getOid();
                            List<String> membersUserA = null;

                            for (RoleMiningUserStructure roleMiningDatum : roleMiningData) {
                                if (roleMiningDatum.getUserObject().getOid().equals(userObjectIdA)) {
                                    membersUserA = roleMiningDatum.getRoleObjectId();
                                }
                            }

                            String roleObjectIdB = roleMiningData.get(finalI).getUserObject().getOid();
                            List<String> membersRoleB = roleMiningData.get(finalI).getRoleObjectId();

                            boolean sameObject = userObjectIdA.equals(roleObjectIdB);
                            double confidence = roleAnalyseHelper.jaccardIndex(membersUserA, membersRoleB, jaccardMinRolesCount);

                            cellItem.add(new AjaxLinkPanel(componentId, Model.of(confidence)) {
                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    customSum = customSum + confidence;
                                    printCounterResult(String.valueOf(customSum));

                                }
                            }.add(new AttributeAppender("class", "row")));

                            if (sameObject) {
                                cellItem.add(new AttributeAppender("class", " table-warning"));
                            } else if (confidence < jaccardThreshold && confidence > 0.0) {
                                cellItem.add(new AttributeAppender("class", " table-info"));
                            } else if (confidence > 0.5) {
                                cellItem.add(new AttributeAppender("class", " table-danger"));
                            } else if (confidence < 0.5 && confidence > 0.3) {
                                cellItem.add(new AttributeAppender("class", " table-active"));
                            }

                        }

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
                            return Model.of("");
                        }

                        @Override
                        public Component getHeader(String componentId) {

                            return new AjaxLinkPanel(componentId, createStringResource(roleMiningData.get(finalI).getUserObject().getName().toString())) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    UserType object = roleMiningData.get(finalI).getUserObject().asObjectable();
                                    PageRoleMiningOld.this.detailsPerformed(PageUser.class, object.getOid());
                                }
                            };
                        }

                        @Override
                        public String getCssClass() {
                            return " role-mining-rotated-header";
                        }
                    };

                    columns.add(column);
                }

                return columns;
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

        confidenceTable.setOutputMarkupId(true);

        return confidenceTable;
    }

    private void basicRoleTypeCell(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem, String componentId) {
        cellItem.add(new Label(componentId, " "));
    }

    private void filledRoleTypeCell(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-dark"));
        cellItem.add(new Label(componentId, " "));
    }

    private void algMatchedRoleTypeCell(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-info"));
        cellItem.add(new Label(componentId, " "));
    }

    private void tableRoleTypeStyle(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem) {
        cellItem.getParent().getParent().add(AttributeAppender.replace("class", " d-flex"));
        cellItem.getParent().getParent().add(AttributeAppender.replace("style", " height:40px"));
        cellItem.add(new AttributeAppender("style", " width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    private void basicUserTypeCell(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId) {
        cellItem.add(new Label(componentId, " "));
    }

    private void filledUserTypeCell(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-dark"));
        cellItem.add(new Label(componentId, " "));
    }

    private void algMatchedUserTypeCell(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-info"));
        cellItem.add(new Label(componentId, " "));
    }

    private void tableUserTypeStyle(Item<ICellPopulator<SelectableBean<UserType>>> cellItem) {
        cellItem.getParent().getParent().add(AttributeAppender.replace("class", " d-flex"));
        cellItem.getParent().getParent().add(AttributeAppender.replace("style", " height:40px"));
        cellItem.add(new AttributeAppender("style", " width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    private List<ObjectReferenceType> getRoleObjectReferenceTypes(AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i)).collect(Collectors.toList());

    }

    private boolean isSearchMode() {
        return searchMode;
    }

    public void setSearchMode(boolean searchMode) {
        this.searchMode = searchMode;
    }

    public List<PrismObject<RoleType>> getRoles() throws CommonException {
        String loadAllUsers = DOT_CLASS + "getAllRoles";
        OperationResult result = new OperationResult(loadAllUsers);
        Task task = ((PageBase) getPage()).createSimpleTask(loadAllUsers);

        return new RoleMiningFilter().filterRoles(((PageBase) getPage()));
    }

    public List<RoleMembersList> getRolesAndMembers(List<PrismObject<RoleType>> roles) throws CommonException {
        String loadAllUsers = DOT_CLASS + "getRolesMembers";
        OperationResult result = new OperationResult(loadAllUsers);
        Task task = ((PageBase) getPage()).createSimpleTask(loadAllUsers);

        return new RoleMiningFilter().filterRolesMembers(roles, ((PageBase) getPage()).getModelService(), task, result);
    }

    public List<RoleMiningUserStructure> getRoleMiningData(List<PrismObject<UserType>> users) {
        return new RoleMiningFilter().filterUsersRoles(users);
    }

    public List<PrismObject<UserType>> getUsers() throws CommonException {
        String loadAllUsers = DOT_CLASS + "getAllUsers";
        OperationResult result = new OperationResult(loadAllUsers);
        Task task = ((PageBase) getPage()).createSimpleTask(loadAllUsers);
        return new RoleMiningFilter().filterUsers(((PageBase) getPage()));
    }

    private List<PrismObject<UserType>> getMembers(String objectId) {
        String getRoleMembers = DOT_CLASS + "getRoleMembers";
        OperationResult result = new OperationResult(getRoleMembers);
        Task task = ((PageBase) getPage()).createSimpleTask(getRoleMembers);
        try {
            return getModelService().searchObjects(UserType.class, createMembersQuery(objectId), null, task, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectQuery createMembersQuery(String roleOid) {
        return getPrismContext().queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(roleOid).build();
    }

    protected PrismObject<RoleType> getRoleByOid(String oid) throws CommonException {
        String getRole = DOT_CLASS + "getRoleByOid";
        OperationResult result = new OperationResult(getRole);
        Task task = ((PageBase) getPage()).createSimpleTask(getRole);
        return getModelService().getObject(RoleType.class, oid, null, task, result);
    }

    private List<List<String>> getMatrix(List<PrismObject<UserType>> users) {

        List<List<String>> matrix = new ArrayList<>();

        for (PrismObject<UserType> user : users) {
            AssignmentHolderType assignmentHolderType = user.asObjectable();
            List<ObjectReferenceType> objectReferenceTypes = getRoleObjectReferenceTypes(assignmentHolderType);
            List<String> objectReferenceOiDs = new ArrayList<>();

            for (ObjectReferenceType objectReferenceType : objectReferenceTypes) {
                objectReferenceOiDs.add(objectReferenceType.getOid());
            }

            matrix.add(objectReferenceOiDs);
        }
        return matrix;
    }

    public String createIterationResultString(int currentResultPosition, int resultSize) {
        StringBuilder stringBuilder = new StringBuilder();
        if (resultSize == 0) {
            stringBuilder.append(" 0 result(s)");
            return String.valueOf(stringBuilder);
        }
        stringBuilder.append(currentResultPosition).append(" of ").append(resultSize).append(" result(s)");
        return String.valueOf(stringBuilder);
    }
}
