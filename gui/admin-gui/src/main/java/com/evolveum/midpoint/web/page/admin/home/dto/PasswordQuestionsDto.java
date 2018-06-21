package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainer;

//import com.evolveum.midpoint.prism.PrismContainer;

/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


public class PasswordQuestionsDto implements Serializable{

	public static final String F_MY_QUESTIONS_ANSWERS="questionAnswers";
	public static final String F_MY_QUESTIONS_ANSWER="passwAnswer";
    public static final String F_MY_QUESTIONS__QUESTIONITSELF="passwQuestion";

	private String passwQuestion;
	private String passwAnswer;

	private List<SecurityQuestionAnswerDTO> questionAnswers;

	private PrismContainer credentials;

	/*public PasswordQuestionsDto(String passwQuestion){
		this.passwQuestion = passwQuestion;
	}*/
	public PasswordQuestionsDto(){

	};

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
