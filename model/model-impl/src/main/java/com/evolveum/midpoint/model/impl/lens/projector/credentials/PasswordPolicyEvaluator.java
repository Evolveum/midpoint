/**
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	private static final ItemPath PASSWORD_CONTAINER_PATH = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD);

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
