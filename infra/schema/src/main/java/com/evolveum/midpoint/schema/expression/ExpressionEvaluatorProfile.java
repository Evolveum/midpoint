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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.AccessDecision;

/**
 * @author Radovan Semancik
 *
 */
public class ExpressionEvaluatorProfile {
	
	private final QName type;
	private AccessDecision decision;
	private final List<ScriptExpressionProfile> scritpProfiles = new ArrayList<>();

	public ExpressionEvaluatorProfile(QName type) {
		this.type = type;
	}

	public QName getType() {
		return type;
	}

	public AccessDecision getDecision() {
		return decision;
	}

	public void setDecision(AccessDecision decision) {
		this.decision = decision;
	}
	
	public void add(ScriptExpressionProfile scriptProfile) {
		scritpProfiles.add(scriptProfile);
	}
	
	public ScriptExpressionProfile getScriptExpressionProfile(String language) {
		for(ScriptExpressionProfile scritpProfile : scritpProfiles) {
			if (language.equals(scritpProfile.getLanguage())) {
				return scritpProfile;
			}
		}
		return null;
	}
	

}
