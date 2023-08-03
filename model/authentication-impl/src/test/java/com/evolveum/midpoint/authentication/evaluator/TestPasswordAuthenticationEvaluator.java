/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.authentication.api.evaluator.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

public class TestPasswordAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<String, PasswordAuthenticationContext, AuthenticationEvaluator<PasswordAuthenticationContext, UsernamePasswordAuthenticationToken>> {

    @Autowired
    private AuthenticationEvaluator<PasswordAuthenticationContext, UsernamePasswordAuthenticationToken> passwordAuthenticationEvaluator;

    @Override
    public AuthenticationEvaluator<PasswordAuthenticationContext, UsernamePasswordAuthenticationToken> getAuthenticationEvaluator() {
        return passwordAuthenticationEvaluator;
    }

    @Override
    public PasswordAuthenticationContext getAuthenticationContext(
            String username, String value, List<ObjectReferenceType> requiredAssignments) {
        return new PasswordAuthenticationContext(username, value, UserType.class, requiredAssignments, null);
    }

    @Override
    public String getGoodPasswordJack() {
        return USER_JACK_PASSWORD;
    }

    @Override
    public String getBadPasswordJack() {
        return "this IS NOT myPassword!";
    }

    @Override
    public String getGoodPasswordGuybrush() {
        return USER_GUYBRUSH_PASSWORD;
    }

    @Override
    public String getBadPasswordGuybrush() {
        return "thisIsNotMyPassword";
    }

    @Override
    public String get103EmptyPasswordJack() {
        return "";
    }

    @Override
    public AbstractCredentialType getCredentialUsedForAuthentication(UserType user) {
        return user.getCredentials().getPassword();
    }

    @Override
    public String getModuleIdentifier() {
        return "basicLogin";
    }

    @Override
    public String getSequenceIdentifier() {
        return "default-gui-login";
    }

    private ProtectedStringType getGuybrushPassword() {
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setClearValue(USER_GUYBRUSH_PASSWORD);
        return protectedString;
    }

    @Override
    public void modifyUserCredential(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {

        modifyUserReplace(USER_GUYBRUSH_OID, SchemaConstants.PATH_PASSWORD_VALUE, task, result, getGuybrushPassword());
    }

    @Override
    public QName getCredentialType() {
        return CredentialsType.F_PASSWORD;
    }

    @Override
    public String getEmptyPasswordExceptionMessageKey(){
        return "web.security.provider.invalid.credentials";
    }

}
