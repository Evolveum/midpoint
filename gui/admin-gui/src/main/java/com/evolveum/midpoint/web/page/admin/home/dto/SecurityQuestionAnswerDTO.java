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
	public SecurityQuestionAnswerDTO(String passwQuestion, String passAnswer,String questionitself){
		this.passwQuestionIdentifier = passwQuestion;
		this.passwAnswer = passAnswer;
		this.questionItself=questionitself;
	}

	public String getPwdQuestion() {
		return passwQuestionIdentifier;
	}
	public void setPwdQuestion(String pwdQuestion) {
		this.passwQuestionIdentifier = pwdQuestion;
	}
	public String getPwdAnswer() {
		return passwAnswer;
	}
	public void setPwdAnswer(String pwdAnswer) {
		this.passwAnswer = pwdAnswer;
	}

	public String getQuestionItself() {
		return questionItself;
	}
	public void setQuestionItself(String questionItself) {
		this.questionItself = questionItself;
	}

}
