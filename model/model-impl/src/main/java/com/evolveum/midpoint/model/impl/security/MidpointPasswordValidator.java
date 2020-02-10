/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.model.api.authentication.FocusProfileService;
import com.evolveum.midpoint.model.api.authentication.MidPointFocusProfilePrincipal;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MidpointPasswordValidator extends UsernameTokenValidator {

    @Autowired private PasswordAuthenticationEvaluatorImpl passwdEvaluator;
    @Autowired private FocusProfileService userService;

    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        try {
            Credential credentialToReturn = super.validate(credential, data);
            recordAuthenticationSuccess(credential);
            return credentialToReturn;
        } catch (WSSecurityException ex) {
            recordAuthenticatonError(credential, ex);
            throw ex;
        }
    }

    private void recordAuthenticationSuccess(Credential credential) throws  WSSecurityException {
        MidPointPrincipal principal = resolveMidpointPrincipal(credential);
        ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        passwdEvaluator.recordPasswordAuthenticationSuccess(principal, connEnv, resolvePassowrd(principal));
    }

    private void recordAuthenticatonError(Credential credential, WSSecurityException originEx) throws WSSecurityException {


        MidPointPrincipal principal = resolveMidpointPrincipal(credential);

        PasswordType passwordType = resolvePassowrd(principal);

        ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_WEB_SERVICE_URI);

        PasswordCredentialsPolicyType passwdPolicy = null;

        if (principal.getApplicableSecurityPolicy() != null) {
            CredentialsPolicyType credentialsPolicyType = principal.getApplicableSecurityPolicy().getCredentials();
             passwdPolicy = credentialsPolicyType.getPassword();
        }

        passwdEvaluator.recordPasswordAuthenticationFailure(principal, connEnv, passwordType, passwdPolicy, originEx.getMessage());
    }

    private MidPointPrincipal resolveMidpointPrincipal(Credential credential) throws  WSSecurityException {
        UsernameToken usernameToken = credential.getUsernametoken();
        String username = usernameToken.getName();

        MidPointFocusProfilePrincipal principal = null;
        try {
            principal = userService.getPrincipal(username, UserType.class);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {

            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e);
        }

        return principal;
    }

    private PasswordType resolvePassowrd(MidPointPrincipal principal) {
        FocusType user = principal.getFocus();
        PasswordType passwordType = null;
        if (user.getCredentials() != null) {
            passwordType = user.getCredentials().getPassword();
        }

        return passwordType;
    }
}
