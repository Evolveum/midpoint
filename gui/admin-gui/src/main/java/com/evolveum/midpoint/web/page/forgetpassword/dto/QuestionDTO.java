/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.forgetpassword.dto;

import java.io.Serializable;

public class QuestionDTO implements Serializable {

    private String questionItself;
    private String answerOftheUser;
    public QuestionDTO(String question,String answer){
        setAnswerOftheUser(answer);
        setQuestionItself(question);

    }
    private String getQuestionItself() {
        return questionItself;
    }
    private void setQuestionItself(String questionItself) {
        this.questionItself = questionItself;
    }
    private String getAnswerOftheUser() {
        return answerOftheUser;
    }
    private void setAnswerOftheUser(String answerOftheUser) {
        this.answerOftheUser = answerOftheUser;
    }


}
