/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationConfigurationType;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InternalsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/system/smartIntegration"),
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                        label = "PageSystemConfiguration.auth.configSystemConfiguration.label",
                        description = "PageSystemConfiguration.auth.configSystemConfiguration.description")
        })
public class PageSmartIntegration extends PageBaseSystemConfiguration {

    private static final long serialVersionUID = 1L;

    public PageSmartIntegration() {
    }

    public PageSmartIntegration(PageParameters parameters) {
        super(parameters);
    }

    public PageSmartIntegration(PrismObject<SystemConfigurationType> object) {
        super(object);
    }

    @Override
    public Class<? extends Containerable> getDetailsType() {
        return SmartIntegrationConfigurationType.class;
    }
}
