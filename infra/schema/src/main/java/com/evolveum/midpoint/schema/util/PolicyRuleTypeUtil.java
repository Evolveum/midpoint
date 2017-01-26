/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class PolicyRuleTypeUtil {

	private static Map<String, String> CONSTRAINT_NAMES = new HashMap<>();
	static {
		CONSTRAINT_NAMES.put(ExclusionPolicyConstraintType.class.getName(), "exc");
		CONSTRAINT_NAMES.put(MultiplicityPolicyConstraintType.class.getName(), "multi");
		CONSTRAINT_NAMES.put(ModificationPolicyConstraintType.class.getName(), "mod");
		CONSTRAINT_NAMES.put(AssignmentPolicyConstraintType.class.getName(), "assign");
		CONSTRAINT_NAMES.put(PolicySituationPolicyConstraintType.class.getName(), "sit");
	}

	public static String getConstraintClassShortcut(Class<?> clazz) {
		String shortcut = CONSTRAINT_NAMES.get(clazz.getName());
		return shortcut != null ? shortcut : clazz.getSimpleName();
	}

	public static String toShortString(PolicyConstraintsType constraints) {
		if (constraints == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		constraints.getExclusion().forEach(excl -> sb.append("exc "));
		constraints.getMinAssignees().forEach(min -> sb.append("min "));
		constraints.getMaxAssignees().forEach(max -> sb.append("max "));
		constraints.getModification().forEach(mod -> sb.append("mod "));
		constraints.getAssignment().forEach(assign -> sb.append("assign "));
		constraints.getSituation().forEach(assign -> sb.append("sit "));
		return sb.toString().trim();
	}

	public static String toShortString(PolicyActionsType actions) {
		if (actions == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		if (actions.getEnforcement() != null) {
			sb.append("enforce ");
		}
		if (actions.getApproval() != null) {
			sb.append("approve ");
		}
		if (actions.getRemediation() != null) {
			sb.append("remedy ");
		}
		if (actions.getCertification() != null) {
			sb.append("certify ");
		}
		if (actions.getNotification() != null) {
			sb.append("notify ");
		}
		return sb.toString().trim();
	}

	public static String toShortString(AbstractPolicyConstraintType constraint) {
		if (constraint == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(getConstraintClassShortcut(constraint.getClass()));
		if (constraint.getName() != null) {
			sb.append(":").append(constraint.getName());
		}
		return sb.toString();
	}

	public static String toDiagShortcut(PolicyConstraintKindType constraintKind) {
		if (constraintKind == null) {
			return "null";
		}
		switch (constraintKind) {
			case ASSIGNMENT: return "assign";
			case EXCLUSION: return "exc";
			case MAX_ASSIGNEES: return "max";
			case MIN_ASSIGNEES: return "min";
			case MODIFICATION: return "mod";
			case SITUATION: return "sit";
			default: return constraintKind.toString();
		}
	}
}
