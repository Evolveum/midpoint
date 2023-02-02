/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.JaccardSortingMethod;

import com.github.openjson.JSONObject;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.RoleAnalyseHelper;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.RoleAnalyseStructure;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.rbam.UA;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.CombinationHelperAlgorithm;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.RBAMAlgorithm;
import com.evolveum.midpoint.gui.api.component.mining.structure.PAStructure;
import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMiningStructureList;
import com.evolveum.midpoint.gui.api.component.mining.structure.RoleMiningUserStructure;
import com.evolveum.midpoint.gui.api.component.mining.structure.UPStructure;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.*;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleMiningSimple", matchUrlForSecurity = "/admin/roleMiningSimple")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL,
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description") })

public class PageRoleMiningSimple extends PageAdmin {
    private static final String DOT_CLASS = PageRoleMiningSimple.class.getName() + ".";
    private static final String ID_MAIN_FORM = "main_form";
    private static final String ID_SECONDARY_FORM = "secondary_form";
    private static final String ID_ROLE_SEARCH = "role_search";
    private static final String ID_USER_SEARCH = "user_search";
    private static final String ID_BUTTON_NEXT_RESULT = "button_next_result";
    private static final String ID_FORM_MIN_SIZE = "min_size_form";
    private static final String ID_MIN_SIZE = "input_min_size";
    private static final String ID_JACCARD_THRESHOLD_INPUT = "jaccard_threshold_input";
    private static final String ID_JACCARD_MIN_ROLES_COUNT_INPUT = "jaccard_min_roles_count_input";
    private static final String ID_FORM_JACCARD_THRESHOLD = "jaccard_threshold_form";
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
    private static final String ID_GENERATE_DATA_PANEL = "generate_data_panel";
    private static final String ID_DATATABLE_MINING_MAIN = "datatable_mining_main";
    private static final String ID_DATATABLE_DETAILS = "datatable_details";
    private static final String ID_DATATABLE_EXTRA = "datatable_extra";
    private static final String ID_DATATABLE_RBAM_UA = "datatableRBAM";

    private static final String ID_NETWORK_GRAPH_FORM = "graph_form";
    private static final String ID_NETWORK_GRAPH_CONTAINER = "network_graph_container_auth";
    private static final String ID_MODEL_COST = "model_cost";
    private static final String ID_BASIC_MODEL_COST = "model_cost_basic";
    private static final String ID_INPUT_SIGMA = "weight_o";
    private static final String ID_INPUT_TAU = "weight_t";
    private static final String ID_SUBMIT_WEIGHTS = "ajax_submit_link_weights";
    private static final String ID_HIDE_GRAPH = "network_graph_ajax_link_auth";

    boolean searchMode = true;  //false: user   true: role

    double weightSigma = 0;
    double weightTau = 1 / (double) (11);
    double modelCost = 0;
    double modelCostBasic = 0;

    List<List<String>> basicCombResult;
    int basicCombMinIntersection = 4; //default 4 role;
    int basicCombDisplayResultIterator = 0;

    List<RoleMiningStructureList> roleMiningStructureLists;
    List<PrismObject<RoleType>> jaccardResultRoles;
    List<PrismObject<UserType>> jaccardUsersAnalysed;
    double jaccardThreshold = 0.5; //default
    int jaccardMinRolesCount = 1; //default

    List<PAStructure> permissionStructureList;
    List<AuthorizationType> authorizationTypeList;
    List<RoleAnalyseStructure> roleAnalyseStructures;
    List<UPStructure> upStructuresList;

    String jaccardJavaScriptChart;
    String javaScriptNetworkAuth;

    boolean containerVisibilityAuth = true;
    int usersCount = 0;

    List<UA> uaList = new ArrayList<>();

    private static final List<String> SEARCH_ENGINES = Arrays.asList("JACCARD", "RM", "RH", "PA", "UP", "UA", "URM", "RHM");
    public String selected = "JACCARD";

    public PageRoleMiningSimple() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        networkGraphSection();
        jaccardSection();

        List<PrismObject<RoleType>> roles;
        List<PrismObject<UserType>> users;
        try {
            roles = getRoles();
            users = getUsers();
            usersCount = users.size();

        } catch (CommonException e) {
            throw new RuntimeException("Failed to load basic role mining list: " + e);
        }

        List<RoleMiningUserStructure> roleMiningData;
        roleMiningData = getRoleMiningData(users);
        fillJaccardData(roleMiningData);

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        mainForm.add(createGeneratePanel(roles, users));
        basicOperationHelper(mainForm, roles, users);
        searchSelector(mainForm);
        mainForm.add(createResetTablePanel());
        mainForm.add(generateTableUA(roleMiningStructureLists, roles, ID_DATATABLE_MINING_MAIN));

        if (isSearchMode()) {
            mainForm.add(basicRoleHelperTable());
        } else {
            mainForm.add(basicUserHelperTable());
        }

        Form<?> secondaryForm = new MidpointForm<>(ID_SECONDARY_FORM);
        secondaryForm.setOutputMarkupId(true);
        add(secondaryForm);

        jaccardThresholdSubmit(secondaryForm, roleMiningData);
        ajaxLinkExecuteJacquardMining(users, secondaryForm);

        fillPermissionStructureList(roles, users);
        fillUP(permissionStructureList);

        secondaryForm.add(choiceTableDropdown(roles));
        secondaryForm.add(generateTableJC(roleMiningStructureLists));

        jaccardChartJS();

