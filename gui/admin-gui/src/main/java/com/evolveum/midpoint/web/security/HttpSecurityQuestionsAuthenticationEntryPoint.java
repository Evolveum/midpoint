/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.NameOfModuleType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.filter.HttpSecurityQuestionsAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.MidpointAuthFilter;
import com.evolveum.midpoint.web.security.filter.SecurityQuestionsAuthenticationFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.cxf.common.util.Base64Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author skublik
 */
public class HttpSecurityQuestionsAuthenticationEntryPoint extends HttpAuthenticationEntryPoint {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAuthFilter.class);

    private static final String WWW_AUTHENTICATION_HEADER = "WWW-Authenticate";
    private static final String AUTHENTICATION_HEADER = "Authorization";
    private static final String DEFAULT_JSON = "{\"user\":\"username\"}";

    @Autowired
    private SecurityContextManager securityContextManager;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private ModelService model;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelInteractionService modelInteractionService;

    private JSONArray generateAnswer(PrismObject<UserType> user) {

        List<SecurityQuestionDefinitionType> questions = getQuestions(user);

        JSONArray answers = new JSONArray();
        if (questions == null) {
            return null;
        }
        for (SecurityQuestionDefinitionType question : questions) {
            if (Boolean.TRUE.equals(question.isEnabled())) {
                JSONObject json  = new JSONObject();
                json.put(SecurityQuestionsAuthenticationFilter.J_QID, question.getIdentifier());
                json.put(SecurityQuestionsAuthenticationFilter.J_QTXT, question.getQuestionText());
                answers.put(json);
            }
        }
        if (answers.length() == 0) {
            return null;
        }
        return answers;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException) throws IOException {

       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

       try {
           if (authentication instanceof MidpointAuthentication) {
               if (request.getHeader(AUTHENTICATION_HEADER) != null
                       && request.getHeader(AUTHENTICATION_HEADER).toLowerCase().startsWith(NameOfModuleType.SECURITY_QUESTIONS.getName().toLowerCase())) {
                   String header = request.getHeader(AUTHENTICATION_HEADER);
                   if (header.toLowerCase().equals(NameOfModuleType.SECURITY_QUESTIONS.getName().toLowerCase())) {
                       createSecurityQuestionAbortMessage(response, DEFAULT_JSON);
                   } else {
                       byte[] jsonByte = Base64Utility.decode(header.substring(NameOfModuleType.SECURITY_QUESTIONS.getName().length() + 1));
                       String json = new String(jsonByte);
                       JSONObject jsonObject =  new JSONObject(json);
                       if (jsonObject.keySet().size() == 1 && jsonObject.keySet().contains(HttpSecurityQuestionsAuthenticationFilter.J_USER)) {
                           String username = jsonObject.getString(HttpSecurityQuestionsAuthenticationFilter.J_USER);
                           SearchResultList<PrismObject<UserType>> users = searchUser(username);

                           if (users == null || users.size() != 1) {
                               super.commence(request, response, authException);
                               return;
                           }

                           PrismObject<UserType> user = users.get(0);
                           JSONArray answers = generateAnswer(user);

                           if (answers == null) {
                               super.commence(request, response, authException);
                               return;
                           }

                           jsonObject.putOpt(HttpSecurityQuestionsAuthenticationFilter.J_ANSWER, answers);
                           createSecurityQuestionAbortMessage(response, jsonObject.toString());

                       } else {
                           super.commence(request, response, authException);
                           return;
                       }
                   }
               } else {
                   super.commence(request, response, authException);
                   return;
               }
           }
       } catch (Exception e) {
           LOGGER.error(e.getMessage(), e);
           super.commence(request, response, authException);
           return;
       }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public static void createSecurityQuestionAbortMessage(HttpServletResponse request, String json){
        String value = NameOfModuleType.SECURITY_QUESTIONS.getName() + " " + Base64Utility.encode(json.getBytes());
        request.setHeader(WWW_AUTHENTICATION_HEADER, value);
    }

    private SearchResultList<PrismObject<UserType>> searchUser(String userName) {
        return securityContextManager.runPrivileged(new Producer<SearchResultList<PrismObject<UserType>>>() {
            @Override
            public SearchResultList<PrismObject<UserType>> run() {
                Task task = taskManager.createTaskInstance("Search user by name");
                OperationResult result = task.getResult();

                SearchResultList<PrismObject<UserType>> users;
                try {
                    users = model.searchObjects(UserType.class, ObjectQueryUtil.createNameQuery(userName, prismContext), null, task, result);
                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException
                        | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                    return null;
                }
                return users;

            }
        });

    }

    private List<SecurityQuestionDefinitionType> getQuestions(PrismObject<UserType> user) {
        return securityContextManager.runPrivileged(new Producer<List<SecurityQuestionDefinitionType>>() {

            @Override
            public List<SecurityQuestionDefinitionType> run() {
                Task task = taskManager.createTaskInstance("Search user by name");
                OperationResult result = task.getResult();
                SecurityPolicyType securityPolicyType;
                try {
                    SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("rest_sec_q_auth", "REST", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
                    securityPolicyType = modelInteractionService.getSecurityPolicy(user, task, result);
                } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
                    return null;
                }
                if (securityPolicyType.getCredentials() != null && securityPolicyType.getCredentials().getSecurityQuestions() != null){
                    return securityPolicyType.getCredentials().getSecurityQuestions().getQuestion();
                }
                return null;
            }
        });

    }
}
