package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailAuthenticationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsAuthenticationPolicyType;

public class SecurityPolicyUtil {

	public static AbstractAuthenticationPolicyType getAuthenticationPolicy(String authPolicyName,
			SecurityPolicyType securityPolicy) throws SchemaException {
		MailAuthenticationPolicyType mailAuthPolicy = getMailAuthenticationPolicy(
				authPolicyName, securityPolicy);
		SmsAuthenticationPolicyType smsAuthPolicy = getSmsAuthenticationPolicy(
				authPolicyName, securityPolicy);
		return checkAndGetAuthPolicyConsistence(mailAuthPolicy, smsAuthPolicy);

	}



	public static NonceCredentialsPolicyType getCredentialPolicy(String policyName,
			SecurityPolicyType securityPolicy) throws SchemaException {
		CredentialsPolicyType credentialsPolicy = securityPolicy.getCredentials();
		if (credentialsPolicy == null) {
			return null;
		}

		List<NonceCredentialsPolicyType> noncePolicies = credentialsPolicy.getNonce();

		List<NonceCredentialsPolicyType> availableNoncePolicies = new ArrayList<>();
		for (NonceCredentialsPolicyType noncePolicy : noncePolicies) {
			if (noncePolicy.getName() == null && policyName == null) {
				availableNoncePolicies.add(noncePolicy);
			}

			if (noncePolicy.getName() == null && policyName != null) {
				continue;
			}

			if (noncePolicy.getName() != null && policyName == null) {
				continue;
			}

			if (noncePolicy.getName().equals(policyName)) {
				availableNoncePolicies.add(noncePolicy);
			}
		}

		if (availableNoncePolicies.size() > 1) {
			throw new SchemaException(
					"Found more than one nonce credentials policy. Please review your configuration");
		}

		if (availableNoncePolicies.size() == 0) {
			return null;
		}

		return availableNoncePolicies.iterator().next();
	}

	private static MailAuthenticationPolicyType getMailAuthenticationPolicy(String authName,
			SecurityPolicyType securityPolicy) throws SchemaException {
		AuthenticationsPolicyType authPolicies = securityPolicy.getAuthentication();
		if (authPolicies == null) {
			return null;
		}
		return getAuthenticationPolicy(authName, authPolicies.getMailAuthentication());
	}

	private static SmsAuthenticationPolicyType getSmsAuthenticationPolicy(String authName,
			SecurityPolicyType securityPolicy) throws SchemaException {
		AuthenticationsPolicyType authPolicies = securityPolicy.getAuthentication();
		if (authPolicies == null) {
			return null;
		}
		return getAuthenticationPolicy(authName, authPolicies.getSmsAuthentication());
	}

	private static AbstractAuthenticationPolicyType checkAndGetAuthPolicyConsistence(
			MailAuthenticationPolicyType mailPolicy, SmsAuthenticationPolicyType smsPolicy)
					throws SchemaException {
		if (mailPolicy != null && smsPolicy != null) {
			throw new SchemaException(
					"Found both, mail and sms authentication method for registration. Only one of them can be present at the moment");
		}

		if (mailPolicy != null) {
			return mailPolicy;
		}

		return smsPolicy;

	}

	private static <T extends AbstractAuthenticationPolicyType> T getAuthenticationPolicy(String authName,
			List<T> authPolicies) throws SchemaException {

		List<T> smsPolicies = new ArrayList<>();

		for (T smsAuthPolicy : authPolicies) {
			if (smsAuthPolicy.getName() == null && authName != null) {
				continue;
			}

			if (smsAuthPolicy.getName() != null && authName == null) {
				continue;
			}

			if (smsAuthPolicy.getName() == null && authName == null) {
				smsPolicies.add(smsAuthPolicy);
			}

			if (smsAuthPolicy.getName().equals(authName)) {
				smsPolicies.add(smsAuthPolicy);
			}

		}

		if (smsPolicies.size() > 1) {
			throw new SchemaException(
					"Found more than one mail authentication policy. Please review your configuration");
		}

		if (smsPolicies.size() == 0) {
			return null;
		}

		return smsPolicies.iterator().next();

	}


}
