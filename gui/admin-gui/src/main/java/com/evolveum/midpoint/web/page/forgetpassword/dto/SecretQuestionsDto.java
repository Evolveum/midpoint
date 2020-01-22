/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.forgetpassword.dto;

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
