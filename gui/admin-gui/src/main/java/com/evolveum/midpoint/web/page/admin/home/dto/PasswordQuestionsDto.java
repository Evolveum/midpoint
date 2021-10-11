/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainer;

public class PasswordQuestionsDto implements Serializable{

    public static final String F_MY_QUESTIONS_ANSWERS = "questionAnswers";
    public static final String F_MY_QUESTIONS_ANSWER = "passwAnswer";
    public static final String F_MY_QUESTIONS_QUESTIONITSELF = "passwQuestion";

    private String passwQuestion;
    private String passwAnswer;

    private List<SecurityQuestionAnswerDTO> questionAnswers;

    private PrismContainer credentials;

    /*public PasswordQuestionsDto(String passwQuestion){
        this.passwQuestion = passwQuestion;
    }*/
    public PasswordQuestionsDto(){

    }

    public PasswordQuestionsDto(String passwQuestion, String passAnswer){
        this.passwQuestion = passwQuestion;
        this.passwAnswer = passAnswer;
    }

    public String getPwdQuestion() {
        return passwQuestion;
    }
    public void setPwdQuestion(String pwdQuestion) {
        this.passwQuestion = pwdQuestion;
    }
    public String getPwdAnswer() {
        return passwAnswer;
    }
    public void setPwdAnswer(String pwdAnswer) {
        this.passwAnswer = pwdAnswer;
    }

       public PrismContainer getCredentials() {
            return credentials;
        }

        public void setCredentials(PrismContainer credentials) {
            this.credentials = credentials;
        }

        public List<SecurityQuestionAnswerDTO> getSecurityAnswers() {
            return questionAnswers;
        }

        public void setSecurityAnswers(List<SecurityQuestionAnswerDTO> securityAnswers) {
            this.questionAnswers = securityAnswers;
        }



}
