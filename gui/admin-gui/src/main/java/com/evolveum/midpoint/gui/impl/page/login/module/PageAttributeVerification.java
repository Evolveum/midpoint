/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.AttributeVerificationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.prism.path.ItemPath;

@PageDescriptor(urls = {
        @Url(mountUrl = "/attributeVerification", matchUrlForSecurity = "/attributeVerification")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION)
public class PageAttributeVerification extends PageAbstractAttributeVerification<AttributeVerificationModuleAuthentication> {
    @Serial private static final long serialVersionUID = 1L;

    public PageAttributeVerification() {
        super();
    }


    @Override
    protected List<VerificationAttributeDto> loadAttrbuteVerificationDtoList() {
        AttributeVerificationModuleAuthentication module = getAuthenticationModuleConfiguration();
        List<ItemPath> moduleAttributes = module.getPathsToVerify();
        return moduleAttributes.stream()
                .map(VerificationAttributeDto::new)
                .collect(Collectors.toList());
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
