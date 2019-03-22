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

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionClassProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionMethodProfileType;

/**
 * Compiled expression permission profile.
 * Compiled from ExpressionPermissionProfileType.
 * 
 * @author Radovan Semancik
 */
public class ExpressionPermissionProfile {
	
	private final String identifier;
	private AccessDecision decision;
	private List<ExpressionPermissionClassProfileType> classProfiles = new ArrayList<>();

	public ExpressionPermissionProfile(String identifier) {
		super();
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public AccessDecision getDecision() {
		return decision;
	}

	public void setDecision(AccessDecision decision) {
		this.decision = decision; 
	}

	public List<ExpressionPermissionClassProfileType> getClassProfiles() {
		return classProfiles;
	}

	public boolean hasRestrictions() {
		return !classProfiles.isEmpty();
	}

	public AccessDecision decideClassAccess(String className, String methodName) {
		ExpressionPermissionClassProfileType classProfile = getClassProfile(className);
		if (classProfile == null) {
			return decision;
		}
		ExpressionPermissionMethodProfileType methodProfile = getMethodProfile(classProfile, methodName);
		if (methodProfile == null) {
			return AccessDecision.translate(classProfile.getDecision());
		} else {
			return AccessDecision.translate(methodProfile.getDecision());
		}
	}
	
	private ExpressionPermissionClassProfileType getClassProfile(String className) {
		for (ExpressionPermissionClassProfileType classProfile : classProfiles) {
			if (className.equals(classProfile.getName())) {
				return classProfile;
			}
		}
		return null;
	}
	
	private void add(ExpressionPermissionClassProfileType classProfile) {
		classProfiles.add(classProfile);
	}
	
	private ExpressionPermissionMethodProfileType getMethodProfile(ExpressionPermissionClassProfileType classProfile, String methodName) {
		if (methodName == null) {
			return null;
		}
		for (ExpressionPermissionMethodProfileType methodProfile : classProfile.getMethod()) {
			if (methodName.equals(methodProfile.getName())) {
				return methodProfile;
			}
		}
		return null;
	}
	
	/**
	 * Used to easily set up access for built-in class access rules.
	 */
	public void addClassAccessRule(String className, String methodName, AccessDecision decision) {
		ExpressionPermissionClassProfileType classProfile = getClassProfile(className);
		if (classProfile == null) {
			classProfile = new ExpressionPermissionClassProfileType();
			classProfile.setName(className);
			add(classProfile);
		}
		if (methodName == null) {
			classProfile.setDecision(decision.getAuthorizationDecisionType());
		} else {
			ExpressionPermissionMethodProfileType methodProfile = getMethodProfile(classProfile, methodName);
			if (methodProfile == null) {
				methodProfile = new ExpressionPermissionMethodProfileType();
				methodProfile.setName(methodName);
				methodProfile.setDecision(decision.getAuthorizationDecisionType());
				classProfile.getMethod().add(methodProfile);
			} else {
				methodProfile.setDecision(decision.getAuthorizationDecisionType());
			}
		}

	}
	

	/**
	 * Used to easily set up access for built-in class access rules (convenience).
	 */
	public void addClassAccessRule(Class<?> clazz, String methodName, AccessDecision decision) {
		addClassAccessRule(clazz.getName(), methodName, decision);
	}
	
	public void addClassAccessRule(Class<?> clazz, AccessDecision decision) {
		addClassAccessRule(clazz.getName(), null, decision);
	}
}
