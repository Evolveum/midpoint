/*
 * Copyright (c) 2024-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.policy;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.policy.component.PolicySummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/policy", matchUrlForSecurity = "/admin/policy")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_POLICIES_ALL_URL,
                label = "PageAdminPolicies.auth.policiesAll.label",
                description = "PageAdminPolicies.auth.policiesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_POLICY_URL,
                label = "PagePolicy.auth.policy.label",
                description = "PagePolicy.auth.policy.description") })
public class PagePolicy extends PageAbstractRole<PolicyType, AbstractRoleDetailsModel<PolicyType>> {

    public PagePolicy() {
        super();
    }

    public PagePolicy(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PagePolicy(PrismObject<PolicyType> service) {
        super(service);
    }

    @Override
    public Class<PolicyType> getType() {
        return PolicyType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<PolicyType> summaryModel) {
        return new PolicySummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }
}
