/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.error.PageError;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/emailNonce", matchUrlForSecurity = "/emailNonce")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.MAIL_NONCE)
public class PageEmailNonce extends PageAbstractAuthenticationModule<CredentialModuleAuthentication> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageEmailNonce.class);

    private static final String DOT_CLASS = PageEmailNonce.class.getName() + ".";

    private static final String ID_SEND_NONCE = "sendNonce";
    private static final String OPERATION_DETERMINE_NONCE_CREDENTIALS_POLICY = DOT_CLASS + "determineNonceCredentialsPolicy";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    public PageEmailNonce() {

        // TODO improve message with the time when the mail was sent (saved in nonce metadata)

        if (!alreadyHasNonce()) {
            generateAndSendNonce(null);
        }

        LOGGER.debug("Nonce won't be generated automatically, user already has one.");
    }

    private boolean alreadyHasNonce() {
        UserType user = searchUser();
        if (user == null || user.getCredentials() == null) {
            return false;
        }
        NonceType nonceType = user.getCredentials().getNonce();
        return nonceType != null;

        //TODO check name nonceType.getName();
    }


    @Override
    protected void initModuleLayout(MidpointForm form) {
        initButtons(form);
    }


    private void initButtons(MidpointForm form) {
        AjaxButton resendNonce = new AjaxButton(ID_SEND_NONCE, createStringResource("PageBase.button.nonce.send.new")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                generateAndSendNonce(target);
            }

        };
        form.add(resendNonce);
    }


    private void generateAndSendNonce(AjaxRequestTarget target) {
        UserType user = searchUser();
        validateUserNotNullOrFail(user);
        LOGGER.trace("Reset Password user: {}", user);

        NonceCredentialsPolicyType noncePolicy = getMailNoncePolicy(user.asPrismObject());

        OperationResult result = saveUserNonce(user, noncePolicy);
        if (result.getStatus() != OperationResultStatus.SUCCESS) {
            LOGGER.error("Failed to send nonce to user: {} ", result.getMessage());
        } else if (target != null) {
            new Toast()
                    .success()
                    .title(getString("PageEmailNonce.sentNonce"))
                    .icon("fas fa-circle-check")
                    .autohide(true)
                    .delay(5_000)
                    .body(getString("PageEmailNonce.sentNonce.message"))
                    .show(target);
        }

    }

    private @Nullable NonceCredentialsPolicyType getMailNoncePolicy(PrismObject<UserType> user) {

        CredentialModuleAuthentication moduleType = getAuthenticationModuleConfiguration();
        String credentialName = moduleType.getCredentialName();

        if (credentialName == null) {
            LOGGER.error("EmailNonceModuleAuthentication " + moduleType.getModuleIdentifier() + " haven't define name of credential");
            return null;
        }

        return resolveNoncePolicy(user, credentialName);
    }

    private NonceCredentialsPolicyType resolveNoncePolicy(PrismObject<UserType> user, String credentialsName) {
        Task task = createAnonymousTask(OPERATION_DETERMINE_NONCE_CREDENTIALS_POLICY);
        task.setChannel(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI);
        OperationResult result = task.getResult();

        try {
            return getModelInteractionService().determineNonceCredentialsPolicy(user, credentialsName, task, result);
        } catch (CommonException e) {
            LOGGER.error("Could not retrieve nonce policy: {}", e.getMessage(), e);
            return null;
        }
    }

    private OperationResult saveUserNonce(final UserType user, final NonceCredentialsPolicyType noncePolicy) {
        return runPrivileged((Producer<OperationResult>) () -> saveNonce(user, noncePolicy));
    }

    private OperationResult saveNonce(UserType user, NonceCredentialsPolicyType noncePolicy) {
        Task task = createAnonymousTask("generateUserNonce");
        task.setChannel(SchemaConstants.CHANNEL_RESET_PASSWORD_URI);
        task.setOwner(user.asPrismObject());
        OperationResult result = new OperationResult("generateUserNonce");
        ProtectedStringType nonceCredentials = new ProtectedStringType();
        try {
            nonceCredentials
                    .setClearValue(generateNonce(noncePolicy, task, user.asPrismObject(), result));

            ObjectDelta<UserType> nonceDelta = getPrismContext().deltaFactory().object()
                    .createModificationReplaceProperty(UserType.class, user.getOid(),
                            SchemaConstants.PATH_NONCE_VALUE, nonceCredentials);

            WebModelServiceUtils.save(nonceDelta, result, task, PageEmailNonce.this);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            result.recordFatalError(getString("PageForgotPassword.message.saveUserNonce.fatalError"));
            LoggingUtils.logException(LOGGER, "Failed to generate nonce for user: " + e.getMessage(),
                    e);
        }

        result.computeStatusIfUnknown();
        return result;
    }

    private <O extends ObjectType> String generateNonce(NonceCredentialsPolicyType noncePolicy, Task task,
                                                        PrismObject<O> user, OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ValuePolicyType policy = resolveValuePolicy(noncePolicy, task, result);
        return getModelInteractionService().generateValue(policy, 24, false, user, "nonce generation", task, result);
    }

    private ValuePolicyType resolveValuePolicy(NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) {
        if (noncePolicy == null || noncePolicy.getValuePolicyRef() == null) {
            return null;
        }

        ObjectReferenceType valuePolicyRef = noncePolicy.getValuePolicyRef();
        PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
                valuePolicyRef.getOid(), PageEmailNonce.this, task, result);

        if (valuePolicy == null) {
            return null;
        }
        return valuePolicy.asObjectable();
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource("PageEmailNonce.checkYourMail").getString();
            }
        };
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource("PageForgotPassword.form.submited.message").getString();
            }
        };
    }

    @Override
    protected String getModuleTypeName() {
        return "mailNonce";
    }
}

