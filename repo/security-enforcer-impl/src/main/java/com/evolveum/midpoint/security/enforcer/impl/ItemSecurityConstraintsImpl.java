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
package com.evolveum.midpoint.security.enforcer.impl;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class ItemSecurityConstraintsImpl implements DebugDumpable {

	private Map<String, PhaseDecisionImpl> actionDecisionMap = new HashMap<>();

	public Map<String, PhaseDecisionImpl> getActionDecisionMap() {
		return actionDecisionMap;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ItemSecurityConstraintsImpl");
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "actionDecisionMap", actionDecisionMap, indent+1);
		return sb.toString();
	}

}
