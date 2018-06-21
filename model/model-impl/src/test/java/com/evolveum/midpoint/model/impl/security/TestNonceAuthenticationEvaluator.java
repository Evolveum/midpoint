package com.evolveum.midpoint.model.impl.security;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.NonceAuthenticationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestNonceAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<String, NonceAuthenticationContext, AuthenticationEvaluator<NonceAuthenticationContext>>{

	private static final String USER_JACK_NONCE = "asdfghjkl123456";
	private static final String USER_GUYBRUSH_NONCE = "asdfghjkl654321";

	@Autowired(required=true)
	private AuthenticationEvaluator<NonceAuthenticationContext> nonceAuthenticationEvaluator;

	@Override
	public AuthenticationEvaluator<NonceAuthenticationContext> getAuthenticationEvaluator() {
		return nonceAuthenticationEvaluator;
	}

	@Override
	public NonceAuthenticationContext getAuthenticationContext(String username, String value) {
		return new NonceAuthenticationContext(username, value, null);
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

}
