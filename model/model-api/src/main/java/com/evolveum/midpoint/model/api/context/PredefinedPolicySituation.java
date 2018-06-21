/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

/**
 * @author semancik
 *
 */
public enum PredefinedPolicySituation {

	EXCLUSION_VIOLATION(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION, PolicyConstraintKindType.EXCLUSION),

	UNDERASSIGNED(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED, PolicyConstraintKindType.MIN_ASSIGNEES_VIOLATION),

	OVERASSIGNED(SchemaConstants.MODEL_POLICY_SITUATION_OVERASSIGNED, PolicyConstraintKindType.MAX_ASSIGNEES_VIOLATION),

	MODIFIED(SchemaConstants.MODEL_POLICY_SITUATION_MODIFIED, PolicyConstraintKindType.OBJECT_MODIFICATION),

	ASSIGNMENT_MODIFIED(SchemaConstants.MODEL_POLICY_SITUATION_ASSIGNMENT_MODIFIED, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION),

	HAS_ASSIGNMENT(SchemaConstants.MODEL_POLICY_SITUATION_HAS_ASSIGNMENT, PolicyConstraintKindType.HAS_ASSIGNMENT),

	HAS_NO_ASSIGNMENT(SchemaConstants.MODEL_POLICY_SITUATION_HAS_NO_ASSIGNMENT, PolicyConstraintKindType.HAS_NO_ASSIGNMENT),

	OBJECT_STATE(SchemaConstants.MODEL_POLICY_SITUATION_OBJECT_STATE, PolicyConstraintKindType.OBJECT_STATE),

	ASSIGNMENT_STATE(SchemaConstants.MODEL_POLICY_SITUATION_ASSIGNMENT_STATE, PolicyConstraintKindType.ASSIGNMENT_STATE),

	OBJECT_TIME_VALIDITY(SchemaConstants.MODEL_POLICY_SITUATION_TIME_VALIDITY, PolicyConstraintKindType.OBJECT_TIME_VALIDITY),

	ASSIGNMENT_TIME_VALIDITY(SchemaConstants.MODEL_POLICY_SITUATION_TIME_VALIDITY, PolicyConstraintKindType.ASSIGNMENT_TIME_VALIDITY);

	private String url;
	private PolicyConstraintKindType constraintKind;

	PredefinedPolicySituation(String url, PolicyConstraintKindType constraintKind) {
		this.url = url;
		this.constraintKind = constraintKind;
	}

	public String getUrl() {
		return url;
	}

	public PolicyConstraintKindType getConstraintKind() {
		return constraintKind;
	}

	public static PredefinedPolicySituation get(PolicyConstraintKindType constraintKind) {
		for (PredefinedPolicySituation val: PredefinedPolicySituation.values()) {
			if (val.getConstraintKind() == constraintKind) {
				return val;
			}
		}
		return null;
	}

}
