/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.getScaleScript;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.LineFieldPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.ClusterSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.FormSessionPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.OperationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.PageOperationsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import javax.xml.datatype.XMLGregorianCalendar;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/miningOperation1", matchUrlForSecurity = "/admin/miningOperation1")
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

public class PageMiningOperation extends PageAdmin {

    public static final String CHILD_PARAMETER_OID = "child_oid";
    public static final String PARENT_PARAMETER_OID = "parent_oid";

    private static final String ID_PANEL = "panel";

    private static final String ID_CONTAINER = "container";

    boolean sessionCluster = true;
    boolean sessionOption = false;
    boolean sessionStatistic = false;

    AjaxIconButton clusters;
    AjaxIconButton options;
    AjaxIconButton statistic;

    WebMarkupContainer panel;
    WebMarkupContainer webMarkupContainer;

    OperationResult operationResult = new OperationResult("deleteSingleClusterObject");

    String getPageParameterChildOid() {
        PageParameters params = getPageParameters();
        return params.get(OnePageParameterEncoder.PARAMETER).toString();
    }

    String getPageParameterParentOid() {
        PageParameters params = getPageParameters();
        return params.get(PARENT_PARAMETER_OID).toString();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
    }

    ObjectDetailsModels<RoleAnalysisClusterType> clusterModel;

    public PageMiningOperation() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        webMarkupContainer = new WebMarkupContainer(ID_CONTAINER);
        webMarkupContainer.setOutputMarkupId(true);
        add(webMarkupContainer);

        LoadableDetachableModel<PrismObject<RoleAnalysisClusterType>> detachableModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<RoleAnalysisClusterType> load() {
                return getClusterTypeObject((PageBase) getPage(), operationResult, getPageParameterChildOid());
//                return clusterTypeObject.asObjectable();
            }
        };
        clusterModel = new ObjectDetailsModels<>(detachableModel, PageMiningOperation.this);

        add(initSummaryPanel());

        OperationPanel operationPanel = new OperationPanel("operation_panel") {

            @Override
            protected void addPanelButton(RepeatingView repeatingView) {

                statistic = new AjaxIconButton(repeatingView.newChildId(), Model.of("fa fa-bar-chart"),
                        getPageBase().createStringResource("role.mining.cluster.statistic.panel")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        if (!sessionStatistic) {
                            sessionStatistic = true;
                            sessionCluster = false;
                            sessionOption = false;

                            ajaxRequestTarget.add(getPanel().replaceWith(createClusterStatisticPanel().setOutputMarkupId(true)));
                            ajaxRequestTarget.add(webMarkupContainer);

                            options.add(new AttributeModifier("class", "btn btn-default btn-sm"));
                            clusters.add(new AttributeModifier("class", "btn btn-default btn-sm"));
                            statistic.add(new AttributeModifier("class", "btn btn-primary btn-sm"));
                            ajaxRequestTarget.add(statistic);
                            ajaxRequestTarget.add(options);
                            ajaxRequestTarget.add(clusters);
                        }

                    }
                };

                statistic.showTitleAsLabel(true);
                statistic.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                repeatingView.add(statistic);

                clusters = new AjaxIconButton(repeatingView.newChildId(), Model.of("fa fa-database"),
                        getPageBase().createStringResource("role.mining.cluster.clusters.panel")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        if (!sessionCluster) {
                            sessionStatistic = false;
                            sessionCluster = true;
                            sessionOption = false;

                            ajaxRequestTarget.add(getPanel().replaceWith(new PageOperationsPanel(ID_PANEL,
                                    getPageParameterParentOid(), getPageParameterChildOid(), clusterModel).setOutputMarkupId(true)));
                            ajaxRequestTarget.add(webMarkupContainer);

                            options.add(new AttributeModifier("class", "btn btn-default btn-sm"));
                            clusters.add(new AttributeModifier("class", "btn btn-primary btn-sm"));
                            statistic.add(new AttributeModifier("class", "btn btn-default btn-sm"));
                            ajaxRequestTarget.add(statistic);
                            ajaxRequestTarget.add(options);
                            ajaxRequestTarget.add(clusters);
                        }

                    }
                };

                clusters.showTitleAsLabel(true);
                clusters.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
                repeatingView.add(clusters);

                options = new AjaxIconButton(repeatingView.newChildId(), Model.of("fa fa-cog"),
                        getPageBase().createStringResource("role.mining.cluster.option.panel")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        if (!sessionOption) {
                            sessionStatistic = false;
                            sessionCluster = false;
                            sessionOption = true;

                            ajaxRequestTarget.add(getPanel().replaceWith(createClusterOptionsPanel()));
                            ajaxRequestTarget.add(webMarkupContainer);

                            options.add(new AttributeModifier("class", "btn btn-primary btn-sm"));
                            clusters.add(new AttributeModifier("class", "btn btn-default btn-sm"));
                            statistic.add(new AttributeModifier("class", "btn btn-default btn-sm"));
                            ajaxRequestTarget.add(statistic);
                            ajaxRequestTarget.add(options);
                            ajaxRequestTarget.add(clusters);
                        }
                    }
                };

                options.showTitleAsLabel(true);
                options.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                repeatingView.add(options);

            }

            @Override
            protected void executeDelete(AjaxRequestTarget target) {
                PrismObject<RoleAnalysisClusterType> clusterTypeObject = getClusterTypeObject((PageBase) getPage(), operationResult, getPageParameterChildOid());
                deleteSingleRoleAnalysisCluster(operationResult, clusterTypeObject.asObjectable(),
                        (PageBase) getPage());
                getPageBase().redirectBack();
            }
        };
        operationPanel.setOutputMarkupId(true);
        add(operationPanel);


        panel = new PageOperationsPanel(ID_PANEL, getPageParameterParentOid(), getPageParameterChildOid(), clusterModel);
        webMarkupContainer.add(panel);

    }



