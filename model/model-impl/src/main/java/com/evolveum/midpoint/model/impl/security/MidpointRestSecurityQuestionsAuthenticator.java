package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.SecurityQuestionsAuthenticationContext;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Component
public class MidpointRestSecurityQuestionsAuthenticator extends MidpointRestAuthenticator<SecurityQuestionsAuthenticationContext> {

	@Autowired(required = true)
	private AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> securityQuestionsAuthenticationEvaluator;
	
	@Override
	protected AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> getAuthenticationEvaluator() {
		return securityQuestionsAuthenticationEvaluator;
	}

	@Override
	protected SecurityQuestionsAuthenticationContext createAuthenticationContext(AuthorizationPolicy policy) throws IOException {
		JsonFactory f = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(f);
		JsonNode node;
			node = mapper.readTree(policy.getAuthorization());
			String userName = node.findPath("user").asText();
			policy.setUserName(userName);
			ArrayNode answers = (ArrayNode) node.findPath("answer");
			Iterator<JsonNode> answersList = answers.elements();
			Map<String, String> questionAnswers = new HashMap<>();
			while (answersList.hasNext()) {
				JsonNode answer = answersList.next();
				String questionId = answer.findPath("qid").asText();
				String questionAnswer = answer.findPath("qans").asText();
				questionAnswers.put(questionId, questionAnswer);
			}
			return new SecurityQuestionsAuthenticationContext(userName, questionAnswers);
		
	}

}
