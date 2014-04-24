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
package com.evolveum.midpoint.security.api;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;

public class ObjectSecurityConstraints {
	
	private Map<ItemPath, ItemSecurityConstraints> itemConstraintMap = new HashMap<>();
	private Map<String, AuthorizationDecisionType> actionDecisionMap = new HashMap<>();
		
	public Map<ItemPath, ItemSecurityConstraints> getItemConstraintMap() {
		return itemConstraintMap;
	}

	/**
	 * Specifies decisions applicable to the entire object (regardless of any specific items)
	 */
	public Map<String, AuthorizationDecisionType> getActionDecisionMap() {
		return actionDecisionMap;
	}
	
	public AuthorizationDecisionType getActionDecistion(String actionUrl) {
		AuthorizationDecisionType actionDecision = actionDecisionMap.get(actionUrl);
		AuthorizationDecisionType allDecision = actionDecisionMap.get(AuthorizationConstants.AUTZ_ALL_URL);
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
	
	public AuthorizationDecisionType findItemDecision(ItemPath itemPath, String actionUrl) {
		// TODO: loop to match possible wildcards
		ItemSecurityConstraints itemSecurityConstraints = itemConstraintMap.get(itemPath);
		if (itemSecurityConstraints == null) {
			return null;
		}
		AuthorizationDecisionType actionDecision = itemSecurityConstraints.getActionDecisionMap().get(actionUrl);
		AuthorizationDecisionType allDecision = itemSecurityConstraints.getActionDecisionMap().get(AuthorizationConstants.AUTZ_ALL_URL);
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
	
}
