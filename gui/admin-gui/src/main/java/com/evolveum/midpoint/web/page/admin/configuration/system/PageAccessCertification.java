/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.system;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page.PageBaseSystemConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system/accessCertification", matchUrlForSecurity = "/admin/config/system/accessCertification"),
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageAccessCertification extends PageBaseSystemConfiguration {

    private static final long serialVersionUID = 1L;

    public PageAccessCertification() {
    }

    public PageAccessCertification(PageParameters parameters) {
        super(parameters);
    }

    public PageAccessCertification(PrismObject<SystemConfigurationType> object) {
        super(object);
    }

//    @Override
//    protected List<ITab> createTabs() {
//        List<ITab> tabs = new ArrayList<>();
//        tabs.add(new PanelTab(createStringResource("pageSystemConfiguration.accessCertification.title")) {
//
//            @Override
//            public WebMarkupContainer createPanel(String panelId) {
//                return createContainerPanel(panelId, getObjectModel(), SystemConfigurationType.F_ACCESS_CERTIFICATION, AccessCertificationConfigurationType.COMPLEX_TYPE);
//            }
//
//        });
//        return tabs;
//    }
}
