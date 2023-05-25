/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.Objects;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SecurityPolicyUtil {

    public static final String DEFAULT_CHANNEL = SchemaConstants.CHANNEL_USER_URI;
    public static final String DEFAULT_MODULE_NAME = "loginForm";
    public static final String HTTP_BASIC_MODULE_NAME = "httpBasic";
    public static final String DEFAULT_SEQUENCE_NAME = "admin-gui-default";
    public static final String DEFAULT_SEQUENCE_DISPLAY_NAME = "Default gui sequence";
    public static final String REST_SEQUENCE_NAME = "rest-default";
    public static final String ACTUATOR_SEQUENCE_NAME = "actuator-default";
    public static final String PASSWORD_RESET_SEQUENCE_NAME = "password-reset-default";

    private static final List<String> DEFAULT_IGNORED_LOCAL_PATH;

    /** Constant representing no custom ignored local paths (can be null or empty collection). */
    public static final List<String> NO_CUSTOM_IGNORED_LOCAL_PATH = null;

    static {
        List<String> list = new ArrayList<>();
        list.add("/actuator");
        list.add("/actuator/health");
        DEFAULT_IGNORED_LOCAL_PATH = Collections.unmodifiableList(list);
    }

    public static AbstractAuthenticationPolicyType getAuthenticationPolicy(
            String authPolicyName, SecurityPolicyType securityPolicy) throws SchemaException {

        MailAuthenticationPolicyType mailAuthPolicy =
                getMailAuthenticationPolicy(authPolicyName, securityPolicy);
        SmsAuthenticationPolicyType smsAuthPolicy =
                getSmsAuthenticationPolicy(authPolicyName, securityPolicy);
        return checkAndGetAuthPolicyConsistence(mailAuthPolicy, smsAuthPolicy);

    }

    public static NonceCredentialsPolicyType getCredentialPolicy(
            String policyName, SecurityPolicyType securityPolicy) throws SchemaException {

        CredentialsPolicyType credentialsPolicy = securityPolicy.getCredentials();
        if (credentialsPolicy == null) {
            return null;
        }

        List<NonceCredentialsPolicyType> noncePolicies = credentialsPolicy.getNonce();

        List<NonceCredentialsPolicyType> availableNoncePolicies = new ArrayList<>();
        for (NonceCredentialsPolicyType noncePolicy : noncePolicies) {
            if (Objects.equals(noncePolicy.getName(), policyName)) {
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

    private static MailAuthenticationPolicyType getMailAuthenticationPolicy(
            String authName, SecurityPolicyType securityPolicy) throws SchemaException {

        AuthenticationsPolicyType authPolicies = securityPolicy.getAuthentication();
        if (authPolicies == null) {
            return null;
        }
        return getAuthenticationPolicy(authName, authPolicies.getMailAuthentication());
    }

    private static SmsAuthenticationPolicyType getSmsAuthenticationPolicy(
            String authName, SecurityPolicyType securityPolicy) throws SchemaException {

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

    private static <T extends AbstractAuthenticationPolicyType> T getAuthenticationPolicy(
            String authName, List<T> authPolicies) throws SchemaException {

        List<T> smsPolicies = new ArrayList<>();
        for (T smsAuthPolicy : authPolicies) {
            if (Objects.equals(smsAuthPolicy.getName(), authName)) {
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

    public static List<AuthenticationSequenceModuleType> getSortedModules(AuthenticationSequenceType sequence) {
        Validate.notNull(sequence);
        ArrayList<AuthenticationSequenceModuleType> modules = new ArrayList<>(sequence.getModule());
        Validate.notNull(modules);
        Comparator<AuthenticationSequenceModuleType> comparator =
                (f1, f2) -> {
                    Integer f1Order = f1.getOrder();
                    Integer f2Order = f2.getOrder();

                    if (f1Order == null) {
                        if (f2Order != null) {
                            return 1;
                        }
                        return 0;
                    }

                    if (f2Order == null) {
                        // f1Order != null already
                        return -1;
                    }
                    return Integer.compare(f1Order, f2Order);
                };
        modules.sort(comparator);
        return Collections.unmodifiableList(modules);
    }

    public static AuthenticationsPolicyType createDefaultAuthenticationPolicy(
            List<String> customIgnoredLocalPaths, SchemaRegistry schemaRegistry)
            throws SchemaException {

        PrismObjectDefinition<SecurityPolicyType> secPolicyDef =
                schemaRegistry.findObjectDefinitionByCompileTimeClass(SecurityPolicyType.class);
        @NotNull PrismObject<SecurityPolicyType> secPolicy = secPolicyDef.instantiate();
        AuthenticationsPolicyType authenticationPolicy = new AuthenticationsPolicyType();
        AuthenticationModulesType modules = new AuthenticationModulesType();
        LoginFormAuthenticationModuleType loginForm = new LoginFormAuthenticationModuleType();
        loginForm.name(DEFAULT_MODULE_NAME);
        modules.loginForm(loginForm);
        HttpBasicAuthenticationModuleType httpBasic = new HttpBasicAuthenticationModuleType();
        httpBasic.name(HTTP_BASIC_MODULE_NAME);
        modules.httpBasic(httpBasic);
        authenticationPolicy.setModules(modules);
        authenticationPolicy.sequence(createDefaultSequence());
        authenticationPolicy.sequence(createRestSequence());
        authenticationPolicy.sequence(createActuatorSequence());
        authenticationPolicy.sequence(createPasswordResetSequence());
        if (customIgnoredLocalPaths == null || customIgnoredLocalPaths.isEmpty()) {
            DEFAULT_IGNORED_LOCAL_PATH.forEach(authenticationPolicy::ignoredLocalPath);
        } else {
            customIgnoredLocalPaths.forEach(authenticationPolicy::ignoredLocalPath);
        }
        secPolicy.asObjectable().setAuthentication(authenticationPolicy);
        return secPolicy.asObjectable().getAuthentication();
    }

    public static AuthenticationSequenceType createDefaultSequence() {
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.name(DEFAULT_SEQUENCE_NAME);
        sequence.setDisplayName(DEFAULT_SEQUENCE_DISPLAY_NAME);
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

    private static AuthenticationSequenceType createRestSequence() {
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

    private static AuthenticationSequenceType createActuatorSequence() {
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

    public static AuthenticationSequenceType createPasswordResetSequence() {
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.name(PASSWORD_RESET_SEQUENCE_NAME);
        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType();
        channel.setDefault(true);
        channel.channelId(SchemaConstants.CHANNEL_RESET_PASSWORD_URI);
        channel.setUrlSuffix("resetPassword");
        sequence.channel(channel);
        AuthenticationSequenceModuleType module = new AuthenticationSequenceModuleType();
        module.name(DEFAULT_MODULE_NAME);
        module.order(1);
        module.necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        sequence.module(module);
        return sequence;
    }

    public static SelfRegistrationPolicyType getSelfRegistrationPolicy(SecurityPolicyType securityPolicyType) {
        RegistrationsPolicyType flowPolicy = securityPolicyType.getFlow();
        SelfRegistrationPolicyType selfRegistrationPolicy = null;
        if (flowPolicy != null) {
            selfRegistrationPolicy = flowPolicy.getSelfRegistration();
        }

        return selfRegistrationPolicy;
    }

    public static AuthenticationSequenceType findSequenceByName(@NotNull SecurityPolicyType securityPolicy, String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        if (securityPolicy.getAuthentication() == null || CollectionUtils.isEmpty(securityPolicy.getAuthentication().getSequence())) {
            return null;
        }
        return securityPolicy
                .getAuthentication()
                .getSequence()
                .stream()
                .filter(s -> name.equals(s.getName()))
                .findFirst()
                .orElse(null);
    }
}
