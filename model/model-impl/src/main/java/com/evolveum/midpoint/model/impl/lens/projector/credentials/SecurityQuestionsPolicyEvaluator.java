/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class SecurityQuestionsPolicyEvaluator<F extends FocusType> extends
        CredentialPolicyEvaluator<SecurityQuestionsCredentialsType, SecurityQuestionsCredentialsPolicyType, F> {

    private SecurityQuestionsPolicyEvaluator(Builder<F> builder) {
        super(builder);
    }

    @Override
    protected ItemPath getCredentialsContainerPath() {
        return SchemaConstants.PATH_SECURITY_QUESTIONS;
    }

    @Override
    protected String getCredentialHumanReadableName() {
        return "security questions";
    }

    @Override
    protected String getCredentialHumanReadableKey() {
        return "securityQuestions";
    }

    @Override
    protected SecurityQuestionsCredentialsPolicyType determineEffectiveCredentialPolicy() {
        return SecurityUtil.getEffectiveSecurityQuestionsCredentialsPolicy(getSecurityPolicy());
    }

    @Override
    protected void validateCredentialContainerValues(PrismContainerValue<SecurityQuestionsCredentialsType> cVal)
            throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        List<SecurityQuestionAnswerType> questionAnswers = cVal.asContainerable().getQuestionAnswer();
        for (SecurityQuestionAnswerType questionAnswer : questionAnswers) {
            ProtectedStringType answer = questionAnswer.getQuestionAnswer();
            validateProtectedStringValue(answer);
        }
    }

    public static class Builder<F extends FocusType> extends CredentialPolicyEvaluator.Builder<F> {
        public SecurityQuestionsPolicyEvaluator<F> build() {
            return new SecurityQuestionsPolicyEvaluator<>(this);
        }
    }
}
