/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionDefinitionType;

public class PasswordQuestionsDto implements Serializable{

    private List<SecurityQuestionAnswerDTO> userQuestionAnswers;
    private List<SecurityQuestionAnswerDTO> actualQuestionAnswers = new ArrayList<>();
    private final String focusOid;

    private PrismContainer credentials;

    public PasswordQuestionsDto(String focusOid){
        this.focusOid = focusOid;
    }

    public String getFocusOid() {
        return focusOid;
    }

    public PrismContainer getCredentials() {
        return credentials;
    }

    public void setCredentials(PrismContainer credentials) {
        this.credentials = credentials;
    }

    public List<SecurityQuestionAnswerDTO> getUserQuestionAnswers() {
        return userQuestionAnswers;
    }

    public void setUserQuestionAnswers(List<SecurityQuestionAnswerDTO> userQuestionAnswers) {
        this.userQuestionAnswers = userQuestionAnswers;
    }

    public List<SecurityQuestionAnswerDTO> getActualQuestionAnswers() {
        return actualQuestionAnswers;
    }

    public void setActualQuestionAnswers(List<SecurityQuestionAnswerDTO> actualQuestionAnswers) {
        this.actualQuestionAnswers = actualQuestionAnswers;
    }
}
