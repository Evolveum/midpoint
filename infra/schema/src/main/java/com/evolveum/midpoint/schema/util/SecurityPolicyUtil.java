/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.*;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.Validate;

public class SecurityPolicyUtil {

    public static final String DEFAULT_CHANNEL = SchemaConstants.CHANNEL_USER_URI;
    public static final String DEFAULT_MODULE_NAME = "loginForm";
    public static final String HTTP_BASIC_MODULE_NAME = "httpBasic";
    public static final String DEFAULT_SEQUENCE_NAME = "admin-gui-default";
    public static final String REST_SEQUENCE_NAME = "rest-default";
    public static final String ACTUATOR_SEQUENCE_NAME = "actuator-default";
    public static final String PASSWORD_RESET_SEQUENCE_NAME = "password-reset-default";
    private static final List<String> IGNORED_LOCAL_PATH;
    static {
        List<String> list = new ArrayList<String>();
        list.add("/actuator");
        list.add("/actuator/health");
        IGNORED_LOCAL_PATH = Collections.unmodifiableList(list);
    }

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

    public static List<AuthenticationSequenceModuleType> getSortedModules(AuthenticationSequenceType sequence){
        Validate.notNull(sequence);
        ArrayList<AuthenticationSequenceModuleType> modules = new ArrayList<AuthenticationSequenceModuleType>();
        modules.addAll(sequence.getModule());
        Validate.notNull(modules);
        Validate.notEmpty(modules);
        Comparator<AuthenticationSequenceModuleType> comparator =
                (f1,f2) -> {

                    Integer f1Order = f1.getOrder();
                    Integer f2Order = f2.getOrder();

                    if (f1Order == null) {
                        if (f2Order != null) {
                            return 1;
                        }
                        return 0;
                    }

                    if (f2Order == null) {
                        if (f1Order != null) {
                            return -1;
                        }
                    }
                    return Integer.compare(f1Order, f2Order);
                };
        modules.sort(comparator);
        return Collections.unmodifiableList(modules);
    }

    public static AuthenticationsPolicyType createDefaultAuthenticationPolicy() {
        AuthenticationsPolicyType authenticationPolicy = new AuthenticationsPolicyType();
        AuthenticationModulesType modules = new AuthenticationModulesType();
        AuthenticationModuleLoginFormType loginForm = new AuthenticationModuleLoginFormType();
        loginForm.name(DEFAULT_MODULE_NAME);
        modules.loginForm(loginForm);
        AuthenticationModuleHttpBasicType httpBasic = new AuthenticationModuleHttpBasicType();
        httpBasic.name(HTTP_BASIC_MODULE_NAME);
        modules.httpBasic(httpBasic);
        authenticationPolicy.setModules(modules);
        authenticationPolicy.sequence(createDefaultSequence());
        authenticationPolicy.sequence(createRestSequence());
        authenticationPolicy.sequence(createActuatorSequence());
        authenticationPolicy.sequence(createPaswordResetSequence());
        for (String ignoredPath : IGNORED_LOCAL_PATH) {
            authenticationPolicy.ignoredLocalPath(ignoredPath);
        }
        return authenticationPolicy;
    }

    public static AuthenticationSequenceType createDefaultSequence() {
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.name(DEFAULT_SEQUENCE_NAME);
        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType();
        channel.setDefault(true);
        channel.channelId(DEFAULT_CHANNEL);
        channel.setUrlSuffix("gui-default");
        sequence.channel(channel);
        AuthenticationSequenceModuleType module = new AuthenticationSequenceModuleType();
        module.name(DEFAULT_MODULE_NAME);
        module.order(1);
        module.necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        sequence.module(module);
        return sequence;
    }

    public static AuthenticationSequenceType createRestSequence() {
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.name(REST_SEQUENCE_NAME);
        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType();
        channel.setDefault(true);
        channel.channelId(SchemaConstants.CHANNEL_REST_URI);
        channel.setUrlSuffix("rest-default");
        sequence.channel(channel);
        AuthenticationSequenceModuleType module = new AuthenticationSequenceModuleType();
        module.name(HTTP_BASIC_MODULE_NAME);
        module.order(1);
        module.necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        sequence.module(module);
        return sequence;
    }

    public static AuthenticationSequenceType createActuatorSequence() {
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.name(ACTUATOR_SEQUENCE_NAME);
        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType();
        channel.setDefault(true);
        channel.channelId(SchemaConstants.CHANNEL_ACTUATOR_URI);
        channel.setUrlSuffix("actuator-default");
        sequence.channel(channel);
        AuthenticationSequenceModuleType module = new AuthenticationSequenceModuleType();
        module.name(HTTP_BASIC_MODULE_NAME);
        module.order(1);
        module.necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        sequence.module(module);
        return sequence;
    }

    public static AuthenticationSequenceType createPaswordResetSequence() {
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.name(PASSWORD_RESET_SEQUENCE_NAME);
        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType();
        channel.setDefault(true);
        channel.channelId(SchemaConstants.CHANNEL_GUI_RESET_PASSWORD_URI);
        channel.setUrlSuffix("resetPassword");
        sequence.channel(channel);
        AuthenticationSequenceModuleType module = new AuthenticationSequenceModuleType();
        module.name(DEFAULT_MODULE_NAME);
        module.order(1);
        module.necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        sequence.module(module);
        return sequence;
    }

}
