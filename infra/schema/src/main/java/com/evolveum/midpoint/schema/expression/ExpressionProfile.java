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

/**
 * NOTE: This is pretty much throw-away implementation. Just the interface is important now.
 * 
 * @author Radovan Semancik
 *
 */
public class ExpressionProfile {
	
	private final String name;
	private final List<Rule> classAccessRules = new ArrayList<>();
	private AccessDecision defaultClassAccessDecision = AccessDecision.DEFAULT;
	
	public ExpressionProfile(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public AccessDecision getDefaultClassAccessDecision() {
		return defaultClassAccessDecision;
	}

	public void setDefaultClassAccessDecision(AccessDecision defaultClassAccessDecision) {
		this.defaultClassAccessDecision = defaultClassAccessDecision;
	}

	public AccessDecision decideClassAccess(String className, String methodName) {
		for (Rule rule : classAccessRules) {
			if (rule.match(className, methodName)) {
				return rule.getDecision();
			}
		}
		return defaultClassAccessDecision;
	}

	/**
	 * Used to easily set up access for built-in class access rules.
	 */
	public void addClassAccessRule(String className, String methodName, AccessDecision decision) {
		classAccessRules.add(new Rule(className, methodName, decision));
	}
	
	/**
	 * Used to easily set up access for built-in class access rules (convenience).
	 */
	public void addClassAccessRule(Class<?> clazz, String methodName, AccessDecision decision) {
		addClassAccessRule(clazz.getName(), methodName, decision);
	}
	
	class Rule {
		private final String className;
		private final String methodName;
		private final AccessDecision decision;
		
		public Rule(String className, String methodName, AccessDecision decision) {
			super();
			this.className = className;
			this.methodName = methodName;
			this.decision = decision;
		}

		public String getClassName() {
			return className;
		}

		public String getMethodName() {
			return methodName;
		}

		public AccessDecision getDecision() {
			return decision;
		}
		
		public boolean match(String aClassName, String aMethodName) {
			if (!aClassName.equals(className)) {
				return false;
			}
			if (methodName == null) {
				return true;
			}
			return methodName.equals(aMethodName);
		}
		
	}

	@Override
	public String toString() {
		return "ExpressionProfile(" + name + ")";
	}

}
