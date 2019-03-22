/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;

/**
 * @author semancik
 *
 */
public class ScriptExpressionProfile {
	
	private final String language;
	private AccessDecision decision;
	private Boolean typeChecking;
	private ExpressionPermissionProfile permissionProfile;
	
	public ScriptExpressionProfile(String language) {
		super();
		this.language = language;
	}
	
	public String getLanguage() {
		return language;
	}

	public AccessDecision getDecision() {
		return decision;
	}
	
	public void setDecision(AccessDecision decision) {
		this.decision = decision;
	}
	
	public Boolean isTypeChecking() {
		return typeChecking;
	}

	public void setTypeChecking(Boolean typeChecking) {
		this.typeChecking = typeChecking;
	}

	public ExpressionPermissionProfile getPermissionProfile() {
		return permissionProfile;
	}

	public void setPermissionProfile(ExpressionPermissionProfile permissionProfile) {
		this.permissionProfile = permissionProfile;
	}

	public boolean hasRestrictions() {
		if (permissionProfile == null) {
			return false;
		}
		return permissionProfile.hasRestrictions();
	}

	public AccessDecision decideClassAccess(String className, String methodName) {
		if (permissionProfile == null) {
			return decision;
		}
		AccessDecision permissionDecision = permissionProfile.decideClassAccess(className, methodName);
		if (permissionDecision == null || permissionDecision == AccessDecision.DEFAULT) {
			return decision;
		}
		return permissionDecision;
	}

	
}
