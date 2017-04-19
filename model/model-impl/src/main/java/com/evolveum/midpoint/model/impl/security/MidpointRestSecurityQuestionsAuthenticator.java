package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

@Component
public class MidpointRestSecurityQuestionsAuthenticator extends MidpointRestAuthenticator<SecurityQuestionsAuthenticationContext> {

	protected static final String USER_CHALLENGE = "\"user\" : \"username\"";
	protected static final String USER_QUESTION_ANSWER_CHALLENGE = ", \"answer\" :";
	protected static final String QUESTION = "{\"qid\" : \"$QID\", \"qtxt\" : \"$QTXT\"}";
	
	private static final String Q_ID = "$QID";
	private static final String Q_TXT = "$QTXT";
	
	
	@Autowired
	private PrismContext prismContext;
	
	@Autowired
	private ModelInteractionService modelInteractionService;
	
	@Autowired(required = true)
	private AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> securityQuestionsAuthenticationEvaluator;
	
	@Override
	protected AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> getAuthenticationEvaluator() {
		return securityQuestionsAuthenticationEvaluator;
	}

	@Override
	protected SecurityQuestionsAuthenticationContext createAuthenticationContext(AuthorizationPolicy policy, ContainerRequestContext requestCtx) {
		JsonFactory f = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(f);
		JsonNode node = null;
			try {
				node = mapper.readTree(policy.getAuthorization());
			} catch (IOException e) {
				RestServiceUtil.createSecurityQuestionAbortMessage(requestCtx, "{" + USER_CHALLENGE + "}");
				return null;
			}
			JsonNode userNameNode = node.findPath("user");
			if (userNameNode instanceof MissingNode) {
				RestServiceUtil.createSecurityQuestionAbortMessage(requestCtx, "{" + USER_CHALLENGE + "}");
				return null;
			}
			String userName = userNameNode.asText();
			policy.setUserName(userName);
			JsonNode answerNode = node.findPath("answer");
			
			if (answerNode instanceof MissingNode) {
				SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("restapi", "REST", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
				SearchResultList<PrismObject<UserType>> users = null;
				try {
					users = searchUser(userName);
				} finally {
					SecurityContextHolder.getContext().setAuthentication(null);
				}
				
				if (users.size() != 1) {
					requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Security question authentication failed. Incorrect username and/or password").build());
					return null;
				}
				
				PrismObject<UserType> user = users.get(0);
				PrismContainer<SecurityQuestionAnswerType> questionAnswerContainer = user.findContainer(SchemaConstants.PATH_SECURITY_QUESTIONS_QUESTION_ANSWER);
				if (questionAnswerContainer == null || questionAnswerContainer.isEmpty()){
					requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Security question authentication failed. Incorrect username and/or password").build());
					return null;
				}
				
				String questionChallenge = "";
				List<SecurityQuestionDefinitionType> questions = null;
				try {
					SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("restapi", "REST", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
					questions = getQuestions(user);
				} finally {
					SecurityContextHolder.getContext().setAuthentication(null);
				}
				Collection<SecurityQuestionAnswerType> questionAnswers = questionAnswerContainer.getRealValues();
				Iterator<SecurityQuestionAnswerType> questionAnswerIterator = questionAnswers.iterator();
				while (questionAnswerIterator.hasNext()) {
					SecurityQuestionAnswerType questionAnswer = questionAnswerIterator.next();
					SecurityQuestionDefinitionType question = questions.stream().filter(q -> q.getIdentifier().equals(questionAnswer.getQuestionIdentifier())).findFirst().get();
					String challenge = QUESTION.replace(Q_ID, question.getIdentifier());
					questionChallenge += challenge.replace(Q_TXT, question.getQuestionText());
					if (questionAnswerIterator.hasNext()) {
						questionChallenge += ",";
					}
				}
				
				String userChallenge = USER_CHALLENGE.replace("username", userName);
				String challenge = "{" + userChallenge + ", \"answer\" : [" + questionChallenge + "]}";
				RestServiceUtil.createSecurityQuestionAbortMessage(requestCtx, challenge);
				return null;
				
			}
			ArrayNode answers = (ArrayNode) answerNode;
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
	
	private SearchResultList<PrismObject<UserType>> searchUser(String userName) {
		return getSecurityEnforcer().runPrivileged(new Producer<SearchResultList<PrismObject<UserType>>>() {
			@Override
			public SearchResultList<PrismObject<UserType>> run() {
				Task task = getTaskManager().createTaskInstance("Search user by name");
				OperationResult result = task.getResult();
				
				SearchResultList<PrismObject<UserType>> users;
				try {
					users = getModel().searchObjects(UserType.class, ObjectQueryUtil.createNameQuery(userName, prismContext), null, task, result);
				} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
						| CommunicationException | ConfigurationException e) {
					return null;
				} finally {
					SecurityContextHolder.getContext().setAuthentication(null);
				}
				return users;
				
			}
		});
	 
	}
	
	private List<SecurityQuestionDefinitionType> getQuestions(PrismObject<UserType> user) {
		return getSecurityEnforcer().runPrivileged(new Producer<List<SecurityQuestionDefinitionType>>() {
			
			@Override
			public List<SecurityQuestionDefinitionType> run() {
				Task task = getTaskManager().createTaskInstance("Search user by name");
				OperationResult result = task.getResult();
				SecurityPolicyType securityPolicyType = null;
				try {
					SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("rest_sec_q_auth", "REST", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
					securityPolicyType = modelInteractionService.getSecurityPolicy(user, task, result);
				} catch (ObjectNotFoundException | SchemaException e) {
					return null;
				} finally {
					SecurityContextHolder.getContext().setAuthentication(null);
				}
				if (securityPolicyType.getCredentials() != null && securityPolicyType.getCredentials().getSecurityQuestions() != null){
					return securityPolicyType.getCredentials().getSecurityQuestions().getQuestion();
				}
				return null;
			}
		});
		
	}

}
