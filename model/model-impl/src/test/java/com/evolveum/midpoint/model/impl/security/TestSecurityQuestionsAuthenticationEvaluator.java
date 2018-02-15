package com.evolveum.midpoint.model.impl.security;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestSecurityQuestionsAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<Map<String, String>, SecurityQuestionsAuthenticationContext, AuthenticationEvaluator<SecurityQuestionsAuthenticationContext>>{

	private static final String SECURITY_QUESTION_ID = "http://midpoint.evolveum.com/xml/ns/public/security/question-2#q001";
	private static final String SECURITY_QUESTION_GOOD_ANSWER_JACK = "Some generic answer";
	private static final String SECURITY_QUESTION_BAD_ANSWER ="This isn't correct answer!";
	private static final String SECURITY_QUESTION_GOOD_ANSWER_GUYBRUSH = "Some some generic answer";

	@Autowired(required=true)
	private AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> securityQuestionsAuthenticationEvaluator;

	@Override
	public AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> getAuthenticationEvaluator() {
		return securityQuestionsAuthenticationEvaluator;
	}

	@Override
	public SecurityQuestionsAuthenticationContext getAuthenticationContext(String username,
			Map<String, String> value) {
		return new SecurityQuestionsAuthenticationContext(username, value);
	}

	@Override
	public Map<String, String> getGoodPasswordJack() {
		return createMap(SECURITY_QUESTION_ID, SECURITY_QUESTION_GOOD_ANSWER_JACK);
	}

	@Override
	public Map<String, String> getBadPasswordJack() {
		return createMap(SECURITY_QUESTION_ID, SECURITY_QUESTION_BAD_ANSWER);
	}

	@Override
	public Map<String, String> getGoodPasswordGuybrush() {
		return createMap(SECURITY_QUESTION_ID, SECURITY_QUESTION_GOOD_ANSWER_GUYBRUSH);
	}

	@Override
	public Map<String, String> getBadPasswordGuybrush() {
		return createMap(SECURITY_QUESTION_ID, SECURITY_QUESTION_BAD_ANSWER);
	}

	@Override
	public Map<String, String> get103EmptyPasswordJack() {
		return new HashMap<>();
	}

	@Override
	public AbstractCredentialType getCredentialUsedForAuthentication(UserType user) {
		return user.getCredentials().getSecurityQuestions();
	}

	@Override
	public QName getCredentialType() {
		return CredentialsType.F_SECURITY_QUESTIONS;
	}

	private SecurityQuestionAnswerType getSecurityQuestionAnswer(){
		SecurityQuestionAnswerType questionAnswer = new SecurityQuestionAnswerType();
		questionAnswer.setQuestionIdentifier(SECURITY_QUESTION_ID);
		ProtectedStringType protectedString = new ProtectedStringType();
		protectedString.setClearValue(SECURITY_QUESTION_GOOD_ANSWER_GUYBRUSH);
		questionAnswer.setQuestionAnswer(protectedString);
		return questionAnswer;
	}

	@Override
	public void modifyUserCredential(Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyObjectReplaceContainer(UserType.class, USER_GUYBRUSH_OID, SchemaConstants.PATH_SECURITY_QUESTIONS_QUESTION_ANSWER, task, result, getSecurityQuestionAnswer());

	}

	private Map<String, String> createMap(String id, String value) {
		Map<String, String> questionAnswers = new HashMap<>();
		questionAnswers.put(id, value);
		return questionAnswers;
	}


}
