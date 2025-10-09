/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security.util;

import java.io.Serializable;

/**
 * @author skublik
 */

public class SecurityQuestionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String identifier;
    private String questionText;
    private String questionAnswer;

    public SecurityQuestionDto(String identifier){
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getQuestionAnswer() {
        return questionAnswer;
    }

    public void setQuestionAnswer(String questionAnswer) {
        this.questionAnswer = questionAnswer;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
}
