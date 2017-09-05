package com.evolveum.midpoint.web.page.forgetpassword;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialsResetPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailAuthenticationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailResetPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsResetPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsAuthenticationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsResetPolicyType;

public class ResetPolicyDto implements Serializable {

	private static final long serialVersionUID = 1L;

	enum ResetMethod {
		SECURITY_QUESTIONS, MAIL, SMS, NONE;
	}

	private String name;
	private ResetMethod resetMethod;
	private MailAuthenticationPolicyType mailAuthentication;
	private SmsAuthenticationPolicyType smsAuthentication;
	private SecurityQuestionsCredentialsPolicyType securityQuestions;
	private NonceCredentialsPolicyType noncePolicy;
	private ObjectReferenceType formRef;

	public void initResetPolicyDto(SecurityPolicyType securityPolicyType) throws SchemaException {
		if (securityPolicyType == null) {
			return;
		}

//		if (securityPolicyType.getCredentials() != null
//				&& securityPolicyType.getCredentials().getSecurityQuestions() != null) {
//			this.securityQuestions = securityPolicyType.getCredentials().getSecurityQuestions();
//			resetMethod.add(ResetMethod.SECURITY_QUESTIONS);
//			return;
//		}

		if (securityPolicyType.getCredentialsReset() == null) {
			return;
		}

		MailResetPolicyType mailResetPolicy = securityPolicyType.getCredentialsReset().getMailReset();
		if (mailResetPolicy != null) {
			this.resetMethod = ResetMethod.MAIL;
			initResetPolicy(mailResetPolicy, securityPolicyType);
			return;
		}

		SmsResetPolicyType smsResetPolicy = securityPolicyType.getCredentialsReset().getSmsReset();
		if (smsResetPolicy != null) {
			this.resetMethod = ResetMethod.SMS;
			initResetPolicy(smsResetPolicy, securityPolicyType);
			return;
		}

		SecurityQuestionsResetPolicyType securityQuestionsResetPolicy = securityPolicyType
				.getCredentialsReset().getSecurityQuestionReset();
		if (securityQuestionsResetPolicy != null) {
			this.resetMethod = ResetMethod.SECURITY_QUESTIONS;
			initResetPolicy(securityQuestionsResetPolicy, securityPolicyType);
			return;
		}

	}

	private void initResetPolicy(AbstractCredentialsResetPolicyType resetPolicy,
			SecurityPolicyType securityPolicyType) throws SchemaException {
		this.formRef = resetPolicy.getFormRef();
		AbstractAuthenticationPolicyType authPolicy = SecurityPolicyUtil
				.getAuthenticationPolicy(resetPolicy.getAdditionalAuthenticationName(), securityPolicyType);
		if (authPolicy instanceof MailAuthenticationPolicyType) {
			this.mailAuthentication = (MailAuthenticationPolicyType) authPolicy;
			noncePolicy = SecurityPolicyUtil.getCredentialPolicy(mailAuthentication.getMailNonce(),
					securityPolicyType);
		} else if (authPolicy instanceof SmsAuthenticationPolicyType) {
			this.smsAuthentication = (SmsAuthenticationPolicyType) authPolicy;
			this.noncePolicy = SecurityPolicyUtil.getCredentialPolicy(smsAuthentication.getSmsNonce(),
					securityPolicyType);
		}
		this.name = resetPolicy.getName();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ResetMethod getResetMethod() {
		return resetMethod;
	}

	public void setResetMethod(ResetMethod resetMethod) {
		this.resetMethod = resetMethod;
	}

	public MailAuthenticationPolicyType getMailAuthentication() {
		return mailAuthentication;
	}

	public void setMailAuthentication(MailAuthenticationPolicyType mailAuthentication) {
		this.mailAuthentication = mailAuthentication;
	}

	public SmsAuthenticationPolicyType getSmsAuthentication() {
		return smsAuthentication;
	}

	public void setSmsAuthentication(SmsAuthenticationPolicyType smsAuthentication) {
		this.smsAuthentication = smsAuthentication;
	}

	public SecurityQuestionsCredentialsPolicyType getSecurityQuestions() {
		return securityQuestions;
	}

	public void setSecurityQuestions(SecurityQuestionsCredentialsPolicyType securityQuestions) {
		this.securityQuestions = securityQuestions;
	}

	public NonceCredentialsPolicyType getNoncePolicy() {
		return noncePolicy;
	}

	public void setNoncePolicy(NonceCredentialsPolicyType noncePolicy) {
		this.noncePolicy = noncePolicy;
	}

	public ObjectReferenceType getFormRef() {
		return formRef;
	}

}
