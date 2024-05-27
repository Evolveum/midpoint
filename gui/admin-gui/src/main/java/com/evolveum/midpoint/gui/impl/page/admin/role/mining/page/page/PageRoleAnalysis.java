/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisInfoPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisInfoItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisSessionTileTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysis", matchUrlForSecurity = "/admin/roleAnalysis")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                        label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                        description = "PageRoleAnalysis.auth.roleAnalysisAll.description")
        })

public class PageRoleAnalysis extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_INFO_FORM = "infoForm";
    private static final String ID_CHART_PANEL = "chartPanel";
    private static final String ID_TABLE = "table";

    public PageRoleAnalysis(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form<?> infoForm = new MidpointForm<>(ID_INFO_FORM);
        add(infoForm);

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        if (!isNativeRepo()) {
            mainForm.add(new ErrorPanel(ID_TABLE, createStringResource("RoleAnalysis.menu.nonNativeRepositoryWarning")));
            add(new EmptyPanel(ID_CHART_PANEL));
            return;
        }

        RoleAnalysisInfoPanel roleAnalysisInfoPanel = new RoleAnalysisInfoPanel(ID_CHART_PANEL) {
            @Override
            public void addItems(RepeatingView repeatingView) {
                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                Task task = pageBase.createSimpleTask("loadRoleAnalysisInfo");
                OperationResult result = task.getResult();
                List<DetectedPattern> topPatters = roleAnalysisService.findTopPatters(task, result);
                for (int i = 0; i < topPatters.size(); i++) {
                    DetectedPattern pattern = topPatters.get(i);
                    double reductionFactorConfidence = pattern.getMetric();
                    String formattedReductionFactorConfidence = String.format("%.0f", reductionFactorConfidence);
                    double itemsConfidence = pattern.getItemsConfidence();
                    String formattedItemConfidence = String.format("%.1f", itemsConfidence);
                    String label = "Detected a potential reduction of " +
                            formattedReductionFactorConfidence +
                            "x relationships with a confidence of  " +
                            formattedItemConfidence + "%";
                    int finalI = i;
                    repeatingView.add(new RoleAnalysisInfoItem(repeatingView.newChildId(), Model.of(label)) {

                        @Override
                        protected String getIconBoxText() {
//                            return "#" + (finalI + 1);
                            return null;
                        }

                        @Override
                        protected String getIconBoxIconStyle() {
                            return super.getIconBoxIconStyle();
                        }

                        @Override
                        protected String getIconContainerCssClass() {
                            return "btn btn-outline-dark";
                        }

                        @Override
                        protected void addDescriptionComponents() {
                            appendText("Detected a potential reduction from ");
                            appendIcon("fe fe-assignment", "color: red;");
                            appendText(" " + formattedReductionFactorConfidence + " assignments, ");
                            appendText("with a attributes confidence of");
                            appendIcon("fa fa-leaf", "color: green");
                            appendText(" " + formattedItemConfidence + "%.");
                        }

                        @Override
                        protected IModel<String> getLinkModel() {
                            IModel<String> linkModel = super.getLinkModel();
                            return Model.of(linkModel.getObject() + " pattern #" + (finalI + 1));
                        }

                        @Override
                        protected void onClickLinkPerform(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String clusterOid = pattern.getClusterRef().getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                            parameters.add("panelId", "clusterDetails");
                            parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
                            StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                            if (fullTableSetting != null && fullTableSetting.toString() != null) {
                                parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                            }

                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisClusterType.class);
                            getPageBase().navigateToNext(detailsPageClass, parameters);
                        }

                        @Override
                        protected void onClickIconPerform(AjaxRequestTarget target) {
                            RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of(pattern));
                            ((PageBase) getPage()).showMainPopup(component, target);
                        }
                    });
                }
            }
        };
        roleAnalysisInfoPanel.setOutputMarkupId(true);
        infoForm.add(roleAnalysisInfoPanel);

        RoleAnalysisSessionTileTable roleAnalysisSessionTileTable = new RoleAnalysisSessionTileTable(ID_TABLE, (PageBase) getPage());
        roleAnalysisSessionTileTable.setOutputMarkupId(true);
        mainForm.add(roleAnalysisSessionTileTable);

    }

}
