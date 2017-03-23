package com.evolveum.midpoint.model.impl.security;

import java.util.Map;

import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;

public class SecurityQuestionsAuthenticationContext extends AbstractAuthenticationContext{

	Map<String, String> questionAnswerMap;
	String questionId;
	String questionAnswer;
	
	public SecurityQuestionsAuthenticationContext(String username, String questionId, String questionAnswer) {
		super(username);
		this.questionId = questionId;
		this.questionAnswer = questionAnswer;
	}
	
	public String getQuestionId() {
		return questionId;
	}
	
	public String getQuestionAnswer() {
		return questionAnswer;
	}
	
	public Map<String, String> getQuestionAnswerMap() {
		return questionAnswerMap;
	}

	@Override
	public Object getEnteredCredential() {
		return getQuestionAnswerMap();
	}
	
	
}
