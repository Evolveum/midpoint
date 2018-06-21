/**
 * Copyright (c) 2014-2018 Evolveum
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
package com.evolveum.midpoint.security.enforcer.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

public class ObjectSecurityConstraintsImpl implements ObjectSecurityConstraints {

	private Map<String, PhasedConstraints> actionMap = new HashMap<>();
	
	public void applyAuthorization(Authorization autz) {
		List<String> actions = autz.getAction();
		AuthorizationPhaseType phase = autz.getPhase();
		AuthorizationDecisionType decision = autz.getDecision();
		if (decision == null) {
			decision = AuthorizationDecisionType.ALLOW;
		}
		for (String action: actions) {
			if (phase == null) {
				getOrCreateItemConstraints(action, AuthorizationPhaseType.REQUEST).collectItems(autz);
				getOrCreateItemConstraints(action, AuthorizationPhaseType.EXECUTION).collectItems(autz);
			} else {
				getOrCreateItemConstraints(action, phase).collectItems(autz);
			}
		}
	}
	
	private ItemSecurityConstraintsImpl getOrCreateItemConstraints(String action, AuthorizationPhaseType phase) {
		PhasedConstraints phasedConstraints = actionMap.computeIfAbsent(action, k -> new PhasedConstraints());
		return phasedConstraints.get(phase);
	}
	
	private ItemSecurityConstraintsImpl getItemConstraints(String action, AuthorizationPhaseType phase) {
		PhasedConstraints phasedConstraints = actionMap.get(action);
		if (phasedConstraints == null) {
			return null;
		}
		return phasedConstraints.get(phase);
	}
	
	@Override
	public AuthorizationDecisionType getActionDecision(String actionUrl, AuthorizationPhaseType phase) {
		return findAllItemsDecision(actionUrl, phase);
	}

	@Override
	public AuthorizationDecisionType findAllItemsDecision(String actionUrl, AuthorizationPhaseType phase) {
		if (phase == null) {
			AuthorizationDecisionType requestDecision = getActionDecisionPhase(actionUrl, AuthorizationPhaseType.REQUEST);
			if (requestDecision == null || AuthorizationDecisionType.DENY.equals(requestDecision)) {
				return requestDecision;
			}
			return getActionDecisionPhase(actionUrl, AuthorizationPhaseType.EXECUTION);
		} else {
			return getActionDecisionPhase(actionUrl, phase);
		}
	}
	
	public AuthorizationDecisionType getActionDecisionPhase(String actionUrl, AuthorizationPhaseType phase) {
		ItemSecurityConstraintsImpl itemConstraints = getItemConstraints(actionUrl, phase);
		if (itemConstraints == null) {
			return null;
		}
		AutzItemPaths deniedItems = itemConstraints.getDeniedItems();
		if (deniedItems.isAllItems()) {
			return AuthorizationDecisionType.DENY;
		}
		AutzItemPaths allowedItems = itemConstraints.getAllowedItems();
		if (allowedItems.isAllItems()) {
			return AuthorizationDecisionType.ALLOW;
		}
		return null;
	}

	@Override
	public AuthorizationDecisionType findItemDecision(ItemPath nameOnlyItemPath, String actionUrl, AuthorizationPhaseType phase) {
		if (phase == null) {
			AuthorizationDecisionType requestDecision = findItemDecisionPhase(nameOnlyItemPath, actionUrl, AuthorizationPhaseType.REQUEST);
			if (requestDecision == null || AuthorizationDecisionType.DENY.equals(requestDecision)) {
				return requestDecision;
			}
			return findItemDecisionPhase(nameOnlyItemPath, actionUrl, AuthorizationPhaseType.EXECUTION);
		} else {
			return findItemDecisionPhase(nameOnlyItemPath, actionUrl, phase);
		}
	}
	
	public AuthorizationDecisionType findItemDecisionPhase(ItemPath nameOnlyItemPath, String actionUrl, AuthorizationPhaseType phase) {
		ItemSecurityConstraintsImpl itemConstraints = getItemConstraints(actionUrl, phase);
		AuthorizationDecisionType decision = null;
		if (itemConstraints != null) {
			decision = itemConstraints.findItemDecision(nameOnlyItemPath);
			if (AuthorizationDecisionType.DENY.equals(decision)) {
				return AuthorizationDecisionType.DENY;
			}
		}
		ItemSecurityConstraintsImpl itemConstraintsActionAll = getItemConstraints(AuthorizationConstants.AUTZ_ALL_URL, phase);
		if (itemConstraintsActionAll == null) {
			return decision;
		}
		AuthorizationDecisionType decisionActionAll = itemConstraintsActionAll.findItemDecision(nameOnlyItemPath);
		if (AuthorizationDecisionType.DENY.equals(decisionActionAll)) {
			return AuthorizationDecisionType.DENY;
		}
		if (AuthorizationDecisionType.ALLOW.equals(decisionActionAll)) {
			return AuthorizationDecisionType.ALLOW;
		}
		return decision;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ObjectSecurityConstraintsImpl.class, indent);
		DebugUtil.debugDumpWithLabel(sb, "actionMap", actionMap, indent+1);
		return sb.toString();
	}

}
