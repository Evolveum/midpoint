/**
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.security.impl;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

public class ObjectSecurityConstraintsImpl implements ObjectSecurityConstraints {
	
	private Map<ItemPath, ItemSecurityConstraintsImpl> itemConstraintMap = new HashMap<>();
	private Map<String, PhaseDecisionImpl> actionDecisionMap = new HashMap<>();
		
	public Map<ItemPath, ItemSecurityConstraintsImpl> getItemConstraintMap() {
		return itemConstraintMap;
	}

	/**
	 * Specifies decisions applicable to the entire object (regardless of any specific items)
	 */
	public Map<String, PhaseDecisionImpl> getActionDecisionMap() {
		return actionDecisionMap;
	}
	
	@Override
	public AuthorizationDecisionType getActionDecistion(String actionUrl, AuthorizationPhaseType phase) {
		AuthorizationDecisionType actionDecision = getSimpleActionDecision(actionDecisionMap, actionUrl, phase);
		AuthorizationDecisionType allDecision = getSimpleActionDecision(actionDecisionMap, AuthorizationConstants.AUTZ_ALL_URL, phase);
		if (actionDecision == null && allDecision == null) {
			return null;
		}
		if (actionDecision == AuthorizationDecisionType.DENY || allDecision == AuthorizationDecisionType.DENY) {
			return AuthorizationDecisionType.DENY;
		}
		if (actionDecision != null) {
			return actionDecision;
		}
		return allDecision;
	}
	
	private AuthorizationDecisionType getSimpleActionDecision(Map<String, PhaseDecisionImpl> actionDecisionMap, String actionUrl, AuthorizationPhaseType phase) {
		PhaseDecisionImpl phaseDecision = actionDecisionMap.get(actionUrl);
		if (phaseDecision == null) {
			return null;
		}
		if (phase == AuthorizationPhaseType.REQUEST) {
			return phaseDecision.getRequestDecision();
		} else if (phase == AuthorizationPhaseType.EXECUTION) {
			return phaseDecision.getExecDecision();
		} else if (phase == null) {
			if (phaseDecision.getRequestDecision() == null && phaseDecision.getExecDecision() == null) {
				return null;
			}
			if (phaseDecision.getRequestDecision() == AuthorizationDecisionType.DENY || 
					phaseDecision.getExecDecision() == AuthorizationDecisionType.DENY) {
				return AuthorizationDecisionType.DENY;
			}
			if (phaseDecision.getRequestDecision() == null || phaseDecision.getExecDecision() == null) {
				return null;
			}
			return AuthorizationDecisionType.ALLOW;
		} else {
			throw new IllegalArgumentException("Unexpected phase "+phase);
		}
	}

	public AuthorizationDecisionType findItemDecision(ItemPath itemPath, String actionUrl, AuthorizationPhaseType phase) {
		// TODO: loop to match possible wildcards
		ItemSecurityConstraintsImpl itemSecurityConstraints = itemConstraintMap.get(itemPath);
		if (itemSecurityConstraints == null) {
			return null;
		}
		AuthorizationDecisionType actionDecision = getSimpleActionDecision(itemSecurityConstraints.getActionDecisionMap(), actionUrl, phase);
		AuthorizationDecisionType allDecision = getSimpleActionDecision(itemSecurityConstraints.getActionDecisionMap(), 
				AuthorizationConstants.AUTZ_ALL_URL, phase);
		if (actionDecision == null && allDecision == null) {
			return null;
		}
		if (actionDecision == AuthorizationDecisionType.DENY || allDecision == AuthorizationDecisionType.DENY) {
			return AuthorizationDecisionType.DENY;
		}
		if (actionDecision != null) {
			return actionDecision;
		}
		return allDecision;
	}

	@Override
	public boolean hasNoItemDecisions() {
		return itemConstraintMap.isEmpty();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ObjectSecurityConstraintsImpl");
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "itemConstraintMap", itemConstraintMap, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "actionDecisionMap", actionDecisionMap, indent+1);
		return sb.toString();
	}
	
}
