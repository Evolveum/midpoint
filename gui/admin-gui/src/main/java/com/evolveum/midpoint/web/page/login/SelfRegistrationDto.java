package com.evolveum.midpoint.web.page.login;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailAuthenticationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelfRegistrationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsAuthenticationPolicyType;

public class SelfRegistrationDto implements Serializable {

	enum AuthenticationPolicy {
		MAIL, SMS, NONE;
	}

	private static final long serialVersionUID = 1L;

	private String name;
	private List<ObjectReferenceType> defaultRoles;
	private MailAuthenticationPolicyType mailAuthenticationPolicy;
	private SmsAuthenticationPolicyType smsAuthenticationPolicy;
	private NonceCredentialsPolicyType noncePolicy;

	private String requiredLifecycleState;
	private String initialLifecycleState;

	private ObjectReferenceType formRef;

	public void initSelfRegistrationDto(SecurityPolicyType securityPolicy) throws SchemaException {
		if (securityPolicy == null) {
			return;
		}
		SelfRegistrationPolicyType selfRegistration = getSelfRegistrationPolicy(securityPolicy);
		if (selfRegistration == null) {
			return;
		}

		init(securityPolicy, selfRegistration);

	}
	
	public void initPostAuthenticationDto(SecurityPolicyType securityPolicy) throws SchemaException {
		if (securityPolicy == null) {
			return;
		}
		SelfRegistrationPolicyType selfRegistration = getPostAuthenticationPolicy(securityPolicy);
		if (selfRegistration == null) {
			return;
		}

		init(securityPolicy, selfRegistration);
	}

	
	private void init(SecurityPolicyType securityPolicy, SelfRegistrationPolicyType selfRegistration) throws SchemaException {
		this.name = selfRegistration.getName();
		this.defaultRoles = selfRegistration.getDefaultRole();
		this.initialLifecycleState = selfRegistration.getInitialLifecycleState();
		this.requiredLifecycleState = selfRegistration.getRequiredLifecycleState();

		this.formRef = selfRegistration.getFormRef();

		AbstractAuthenticationPolicyType authPolicy = SecurityPolicyUtil.getAuthenticationPolicy(
				selfRegistration.getAdditionalAuthenticationName(), securityPolicy);

		if (authPolicy instanceof MailAuthenticationPolicyType) {
			this.mailAuthenticationPolicy = (MailAuthenticationPolicyType) authPolicy;
			noncePolicy = SecurityPolicyUtil.getCredentialPolicy(((MailAuthenticationPolicyType) authPolicy).getMailNonce(), securityPolicy);
		} else if (authPolicy instanceof SmsAuthenticationPolicyType) {
			this.smsAuthenticationPolicy = (SmsAuthenticationPolicyType) authPolicy;
			noncePolicy = SecurityPolicyUtil.getCredentialPolicy(((SmsAuthenticationPolicyType) authPolicy).getSmsNonce(), securityPolicy);
		}
	}
	
	public boolean isEmpty() {
		return StringUtils.isEmpty(name) && CollectionUtils.isEmpty(defaultRoles)
				&& mailAuthenticationPolicy == null && smsAuthenticationPolicy == null && noncePolicy == null;
	}

	public AuthenticationPolicy getAuthenticationMethod () {
		if (mailAuthenticationPolicy != null) {
			return AuthenticationPolicy.MAIL;
		}

		if (smsAuthenticationPolicy != null) {
			return AuthenticationPolicy.SMS;
		}

		return AuthenticationPolicy.NONE;
	}

	private SelfRegistrationPolicyType getSelfRegistrationPolicy(SecurityPolicyType securityPolicyType) {
		RegistrationsPolicyType flowPolicy = securityPolicyType.getFlow();
		SelfRegistrationPolicyType selfRegistrationPolicy = null;
		if (flowPolicy != null) {
			selfRegistrationPolicy = flowPolicy.getSelfRegistration();
		}
		
		if (selfRegistrationPolicy != null) {
			return selfRegistrationPolicy;
		}
		
		RegistrationsPolicyType registrationPolicy = securityPolicyType.getRegistration();

		if (registrationPolicy == null) {
			return null;
		}

		return registrationPolicy.getSelfRegistration();
	}
	
