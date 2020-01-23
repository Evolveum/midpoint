/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;

public class SecurityQuestionsAuthenticationContext extends AbstractAuthenticationContext{

    private Map<String, String> questionAnswerMap;
    private SecurityQuestionsCredentialsPolicyType policy;


    public SecurityQuestionsAuthenticationContext(String username, Map<String, String> questionAnswerMap) {
        this(username, questionAnswerMap, null);
    }

    public SecurityQuestionsAuthenticationContext(String username, Map<String, String> questionAnswerMap, List<ObjectReferenceType> requireAssignment) {
        super(username, requireAssignment);
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
