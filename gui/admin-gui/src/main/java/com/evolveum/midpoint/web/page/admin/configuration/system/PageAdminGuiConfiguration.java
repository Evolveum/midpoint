/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.system;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system/adminGuiConfig", matchUrlForSecurity = "/admin/config/system/adminGuiConfig"),
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageAdminGuiConfiguration extends PageAbstractSystemConfiguration {

    @Override
    protected List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();

        PrismContainerWrapperModel<SystemConfigurationType, AdminGuiConfigurationType> adminGuiModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION);

        tabs.add(new AbstractTab(createStringResource("User dashboard links details")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_USER_DASHBOARD_LINK, RichHyperlinkType.COMPLEX_TYPE);
            }
        });

        tabs.add(new AbstractTab(createStringResource("Object collection views")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
//                return new AdminGuiObjectCollectionViewsPanel(panelId,
//                        PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ItemPath.create(SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS, GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW)));
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS, GuiObjectListViewsType.COMPLEX_TYPE);
            }
        });

        tabs.add(new AbstractTab(createStringResource("Object details")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_OBJECT_DETAILS, GuiObjectDetailsSetType.COMPLEX_TYPE);
            }
        });


        tabs.add(new AbstractTab(createStringResource("Additional menu link")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_ADDITIONAL_MENU_LINK, RichHyperlinkType.COMPLEX_TYPE);
            }
        });

        tabs.add(new AbstractTab(createStringResource("Configurable user dashboard")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_CONFIGURABLE_USER_DASHBOARD, ConfigurableUserDashboardType.COMPLEX_TYPE);
            }
        });


        return tabs;
    }


}
