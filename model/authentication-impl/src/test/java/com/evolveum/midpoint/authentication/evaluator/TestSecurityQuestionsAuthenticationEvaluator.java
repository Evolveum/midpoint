/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.evaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.evaluator.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class TestSecurityQuestionsAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<Map<String, String>, SecurityQuestionsAuthenticationContext, AuthenticationEvaluator<SecurityQuestionsAuthenticationContext, UsernamePasswordAuthenticationToken>>{

    private static final String SECURITY_QUESTION_ID = "http://midpoint.evolveum.com/xml/ns/public/security/question-2#q001";
    private static final String SECURITY_QUESTION_GOOD_ANSWER_JACK = "Some generic answer";
    private static final String SECURITY_QUESTION_BAD_ANSWER ="This isn't correct answer!";
    private static final String SECURITY_QUESTION_GOOD_ANSWER_GUYBRUSH = "Some some generic answer";

    @Autowired
    private AuthenticationEvaluator<SecurityQuestionsAuthenticationContext, UsernamePasswordAuthenticationToken> securityQuestionsAuthenticationEvaluator;

    @Override
    public AuthenticationEvaluator<SecurityQuestionsAuthenticationContext, UsernamePasswordAuthenticationToken> getAuthenticationEvaluator() {
        return securityQuestionsAuthenticationEvaluator;
    }

    @Override
    public SecurityQuestionsAuthenticationContext getAuthenticationContext(
            String username, Map<String, String> value, List<ObjectReferenceType> requiredAssignments) {
        return new SecurityQuestionsAuthenticationContext(username, UserType.class, value, requiredAssignments, null);
    }

    @Override
    public Map<String, String> getGoodPasswordJack() {
        return createMap(SECURITY_QUESTION_GOOD_ANSWER_JACK);
    }

    @Override
    public Map<String, String> getBadPasswordJack() {
        return createMap(SECURITY_QUESTION_BAD_ANSWER);
    }

    @Override
    public Map<String, String> getGoodPasswordGuybrush() {
        return createMap(SECURITY_QUESTION_GOOD_ANSWER_GUYBRUSH);
    }

    @Override
    public Map<String, String> getBadPasswordGuybrush() {
        return createMap(SECURITY_QUESTION_BAD_ANSWER);
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
    public String getModuleIdentifier() {
        return "SecQ";
    }

    @Override
    public String getSequenceIdentifier() {
        return "default-security-questions";
    }

    @Override
    public ItemName getCredentialType() {
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

    private Map<String, String> createMap(String value) {
        Map<String, String> questionAnswers = new HashMap<>();
        questionAnswers.put(TestSecurityQuestionsAuthenticationEvaluator.SECURITY_QUESTION_ID, value);
        return questionAnswers;
    }

    @Override
    public String getEmptyPasswordExceptionMessageKey() {
        return "web.security.provider.invalid.credentials";
    }
}
