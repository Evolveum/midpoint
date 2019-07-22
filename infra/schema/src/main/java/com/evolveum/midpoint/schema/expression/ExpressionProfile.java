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
import com.evolveum.midpoint.util.QNameUtil;

/**
 * NOTE: This is pretty much throw-away implementation. Just the interface is important now.
 * 
 * @author Radovan Semancik
 *
 */
public class ExpressionProfile { // TODO: DebugDumpable
	
	private final String identifier;
	private AccessDecision decision;
	private final List<ExpressionEvaluatorProfile> evaluatorProfiles = new ArrayList<>();
	
	public ExpressionProfile(String identifier) {
		super();
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}
	
	public AccessDecision getDecision() {
		return decision;
	}

	public void setDecision(AccessDecision defaultDecision) {
		this.decision = defaultDecision;
	}
	
	public void add(ExpressionEvaluatorProfile evaluatorProfile) {
		evaluatorProfiles.add(evaluatorProfile);
	}
	
	public ExpressionEvaluatorProfile getEvaluatorProfile(QName type) {
		for (ExpressionEvaluatorProfile evaluatorProfile : evaluatorProfiles) {
			if (QNameUtil.match(evaluatorProfile.getType(), type)) {
				return evaluatorProfile;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "ExpressionProfile(" + identifier + ")";
	}



}
