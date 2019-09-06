/**
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class PasswordPolicyEvaluator extends CredentialPolicyEvaluator<PasswordType,PasswordCredentialsPolicyType> {

	private static final ItemPath PASSWORD_CONTAINER_PATH = ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD);

	@Override
	public ItemPath getCredentialsContainerPath() {
		return PASSWORD_CONTAINER_PATH;
	}

	@Override
	protected String getCredentialHumanReadableName() {
		return "password";
	}

	@Override
	protected String getCredentialHumanReadableKey() {
		return "password";
	}

	@Override
	protected boolean supportsHistory() {
		return true;
	}


	@Override
	protected PasswordCredentialsPolicyType determineEffectiveCredentialPolicy() {
		return SecurityUtil.getEffectivePasswordCredentialsPolicy(getSecurityPolicy());
	}


}
