/*
 * Copyright (c) 2010-2022 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system/adminGuiConfig"),
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageAdminGuiConfiguration extends PageBaseSystemConfiguration {

    private static final long serialVersionUID = 1L;

    public PageAdminGuiConfiguration() {
    }

    public PageAdminGuiConfiguration(PageParameters parameters) {
        super(parameters);
    }

    public PageAdminGuiConfiguration(PrismObject<SystemConfigurationType> object) {
        super(object);
    }

    //    @Override
//    protected List<ITab> createTabs() {
//        List<ITab> tabs = new ArrayList<>();
//
//        PrismContainerWrapperModel<SystemConfigurationType, AdminGuiConfigurationType> adminGuiModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION);
//
//        tabs.add(new AbstractTab(createStringResource("User dashboard links details")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_USER_DASHBOARD_LINK, RichHyperlinkType.COMPLEX_TYPE);
//            }
//        });
//
//        tabs.add(new AbstractTab(createStringResource("Object collection views")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
////                return new AdminGuiObjectCollectionViewsPanel(panelId,
////                        PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ItemPath.create(SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS, GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW)));
//                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS, GuiObjectListViewsType.COMPLEX_TYPE);
//            }
//        });
//
//        tabs.add(new AbstractTab(createStringResource("Object details")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_OBJECT_DETAILS, GuiObjectDetailsSetType.COMPLEX_TYPE);
//            }
//        });
//
//
//        tabs.add(new AbstractTab(createStringResource("Additional menu link")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_ADDITIONAL_MENU_LINK, RichHyperlinkType.COMPLEX_TYPE);
//            }
//        });
//
//        tabs.add(new AbstractTab(createStringResource("Configurable user dashboard")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                return createContainerPanel(panelId, adminGuiModel, AdminGuiConfigurationType.F_CONFIGURABLE_USER_DASHBOARD, ConfigurableUserDashboardType.COMPLEX_TYPE);
//            }
//        });
//
//
//        return tabs;
//    }


}
