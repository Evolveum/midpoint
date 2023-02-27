/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author skublik
 */

public class SecurityQuestionFormModuleAuthentication extends CredentialModuleAuthenticationImpl {

    public SecurityQuestionFormModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.SECURITY_QUESTIONS_FORM, sequenceModule);
    }

    public ModuleAuthenticationImpl clone() {
        SecurityQuestionFormModuleAuthentication module = new SecurityQuestionFormModuleAuthentication(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
        return module;
    }

    @Override
    public boolean applicable() {
        if (!canSkipWhenEmptyCredentials()) {
            return super.applicable();
        }
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return true;
        }

        if (!(principal instanceof MidPointPrincipal)) {
            return false;
        }

        FocusType focus = principal.getFocus();
        CredentialsType credentialsType = focus.getCredentials();
        if (credentialsType == null) {
            return false;
        }
        SecurityQuestionsCredentialsType secQ = credentialsType.getSecurityQuestions();
        if (secQ == null) {
            return false;
        }

        return CollectionUtils.isNotEmpty(secQ.getQuestionAnswer());
    }
}
