/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.rbam.UA;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.RBAMAlgorithm;
import com.evolveum.midpoint.gui.api.component.mining.structure.UPStructure;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.github.openjson.JSONObject;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleMiningPrune", matchUrlForSecurity = "/admin/roleMiningPrune")
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

public class PageRoleMiningRBAM extends PageAdmin {

    private static final String ID_DATATABLE_RBAM_UA = "datatableRBAM";
    private static final String ID_BASIC_FORM = "secondary_form";
    private static final String ID_NETWORK_GRAPH_FORM = "graph_form";
    private static final String ID_NETWORK_GRAPH_CONTAINER = "network_graph_container_auth";
    private static final String ID_MODEL_COST = "model_cost";
    private static final String ID_BASIC_MODEL_COST = "model_cost_basic";
    private static final String ID_INPUT_SIGMA = "weight_o";
    private static final String ID_INPUT_TAU = "weight_t";
    private static final String ID_SUBMIT_WEIGHTS = "ajax_submit_link_weights";
    private static final String ID_HIDE_GRAPH = "network_graph_ajax_link_auth";

    double weightSigma = 0;
    double weightTau = 1 / (double) (11);
    double modelCost = 0;
    double modelCostBasic = 0;

    List<AuthorizationType> authorizationTypeList;
    List<UPStructure> upStructuresList;
    String javaScriptNetworkAuth;

    boolean containerVisibilityAuth = true;
    int usersCount = 0;

    List<UA> uaList = new ArrayList<>();

    public PageRoleMiningRBAM() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        networkGraphSection();

        List<PrismObject<RoleType>> roles;
        List<PrismObject<UserType>> users;
        try {
            roles = getRoles();
            users = getUsers();
            usersCount = users.size();
            fill(users, roles);

        } catch (CommonException e) {
            throw new RuntimeException("Failed to load basic role mining list: " + e);
        }

        Form<?> basicForm = new MidpointForm<>(ID_BASIC_FORM);
        basicForm.setOutputMarkupId(true);
        add(basicForm);
        basicForm.add(generateTableRBAM(uaList));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        responseRenderAuthGraph(response, javaScriptNetworkAuth);
    }

    void responseRenderAuthGraph(@NotNull IHeaderResponse response, String javaScript) {

        response.render(JavaScriptHeaderItem.forReference(
                new PackageResourceReference(PageRoleMiningRBAM.class, "js/network_graph_auth.js")));

        response.render(OnDomReadyHeaderItem.forScript(javaScript));
    }

    public void generateJsRBAM() {

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

    public void fill(List<PrismObject<UserType>> userList, List<PrismObject<RoleType>> roleList) {

        upStructuresList = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            PrismObject<UserType> userObject = userList.get(i);
            List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject.asObjectable(), getPageBase());
            List<String> assignRolesObjectIds = new ArrayList<>();
            List<AuthorizationType> assignPermission = new ArrayList<>();

            for (int j = 0; j < userRoles.size(); j++) {
                RoleType roleType = userRoles.get(j);
                assignRolesObjectIds.add(String.valueOf(roleType.getOid()));

                List<AuthorizationType> authorization = roleType.getAuthorization();

                authorization.stream().filter(authorizationType -> !assignPermission
                                .contains(authorizationType))
                        .forEach(assignPermission::add);
            }

            upStructuresList.add(new UPStructure(userObject, assignRolesObjectIds, assignPermission, i));
        }

        authorizationTypeList = new ArrayList<>();
        for (int i = 0; i < roleList.size(); i++) {
            PrismObject<RoleType> roleTypePrismObject = roleList.get(i);
            List<AuthorizationType> authorization = roleTypePrismObject.asObjectable().getAuthorization();

            authorization.stream().filter(authorizationType -> !authorizationTypeList
                            .contains(authorizationType))
                    .forEach(authorizationType -> authorizationTypeList.add(authorizationType));
        }
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
                        PageRoleMiningRBAM.this.detailsPerformed(object.getOid());
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
                createStringResource("Originally roles")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<UA>> cellItem,
                    String componentId, IModel<UA> model) {

                UserType userObject = model.getObject().getUserUP().getUserObject().asObjectable();

                List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject, getPageBase());

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("{");
                IntStream.range(0, userRoles.size()).forEach(i -> stringBuilder.append(userRoles.get(i).getName()).append(", "));
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

    protected BoxedTablePanel<?> getBoxedTableRBAM() {
        return (BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_BASIC_FORM, ID_DATATABLE_RBAM_UA));
    }

    protected Label getCostLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_NETWORK_GRAPH_FORM, ID_MODEL_COST));
    }

    protected Label getCostBasicLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_NETWORK_GRAPH_FORM, ID_BASIC_MODEL_COST));
    }

    private List<PrismObject<RoleType>> getRoles() throws CommonException {
        return new RoleMiningFilter().filterRoles(((PageBase) getPage()));
    }

    private List<PrismObject<UserType>> getUsers() throws CommonException {
        return new RoleMiningFilter().filterUsers(((PageBase) getPage()));
    }

    private void detailsPerformed(String objectOid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, objectOid);
        ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}

