package com.evolveum.midpoint.web.page.forgetpassword.dto;

import java.io.Serializable;
import java.util.List;

public class SecretQuestionsDto implements Serializable {


	    public static final String F_ACCOUNTS = "accounts";
	    public static final String F_ = "password";

	    private List<QuestionDTO> secretQuestions;




	    public  List<QuestionDTO> getSecretQuestions() {
	
	           return secretQuestions;
	    }

	    public void addToList(QuestionDTO question){
	    	secretQuestions.add(question);
	    }


}
