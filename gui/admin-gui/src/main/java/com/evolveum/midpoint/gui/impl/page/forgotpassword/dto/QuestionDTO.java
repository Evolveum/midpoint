/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.forgotpassword.dto;

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
