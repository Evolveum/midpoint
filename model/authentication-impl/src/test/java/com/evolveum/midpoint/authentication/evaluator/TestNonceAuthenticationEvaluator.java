/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.evaluator;

import com.evolveum.midpoint.authentication.api.evaluator.context.NonceAuthenticationContext;
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.xml.namespace.QName;
import java.util.List;

public class TestNonceAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<String, NonceAuthenticationContext, AuthenticationEvaluator<NonceAuthenticationContext, UsernamePasswordAuthenticationToken>> {

    private static final String USER_JACK_NONCE = "asdfghjkl123456";
    private static final String USER_GUYBRUSH_NONCE = "asdfghjkl654321";

    @Autowired
    private AuthenticationEvaluator<NonceAuthenticationContext, UsernamePasswordAuthenticationToken> nonceAuthenticationEvaluator;

    @Override
    public AuthenticationEvaluator<NonceAuthenticationContext, UsernamePasswordAuthenticationToken> getAuthenticationEvaluator() {
        return nonceAuthenticationEvaluator;
    }

    @Override
    public NonceAuthenticationContext getAuthenticationContext(
            String username, String value, List<ObjectReferenceType> requiredAssignments) {
        return new NonceAuthenticationContext(username, UserType.class, value, null, requiredAssignments, null);
    }

    @Override
    public String getGoodPasswordJack() {
        return USER_JACK_NONCE;
    }

    @Override
    public String getBadPasswordJack() {
        return "BAD1bad_Bad#Token";
    }

    @Override
    public String getGoodPasswordGuybrush() {
        return USER_GUYBRUSH_NONCE;
    }

    @Override
    public String getBadPasswordGuybrush() {
        return "BAD1bad_Bad#Token";
    }

    @Override
    public String get103EmptyPasswordJack() {
        return "";
    }

    @Override
    public AbstractCredentialType getCredentialUsedForAuthentication(UserType user) {
        return user.getCredentials().getNonce();
    }

    @Override
    public String getModuleIdentifier() {
        return "MailNonce";
    }

    @Override
    public String getSequenceIdentifier() {
        return "default-nonce";
    }

    private ProtectedStringType getGuybrushNonce() {
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setClearValue(USER_GUYBRUSH_NONCE);
        return protectedString;
    }

    @Override
    public void modifyUserCredential(Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        modifyUserReplace(USER_GUYBRUSH_OID, SchemaConstants.PATH_NONCE_VALUE, task, result, getGuybrushNonce());

    }

    @Override
    public QName getCredentialType() {
        return CredentialsType.F_NONCE;
    }

    @Override
    public String getEmptyPasswordExceptionMessageKey(){
        return "web.security.provider.nonce.bad";
    }
}
