/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.ClusterSummaryPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.getScaleScript;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisCluster", matchUrlForSecurity = "/admin/roleAnalysisCluster")
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

public class PageRoleAnalysisCluster extends AbstractPageObjectDetails<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
    }

    @Override
    public IModel<List<ContainerPanelConfigurationType>> getPanelConfigurations() {
        IModel<List<ContainerPanelConfigurationType>> panelConfigurations = super.getPanelConfigurations();

        UserAnalysisClusterStatistic clusterUserBasedStatistic = getObjectDetailsModels().getObjectType()
                .getClusterUserBasedStatistic();

        boolean isUserBased = clusterUserBasedStatistic != null;

        List<ContainerPanelConfigurationType> object = panelConfigurations.getObject();
        for (ContainerPanelConfigurationType containerPanelConfigurationType : object) {
            if (containerPanelConfigurationType.getIdentifier().equals("clusterStatistic")) {
                List<VirtualContainersSpecificationType> container = containerPanelConfigurationType.getContainer();

                for (VirtualContainersSpecificationType virtualContainersSpecificationType : container) {
                    if (!isUserBased) {
                        if (virtualContainersSpecificationType.getPath().getItemPath()
                                .equivalent(RoleAnalysisClusterType.F_CLUSTER_USER_BASED_STATISTIC)) {
                            containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                        }
                    } else {
                        if (virtualContainersSpecificationType.getPath().getItemPath()
                                .equivalent(RoleAnalysisClusterType.F_CLUSTER_ROLE_BASED_STATISTIC)) {
                            containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                        }
                    }

                }

            }
        }
        return panelConfigurations;
    }

    public PageRoleAnalysisCluster() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public Class<RoleAnalysisClusterType> getType() {
        return RoleAnalysisClusterType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisClusterType> summaryModel) {
        return new ClusterSummaryPanel(id, summaryModel, null);
    }


    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMiningOperation.title");
    }

}

