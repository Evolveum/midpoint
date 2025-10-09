/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.forgotpassword.dto;

import java.io.Serializable;
import java.util.List;

public class SecretQuestionsDto implements Serializable {

        public static final String F_ACCOUNTS = "accounts";
        public static final String F_PASSWORD = "password";

        private List<QuestionDTO> secretQuestions;

        public  List<QuestionDTO> getSecretQuestions() {

               return secretQuestions;
        }

        public void addToList(QuestionDTO question){
            secretQuestions.add(question);
        }

}
