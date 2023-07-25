/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@PageDescriptor(urls = {
        @Url(mountUrl = "/attributeVerification", matchUrlForSecurity = "/attributeVerification")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION)
public class PageAttributeVerification extends PageAbstractAttributeVerification<AttributeVerificationAuthenticationModuleType> {
    private static final long serialVersionUID = 1L;
    private LoadableDetachableModel<UserType> userModel;

    public PageAttributeVerification() {
    }

    @Override
    protected void initModels() {
        super.initModels();
        userModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected UserType load() {
                MidPointPrincipal principal = AuthUtil.getPrincipalUser();
                return principal != null ? (UserType) principal.getFocus() : null;
            }
        };
    }

    @Override
    protected List<VerificationAttributeDto> loadAttrbuteVerificationDtoList() {
        AttributeVerificationAuthenticationModuleType module = getAutheticationModuleConfiguration();
                List<ItemPathType> moduleAttributes = module.getPath();
        return moduleAttributes.stream()
                .map(attr -> new VerificationAttributeDto(attr))
                .collect(Collectors.toList());
    }

    private AttributeVerificationAuthenticationModuleType getModuleByIdentifier(String moduleIdentifier) {
        if (StringUtils.isEmpty(moduleIdentifier)) {
            return null;
        }
        UserType user = userModel.getObject();
        if (user == null) {
            getSession().error(getString("User not found"));
            throw new RestartResponseException(PageError.class);
        }
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(user.asPrismObject());
        if (securityPolicy == null || securityPolicy.getAuthentication() == null) {
            getSession().error(getString("Security policy not found"));
            throw new RestartResponseException(PageError.class);
        }
        return securityPolicy.getAuthentication().getModules().getAttributeVerification()
                .stream()
                .filter(m -> moduleIdentifier.equals(m.getIdentifier()) || moduleIdentifier.equals(m.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected ObjectQuery createStaticFormQuery() {
        String username = "";
        return getPrismContext().queryFor(UserType.class).item(UserType.F_NAME)
                .eqPoly(username).matchingNorm().build();
    }

    @Override
    protected DynamicFormPanel<UserType> getDynamicForm() {
        return null;
    }

    @Override
    protected String getModuleTypeName() {
        return AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION;
    }

    @Override
    protected List<AttributeVerificationAuthenticationModuleType> getAuthetcationModules(AuthenticationModulesType modules) {
        return modules.getAttributeVerification();
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageAttributeVerification.attributeVerificationLabel");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PageAttributeVerification.description");
    }

}
