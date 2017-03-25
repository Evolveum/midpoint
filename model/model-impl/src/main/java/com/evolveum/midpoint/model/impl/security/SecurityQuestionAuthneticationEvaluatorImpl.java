package com.evolveum.midpoint.model.impl.security;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;

@Component("securityQuestionsAuthenticationEvaluator")
public class SecurityQuestionAuthneticationEvaluatorImpl extends AuthenticationEvaluatorImpl<SecurityQuestionsCredentialsType, SecurityQuestionsAuthenticationContext>{

	@Override
	protected void checkEnteredCredentials(ConnectionEnvironment connEnv,
			SecurityQuestionsAuthenticationContext authCtx) {
		if (MapUtils.isEmpty(authCtx.getQuestionAnswerMap())) {
			recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty password provided");
			throw new BadCredentialsException("web.security.provider.password.encoding");
		}
	}

	@Override
	protected boolean suportsAuthzCheck() {
		return true;
	}

	@Override
	protected SecurityQuestionsCredentialsType getCredential(CredentialsType credentials) {
		return credentials.getSecurityQuestions();
	}

	@Override
	protected void validateCredentialNotNull(ConnectionEnvironment connEnv, MidPointPrincipal principal,
			SecurityQuestionsCredentialsType credential) {
		List<SecurityQuestionAnswerType> securityQuestionsAnswers = credential.getQuestionAnswer();
		
		if (securityQuestionsAnswers == null || securityQuestionsAnswers.isEmpty()) {
			recordAuthenticationFailure(principal, connEnv, "no stored security questions");
			throw new AuthenticationCredentialsNotFoundException("web.security.provider.password.bad");
		}
		
	}

	@Override
	protected boolean passwordMatches(ConnectionEnvironment connEnv, MidPointPrincipal principal,
			SecurityQuestionsCredentialsType passwordType, SecurityQuestionsAuthenticationContext authCtx) {
		
		SecurityQuestionsCredentialsPolicyType policy = authCtx.getPolicy();
		Integer iNumberOfQuestions = policy.getQuestionNumber();
		int numberOfQuestions = 0;
		if (iNumberOfQuestions != null){
			numberOfQuestions = iNumberOfQuestions.intValue();
		}
		
		Map<String, String> enteredQuestionsAnswers = authCtx.getQuestionAnswerMap();
		if (numberOfQuestions > enteredQuestionsAnswers.size()){
			return false;
		}		
		
		List<SecurityQuestionAnswerType> quetionsAnswers = passwordType.getQuestionAnswer();
		int matched = 0;
		for (SecurityQuestionAnswerType questionAnswer : quetionsAnswers){
			String enteredAnswer = enteredQuestionsAnswers.get(questionAnswer.getQuestionIdentifier());
			if (StringUtils.isNotBlank(enteredAnswer)) {
				if (decryptAndMatch(connEnv, principal, questionAnswer.getQuestionAnswer(), enteredAnswer)) {
					matched++;
				}
			}
		}
		
		return matched > 0 && matched >= numberOfQuestions;
		
	}

	@Override
	protected CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy,
			SecurityQuestionsAuthenticationContext authnCtx) throws SchemaException {
		SecurityQuestionsCredentialsPolicyType policy = authnCtx.getPolicy();
		if (policy == null){
			policy = SecurityUtil.getEffectiveSecurityQuestionsCredentialsPolicy(securityPolicy);
		}
		authnCtx.setPolicy(policy);
		return policy;
	}

}
