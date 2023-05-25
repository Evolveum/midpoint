/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.NonceAuthenticationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.forgetpassword.ResetPolicyDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

public class PageRegistrationBase extends PageBase {

    private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = PageRegistrationBase.class.getName() + ".";
    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";

    protected static final String OPERATION_LOAD_DYNAMIC_FORM = DOT_CLASS + "loadDynamicForm";

    private static final Trace LOGGER = TraceManager.getTrace(PageRegistrationBase.class);

    @SpringBean(name = "nonceAuthenticationEvaluator")
    private AuthenticationEvaluator<NonceAuthenticationContext> authenticationEvaluator;

    private ResetPolicyDto resetPasswordPolicy;
    private SelfRegistrationDto selfRegistrationDto;
    private SelfRegistrationDto postAuthenticationDto;

    public PageRegistrationBase() {
//        initSelfRegistrationConfiguration();
//        initResetCredentialsConfiguration();
    }

    private void initSelfRegistrationConfiguration() {

        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.selfRegistrationDto = new SelfRegistrationDto();
        try {
            this.selfRegistrationDto.initSelfRegistrationDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    private void initPostAuthenticationConfiguration() {

        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.postAuthenticationDto = new SelfRegistrationDto();
        try {
            this.postAuthenticationDto.initPostAuthenticationDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    private void initResetCredentialsConfiguration() {

        // TODO: cleanup, the same as in the PageRegistrationBase
        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.resetPasswordPolicy = new ResetPolicyDto();
        try {
            this.resetPasswordPolicy.initResetPolicyDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    protected SecurityPolicyType resolveSecurityPolicy() {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(null);

        if (securityPolicy == null) {
            LOGGER.error("No security policy defined.");
            getSession()
                    .error(createStringResource("PageSelfRegistration.securityPolicy.notFound").getString());
            throw new RestartResponseException(PageLogin.class);
        }

        return securityPolicy;
    }

    protected SecurityPolicyType resolveSecurityPolicy(PrismObject<UserType> user) {
        SecurityPolicyType securityPolicy = runPrivileged(new Producer<SecurityPolicyType>() {
            private static final long serialVersionUID = 1L;

            @Override
            public SecurityPolicyType run() {

                Task task = createAnonymousTask(OPERATION_GET_SECURITY_POLICY);
                task.setChannel(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI);
                OperationResult result = new OperationResult(OPERATION_GET_SECURITY_POLICY);

                try {
                    return getModelInteractionService().getSecurityPolicy(user, task, result);
                } catch (CommonException e) {
                    LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                    return null;
                }

            }

        });

        return securityPolicy;

    }

    public SelfRegistrationDto getSelfRegistrationConfiguration() {

        if (selfRegistrationDto == null) {
            initSelfRegistrationConfiguration();
        }

        return selfRegistrationDto;

    }

    public ResetPolicyDto getResetPasswordPolicy() {
        if (resetPasswordPolicy == null) {
            initResetCredentialsConfiguration();
        }
        return resetPasswordPolicy;
    }

    public SelfRegistrationDto getPostAuthenticationConfiguration() {

        if (postAuthenticationDto == null) {
            initPostAuthenticationConfiguration();
        }

        return postAuthenticationDto;

    }

    public AuthenticationEvaluator<NonceAuthenticationContext> getAuthenticationEvaluator() {
        return authenticationEvaluator;
    }

}
