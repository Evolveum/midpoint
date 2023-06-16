/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.resetAll;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.ClusterObjectUtils.deleteClusterObjects;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.ClusterObjectUtils.importClusterTypeObject;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils.MiningObjectUtils.*;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.GenerateDataPanelRBAM;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.ClusterBasicTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.ClusterTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.test.cluster.ClusterAlgorithm;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleMiningCl", matchUrlForSecurity = "/admin/roleMiningCl")
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

public class PageRoleMining extends PageAdmin {

    private static final String ID_GENERATE_DATA_PANEL = "generate_data_panel";
    private static final String ID_FORM_THRESHOLDS = "thresholds_form";

    private static final String ID_FORM_THRESHOLDS_CLUSTER = "thresholds_form_cluster";
    private static final String ID_DATATABLE_CLUSTER = "datatable_cluster";
    private static final String ID_DATATABLE_CLUSTER_DS = "datatable_cluster_ds";

    double jcThreshold = 0.80;
    int minIntersection = 5;

    double eps = 0.2;
    int minIntersectionEps = 10;

    int minGroup = 10;

    public PageRoleMining() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(generateObjectsPanel());
        add(getGenerateMiningTypeButton());
        add(getDeleteMiningTypeButton());

        add(getSimilarityTypeButton());
        add(similarityMining());

        add(clusterForm());
        add(new ClusterTable(ID_DATATABLE_CLUSTER).setOutputMarkupId(true));
        add(new ClusterBasicTable(ID_DATATABLE_CLUSTER_DS).setOutputMarkupId(true));

    }

    protected Component getDSTable() {
        return get(((PageBase) getPage()).createComponentPath(ID_DATATABLE_CLUSTER_DS));
    }

    public AjaxButton getGenerateMiningTypeButton() {
        AjaxButton ajaxLinkAssign = new AjaxButton("id_generate_mining_set", Model.of("Import MiningType Objects")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                OperationResult result = new OperationResult("Generate miningType object");
                try {

                    importMiningGroups(result, getPageBase(), 15);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;

    }

    public AjaxButton getSimilarityTypeButton() {
        AjaxButton ajaxLinkAssign = new AjaxButton("id_similarity_mining_set",
                Model.of("Similarity MiningType Objects")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    similarityUpdaterIntersection(getPageBase(), 10);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;

    }

    public AjaxButton getDeleteMiningTypeButton() {
        AjaxButton ajaxLinkAssign = new AjaxButton("id_delete_mining_set", Model.of("Delete Mining Objects")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                OperationResult result = new OperationResult("Delete miningType objects");
                try {
                    deleteMiningObjects(result, getPageBase());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;

    }

    private @NotNull
    AjaxButton generateObjectsPanel() {
        AjaxButton ajaxLinkAssign = new AjaxButton(ID_GENERATE_DATA_PANEL, Model.of("Generate data")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                resetAll();

                GenerateDataPanelRBAM pageGenerateData = new GenerateDataPanelRBAM(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("RoleMining.generateDataPanel.title"));
                getPageBase().showMainPopup(pageGenerateData, target);
            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;
    }

    public Form<?> clusterForm() {

        Form<?> form = new Form<Void>(ID_FORM_THRESHOLDS_CLUSTER);

        TextField<Double> thresholdField = new TextField<>("eps_cluster", Model.of(eps));
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        form.add(thresholdField);

        TextField<Integer> minIntersectionField = new TextField<>("intersection_field_min_cluster", Model.of(minIntersectionEps));
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        form.add(minIntersectionField);

        TextField<Integer> minGroupField = new TextField<>("group_min_cluster", Model.of(minGroup));
        minGroupField.setOutputMarkupId(true);
        minGroupField.setOutputMarkupPlaceholderTag(true);
        minGroupField.setVisible(true);
        form.add(minGroupField);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink("ajax_submit_link_cluster", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                eps = thresholdField.getModelObject();
                minIntersectionEps = minIntersectionField.getModelObject();
                minGroup = minGroupField.getModelObject();

                ClusterAlgorithm clusterAlgorithm = new ClusterAlgorithm(getPageBase());
                List<PrismObject<ClusterType>> miningTypeList = clusterAlgorithm.executeClustering(eps, minGroup, minIntersection);
                OperationResult resultD = new OperationResult("Delete Cluster object");

                try {
                    deleteClusterObjects(resultD, getPageBase());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                OperationResult result = new OperationResult("Generate Cluster object");
                try {
                    for (PrismObject<ClusterType> clusterTypePrismObject : miningTypeList) {
                        importClusterTypeObject(result, getPageBase(), clusterTypePrismObject);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                target.add(getDSTable());

                target.add(thresholdField);
                target.add(minIntersectionField);
                target.add(minGroupField);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);
        form.add(ajaxSubmitLink);

        return form;
    }

    public Form<?> similarityMining() {

        Form<?> form = new Form<Void>(ID_FORM_THRESHOLDS);

        TextField<Double> thresholdField = new TextField<>("threshold_field_jc", Model.of(jcThreshold));
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        form.add(thresholdField);

        TextField<Integer> minIntersectionField = new TextField<>("intersection_field_min", Model.of(minIntersection));
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        form.add(minIntersectionField);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink("ajax_submit_link_mn", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                jcThreshold = thresholdField.getModelObject();
                minIntersection = minIntersectionField.getModelObject();
                try {
                    similarityUpdaterOidJaccard(getPageBase(), minIntersection, jcThreshold);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                target.add(thresholdField);
                target.add(minIntersectionField);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);
        form.add(ajaxSubmitLink);

        return form;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}

