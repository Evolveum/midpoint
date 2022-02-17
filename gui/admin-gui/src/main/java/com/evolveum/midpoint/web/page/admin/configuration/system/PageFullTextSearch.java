/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.system;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page.PageBaseSystemConfiguration;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system/fullTextSearch", matchUrlForSecurity = "/admin/config/system/fullTextSearch"),
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageFullTextSearch extends PageBaseSystemConfiguration {

//    @Override
//    protected List<ITab> createTabs() {
//        List<ITab> tabs = new ArrayList<>();
//        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.fullTextSearch.title")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                return createContainerPanel(panelId, getObjectModel(), SystemConfigurationType.F_FULL_TEXT_SEARCH, FullTextSearchConfigurationType.COMPLEX_TYPE);
//            }
//        });
//        return tabs;
//    }
}
