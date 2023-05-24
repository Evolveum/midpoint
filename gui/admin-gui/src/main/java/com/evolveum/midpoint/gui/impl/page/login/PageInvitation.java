/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.IModel;

@PageDescriptor(urls = { @Url(mountUrl = "/invitation", matchUrlForSecurity = "/invitation") },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_INVITATION_URL) },
        authModule = AuthenticationModuleNameConstants.MAIL_NONCE)
public class PageInvitation extends PageSelfRegistration {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageInvitation.class);

    private static final String DOT_CLASS = PageInvitation.class.getName() + ".";

    public PageInvitation() {
        super();
    }

    @Override
    protected UserType instantiateUser() {
        return (UserType) getPrincipalFocus();
    }

    @Override
    protected ObjectDelta<UserType> prepareUserDelta(Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        LOGGER.trace("Preparing user MODIFY delta (preregistered user registration)");
        ObjectDelta<UserType> delta;
        if (!isCustomFormDefined()) {
            delta = getPrismContext().deltaFactory().object().createEmptyModifyDelta(UserType.class,
                    userModel.getObject().getOid());
            if (getSelfRegistrationConfiguration().getInitialLifecycleState() != null) {
                delta.addModificationReplaceProperty(UserType.F_LIFECYCLE_STATE,
                        getSelfRegistrationConfiguration().getInitialLifecycleState());
            }
            delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, createPassword().getValue());
        } else {
            delta = getDynamicFormPanel().getObjectDelta();
        }

        delta.addModificationReplaceContainer(SchemaConstants.PATH_NONCE,
                createNonce(getNonceCredentialsPolicy(), task, result).asPrismContainerValue());
        LOGGER.trace("Going to register user with modifications {}", delta);
        return delta;
    }

    private NonceCredentialsPolicyType getNonceCredentialsPolicy() {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy();
        if (securityPolicy == null) {
            return null;
        }
        String invitationSequenceIdentifier = SecurityUtil.getInvitationSequenceIdentifier(securityPolicy);
        AuthenticationSequenceType invitationSequence = SecurityPolicyUtil.findSequenceByIdentifier(securityPolicy, invitationSequenceIdentifier);
        if (invitationSequence == null || invitationSequence.getModule().isEmpty()) {
            return null;
        }
        String moduleIdentifier = invitationSequence.getModule().get(0).getIdentifier();
        if (moduleIdentifier == null) {
            return null;
        }
        MailNonceAuthenticationModuleType nonceModule = securityPolicy
                .getAuthentication()
                .getModules()
                .getMailNonce()
                .stream()
                .filter(m -> moduleIdentifier.equals(m.getIdentifier()))
                .findFirst()
                .orElse(null);
        if (nonceModule == null) {
            return null;
        }
        String credentialName = nonceModule.getCredentialName();
        if (credentialName == null) {
            return null;
        }
        return securityPolicy
                .getCredentials()
                .getNonce()
                .stream()
                .filter(n -> credentialName.equals(n.getName()))
                .findFirst()
                .orElse(null);
    }

}
