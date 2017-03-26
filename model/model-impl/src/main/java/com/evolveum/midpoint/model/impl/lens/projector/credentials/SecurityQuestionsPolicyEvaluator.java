package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class SecurityQuestionsPolicyEvaluator extends CredentialPolicyEvaluator<SecurityQuestionsCredentialsType, SecurityQuestionsCredentialsPolicyType>{

	@Override
	protected ItemPath getCredentialsContainerPath() {
		return SchemaConstants.PATH_SECURITY_QUESTIONS;
	}

	@Override
	protected String getCredentialHumanReadableName() {
		return "security questions";
	}

	@Override
	protected SecurityQuestionsCredentialsPolicyType determineEffectiveCredentialPolicy()
			throws SchemaException {
		return SecurityUtil.getEffectiveSecurityQuestionsCredentialsPolicy(getSecurityPolicy());
	}

	@Override
	protected void validateCredentialContainerValues(
			PrismContainerValue<SecurityQuestionsCredentialsType> cVal) throws PolicyViolationException,
					SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		SecurityQuestionsCredentialsType securityQuestions = cVal.asContainerable();
		if (securityQuestions != null) {
			List<SecurityQuestionAnswerType> questionAnswers = securityQuestions.getQuestionAnswer();
			for (SecurityQuestionAnswerType questionAnswer : questionAnswers) {
				ProtectedStringType answer = questionAnswer.getQuestionAnswer();
				validateProtectedStringValue(answer);
			}
		}
	}
}