        secondaryForm.add(generateTableRBAM(uaList));

    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        responseRenderJaccardChart(response, jaccardJavaScriptChart);
        responseRenderAuthGraph(response, javaScriptNetworkAuth);
    }

    void responseRenderJaccardChart(@NotNull IHeaderResponse response, String javaScript) {
        response.render(JavaScriptHeaderItem.forReference(
                new PackageResourceReference(PageRoleMiningSimple.class, "js/jaccard_chart_sortable.js")));

        response.render(OnDomReadyHeaderItem.forScript(javaScript));
    }

    void responseRenderAuthGraph(@NotNull IHeaderResponse response, String javaScript) {

        response.render(JavaScriptHeaderItem.forReference(
                new PackageResourceReference(PageRoleMiningSimple.class, "js/network_graph_auth.js")));

        response.render(OnDomReadyHeaderItem.forScript(javaScript));
    }

    void jaccardChartJS() {
        List<String> objectName = new ArrayList<>();
        List<Double> totalJaccard = new ArrayList<>();
        List<List<Double>> partialJaccardMap = new ArrayList<>();
        List<Integer> staticIndex = new ArrayList<>();

        for (RoleMiningStructureList jaccardDataStructure : roleMiningStructureLists) {
            objectName.add(jaccardDataStructure.getUserObject().getName().toString());
            totalJaccard.add(jaccardDataStructure.getObjectTotalResult());
            partialJaccardMap.add(jaccardDataStructure.getObjectPartialResult());
            staticIndex.add(jaccardDataStructure.getStaticIndex());
        }

        jaccardJavaScriptChart = "jaccard_chart_sortable('"
                + objectName + "', '"
                + totalJaccard + "', '"
                + partialJaccardMap + "', '"
                + jaccardThreshold + "', '"
                + staticIndex + "');";
    }

    public void generateJsRBAM() {

        List<AuthorizationType> tempAuthorizationExample = new ArrayList<>();


        if (authorizationTypeList.isEmpty()) {
            return;
        }

        if(upStructuresList.isEmpty()){
            return;
        }

        int usersCount = upStructuresList.size();
        double vC = 0;

        RBAMAlgorithm algorithm = new RBAMAlgorithm(upStructuresList, authorizationTypeList, usersCount, weightSigma, weightTau, vC);
        algorithm.preprocess();
        uaList = algorithm.getUaList();
        modelCost = algorithm.getUpdatedModelCost();
        modelCostBasic = algorithm.getBasicModelCost();
        getCostLabel().setDefaultModelObject("Cost: " + modelCost);
        getCostBasicLabel().setDefaultModelObject("Basic cost: " + modelCostBasic);

        List<JSONObject> jsonObjectIds = algorithm.getJsonIds();
        List<JSONObject> jsonObjectEdges = algorithm.getJsonEdges();

        javaScriptNetworkAuth = "network_graph_auth('"
                + jsonObjectIds + "', '"
                + jsonObjectEdges + "');";
    }

    protected Label getCostLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath("graph_form", "model_cost"));
    }

    protected Label getCostBasicLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath("graph_form", "model_cost_basic"));
    }

    public void jaccardSection() {
        Form<?> jaccardForm = new Form<>("jaccard_form");
        jaccardForm.setOutputMarkupId(true);

        AjaxButton jaccardButton = new AjaxButton(
                "run_process_jaccard", Model.of("Test")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                JaccardSortingMethod jaccardSortingMethod = new JaccardSortingMethod(getPageBase());

                jaccardSortingMethod.sortDescending();
            }
        };
        jaccardButton.setOutputMarkupId(true);
        jaccardForm.add(jaccardButton);
        add(jaccardForm);

    }

    public void networkGraphSection() {

        Label costModel = new Label(ID_MODEL_COST, Model.of("Cost: " + modelCost));
        costModel.setOutputMarkupId(true);
        costModel.setOutputMarkupPlaceholderTag(true);

        Label costModelBasic = new Label(ID_BASIC_MODEL_COST, Model.of("Basic cost: " + modelCostBasic));
        costModelBasic.setOutputMarkupId(true);
        costModelBasic.setOutputMarkupPlaceholderTag(true);

        final TextField<Double> weightO = new TextField<>(ID_INPUT_SIGMA, Model.of(weightSigma));
        weightO.setOutputMarkupId(true);

        final TextField<Double> weightT = new TextField<>(
                ID_INPUT_TAU, Model.of(weightTau));
        weightT.setOutputMarkupId(true);

        Form<?> graphForm = new Form<>(ID_NETWORK_GRAPH_FORM);
        graphForm.setOutputMarkupId(true);
        graphForm.add(weightO);
        graphForm.add(weightT);

        graphForm.add(WebComponentUtil.createHelp("weight_o_info"));
        graphForm.add(WebComponentUtil.createHelp("weight_t_info"));
        add(graphForm);

        AjaxSubmitLink ajaxSubmitDropdown = new AjaxSubmitLink(ID_SUBMIT_WEIGHTS, graphForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                weightSigma = weightO.getModelObject();
                weightTau = weightT.getModelObject();
                generateJsRBAM();
                target.appendJavaScript(javaScriptNetworkAuth);
                getBoxedTableRBAM().replaceWith(generateTableRBAM(uaList));
                target.add(getBoxedTableRBAM());
                getCostLabel().setDefaultModelObject("Cost: " + modelCost);
                getCostBasicLabel().setDefaultModelObject("Basic cost: " + modelCostBasic);
                target.add(getCostLabel());
                target.add(getCostBasicLabel());
            }
        };
        graphForm.add(ajaxSubmitDropdown);

        WebMarkupContainer networkGraphContainerAuth = new WebMarkupContainer(ID_NETWORK_GRAPH_CONTAINER);
        networkGraphContainerAuth.setOutputMarkupId(true);
        networkGraphContainerAuth.setOutputMarkupPlaceholderTag(true);
        graphForm.add(networkGraphContainerAuth);

        AjaxButton networkGraphButtonAuth = new AjaxButton(
                ID_HIDE_GRAPH, Model.of("Show/Hide network auth graph")) {
            @Override
            public void onClick(AjaxRequestTarget target) {

                generateJsRBAM();
                target.appendJavaScript(javaScriptNetworkAuth);

                containerVisibilityAuth = !containerVisibilityAuth;
                target.add(networkGraphContainerAuth.setVisible(containerVisibilityAuth));
            }
        };
        graphForm.add(networkGraphButtonAuth);
        graphForm.add(costModel);
        graphForm.add(costModelBasic);
    }

    private @NotNull Form<?> choiceTableDropdown(List<PrismObject<RoleType>> roles) {
        DropDownChoice<String> listSites = new DropDownChoice<>(
                "dropdown_choice", new PropertyModel<>(this, "selected"), SEARCH_ENGINES);

        Form<?> formDropdown = new Form<Void>("form_dropdown");

        AjaxSubmitLink ajaxSubmitDropdown = new AjaxSubmitLink("ajax_submit_link_dropdown", formDropdown) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                switch (selected) {
                    case "RM":
                        getBoxedTableExtra().replaceWith(generateTableRM(permissionStructureList, roleMiningStructureLists));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(false));
                        target.add(getAjaxExecuteJC());
                        break;
                    case "JACCARD":
                        getBoxedTableExtra().replaceWith(generateTableJC(roleMiningStructureLists));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(true));
                        target.add(getAjaxExecuteJC());
                        break;
                    case "RH":
                        getBoxedTableExtra().replaceWith(generateTableRH(permissionStructureList));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(false));
                        target.add(getAjaxExecuteJC());
                        break;
                    case "RHM":
                        getBoxedTableExtra().replaceWith(generateTableRHM(permissionStructureList));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(false));
                        target.add(getAjaxExecuteJC());
                        break;
                    case "PA":
                        getBoxedTableExtra().replaceWith(generateTablePA(permissionStructureList));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(false));
                        target.add(getAjaxExecuteJC());
                        break;
                    case "UP":
                        getBoxedTableExtra().replaceWith(generateTableUP(upStructuresList));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(false));
                        target.add(getAjaxExecuteJC());
                        break;
                    case "URM":
                        getBoxedTableExtra().replaceWith(generateTableURM(upStructuresList));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(false));
                        target.add(getAjaxExecuteJC());
                        break;
                    case "UA":
                        getBoxedTableExtra().replaceWith(generateTableUA(roleMiningStructureLists, roles, ID_DATATABLE_EXTRA));
                        target.add(getBoxedTableExtra());
                        getAjaxExecuteJC().replaceWith(getAjaxExecuteJC().setEnabled(false));
                        target.add(getAjaxExecuteJC());
                        break;
                    default:
                }
            }
        };

        formDropdown.add(listSites);
        formDropdown.add(ajaxSubmitDropdown);
        return formDropdown;
    }

    private @NotNull AjaxButton createResetTablePanel() {
        AjaxButton ajaxLinkPanel = new AjaxButton("reset_table", Model.of("Reset table")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                basicCombResult = null;
                basicCombDisplayResultIterator = 0;
                jaccardUsersAnalysed = null;
                jaccardResultRoles = null;
                getResultCountLabel().setDefaultModel(Model.of(createIterationResultString(0, 0)));
                target.add(getResultCountLabel());

                getBoxedTableUA().getDataTable().setItemsPerPage(30);
                getBoxedTableUA().replaceWith(getBoxedTableUA());
                target.add(getBoxedTableUA());
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);

        return ajaxLinkPanel;
    }

    private @NotNull AjaxButton createGeneratePanel(List<PrismObject<RoleType>> roles, List<PrismObject<UserType>> users) {
        AjaxButton ajaxLinkAssign = new AjaxButton(ID_GENERATE_DATA_PANEL, Model.of("Generate data")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                GenerateDataPanel pageGenerateData = new GenerateDataPanel(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("RoleMining.generateDataPanel.title"), users, roles);
                getPageBase().showMainPopup(pageGenerateData, target);
            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;
    }

    protected MainObjectListPanel<?> basicUserHelperTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE_DETAILS, UserType.class) {

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<UserType> helper = new FocusListInlineMenuHelper<>(
                        UserType.class, PageRoleMiningSimple.this, this) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isShowConfirmationDialog(ColumnMenuAction<?> action) {
                        return PageRoleMiningSimple.this.getBasicTable().getSelectedObjectsCount() > 0;
                    }

                    @Override
                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action, String actionName) {
                        return PageRoleMiningSimple.this.getBasicTable().getConfirmationMessageModel(action, actionName);
                    }
                };

                return helper.createRowActions(UserType.class);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<UserType>> createProvider() {
                return super.createProvider();
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<UserType>, String> column = new PropertyColumn<>(
                        createStringResource("UserType.givenName"),
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
                        return Model.of("");
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

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE_DETAILS, RoleType.class) {
            @Override
            public List<SelectableBean<RoleType>> isAnythingSelected(
                    AjaxRequestTarget target, IModel<SelectableBean<RoleType>> selectedObject) {
                return super.isAnythingSelected(target, selectedObject);
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<RoleType> helper = new FocusListInlineMenuHelper<>(
                        RoleType.class, PageRoleMiningSimple.this, this) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isShowConfirmationDialog(ColumnMenuAction<?> action) {
                        return PageRoleMiningSimple.this.getBasicTable().getSelectedObjectsCount() > 0;
                    }

                    @Override
                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action, String actionName) {
                        return PageRoleMiningSimple.this.getBasicTable().getConfirmationMessageModel(action, actionName);
                    }
                };

                return helper.createRowActions(RoleType.class);
            }

            @Override
            public String getTb(String s) {
                return super.getTb(s);
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleType.assignments.count")) {
                    @Override
                    public String getSortProperty() {
                        return super.getSortProperty();
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getAssignment() != null) {
                            int object = model.getObject().getValue().getAssignment().size();
                            cellItem.add(new Label(componentId, object));
                        } else {
                            cellItem.add(new Label(componentId, 0));
                        }
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                        return Model.of("");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
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
                        return Model.of(" ");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("PageRoleEditor.label.riskLevel"),
                        RoleType.F_RISK_LEVEL.getLocalPart()) {

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
                        return Model.of(" ");
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

    public BoxedTablePanel<RoleMiningStructureList> generateTableUA(
            List<RoleMiningStructureList> roleMiningStructureLists, List<PrismObject<RoleType>> roles, String DATATABLE_ID) {

        RoleMiningProvider<RoleMiningStructureList> provider = new RoleMiningProvider<>(
                this, new ListModel<>(roleMiningStructureLists) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleMiningStructureList> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(RoleMiningStructureList.F_SUM_RESULT, SortOrder.DESCENDING);

        BoxedTablePanel<RoleMiningStructureList> table = new BoxedTablePanel<>(
                DATATABLE_ID, provider, initColumnsUA(roles),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public BoxedTablePanel<RoleMiningStructureList> generateTableJC(
            List<RoleMiningStructureList> roleMiningStructureLists) {

        RoleMiningProvider<RoleMiningStructureList> provider = new RoleMiningProvider<>(
                this, new ListModel<>(roleMiningStructureLists) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleMiningStructureList> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(RoleMiningStructureList.F_SUM_RESULT, SortOrder.DESCENDING);

        BoxedTablePanel<RoleMiningStructureList> table = new BoxedTablePanel<>(
                ID_DATATABLE_EXTRA, provider, initColumnsJC(),
                null, true, true);
        table.setOutputMarkupId(true);
        table.setItemsPerPage(30);
        return table;
    }

    public BoxedTablePanel<PAStructure> generateTableRM(
            List<PAStructure> permissionStructureList, List<RoleMiningStructureList> roleMiningStructureLists) {

        RoleMiningProvider<PAStructure> provider = new RoleMiningProvider<>(
                this, new ListModel<>(permissionStructureList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<PAStructure> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(PAStructure.F_TOTAL_RESULT, SortOrder.DESCENDING);

        BoxedTablePanel<PAStructure> table = new BoxedTablePanel<>(
                ID_DATATABLE_EXTRA, provider, initColumnsRM(roleMiningStructureLists),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public BoxedTablePanel<PAStructure> generateTableRH(
            List<PAStructure> permissionStructureList) {

        RoleMiningProvider<PAStructure> provider = new RoleMiningProvider<>(
                this, new ListModel<>(permissionStructureList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<PAStructure> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(PAStructure.F_NAME, SortOrder.ASCENDING);

        BoxedTablePanel<PAStructure> table = new BoxedTablePanel<>(
                ID_DATATABLE_EXTRA, provider, initColumnsRH(),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public BoxedTablePanel<PAStructure> generateTableRHM(
            List<PAStructure> permissionStructureList) {

        RoleMiningProvider<PAStructure> provider = new RoleMiningProvider<>(
                this, new ListModel<>(permissionStructureList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<PAStructure> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(PAStructure.F_NAME, SortOrder.ASCENDING);

        BoxedTablePanel<PAStructure> table = new BoxedTablePanel<>(
                ID_DATATABLE_EXTRA, provider, initColumnsRHM(),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public BoxedTablePanel<PAStructure> generateTablePA(
            List<PAStructure> permissionStructureList) {

        RoleMiningProvider<PAStructure> provider = new RoleMiningProvider<>(
                this, new ListModel<>(permissionStructureList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<PAStructure> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(PAStructure.F_NAME, SortOrder.ASCENDING);

        BoxedTablePanel<PAStructure> table = new BoxedTablePanel<>(
                ID_DATATABLE_EXTRA, provider, initColumnsPA(),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public BoxedTablePanel<UA> generateTableRBAM(
            List<UA> uaList) {

        RoleMiningProvider<UA> provider = new RoleMiningProvider<>(
                this, new ListModel<>(uaList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<UA> object) {
                super.setObject(object);
            }

        }, false);

        //provider.setSort(PAStructure.F_NAME, SortOrder.ASCENDING);

        BoxedTablePanel<UA> table = new BoxedTablePanel<>(
                ID_DATATABLE_RBAM_UA, provider, initColumnsRBAM(),
                null, true, false);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public BoxedTablePanel<UPStructure> generateTableUP(
            List<UPStructure> upStructuresList) {

        RoleMiningProvider<UPStructure> provider = new RoleMiningProvider<>(
                this, new ListModel<>(upStructuresList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<UPStructure> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(UPStructure.F_NAME, SortOrder.ASCENDING);

        BoxedTablePanel<UPStructure> table = new BoxedTablePanel<>(
                ID_DATATABLE_EXTRA, provider, initColumnsUP(),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    public BoxedTablePanel<UPStructure> generateTableURM(
            List<UPStructure> upStructuresList) {

        RoleMiningProvider<UPStructure> provider = new RoleMiningProvider<>(
                this, new ListModel<>(upStructuresList) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<UPStructure> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(UPStructure.F_NAME, SortOrder.ASCENDING);

        BoxedTablePanel<UPStructure> table = new BoxedTablePanel<>(
                ID_DATATABLE_EXTRA, provider, initColumnsURM(),
                null, true, true);
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(30);
        table.enableSavePageSize();

        return table;
    }

    protected BoxedTablePanel<?> getBoxedTableExtra() {
        return (BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_SECONDARY_FORM, ID_DATATABLE_EXTRA));
    }

    protected BoxedTablePanel<?> getBoxedTableUA() {
        return (BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_DATATABLE_MINING_MAIN));
    }

    protected BoxedTablePanel<?> getBoxedTableRBAM() {
        return (BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_SECONDARY_FORM, ID_DATATABLE_RBAM_UA));
    }

    public List<IColumn<RoleMiningStructureList, String>> initColumnsUA(@NotNull List<PrismObject<RoleType>> roles) {

        List<IColumn<RoleMiningStructureList, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<RoleMiningStructureList> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleMiningStructureList> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return RoleMiningStructureList.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<RoleMiningStructureList> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleMiningStructureList>> item, String componentId,
                    IModel<RoleMiningStructureList> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getUserObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        UserType object = rowModel.getObject().getUserObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender(
                                "style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<RoleMiningStructureList, String> column;

        for (int i = 0; i < roles.size(); i++) {
            int finalI = i;
            column = new AbstractExportableColumn<>(
                    createStringResource(roles.get(i).getName())) {

                @Override
                public void populateItem(Item<ICellPopulator<RoleMiningStructureList>> cellItem,
                        String componentId, IModel<RoleMiningStructureList> model) {

                    tableStyle(cellItem);

                    List<String> rolesObjectIds = model.getObject().getRoleObjectRef();

                    if (basicCombResult != null) {

                        if (basicCombDisplayResultIterator == basicCombResult.size()) {
                            basicCombDisplayResultIterator = 0;
                        }
                        ArrayList<String> rolesOid = new ArrayList<>(basicCombResult.get(basicCombDisplayResultIterator));
                        if (rolesObjectIds.containsAll(rolesOid)) {
                            if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                if (rolesOid.contains(roles.get(finalI).getOid())) {
                                    algMatchedCell(cellItem, componentId);
                                } else {
                                    filledCell(cellItem, componentId);
                                }
                            } else {
                                emptyCell(cellItem, componentId);
                            }
                        } else {
                            if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                filledCell(cellItem, componentId);
                            } else {
                                emptyCell(cellItem, componentId);

                            }

                        }
                    } else if (jaccardUsersAnalysed != null && jaccardResultRoles != null) {

                        ArrayList<String> jaccardResultRolesOid = new ArrayList<>();
                        for (PrismObject<RoleType> jaccardResultRole : jaccardResultRoles) {
                            jaccardResultRolesOid.add(jaccardResultRole.getOid());
                        }
                        if (jaccardUsersAnalysed.contains(model.getObject().getUserObject()) &&
                                rolesObjectIds.containsAll(jaccardResultRolesOid)) {
                            if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                if (jaccardResultRolesOid.contains(roles.get(finalI).getOid())) {
                                    algMatchedCell(cellItem, componentId);
                                } else {
                                    filledCell(cellItem, componentId);
                                }
                            } else {
                                emptyCell(cellItem, componentId);

                            }

                        } else {

                            if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                                filledCell(cellItem, componentId);
                            } else {
                                emptyCell(cellItem, componentId);
                            }

                        }
                    } else {

                        if (rolesObjectIds.contains(roles.get(finalI).getOid())) {
                            filledCell(cellItem, componentId);
                        } else {
                            emptyCell(cellItem, componentId);
                        }
                    }

                }

                @Override
                public IModel<String> getDataModel(IModel<RoleMiningStructureList> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {
                    String headerName = roles.get(finalI).getName().toString();
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            RoleType object = roles.get(finalI).asObjectable();
                            PageRoleMiningSimple.this.detailsPerformed(PageRole.class, object.getOid());
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return RoleMiningStructureList.F_SUM_RESULT;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleMiningStructureList>> cellItem,
                    String componentId, IModel<RoleMiningStructureList> model) {

                tableStyle(cellItem);

                double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(Math.round(confidence * 100.0) / 100.0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<RoleMiningStructureList> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<RoleMiningStructureList, String>> initColumnsJC() {

        List<IColumn<RoleMiningStructureList, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<RoleMiningStructureList> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleMiningStructureList> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return RoleMiningStructureList.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<RoleMiningStructureList> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleMiningStructureList>> item, String componentId,
                    IModel<RoleMiningStructureList> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getUserObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        UserType object = rowModel.getObject().getUserObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender("style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-row-header overflow-auto role-mining-static-header-name";
            }
        });

        IColumn<RoleMiningStructureList, String> column;

        for (int i = 0; i < roleMiningStructureLists.size(); i++) {
            int finalI = i;
            column = new AbstractExportableColumn<>(
                    createStringResource(roleMiningStructureLists.get(i).getUserObject().getName())) {

                @Override
                public void populateItem(Item<ICellPopulator<RoleMiningStructureList>> cellItem,
                        String componentId, IModel<RoleMiningStructureList> model) {

                    tableStyle(cellItem);

                    int position = roleMiningStructureLists.get(finalI).getStaticIndex();
                    double confidence = model.getObject().getObjectPartialResult().get(position);

                    cellItem.add(new Label(componentId, Model.of(confidence)).add(
                            new AttributeAppender("class", "row")));

                    if (model.getObject().getUserObject().getName().equals(roleMiningStructureLists.get(finalI).getUserObject().getName())) {
                        cellItem.add(new AttributeAppender("class", " table-warning"));
                    } else if (confidence < jaccardThreshold && confidence > 0.0) {
                        cellItem.add(new AttributeAppender("class", " table-info"));
                    } else if (confidence >= jaccardThreshold) {
                        cellItem.add(new AttributeAppender("class", "table-danger"));
                    }

                }

                @Override
                public IModel<String> getDataModel(IModel<RoleMiningStructureList> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {
                    String headerName = roleMiningStructureLists.get(finalI).getUserObject().getName().toString();
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            UserType object = roleMiningStructureLists.get(finalI).getUserObject().asObjectable();
                            PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return RoleMiningStructureList.F_SUM_RESULT;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleMiningStructureList>> cellItem,
                    String componentId, IModel<RoleMiningStructureList> model) {

                tableStyle(cellItem);

                double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(Math.round(confidence * 100.0) / 100.0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<RoleMiningStructureList> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<PAStructure, String>> initColumnsRM(@NotNull List<RoleMiningStructureList> roleMiningStructureLists) {

        List<IColumn<PAStructure, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<PAStructure> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PAStructure> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return PAStructure.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<PAStructure> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> item, String componentId,
                    IModel<PAStructure> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getRoleObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        RoleType object = rowModel.getObject().getRoleObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender("style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<PAStructure, String> column;

        for (int i = 0; i < roleMiningStructureLists.size(); i++) {
            int finalI = i;
            column = new AbstractExportableColumn<>(
                    createStringResource(roleMiningStructureLists.get(i).getUserObject().getName())) {

                @Override
                public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                        String componentId, IModel<PAStructure> model) {

                    tableStyle(cellItem);

                    PrismObject<UserType> userTypePrismObject = roleMiningStructureLists.get(finalI).getUserObject();
                    String userObjectId = userTypePrismObject.getOid();
                    List<PrismObject<UserType>> prismObjectMemberList = model.getObject().getRoleMembers();
                    List<String> membersObjectIdsList = prismObjectMemberList
                            .stream()
                            .map(PrismObject::getOid)
                            .collect(Collectors.toList());

                    if (membersObjectIdsList.contains(userObjectId)) {
                        filledCell(cellItem, componentId);
                    } else {
                        emptyCell(cellItem, componentId);
                    }

                }

                @Override
                public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {
                    double totalValue = roleMiningStructureLists.get(finalI).getObjectTotalResult();
                    String headerName = (Math.round(totalValue * 100.0) / 100.0) + " - " +
                            roleMiningStructureLists.get(finalI).getUserObject().getName().toString();
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            UserType object = roleMiningStructureLists.get(finalI).getUserObject().asObjectable();
                            PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return PAStructure.F_TOTAL_RESULT;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                    String componentId, IModel<PAStructure> model) {

                tableStyle(cellItem);

                double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(Math.round(confidence * 100.0) / 100.0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<PAStructure, String>> initColumnsRH() {

        List<IColumn<PAStructure, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<PAStructure> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PAStructure> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return PAStructure.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<PAStructure> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> item, String componentId,
                    IModel<PAStructure> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getRoleObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        RoleType object = rowModel.getObject().getRoleObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender("style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<PAStructure, String> column;

        for (int i = 0; i < permissionStructureList.size(); i++) {
            int finalI = i;
            column = new AbstractExportableColumn<>(
                    createStringResource(permissionStructureList.get(i).getRoleObject().getName())) {

                @Override
                public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                        String componentId, IModel<PAStructure> model) {

                    tableStyle(cellItem);

                    List<AuthorizationType> columnObject = permissionStructureList.get(finalI).getAuthorizationTypeList();
                    List<AuthorizationType> modelObject = model.getObject().getAuthorizationTypeList();

                    RoleType parent = model.getObject().getRoleObject().asObjectable();
                    RoleType child = permissionStructureList.get(finalI).getRoleObject().asObjectable();

                    boolean checkParent = new RoleAnalyseHelper().logicParentCheck(
                            parent, child, upStructuresList);

                    double confidence = new RoleAnalyseHelper().confidenceR2R1(
                            parent, child, upStructuresList, upStructuresList.size());

                    if (parent.equals(child)) {
                        cellItem.add(new AttributeAppender("class", " bg-warning"));
                    } else if (checkParent) {
                        cellItem.add(new AttributeAppender("class", " bg-danger"));
                    } else {
                        cellItem.add(new AttributeAppender("class", " bg-primary"));
                    }

                    if (modelObject.containsAll(columnObject)) {

                        cellItem.add(new Label(componentId, (Math.round(confidence * 100.0) / 100.0))
                                .add(new AttributeAppender("class", " row")));
                    } else {
                        emptyCell(cellItem, componentId);
                    }

                }

                @Override
                public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {
                    String headerName = permissionStructureList.get(finalI).getRoleObject().getName().toString();
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(
                            componentId, createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            RoleType object = permissionStructureList.get(finalI).getRoleObject().asObjectable();
                            PageRoleMiningSimple.this.detailsPerformed(PageRole.class, object.getOid());
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return PAStructure.F_TOTAL_RESULT;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                    String componentId, IModel<PAStructure> model) {

                tableStyle(cellItem);

                double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(Math.round(confidence * 100.0) / 100.0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<PAStructure, String>> initColumnsRHM() {

        List<IColumn<PAStructure, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<PAStructure> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PAStructure> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return PAStructure.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<PAStructure> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> item, String componentId,
                    IModel<PAStructure> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getRoleObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        RoleType object = rowModel.getObject().getRoleObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender(
                                "style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<PAStructure, String> column;

        for (int i = 0; i < permissionStructureList.size(); i++) {
            int finalI = i;
            column = new AbstractExportableColumn<>(
                    createStringResource(permissionStructureList.get(i).getRoleObject().getName())) {

                @Override
                public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                        String componentId, IModel<PAStructure> model) {

                    tableStyle(cellItem);

                    List<AuthorizationType> columnObject = permissionStructureList.get(finalI).getAuthorizationTypeList();
                    List<AuthorizationType> modelObject = model.getObject().getAuthorizationTypeList();

                    RoleType parent = model.getObject().getRoleObject().asObjectable();
                    RoleType child = permissionStructureList.get(finalI).getRoleObject().asObjectable();

                    List<PrismObject<UserType>> membersRoleA = model.getObject().getRoleMembers();
                    List<PrismObject<UserType>> membersRoleB = permissionStructureList.get(finalI).getRoleMembers();

                    int iterateMembersIntersection = 0;
                    if (membersRoleA != null && membersRoleB != null) {
                        iterateMembersIntersection = (int) membersRoleB.stream().filter(membersRoleA::contains).count();
                    }
                    boolean checkParent = new RoleAnalyseHelper().logicParentCheck(
                            parent, child, upStructuresList);

                    if (modelObject.containsAll(columnObject)) {

                        if (parent.equals(child)) {
                            cellItem.add(new AttributeAppender("class", " table-warning"));
                        } else if (checkParent) {
                            cellItem.add(new AttributeAppender("class", " table-success"));
                        } else {
                            cellItem.add(new AttributeAppender("class", " table-dark"));
                        }

                        cellItem.add(new Label(componentId, iterateMembersIntersection)
                                .add(new AttributeAppender("class", " row")));
                    } else {
                        cellItem.add(new Label(componentId, iterateMembersIntersection)
                                .add(new AttributeAppender("class", " row")));
                    }

                }

                @Override
                public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {
                    String headerName = permissionStructureList.get(finalI).getRoleObject().getName().toString();
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(
                            componentId, createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            RoleType object = permissionStructureList.get(finalI).getRoleObject().asObjectable();
                            PageRoleMiningSimple.this.detailsPerformed(PageRole.class, object.getOid());
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return PAStructure.F_TOTAL_RESULT;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                    String componentId, IModel<PAStructure> model) {

                tableStyle(cellItem);

                double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(Math.round(confidence * 100.0) / 100.0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<PAStructure, String>> initColumnsPA() {

        List<IColumn<PAStructure, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<PAStructure> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PAStructure> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return PAStructure.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<PAStructure> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> item, String componentId,
                    IModel<PAStructure> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getRoleObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        RoleType object = rowModel.getObject().getRoleObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageRole.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender(
                                "style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<PAStructure, String> column;

        for (AuthorizationType authorizationType : authorizationTypeList) {
            String s = authorizationType.getName() == null ? "Undefined" : authorizationType.getName();
            column = new AbstractExportableColumn<>(
                    createStringResource(s)) {

                @Override
                public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                        String componentId, IModel<PAStructure> model) {

                    tableStyle(cellItem);

                    List<AuthorizationType> modelObject = permissionStructureList.get(
                            model.getObject().getStaticIndex()).getAuthorizationTypeList();

                    if (modelObject.contains(authorizationType)) {
                        filledCell(cellItem, componentId);
                    } else {
                        emptyCell(cellItem, componentId);
                    }

                }

                @Override
                public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {

                    String headerName = authorizationType.getName() == null ? "Undefined" : authorizationType.getName();
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.CLASS_LOCK_STATUS);

                    return new AjaxLinkTruncatePanel(
                            componentId, createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public boolean isEnabled() {
                            return false;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return PAStructure.F_TOTAL_RESULT;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PAStructure>> cellItem,
                    String componentId, IModel<PAStructure> model) {

                tableStyle(cellItem);

                //  double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<PAStructure> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<UA, String>> initColumnsRBAM() {

        List<IColumn<UA, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<UA> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public IModel<?> getDataModel(IModel<UA> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UA>> item, String componentId,
                    IModel<UA> rowModel) {

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getUserUP().getUserObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        UserType object = rowModel.getObject().getUserUP().getUserObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column"));
            }

        });

        IColumn<UA, String> column;

        column = new AbstractExportableColumn<>(
                createStringResource("Permission")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UA>> cellItem,
                    String componentId, IModel<UA> model) {

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("{");
                for (int i = 0; i < model.getObject().getUserUP().getAssignPermission().size(); i++) {
                    stringBuilder.append(model.getObject().getUserUP().getAssignPermission().get(i).getName()).append(", ");
                }
                stringBuilder.append("}");
                cellItem.add(new Label(componentId, String.valueOf(stringBuilder).replace(", }", "}")));
            }

            @Override
            public IModel<String> getDataModel(IModel<UA> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                createStringResource("Candidate roles")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UA>> cellItem,
                    String componentId, IModel<UA> model) {

                Set<Integer> keys = model.getObject().getCandidateRoleSet().keySet();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("{");
                for (Integer key : keys) {
                    stringBuilder.append(key).append(", ");
                }
                stringBuilder.append("}");
                cellItem.add(new Label(componentId, String.valueOf(stringBuilder).replace(", }", "}")));
            }

            @Override
            public IModel<String> getDataModel(IModel<UA> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                createStringResource("Candidate roles permission")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UA>> cellItem,
                    String componentId, IModel<UA> model) {

                Set<Integer> keys = model.getObject().getCandidateRoleSet().keySet();
                StringBuilder stringBuilder = new StringBuilder();

                for (Integer key : keys) {
                    List<AuthorizationType> authorizationTypes = model.getObject().getCandidateRoleSet().get(key);
                    stringBuilder.append("{");
                    for (int i = 0; i < authorizationTypes.size(); i++) {
                        stringBuilder.append(authorizationTypes.get(i).getName()).append(", ");
                    }
                    stringBuilder.append("} + ");
                }
                stringBuilder.append(". ");

                cellItem.add(new Label(componentId, String.valueOf(stringBuilder).replace(", }", "}")
                        .replace("+ .", " "))
                );
            }

            @Override
            public IModel<String> getDataModel(IModel<UA> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                createStringResource("Status")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UA>> cellItem,
                    String componentId, IModel<UA> model) {

                if (!model.getObject().isCorrelateStatus()) {
                    cellItem.add(new Label(componentId, "UNPROCESSED"));
                } else {
                    cellItem.add(new Label(componentId, "PROCESSED"));
                }
            }

            @Override
            public IModel<String> getDataModel(IModel<UA> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<UPStructure, String>> initColumnsUP() {

        List<IColumn<UPStructure, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<UPStructure> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<UPStructure> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return UPStructure.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<UPStructure> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UPStructure>> item, String componentId,
                    IModel<UPStructure> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getUserObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        UserType object = rowModel.getObject().getUserObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender(
                                "style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<UPStructure, String> column;

        for (AuthorizationType authorizationType : authorizationTypeList) {
            String s = authorizationType.getName() == null ? "Undefined" : authorizationType.getName();
            column = new AbstractExportableColumn<>(
                    createStringResource(s)) {

                @Override
                public void populateItem(Item<ICellPopulator<UPStructure>> cellItem,
                        String componentId, IModel<UPStructure> model) {

                    tableStyle(cellItem);

                    List<AuthorizationType> modelObject = model.getObject().getAssignPermission();

                    if (modelObject.contains(authorizationType)) {
                        cellItem.add(new AttributeAppender("class", " table-dark"));
                        cellItem.add(new Label(componentId, "1"));
                    } else {
                        cellItem.add(new Label(componentId, "0"));
                    }

                }

                @Override
                public IModel<String> getDataModel(IModel<UPStructure> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {

                    String headerName = authorizationType.getName() == null ? "Undefined" : authorizationType.getName();
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.CLASS_LOCK_STATUS);

                    return new AjaxLinkTruncatePanel(
                            componentId, createStringResource(headerName), createStringResource(headerName), displayType) {
                        @Override
                        public boolean isEnabled() {
                            return false;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return PAStructure.F_TOTAL_RESULT;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UPStructure>> cellItem,
                    String componentId, IModel<UPStructure> model) {

                tableStyle(cellItem);

                //  double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<UPStructure> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    public List<IColumn<UPStructure, String>> initColumnsURM() {

        List<IColumn<UPStructure, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxColumn<>(createStringResource(" ")) {
            @Override
            protected IModel<Boolean> getEnabled(IModel<UPStructure> rowModel) {
                return () -> rowModel.getObject() != null;
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

        });

        columns.add(new IconColumn<>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<UPStructure> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("RoleMining.name.column")) {

            @Override
            public String getSortProperty() {
                return UPStructure.F_NAME;
            }

            @Override
            public IModel<?> getDataModel(IModel<UPStructure> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UPStructure>> item, String componentId,
                    IModel<UPStructure> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                item.add(new AjaxLinkPanel(componentId, createStringResource(rowModel.getObject().getUserObject().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        UserType object = rowModel.getObject().getUserObject().asObjectable();
                        PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                    }
                });
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, createStringResource("RoleMining.name.column")).add(
                        new AttributeAppender(
                                "style", "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<UPStructure, String> column;

        for (UPStructure userTypePrismObject : upStructuresList) {
            String columnName = userTypePrismObject.getUserObject().getName() == null ? "Undefined" :
                    String.valueOf(userTypePrismObject.getUserObject().getName());
            column = new AbstractExportableColumn<>(
                    createStringResource(columnName)) {

                @Override
                public void populateItem(Item<ICellPopulator<UPStructure>> cellItem,
                        String componentId, IModel<UPStructure> model) {

                    tableStyle(cellItem);
                    List<String> membersRoleA = model.getObject().getAssignRolesObjectIds();
                    List<String> membersRoleB = userTypePrismObject.getAssignRolesObjectIds();

                    String modelUserObjectId = model.getObject().getUserObject().getOid();
                    String columnUserObjectId = userTypePrismObject.getUserObject().getOid();

                    int iterateMembersIntersection = 0;
                    int modelMembersSize;
                    double percentageIntersection = 0;
                    if (membersRoleA != null && membersRoleB != null) {
                        iterateMembersIntersection = (int) membersRoleB.stream().filter(membersRoleA::contains).count();
                        modelMembersSize = membersRoleA.size();
                        if (modelMembersSize == 0) {
                            percentageIntersection = 0;
                        } else {
                            percentageIntersection = ((((double) iterateMembersIntersection) / modelMembersSize)) * 100.00;
                        }
                    }
                    //    System.out.println(percentageIntersection);

                    if (modelUserObjectId.equals(columnUserObjectId)) {
                        cellItem.add(new AttributeAppender("class", " table-warning"));
                        cellItem.add(new Label(componentId, iterateMembersIntersection));
                    } else if (percentageIntersection > 70.0) {
                        cellItem.add(new AttributeAppender("class", " table-danger"));
                        cellItem.add(new Label(componentId, iterateMembersIntersection));
                    } else if (percentageIntersection > 50.0) {
                        cellItem.add(new AttributeAppender("class", " table-info"));
                        cellItem.add(new Label(componentId, iterateMembersIntersection));
                    } else {
                        cellItem.add(new Label(componentId, iterateMembersIntersection));
                    }
                }

                @Override
                public IModel<String> getDataModel(IModel<UPStructure> rowModel) {
                    return Model.of(" ");
                }

                @Override
                public Component getHeader(String componentId) {

                    String headerName = userTypePrismObject.getUserObject().getName() == null ? "Undefined" :
                            String.valueOf(userTypePrismObject.getUserObject().getName());
                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId, createStringResource(headerName),
                            createStringResource(headerName), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            UserType object = userTypePrismObject.getUserObject().asObjectable();
                            PageRoleMiningSimple.this.detailsPerformed(PageUser.class, object.getOid());
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        column = new AbstractExportableColumn<>(
                createStringResource("Overlay (%)")) {
            @Override
            public String getCssClass() {
                return " role-mining-rotated-header";
            }

            @Override
            public String getSortProperty() {
                return PAStructure.F_TOTAL_RESULT;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UPStructure>> cellItem,
                    String componentId, IModel<UPStructure> model) {

                tableStyle(cellItem);

                //  double confidence = model.getObject().getObjectTotalResult();
                cellItem.add(new Label(componentId, Model.of(0)).add(
                        new AttributeAppender("class", "row font-weight-bold")));
            }

            @Override
            public IModel<String> getDataModel(IModel<UPStructure> rowModel) {
                return Model.of(" ");
            }

        };
        columns.add(column);

        return columns;
    }

    private void ajaxLinkExecuteJacquardMining(List<PrismObject<UserType>> users, @NotNull Form<?> secondForm) {
        AjaxButton ajaxLinkPanel = new AjaxButton(ID_JACCARD_AJAX_LINK, Model.of("Execute intersection search")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                basicCombResult = null;
                basicCombDisplayResultIterator = 0;
                executeJaccardMining(users);

                getBoxedTableUA().getDataTable().setItemsPerPage(30);
                getBoxedTableUA().replaceWith(getBoxedTableUA());

                getResultCountLabel().setDefaultModel(Model.of(createIterationResultString(0, 0)));

                target.add(getResultCountLabel());
                target.add(getBoxedTableUA());
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);
        secondForm.add(WebComponentUtil.createHelp(ID_JACCARD_EXECUTE_SEARCH_INFO));
        secondForm.add(ajaxLinkPanel);
    }

    private void executeJaccardMining(List<PrismObject<UserType>> users) {
        RoleMiningStructureList selectedRoleMiningTableData = null;
        for (RoleMiningStructureList data : roleMiningStructureLists) {
            if (data.isSelected()) {
                selectedRoleMiningTableData = data;
                data.setSelected(false);
            }
        }

        if (selectedRoleMiningTableData != null) {
            jaccardUsersAnalysed = new RoleAnalyseHelper()
                    .jaccardGetUserGroup(selectedRoleMiningTableData, jaccardThreshold, users);
            jaccardResultRoles = new RoleAnalyseHelper()
                    .jaccardGetRolesGroup(selectedRoleMiningTableData, jaccardUsersAnalysed, getPageBase());
        }

    }

    private void fillUP(List<PAStructure> permissionStructureList) {

        upStructuresList = new ArrayList<>();
        for (int i = 0; i < roleMiningStructureLists.size(); i++) {
            PrismObject<UserType> userTypePrismObject = roleMiningStructureLists.get(i).getUserObject();
            List<String> prismObjectListRoles = roleMiningStructureLists.get(i).getRoleObjectRef();
            List<AuthorizationType> userAuthorization = new ArrayList<>();
            for (String prismObjectListRole : prismObjectListRoles) {
                for (PAStructure paStructure : permissionStructureList) {
                    PrismObject<RoleType> permRoleTypePrismObject = paStructure.getRoleObject();
                    if (permRoleTypePrismObject.getOid().equals(prismObjectListRole)) {
                        List<AuthorizationType> permAuthTypeList = paStructure.getAuthorizationTypeList();
                        userAuthorization.addAll(permAuthTypeList);
                    }
                }
            }

            upStructuresList.add(new UPStructure(userTypePrismObject, prismObjectListRoles, userAuthorization, i));
        }
    }

    private void fillPermissionStructureList(@NotNull List<PrismObject<RoleType>> roles, List<PrismObject<UserType>> users) {
        authorizationTypeList = new ArrayList<>();
        permissionStructureList = new ArrayList<>();
        roleAnalyseStructures = new ArrayList<>();

        for (int i = 0; i < roles.size(); i++) {
            PrismObject<RoleType> roleTypePrismObject = roles.get(i);
            List<PrismObject<UserType>> roleMembers = new RoleMiningFilter().getRoleMembers(
                    getPageBase(), roleTypePrismObject.getOid());

            double totalResult = (double) roleMembers.size() / users.size();

            List<AuthorizationType> authorizationList = new RoleMiningFilter().getAuthorization(
                    roleTypePrismObject.asObjectable());

            authorizationList.stream().filter(
                    authorizationType -> !authorizationTypeList.contains(authorizationType)).forEach(
                    authorizationType -> authorizationTypeList.add(authorizationType));

            permissionStructureList.add(
                    new PAStructure(roles.get(i),
                            roleMembers, totalResult, null, authorizationList, i));

            analyseFillRoleStructure(roles.get(i).asObjectable(), roleMembers, authorizationList);
        }
    }

    private void fillJaccardData(@NotNull List<RoleMiningUserStructure> roleMiningUserStructureList) {

        roleMiningStructureLists = new ArrayList<>();
        int dataCount = roleMiningUserStructureList.size();
        for (int i = 0; i < dataCount; i++) {
            PrismObject<UserType> objectName = roleMiningUserStructureList.get(i).getUserObject();
            double objectTotalResult = 0.0;
            ArrayList<Double> objectPartialResult = new ArrayList<>();
            for (RoleMiningUserStructure miningUserStructure : roleMiningUserStructureList) {
                double jaccardIndex = new RoleAnalyseHelper().jaccardIndex(
                        roleMiningUserStructureList.get(i).getRoleObjectId(),
                        miningUserStructure.getRoleObjectId(), jaccardMinRolesCount
                );

                objectTotalResult = objectTotalResult + jaccardIndex;
                objectPartialResult.add(jaccardIndex);
            }
            objectTotalResult = objectTotalResult / dataCount;

            List<String> rolesObjectId = new ArrayList<>();

            List<ObjectReferenceType> objectReferenceTypeList = getRoleObjectReferenceTypes(objectName.asObjectable());

            for (ObjectReferenceType objectReferenceType : objectReferenceTypeList) {
                rolesObjectId.add(objectReferenceType.getOid());
            }

            roleMiningStructureLists.add(
                    new RoleMiningStructureList(objectName, objectPartialResult, objectTotalResult, rolesObjectId, i));

        }

    }

    private void jaccardThresholdSubmit(@NotNull Form<?> mainForm, List<RoleMiningUserStructure> roleMiningData) {
        final TextField<Double> inputThreshold = new TextField<>(ID_JACCARD_THRESHOLD_INPUT, Model.of(jaccardThreshold));
        inputThreshold.setOutputMarkupId(true);

        final TextField<Integer> inputMinRolesCount = new TextField<>(
                ID_JACCARD_MIN_ROLES_COUNT_INPUT, Model.of(jaccardMinRolesCount));
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
                fillJaccardData(roleMiningData);

                getBoxedTableExtra().replaceWith(generateTableJC(roleMiningStructureLists));
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

        if (roles != null && users != null) {
            for (PrismObject<RoleType> role : roles) {
                rolesOid.add(role.getOid());
            }

            List<List<String>> allCombinations = new CombinationHelperAlgorithm().generateCombinations(rolesOid, minSize);

            List<List<String>> matrix = getMatrix(users);
            basicCombResult = new CombinationHelperAlgorithm().combinationsResult(allCombinations, matrix);
        }
    }

    private void basicOperationHelper(@NotNull Form<?> mainForm, List<PrismObject<RoleType>> roles, List<PrismObject<UserType>> users) {

        Label repeatingCountBasicTable = new Label(ID_LABEL_DUPLICATES_BASIC, Model.of("0 duplicates"));
        repeatingCountBasicTable.setOutputMarkupId(true);
        repeatingCountBasicTable.setOutputMarkupPlaceholderTag(true);
        repeatingCountBasicTable.add(new VisibleBehaviour((this::isSearchMode)));
        mainForm.add(repeatingCountBasicTable);

        Label resultCountLabel = new Label(ID_LABEL_RESULT_COUNT, Model.of(
                createIterationResultString(0, 0)));
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
                        basicCombDisplayResultIterator = 1;
                    }
                    getResultCountLabel().setDefaultModel(Model.of(
                            createIterationResultString(basicCombDisplayResultIterator, basicCombResult.size())));
                } else {
                    getResultCountLabel().setDefaultModel(
                            Model.of(createIterationResultString(0, 0)));
                }
                getBoxedTableUA().getDataTable().setItemsPerPage(30);
                getBoxedTableUA().replaceWith(getBoxedTableUA());
            }

        };

        form.setOutputMarkupId(true);
        add(form);

        form.add(WebComponentUtil.createHelp(ID_HELPER_ALG_EXECUTE_INFO));
        form.add(WebComponentUtil.createHelp(ID_MIN_INTERSECTION_INPUT_INFO));
        form.add(inputMinIntersectionSize);

        mainForm.add(form);

        AjaxButton duplicateRolesComb = new AjaxButton(ID_AJAX_CHECK_DUPLICATE_BASIC) {
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

    private void searchSelector(@NotNull Form<?> mainForm) {

        AjaxButton buttonNextResult = new AjaxButton(ID_BUTTON_NEXT_RESULT) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (basicCombResult != null) {
                    if (basicCombDisplayResultIterator != 0) {
                        if (basicCombDisplayResultIterator == basicCombResult.size()) {
                            basicCombDisplayResultIterator = 0;
                        }
                    }
                    basicCombDisplayResultIterator++;
                    getBoxedTableUA().getDataTable().setItemsPerPage(30);
                    getBoxedTableUA().replaceWith(getBoxedTableUA());
                    ajaxRequestTarget.add(getBoxedTableUA());
                    getResultCountLabel().setDefaultModel(
                            Model.of(createIterationResultString(basicCombDisplayResultIterator, basicCombResult.size())));

                } else {
                    getResultCountLabel().setDefaultModel(
                            Model.of(createIterationResultString(0, 0)));
                }

                ajaxRequestTarget.add(getResultCountLabel());
            }
        };

        mainForm.add(buttonNextResult);

        AjaxButton roleSearch = new AjaxButton(ID_ROLE_SEARCH) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setSearchMode(true);

                target.add(getRepeatingCountBasicTable());
                target.add(getAjaxCheckDuplicateBasic());
                getBasicTable().replaceWith(basicRoleHelperTable());
                target.add(getBasicTable());

                getUserSearchLink().add(new AttributeModifier("class", " btn btn-default"));
                target.add(getUserSearchLink());

                getRoleSearchLink().add(new AttributeModifier("class", " btn btn-secondary"));
                target.add(getRoleSearchLink());
            }
        };

        roleSearch.setOutputMarkupId(true);

        if (!isSearchMode()) {
            roleSearch.add(new AttributeModifier("class", " btn btn-default"));
        } else {
            roleSearch.add(new AttributeModifier("class", " btn btn-secondary"));
        }

        mainForm.add(roleSearch);

        AjaxButton userSearch = new AjaxButton(ID_USER_SEARCH) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setSearchMode(false);
                target.add(getAjaxCheckDuplicateBasic());
                target.add(getRepeatingCountBasicTable());
                getBasicTable().replaceWith(basicUserHelperTable());
                target.add(getBasicTable());

                getRoleSearchLink().add(new AttributeModifier("class", " btn btn-default"));
                target.add(getRoleSearchLink());

                getUserSearchLink().add(new AttributeModifier("class", " btn btn-secondary"));
                target.add(getUserSearchLink());
            }

        };
        userSearch.setOutputMarkupId(true);

        if (!isSearchMode()) {
            userSearch.add(new AttributeModifier("class", " btn btn-secondary"));
        } else {
            userSearch.add(new AttributeModifier("class", " btn btn-default"));
        }

        mainForm.add(userSearch);
    }

    private void detailsPerformed(Class<? extends WebPage> pageClass, String objectOid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, objectOid);
        ((PageBase) getPage()).navigateToNext(pageClass, parameters);
    }

    private void emptyCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new Label(componentId, " "));
    }

    private void filledCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-dark"));
        cellItem.add(new Label(componentId, " "));
    }

    private void algMatchedCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-info"));
        cellItem.add(new Label(componentId, " "));
    }

    private void tableStyle(@NotNull Item<?> cellItem) {
        cellItem.getParent().getParent().add(AttributeAppender.replace("class", " d-flex"));
        cellItem.getParent().getParent().add(AttributeAppender.replace("style", " height:40px"));
        cellItem.add(new AttributeAppender("style", " width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    private List<ObjectReferenceType> getRoleObjectReferenceTypes(@NotNull AssignmentHolderType object) {
        return IntStream.range(0, object.getRoleMembershipRef().size())
                .filter(i -> object.getRoleMembershipRef().get(i).getType().getLocalPart()
                        .equals("RoleType")).mapToObj(i -> object.getRoleMembershipRef().get(i)).collect(Collectors.toList());

    }

    private boolean isSearchMode() {
        return searchMode;
    }

    private void setSearchMode(boolean searchMode) {
        this.searchMode = searchMode;
    }

    private List<PrismObject<RoleType>> getRoles() throws CommonException {
        return new RoleMiningFilter().filterRoles(((PageBase) getPage()));
    }

    private List<RoleMiningUserStructure> getRoleMiningData(List<PrismObject<UserType>> users) {
        return new RoleMiningFilter().filterUsersRoles(users);
    }

    private List<PrismObject<UserType>> getUsers() throws CommonException {
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

    List<List<String>> getMatrix(@NotNull List<PrismObject<UserType>> users) {

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

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected AjaxButton getAjaxCheckDuplicateBasic() {
        return (AjaxButton) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_AJAX_CHECK_DUPLICATE_BASIC));
    }

    protected AjaxButton getAjaxExecuteJC() {
        return (AjaxButton) get(((PageBase) getPage()).createComponentPath(ID_SECONDARY_FORM, ID_JACCARD_AJAX_LINK));
    }

    protected Label getResultCountLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_LABEL_RESULT_COUNT));
    }

    protected Label getRepeatingCountBasicTable() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_LABEL_DUPLICATES_BASIC));
    }

    protected AjaxButton getUserSearchLink() {
        return (AjaxButton) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_USER_SEARCH));
    }

    protected AjaxButton getRoleSearchLink() {
        return (AjaxButton) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_ROLE_SEARCH));
    }

    protected MainObjectListPanel<?> getBasicTable() {
        return (MainObjectListPanel<?>) get(((PageBase) getPage()).createComponentPath(ID_MAIN_FORM, ID_DATATABLE_DETAILS));
    }

    private void analyseFillRoleStructure(RoleType roleType, List<PrismObject<UserType>> roleMembers,
            List<AuthorizationType> authorizationTypeList) {
        roleAnalyseStructures.add(
                new RoleAnalyseStructure(roleType, roleMembers, authorizationTypeList)
        );
    }
}

