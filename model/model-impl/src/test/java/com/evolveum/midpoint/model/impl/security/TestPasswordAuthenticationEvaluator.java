package com.evolveum.midpoint.model.impl.security;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestPasswordAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<String, PasswordAuthenticationContext, AuthenticationEvaluator<PasswordAuthenticationContext>>{

	@Autowired(required=true)
	private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;
	
	@Override
	public AuthenticationEvaluator<PasswordAuthenticationContext> getAuthenticationEvaluator() {
		return passwordAuthenticationEvaluator;
	}

	@Override
	public PasswordAuthenticationContext getAuthenticationContext(String username, String value) {
		return new PasswordAuthenticationContext(username, value);
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
	
	private ProtectedStringType getGuybrushPassword() {
		ProtectedStringType protectedString = new ProtectedStringType();
		protectedString.setClearValue(USER_GUYBRUSH_PASSWORD);
		return protectedString;
	}

	@Override
	public void modifyUserCredential(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserReplace(USER_GUYBRUSH_OID, SchemaConstants.PATH_PASSWORD_VALUE, task, result, getGuybrushPassword());
	}

	@Override
	public QName getCredentialType() {
		return CredentialsType.F_PASSWORD;
	}

}