//    String getPageParameterChildOid() {
//        PageParameters params = getPageParameters();
//        return params.get(CHILD_PARAMETER_OID).toString();
//    }
//
//    String getPageParameterParentOid() {
//        PageParameters params = getPageParameters();
//        return params.get(PARENT_PARAMETER_OID).toString();
//    }

    private Component createClusterOptionsPanel() {
        FormSessionPanel components = new FormSessionPanel(ID_PANEL, Model.of("Options")) {
            @Override
            protected void addPanelButton(RepeatingView repeatingView) {

                @NotNull RoleAnalysisClusterType clusterTypeObject = getClusterTypeObject((PageBase) getPage(), operationResult,
                        getPageParameterChildOid()).asObjectable();

                RoleAnalysisDetectionOptionType clusterOptions = clusterTypeObject.getDetectionOption();

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Name")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterTypeObject.getName()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.option.name");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Jaccard similarity")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterOptions.getJaccardSimilarityThreshold()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.option.similarity");
                    }
                }.setOutputMarkupId(true));
                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Detection mode")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(clusterOptions.getDetectionMode().value().toUpperCase());
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.option.detection.mode");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Min members occupancy")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterOptions.getMinMembersOccupancy()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.option.min.members.occupancy");
                    }
                }.setOutputMarkupId(true));
                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Min properties occupancy")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterOptions.getMinPropertiesOccupancy()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.option.min.properties.occupancy");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Frequency range")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of("from "
                                + (clusterOptions.getMinFrequencyThreshold() * 100)
                                + " to "
                                + (clusterOptions.getMaxFrequencyThreshold() * 100) + " % ");
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.option.frequency.range");
                    }
                }.setOutputMarkupId(true));
            }
        };

        components.setOutputMarkupId(true);
        return components;
    }

    private Component createClusterStatisticPanel() {
        FormSessionPanel components = new FormSessionPanel(ID_PANEL, Model.of("Statistic")) {
            @Override
            protected void addPanelButton(RepeatingView repeatingView) {

                @NotNull RoleAnalysisClusterType clusterTypeObject = getClusterTypeObject((PageBase) getPage(), operationResult,
                        getPageParameterChildOid()).asObjectable();

                RoleAnalysisClusterStatisticType clusterStatistic = clusterTypeObject.getClusterStatistic();

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Name")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterTypeObject.getName()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.option.name");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Creation time")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        XMLGregorianCalendar createTimestamp = clusterTypeObject.getMetadata().getCreateTimestamp();

                        if (createTimestamp != null) {
                            return Model.of(resolveDateAndTime(createTimestamp));

                        }
                        return Model.of("Unknown");
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.creation.time");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Last modify")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        XMLGregorianCalendar modifyTimestamp = clusterTypeObject.getMetadata().getModifyTimestamp();

                        if (modifyTimestamp != null) {
                            return Model.of(resolveDateAndTime(modifyTimestamp));

                        }
                        return Model.of(resolveDateAndTime(clusterTypeObject.getMetadata().getCreateTimestamp()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.modify.time");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Members count")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterStatistic.getMemberCount()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.member.count");
                    }
                }.setOutputMarkupId(true));
                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Properties count")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(clusterStatistic.getPropertiesCount().toString());
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.properties.count");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Properties range")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(clusterStatistic.getPropertiesMinOccupancy() + " - " + clusterStatistic.getPropertiesMaxOccupancy());
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.properties.range");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Properties mean")) {
                    @Override
                    protected IModel<String> getValueModel() {

                        String mean = String.format("%.3f", clusterStatistic.getPropertiesMean());
                        return Model.of(String.valueOf(mean));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.properties.mean");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Properties density")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        String density = String.format("%.3f", clusterStatistic.getPropertiesDensity());
                        return Model.of(density + " (%)");
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.members.density");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Risk level")) {
                    @Override
                    protected IModel<String> getValueModel() {

                        if (clusterStatistic.getRiskLevel() == null) {
                            return Model.of(" ");

                        }
                        return Model.of(String.valueOf(clusterStatistic.getRiskLevel()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.cluster.statistic.risk.level");
                    }
                }.setOutputMarkupId(true));

            }
        };

        components.setOutputMarkupId(true);
        return components;
    }

    private Component getPanel() {
        return get(createComponentPath(ID_CONTAINER, ID_PANEL));
    }

    private Panel initSummaryPanel() {
        PrismObject<RoleAnalysisClusterType> clusterTypeObject = getClusterTypeObject((PageBase) getPage(), operationResult,
                getPageParameterChildOid());
        return new ClusterSummaryPanel("summary", Model.of(clusterTypeObject.asObjectable()), null);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMiningOperation.title");
    }

}

