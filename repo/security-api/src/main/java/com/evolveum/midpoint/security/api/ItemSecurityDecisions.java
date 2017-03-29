/**
 * Copyright (c) 2017 Evolveum
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;

/**
 * @author semancik
 *
 */
public class ItemSecurityDecisions implements DebugDumpable, Serializable {
	
	private AuthorizationDecisionType defaultDecision = null;
	private Map<ItemPath,AuthorizationDecisionType> itemDecisionMap = new HashMap<>();
	
	public AuthorizationDecisionType getDefaultDecision() {
		return defaultDecision;
	}
	
	public void setDefaultDecision(AuthorizationDecisionType defaultDecision) {
		this.defaultDecision = defaultDecision;
	}
	
	public Map<ItemPath, AuthorizationDecisionType> getItemDecisionMap() {
		return itemDecisionMap;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "ItemSecurityDecisions", indent);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "defaultDecision", defaultDecision, indent + 1);
		DebugUtil.debugDumpLabelLn(sb, "itemDecisionMap", indent + 1);
		DebugUtil.debugDumpMapMultiLine(sb, itemDecisionMap, indent + 2);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "ItemSecurityDecisions(defaultDecision=" + defaultDecision + ", itemDecisionMap=" + itemDecisionMap
				+ ")";
	}
	
}
