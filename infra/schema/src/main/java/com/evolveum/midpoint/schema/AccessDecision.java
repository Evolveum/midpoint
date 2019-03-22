/**
 * Copyright (c) 2017-2019 Evolveum
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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;

/**
 * Decision about access to something. Used as an output of authorization processing code. But may be also used
 * for other things, such as decisions to access classes and methods in sandboxes.
 * 
 * @author Radovan Semancik
 */
public enum AccessDecision {
	
	/**
	 * Access explicitly allowed.
	 */
	ALLOW(AuthorizationDecisionType.ALLOW),
	
	/**
	 * Access explicitly denied.
	 */
	DENY(AuthorizationDecisionType.DENY),
	
	/**
	 * Means "no decision" or "not allowed yet".
	 */
	DEFAULT(null);

	private final AuthorizationDecisionType authorizationDecisionType;

	private AccessDecision(AuthorizationDecisionType authorizationDecisionType) {
		this.authorizationDecisionType = authorizationDecisionType;
	}

	public AuthorizationDecisionType getAuthorizationDecisionType() {
		return authorizationDecisionType;
	}
	
	public static AccessDecision combine(AccessDecision oldDecision, AccessDecision newDecision) {
		if (oldDecision == null && newDecision == null) {
			return null;
		}
		if (oldDecision == null && newDecision != null) {
			return newDecision;
		}
		if (oldDecision != null && newDecision == null) {
			return oldDecision;
		}
		if (oldDecision == DENY || newDecision == DENY) {
			return DENY;
		}
		if (oldDecision == DEFAULT || newDecision == DEFAULT) {
			return DEFAULT;
		}
		if (oldDecision == ALLOW || newDecision == ALLOW) {
			return ALLOW;
		}
		throw new IllegalStateException("Unexpected combine "+oldDecision+"+"+newDecision);
	}
	
	public static AccessDecision translate(AuthorizationDecisionType authorizationDecisionType) {
		if (authorizationDecisionType == null) {
			return AccessDecision.DEFAULT;
		}
		switch (authorizationDecisionType) {
			case ALLOW:
				return ALLOW;
			case DENY:
				return DENY;
			default:
				throw new IllegalStateException("Unknown AuthorizationDecisionType "+authorizationDecisionType);
		}
	}
	
}
