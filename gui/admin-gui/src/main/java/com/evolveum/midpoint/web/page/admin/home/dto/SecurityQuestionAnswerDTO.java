/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;

public class SecurityQuestionAnswerDTO implements Serializable {

    public static final String F_PASSWORD_QUESTION_IDENTIFIER = "passwQuestionIdentifier";

    public static final String F_PASSWORD_QUESTION_ANSWER = "passwAnswer";


    public static final String F_PASSWORD_QUESTION_ITSELF = "questionItself";



    private String passwQuestionIdentifier;
    private String passwAnswer;
    private String questionItself;

    public SecurityQuestionAnswerDTO(String passwQuestion, String passAnswer){
        this.passwQuestionIdentifier = passwQuestion;
        this.passwAnswer = passAnswer;
    }
    public SecurityQuestionAnswerDTO(String passwQuestion, String passAnswer, String questionitself){
        this.passwQuestionIdentifier = passwQuestion;
        this.passwAnswer = passAnswer;
        this.questionItself=questionitself;
    }

    public String getPwdQuestionIdentifier() {
        return passwQuestionIdentifier;
    }
    public void setPwdQuestionIdentifier(String passwQuestionIdentifier) {
        this.passwQuestionIdentifier = passwQuestionIdentifier;
    }
    public String getPwdAnswer() {
        return passwAnswer;
    }
    public void setPwdAnswer(String pwdAnswer) {
        this.passwAnswer = pwdAnswer;
    }

    public String getPwdQuestion() {
        return questionItself;
    }
    public void setPwdQuestion(String questionItself) {
        this.questionItself = questionItself;
    }

}
