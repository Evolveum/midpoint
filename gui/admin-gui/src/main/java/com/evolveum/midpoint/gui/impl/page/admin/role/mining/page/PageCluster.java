/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.LineFieldPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.FormSessionPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.OperationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.ClustersPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.SessionSummaryPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionStatisticType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/clusterTable", matchUrlForSecurity = "/admin/clusterTable")
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

public class PageCluster extends PageAdmin {

    private static final String ID_PANEL = "panel";

    private static final String ID_CONTAINER = "container";

    public static final String PARAMETER_PARENT_OID = "id";
    public static final String PARAMETER_MODE = "mode";

    OperationResult operationResult = new OperationResult("ExecuteOperation");

    String getPageParameterParentOid() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_PARENT_OID).toString();
    }

    String getPageParameterMode() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_MODE).toString();
    }

    public PageCluster() {
        super();
    }

    WebMarkupContainer panel;
    WebMarkupContainer webMarkupContainer;

    boolean sessionCluster = true;
    boolean sessionOption = false;
    boolean sessionStatistic = false;

    AjaxIconButton clusters;
    AjaxIconButton options;
    AjaxIconButton statistic;

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(initSummaryPanel());

        webMarkupContainer = new WebMarkupContainer(ID_CONTAINER);
        webMarkupContainer.setOutputMarkupId(true);
        add(webMarkupContainer);

        OperationPanel operationPanel = new OperationPanel("operation_panel") {

            @Override
            protected void addPanelButton(RepeatingView repeatingView) {

                statistic = new AjaxIconButton(repeatingView.newChildId(), Model.of("fa fa-bar-chart"),
                        getPageBase().createStringResource("role.mining.session.statistic.panel")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        if (!sessionStatistic) {
                            sessionStatistic = true;
                            sessionCluster = false;
                            sessionOption = false;

                            ajaxRequestTarget.add(getPanel().replaceWith(createSessionStatisticPanel().setOutputMarkupId(true)));
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
                        getPageBase().createStringResource("role.mining.session.clusters.panel")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        if (!sessionCluster) {
                            sessionStatistic = false;
                            sessionCluster = true;
                            sessionOption = false;

                            ajaxRequestTarget.add(getPanel().replaceWith(new ClustersPanel(ID_PANEL, getPageParameterParentOid(),
                                    getPageParameterMode()).setOutputMarkupId(true)));
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
                        getPageBase().createStringResource("role.mining.session.option.panel")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                        if (!sessionOption) {
                            sessionStatistic = false;
                            sessionCluster = false;
                            sessionOption = true;

                            ajaxRequestTarget.add(getPanel().replaceWith(createSessionOptionsPanel()));
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
                PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject((PageBase) getPage(), getPageParameterParentOid());
                deleteSingleRoleAnalysisSession(operationResult, sessionTypeObject.asObjectable(),
                        (PageBase) getPage());
                getPageBase().redirectBack();
            }
        };
        operationPanel.setOutputMarkupId(true);
        add(operationPanel);

        panel = new ClustersPanel(ID_PANEL, getPageParameterParentOid(), getPageParameterMode());
        panel.setOutputMarkupId(true);
        webMarkupContainer.add(panel);
    }

    private Component createSessionOptionsPanel() {
        FormSessionPanel components = new FormSessionPanel(ID_PANEL, Model.of("Options")) {
            @Override
            protected void addPanelButton(RepeatingView repeatingView) {

                @NotNull RoleAnalysisSessionType sessionTypeObject = getSessionTypeObject((PageBase) getPage(),
                        getPageParameterParentOid()).asObjectable();

                RoleAnalysisSessionOptionType clusterOptions = sessionTypeObject.getClusterOptions();

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Name")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(sessionTypeObject.getName()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.option.name");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Similarity")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterOptions.getSimilarityThreshold()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.option.similarity");
                    }
                }.setOutputMarkupId(true));
                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Process mode")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(clusterOptions.getProcessMode().value().toUpperCase());
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.option.process.mode");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Min unique members")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterOptions.getMinUniqueGroupCount()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.option.min.unique.members");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Min overlap")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(clusterOptions.getMinPropertiesOverlap()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.option.min.overlap");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Properties range")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of("from "
                                + clusterOptions.getMinPropertiesCount()
                                + " to "
                                + clusterOptions.getMaxPropertiesCount());
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.option.properties.range");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Axiom query")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(clusterOptions.getFilter());
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.option.axiom.query");
                    }
                }.setOutputMarkupId(true));

            }
        };

        components.setOutputMarkupId(true);
        return components;
    }

    private Component createSessionStatisticPanel() {
        FormSessionPanel components = new FormSessionPanel(ID_PANEL, Model.of("Statistic")) {
            @Override
            protected void addPanelButton(RepeatingView repeatingView) {

                @NotNull RoleAnalysisSessionType sessionTypeObject = getSessionTypeObject((PageBase) getPage(),
                        getPageParameterParentOid()).asObjectable();

                RoleAnalysisSessionStatisticType sessionStatistics = sessionTypeObject.getSessionStatistic();

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Name")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(sessionTypeObject.getName()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.option.name");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Creation time")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        XMLGregorianCalendar createTimestamp = sessionTypeObject.getMetadata().getCreateTimestamp();

                        if (createTimestamp != null) {
                            return Model.of(resolveDateAndTime(createTimestamp));

                        }
                        return Model.of("Unknown");
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.statistic.creation.time");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Last modify")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        XMLGregorianCalendar modifyTimestamp = sessionTypeObject.getMetadata().getModifyTimestamp();

                        if (modifyTimestamp != null) {
                            return Model.of(resolveDateAndTime(modifyTimestamp));

                        }
                        return Model.of(resolveDateAndTime(sessionTypeObject.getMetadata().getCreateTimestamp()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.statistic.modify.time");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Processed objects")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        return Model.of(String.valueOf(sessionStatistics.getProcessedObjectCount()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.statistic.proccesed.objects");
                    }
                }.setOutputMarkupId(true));
                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Clusters density")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        String density = String.format("%.3f", sessionStatistics.getMeanDensity());
                        return Model.of(density + " (%)");
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.statistic.cluster.density");
                    }
                }.setOutputMarkupId(true));

                repeatingView.add(new LineFieldPanel(repeatingView.newChildId(), Model.of("Risk level")) {
                    @Override
                    protected IModel<String> getValueModel() {
                        if (sessionStatistics.getRiskLevel() == null) {
                            return Model.of("");
                        }
                        return Model.of(String.valueOf(sessionStatistics.getRiskLevel()));
                    }

                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleMining.session.statistic.risk.level");
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
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject((PageBase) getPage(), getPageParameterParentOid());
        return new SessionSummaryPanel("summary", Model.of(sessionTypeObject.asObjectable()), null);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("RoleMining.page.cluster.title");
    }
}

