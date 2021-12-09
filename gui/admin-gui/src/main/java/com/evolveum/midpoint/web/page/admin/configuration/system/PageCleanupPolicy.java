/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.system;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.PageDescriptor;
import com.evolveum.midpoint.authentication.api.Url;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system/cleanupPolicy", matchUrlForSecurity = "/admin/config/system/cleanupPolicy"),
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageCleanupPolicy extends PageAbstractSystemConfiguration {

    @Override
    protected List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(createStringResource("pageSystemConfiguration.cleanupPolicy.title")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createContainerPanel(panelId, getObjectModel(), SystemConfigurationType.F_CLEANUP_POLICY, CleanupPoliciesType.COMPLEX_TYPE);
            }

        });
        return tabs;
    }
}
