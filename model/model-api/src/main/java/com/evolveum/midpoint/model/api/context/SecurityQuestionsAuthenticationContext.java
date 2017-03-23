package com.evolveum.midpoint.model.api.context;

import java.util.Map;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;

public class SecurityQuestionsAuthenticationContext extends AbstractAuthenticationContext{

	private Map<String, String> questionAnswerMap;
	private SecurityQuestionsCredentialsPolicyType policy;
	
	
	public SecurityQuestionsAuthenticationContext(String username, Map<String, String> questionAnswerMap) {
		super(username);
		this.questionAnswerMap = questionAnswerMap;
	}
	
	
	public Map<String, String> getQuestionAnswerMap() {
		return questionAnswerMap;
	}

	@Override
	public Object getEnteredCredential() {
		return getQuestionAnswerMap();
	}
	
	public SecurityQuestionsCredentialsPolicyType getPolicy() {
		return policy;
	}
	
	public void setPolicy(SecurityQuestionsCredentialsPolicyType policy) {
		this.policy = policy;
	}
	
	
}
