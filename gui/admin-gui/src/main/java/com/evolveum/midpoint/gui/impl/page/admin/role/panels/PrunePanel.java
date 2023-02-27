/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.*;

import java.util.*;
import java.util.stream.IntStream;

import com.github.openjson.JSONObject;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CandidateRole;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CostResult;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UpType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UrType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune.PruneBusinessProcess;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune.PruneCandidateProcess;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.ResultTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.TableResultCost;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class PrunePanel extends Panel {

    private static final String ID_FORM_TABLES_RBAC = "table_dropdown_rbac";
    private static final String ID_DROPDOWN_TABLE_RBAC = "dropdown_choice_rbac";
    private static final String ID_SUBMIT_DROPDOWN_RBAC = "ajax_submit_link_dropdown_rbac";

    private static final String ID_DATATABLE_EXTRA_RBAC = "datatable_extra_rbac";

    private static final List<String> SEARCH_ENGINES_RBAC = Arrays.asList("CR", "BR");
    public String selectedTable = "CR";

    private static final String ID_NETWORK_GRAPH_FORM = "graph_form";
    private static final String ID_NETWORK_GRAPH_CONTAINER = "network_graph_container_auth";
    private static final String ID_MODEL_COST = "model_cost";
    private static final String ID_BASIC_MODEL_COST = "model_cost_basic";
    private static final String ID_PROCESSED_VALUE = "processed_value";
    private static final String ID_TIME_VALUE = "time_value";
    private static final String ID_INPUT_SIGMA = "weight_o";
    private static final String ID_INPUT_TAU = "weight_t";
    private static final String ID_SUBMIT_WEIGHTS = "ajax_submit_link_weights";
    private static final String ID_HIDE_GRAPH = "network_graph_ajax_link_auth";
    private static final String ID_STORY_MINING_TABLE = "ajax_story_mining";

    double weightSigma = 0;
    double weightTau = 1 / (double) (11);
    double modelCost = 0;
    double modelCostBasic = 0;
    double modelProcessed = 0;
    long modelTime = 0;

    boolean candidateSearch = true;

    String javaScriptNetworkAuth;
    boolean containerVisibilityAuth = true;
    HeaderItem headerItem;

    public PrunePanel(String id, HeaderItem headerItem) {
        super(id);
        this.headerItem = headerItem;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(addNetworkForm().setOutputMarkupId(true));

        add(choiceTableFormRBAC());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        responseRenderAuthGraph(response, javaScriptNetworkAuth);
    }

    void responseRenderAuthGraph(@NotNull IHeaderResponse response, String javaScript) {

        response.render(headerItem);
        response.render(OnDomReadyHeaderItem.forScript(javaScript));
    }

    public Form<?> addNetworkForm() {

        Label costModel = new Label(ID_MODEL_COST, Model.of("Cost: " + modelCost));
        costModel.setOutputMarkupId(true);
        costModel.setOutputMarkupPlaceholderTag(true);

        Label costModelBasic = new Label(ID_BASIC_MODEL_COST, Model.of("Basic cost: " + modelCostBasic));
        costModelBasic.setOutputMarkupId(true);
        costModelBasic.setOutputMarkupPlaceholderTag(true);

        Label processedValueLabel = new Label(ID_PROCESSED_VALUE, Model.of("Processed: " + modelProcessed + "%"));
        processedValueLabel.setOutputMarkupId(true);
        processedValueLabel.setOutputMarkupPlaceholderTag(true);

        Label timeValueLabel = new Label(ID_TIME_VALUE, Model.of("Time: " + modelTime + "second(s)"));
        timeValueLabel.setOutputMarkupId(true);
        timeValueLabel.setOutputMarkupPlaceholderTag(true);

        final TextField<Double> weightO = new TextField<>(ID_INPUT_SIGMA, Model.of(weightSigma));
        weightO.setOutputMarkupId(true);

        final TextField<Double> weightT = new TextField<>(
                ID_INPUT_TAU, Model.of(weightTau));
        weightT.setOutputMarkupId(true);

        Form<?> graphForm = new Form<>(ID_NETWORK_GRAPH_FORM);
        graphForm.setOutputMarkupId(true);
        graphForm.add(weightO);
        graphForm.add(weightT);
        graphForm.add(processedValueLabel);
        graphForm.add(timeValueLabel);

        graphForm.add(WebComponentUtil.createHelp("weight_o_info"));
        graphForm.add(WebComponentUtil.createHelp("weight_t_info"));
        add(graphForm);

        graphForm.add(selectStory());

        AjaxSubmitLink ajaxSubmitDropdown = new AjaxSubmitLink(ID_SUBMIT_WEIGHTS, graphForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                weightSigma = weightO.getModelObject();
                weightTau = weightT.getModelObject();

                if (candidateSearch) {
                    javaScriptPermissionsMining();
                } else {
                    javaScriptBusinessRoleMining();
                }

                target.appendJavaScript(javaScriptNetworkAuth);
                //TODO result

                if (candidateSearch) {

                    getCostLabel().setDefaultModelObject("Cost: " + modelCost);
                    getCostBasicLabel().setDefaultModelObject("Basic cost: " + modelCostBasic);
                    getProcessedValueLabel().setDefaultModelObject("Processed: " + modelProcessed + "%");

                } else {

                    getCostLabel().setDefaultModelObject("Cost: " + modelCost);
                    getCostBasicLabel().setDefaultModelObject("Basic cost: " + modelCostBasic);
                    getProcessedValueLabel().setDefaultModelObject("Processed: " + modelProcessed + "%");

                }

                target.add(getCostLabel());
                target.add(getCostBasicLabel());
                target.add(getProcessedValueLabel());

                getTimeValueLabel().setDefaultModelObject("Time: " + modelTime + "second(s)");
                target.add(getTimeValueLabel());
            }
        };
        graphForm.add(ajaxSubmitDropdown);

        AjaxButton ajaxButtonReloadData = new AjaxButton("reload_data", Model.of("Reload data")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                resetAll();
                //new DataStorage().fillStructures(getPageBase());
            }

        };
        graphForm.add(ajaxButtonReloadData);

        WebMarkupContainer networkGraphContainerAuth = new WebMarkupContainer(ID_NETWORK_GRAPH_CONTAINER);
        networkGraphContainerAuth.setOutputMarkupId(true);
        networkGraphContainerAuth.setOutputMarkupPlaceholderTag(true);
        graphForm.add(networkGraphContainerAuth);

        AjaxButton networkGraphButtonAuth = new AjaxButton(
                ID_HIDE_GRAPH, Model.of("Show/Hide network auth graph")) {
            @Override
            public void onClick(AjaxRequestTarget target) {

                if (candidateSearch) {
                    javaScriptPermissionsMining();
                } else {
                    javaScriptBusinessRoleMining();
                }

                target.appendJavaScript(javaScriptNetworkAuth);

                containerVisibilityAuth = !containerVisibilityAuth;
                target.add(networkGraphContainerAuth.setVisible(containerVisibilityAuth));
            }
        };
        graphForm.add(networkGraphButtonAuth);
        graphForm.add(costModel);
        graphForm.add(costModelBasic);

        return graphForm;
    }

    private AjaxButton selectStory() {

        AjaxButton ajaxButton = new AjaxButton(ID_STORY_MINING_TABLE, Model.of("Permission mining")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {

                resetAll();

                candidateSearch = !candidateSearch;
                String info;
                if (candidateSearch) {
                    info = "Permission mining";
                } else {
                    info = "Business role mining";
                }

                modelCost = 0;
                modelCostBasic = 0;
                modelProcessed = 0;
                modelTime = 0;

                getCostLabel().setDefaultModelObject("Cost: " + modelCost);
                getCostBasicLabel().setDefaultModelObject("Basic cost: " + modelCostBasic);
                getProcessedValueLabel().setDefaultModelObject("Processed: " + modelProcessed + "%");

                target.add(getCostLabel());
                target.add(getCostBasicLabel());
                target.add(getProcessedValueLabel());

                getTimeValueLabel().setDefaultModelObject("Time: " + modelTime + "second(s)");
                target.add(getTimeValueLabel());

                setDefaultModelObject(info);
                target.add(this);
            }
        };
        ajaxButton.setOutputMarkupId(true);
        ajaxButton.setOutputMarkupPlaceholderTag(true);
        return ajaxButton;
    }

    public void javaScriptPermissionsMining() {
        double vC = 0;

        //  PruneCandidateProcess pruneAlgorithm = new PruneCandidateProcess(getPageBase());
        PruneCandidateProcess pruneAlgorithm = new PruneCandidateProcess(getPageBase());

        pruneAlgorithm.process(weightSigma, weightTau, vC);

        // modelCost = apiary.getUpdatedModelCost();
        modelCostBasic = pruneAlgorithm.calculateModelBasicCost();
        modelCost = pruneAlgorithm.calculateNewModelCost();
        modelProcessed = pruneAlgorithm.processedValue();
        modelTime = pruneAlgorithm.getTimeMining();
        getCostLabel().setDefaultModelObject("Cost: " + modelCost);
        getCostBasicLabel().setDefaultModelObject("Basic cost: " + modelCostBasic);
        getProcessedValueLabel().setDefaultModelObject("Processed: " + modelProcessed + "%");
        getTimeValueLabel().setDefaultModelObject("Time: " + modelTime + "second(s)");

        List<JSONObject> jsonObjectIds = pruneAlgorithm.getJsonIds();
        List<JSONObject> jsonObjectEdges = pruneAlgorithm.getJsonEdges();

        javaScriptNetworkAuth = "network_graph_auth('"
                + jsonObjectIds + "', '"
                + jsonObjectEdges + "');";

    }

    public void javaScriptBusinessRoleMining() {
        double vC = 0;

        PruneBusinessProcess pruneRoleBusinessAlgorithm = new PruneBusinessProcess(getPageBase());

        pruneRoleBusinessAlgorithm.process(weightSigma, weightTau, vC);

        // modelCost = apiary.getUpdatedModelCost();
        modelCostBasic = pruneRoleBusinessAlgorithm.calculateModelBasicCost();
        modelCost = pruneRoleBusinessAlgorithm.calculateNewModelCost();
        modelProcessed = pruneRoleBusinessAlgorithm.processedValue();
        modelTime = pruneRoleBusinessAlgorithm.getTimeMining();
        getCostLabel().setDefaultModelObject("Cost: " + modelCost);
        getCostBasicLabel().setDefaultModelObject("Basic cost: " + modelCostBasic);
        getProcessedValueLabel().setDefaultModelObject("Processed: " + modelProcessed + "%");
        getTimeValueLabel().setDefaultModelObject("Time: " + modelTime + "second(s)");

        List<JSONObject> jsonObjectIds = pruneRoleBusinessAlgorithm.getJsonIds();
        List<JSONObject> jsonObjectEdges = pruneRoleBusinessAlgorithm.getJsonEdges();

        javaScriptNetworkAuth = "network_graph_auth('"
                + jsonObjectIds + "', '"
                + jsonObjectEdges + "');";

    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected Label getCostLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_NETWORK_GRAPH_FORM, ID_MODEL_COST));
    }

    protected Label getCostBasicLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_NETWORK_GRAPH_FORM, ID_BASIC_MODEL_COST));
    }

    protected Label getProcessedValueLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_NETWORK_GRAPH_FORM, ID_PROCESSED_VALUE));
    }

    protected Label getTimeValueLabel() {
        return (Label) get(((PageBase) getPage()).createComponentPath(ID_NETWORK_GRAPH_FORM, ID_TIME_VALUE));
    }

    private @NotNull Form<?> choiceTableFormRBAC() {

        DropDownChoice<String> listSites = new DropDownChoice<>(
                ID_DROPDOWN_TABLE_RBAC, new PropertyModel<>(this, "selectedTable"), SEARCH_ENGINES_RBAC);

        Form<?> formDropdown = new Form<Void>(ID_FORM_TABLES_RBAC);

        formDropdown.setOutputMarkupId(true);

        formDropdown.add(new Label(ID_DATATABLE_EXTRA_RBAC).setOutputMarkupId(true));

        AjaxSubmitLink ajaxSubmitDropdown = new AjaxSubmitLink(ID_SUBMIT_DROPDOWN_RBAC, formDropdown) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                switch (selectedTable) {
                    case "CR":
                        if (candidateSearch) {
                            if (getCandidateKeyUpStructureMapUP().size() != 0 && getRolesDegrees().size() != 0) {
                                getBoxedTableExtraRBAC().replaceWith(new TableResultCost(ID_DATATABLE_EXTRA_RBAC,
                                        generateCostResultUP(), true, false,true));
                                target.add(getBoxedTableExtraRBAC().setOutputMarkupId(true));
                            }
                        } else {
                            if (getCandidateKeyUpStructureMapUR().size() != 0 && getRolesDegrees().size() != 0) {
                                getBoxedTableExtraRBAC().replaceWith(new TableResultCost(ID_DATATABLE_EXTRA_RBAC,
                                        generateCostResultUR(), false, true,true));
                                target.add(getBoxedTableExtraRBAC().setOutputMarkupId(true));
                            }
                        }
                        break;
                    case "BR":
                        if (getResultData().size() != 0) {
                            if (candidateSearch) {
                                getBoxedTableExtraRBAC().replaceWith(new ResultTable(ID_DATATABLE_EXTRA_RBAC,
                                        getResultData(), true, false).setOutputMarkupId(true));
                            } else {
                                getBoxedTableExtraRBAC().replaceWith(new ResultTable(ID_DATATABLE_EXTRA_RBAC,
                                        getResultData(), false, true).setOutputMarkupId(true));
                            }
                            target.add(getBoxedTableExtraRBAC().setOutputMarkupId(true));
                        }
                        break;
                    default:
                }
            }
        };

        formDropdown.add(listSites);
        formDropdown.add(ajaxSubmitDropdown);
        return formDropdown;
    }

    protected Component getBoxedTableExtraRBAC() {
        return get(((PageBase) getPage()).createComponentPath(ID_FORM_TABLES_RBAC, ID_DATATABLE_EXTRA_RBAC));
    }

    public List<CostResult> generateCostResultUR() {
        List<CostResult> costResults = new ArrayList<>();

        HashMap<Integer, List<UrType>> candidateKeyUpStructureMap = getCandidateKeyUpStructureMapUR();
        List<HashMap<Integer, CandidateRole>> rolesDegrees = getRolesDegrees();

        List<PrismObject<RoleType>> checkRoles = new RoleMiningFilter().filterRoles(getPageBase());

        for (int i = 1; i < rolesDegrees.size(); i++) {
            HashMap<Integer, CandidateRole> integerCandidateRoleHashMap = rolesDegrees.get(i);

            for (Map.Entry<Integer, CandidateRole> entry : integerCandidateRoleHashMap.entrySet()) {

                CandidateRole candidateRole = entry.getValue();
                double roleCost = candidateRole.getActualSupport();

                if (roleCost == 0) {
                    continue;
                }
                int roleKey = entry.getKey();
                int roleDegree = candidateRole.getDegree() + 1;

                List<RoleType> candidateRoles = candidateRole.getCandidateRoles();

                List<UrType> urTypeList = candidateKeyUpStructureMap.get(roleKey);

                int urListSize = urTypeList.size();
                int assignBefore = 0;
                int reducedCount = 0;
                for (UrType urType : urTypeList) {
                    List<RoleType> roleMembers = urType.getRoleMembers();
                    int membersSize = roleMembers.size();
                    assignBefore = assignBefore + membersSize;
                    reducedCount += IntStream.range(0, membersSize)
                            .filter(m -> candidateRoles.contains(roleMembers.get(m)))
                            .count();
                }

                reducedCount = reducedCount - urListSize;

                List<RoleType> equivalentRoles = new ArrayList<>();
                for (int j = 0; j < checkRoles.size(); j++) {
                    if (checkRoles.get(j).asObjectable().getAuthorization().equals(candidateRole.getCandidatePermissions())) {
                        equivalentRoles.add(checkRoles.get(j).asObjectable());
                        checkRoles.remove(checkRoles.get(j));
                    }
                }

                double reduceValue = ((assignBefore - reducedCount) / (double) assignBefore) * 100;
                costResults.add(new CostResult(roleKey, roleDegree, roleCost, reduceValue, urTypeList, candidateRole,
                        equivalentRoles));

            }
        }
        return costResults;
    }

    public List<CostResult> generateCostResultUP() {
        List<CostResult> costResults = new ArrayList<>();

        HashMap<Integer, List<UpType>> candidateKeyUpStructureMap = getCandidateKeyUpStructureMapUP();
        List<HashMap<Integer, CandidateRole>> rolesDegrees = getRolesDegrees();

        List<PrismObject<RoleType>> checkRoles = new RoleMiningFilter().filterRoles(getPageBase());

        for (int i = 1; i < rolesDegrees.size(); i++) {
            HashMap<Integer, CandidateRole> integerCandidateRoleHashMap = rolesDegrees.get(i);

            for (Map.Entry<Integer, CandidateRole> entry : integerCandidateRoleHashMap.entrySet()) {

                CandidateRole candidateRole = entry.getValue();
                double roleCost = candidateRole.getActualSupport();

                if (roleCost == 0) {
                    continue;
                }
                int roleKey = entry.getKey();
                int roleDegree = candidateRole.getDegree() + 1;

                List<AuthorizationType> candidatePermissions = candidateRole.getCandidatePermissions();

                List<UpType> upTypeList = candidateKeyUpStructureMap.get(roleKey);

                List<UrType> urTypeList = new ArrayList<>();
                int upListSize = upTypeList.size();
                int assignBefore = 0;
                int reducedCount = 0;
                for (UpType upType : upTypeList) {
                    UserType userObjectType = upType.getUserObjectType();
                    urTypeList.add(new UrType(userObjectType, new RoleMiningFilter().getUserRoles(userObjectType, getPageBase())));
                    List<AuthorizationType> rolePermissions = upType.getPermission();
                    int membersSize = rolePermissions.size();
                    assignBefore = assignBefore + membersSize;
                    long count = 0L;
                    for (AuthorizationType rolePermission : rolePermissions) {
                        if (candidatePermissions.contains(rolePermission)) {
                            count++;
                        }
                    }
                    reducedCount += count;
                }

                reducedCount = reducedCount - upListSize;

                List<RoleType> equivalentRoles = new ArrayList<>();
                for (int j = 0; j < checkRoles.size(); j++) {
                    if (checkRoles.get(j).asObjectable().getAuthorization().equals(candidateRole.getCandidatePermissions())) {
                        equivalentRoles.add(checkRoles.get(j).asObjectable());
                        checkRoles.remove(checkRoles.get(j));
                    }
                }

                double reduceValue = ((assignBefore - reducedCount) / (double) assignBefore) * 100;
                costResults.add(new CostResult(roleKey, roleDegree, roleCost, reduceValue, urTypeList, candidateRole,
                        equivalentRoles));

            }
        }
        return costResults;
    }
}