	private SelfRegistrationPolicyType getPostAuthenticationPolicy(SecurityPolicyType securityPolicyType) {
		RegistrationsPolicyType flowPolicy = securityPolicyType.getFlow();
		SelfRegistrationPolicyType selfRegistrationPolicy = null;
		if (flowPolicy != null) {
			selfRegistrationPolicy = flowPolicy.getPostAuthentication();
		}
		
		return selfRegistrationPolicy;
	}

//	private AbstractAuthenticationPolicyType getAuthenticationPolicy(String selfRegistrationAuthPoliocyName,
//			SecurityPolicyType securityPolicy) throws SchemaException {
//		MailAuthenticationPolicyType mailAuthPolicy = getMailAuthenticationPolicy(
//				selfRegistrationAuthPoliocyName, securityPolicy);
//		SmsAuthenticationPolicyType smsAuthPolicy = getSmsAuthenticationPolicy(
//				selfRegistrationAuthPoliocyName, securityPolicy);
//		return checkAndGetAuthPolicyConsistence(mailAuthPolicy, smsAuthPolicy);
//
//	}

//	private MailAuthenticationPolicyType getMailAuthenticationPolicy(String authName,
//			SecurityPolicyType securityPolicy) throws SchemaException {
//		AuthenticationsPolicyType authPolicies = securityPolicy.getAuthentication();
//		if (authPolicies == null) {
//			return null;
//		}
//		return getAuthenticationPolicy(authName, authPolicies.getMailAuthentication());
//	}
//
//	private SmsAuthenticationPolicyType getSmsAuthenticationPolicy(String authName,
//			SecurityPolicyType securityPolicy) throws SchemaException {
//		AuthenticationsPolicyType authPolicies = securityPolicy.getAuthentication();
//		if (authPolicies == null) {
//			return null;
//		}
//		return getAuthenticationPolicy(authName, authPolicies.getSmsAuthentication());
//	}
//
//	private AbstractAuthenticationPolicyType checkAndGetAuthPolicyConsistence(
//			MailAuthenticationPolicyType mailPolicy, SmsAuthenticationPolicyType smsPolicy)
//					throws SchemaException {
//		if (mailPolicy != null && smsPolicy != null) {
//			throw new SchemaException(
//					"Found both, mail and sms authentication method for registration. Only one of them can be present at the moment");
//		}
//
//		if (mailPolicy != null) {
//			return mailPolicy;
//		}
//
//		return smsPolicy;
//
//	}

//	private <T extends AbstractAuthenticationPolicyType> T getAuthenticationPolicy(String authName,
//			List<T> authPolicies) throws SchemaException {
//
//		List<T> smsPolicies = new ArrayList<>();
//
//		for (T smsAuthPolicy : authPolicies) {
//			if (smsAuthPolicy.getName() == null && authName != null) {
//				continue;
//			}
//
//			if (smsAuthPolicy.getName() != null && authName == null) {
//				continue;
//			}
//
//			if (smsAuthPolicy.getName() == null && authName == null) {
//				smsPolicies.add(smsAuthPolicy);
//			}
//
//			if (smsAuthPolicy.getName().equals(authName)) {
//				smsPolicies.add(smsAuthPolicy);
//			}
//
//		}
//
//		if (smsPolicies.size() > 1) {
//			throw new SchemaException(
//					"Found more than one mail authentication policy. Please review your configuration");
//		}
//
//		if (smsPolicies.size() == 0) {
//			return null;
//		}
//
//		return smsPolicies.iterator().next();
//
//	}

//	private NonceCredentialsPolicyType getCredentialPolicy(String policyName,
//			SecurityPolicyType securityPolicy) throws SchemaException {
//		CredentialsPolicyType credentialsPolicy = securityPolicy.getCredentials();
//		if (credentialsPolicy == null) {
//			return null;
//		}
//
//		List<NonceCredentialsPolicyType> noncePolicies = credentialsPolicy.getNonce();
//
//		List<NonceCredentialsPolicyType> availableNoncePolicies = new ArrayList<>();
//		for (NonceCredentialsPolicyType noncePolicy : noncePolicies) {
//			if (noncePolicy.getName() == null && policyName == null) {
//				availableNoncePolicies.add(noncePolicy);
//			}
//
//			if (noncePolicy.getName() == null && policyName != null) {
//				continue;
//			}
//
//			if (noncePolicy.getName() != null && policyName == null) {
//				continue;
//			}
//
//			if (noncePolicy.getName().equals(policyName)) {
//				availableNoncePolicies.add(noncePolicy);
//			}
//		}
//
//		if (availableNoncePolicies.size() > 1) {
//			throw new SchemaException(
//					"Found more than one nonce credentials policy. Please review your configuration");
//		}
//
//		if (availableNoncePolicies.size() == 0) {
//			return null;
//		}
//
//		return availableNoncePolicies.iterator().next();
//	}

	public boolean isMailMailAuthentication() {
		return mailAuthenticationPolicy != null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ObjectReferenceType> getDefaultRoles() {
		return defaultRoles;
	}

	public void setDefaultRoles(List<ObjectReferenceType> defaultRoles) {
		this.defaultRoles = defaultRoles;
	}

	public MailAuthenticationPolicyType getMailAuthenticationPolicy() {
		return mailAuthenticationPolicy;
	}

	public void setMailAuthenticationPolicy(MailAuthenticationPolicyType mailAuthenticationPolicy) {
		this.mailAuthenticationPolicy = mailAuthenticationPolicy;
	}

	public SmsAuthenticationPolicyType getSmsAuthenticationPolicy() {
		return smsAuthenticationPolicy;
	}

	public void setSmsAuthenticationPolicy(SmsAuthenticationPolicyType smsAuthenticationPolicy) {
		this.smsAuthenticationPolicy = smsAuthenticationPolicy;
	}

	public NonceCredentialsPolicyType getNoncePolicy() {
		return noncePolicy;
	}

	public void setNoncePolicy(NonceCredentialsPolicyType noncePolicy) {
		this.noncePolicy = noncePolicy;
	}

	public String getInitialLifecycleState() {
		return initialLifecycleState;
	}

	public void setInitialLifecycleState(String initialLifecycleState) {
		this.initialLifecycleState = initialLifecycleState;
	}

	public String getRequiredLifecycleState() {
		return requiredLifecycleState;
	}

	public void setRequiredLifecycleState(String requiredLifecycleState) {
		this.requiredLifecycleState = requiredLifecycleState;
	}

	public ObjectReferenceType getFormRef() {
		return formRef;
	}
}
