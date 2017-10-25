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

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;

/**
 * @author semancik
 *
 */
public class PhaseDecisionImpl implements DebugDumpable {

	private AuthorizationDecisionType requestDecision;
	private AuthorizationDecisionType execDecision;

	public AuthorizationDecisionType getRequestDecision() {
		return requestDecision;
	}

	public void setRequestDecision(AuthorizationDecisionType requestDecision) {
		this.requestDecision = requestDecision;
	}

	public AuthorizationDecisionType getExecDecision() {
		return execDecision;
	}

	public void setExecDecision(AuthorizationDecisionType execDecision) {
		this.execDecision = execDecision;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("PhaseDecisionImpl");
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "requestDecision", requestDecision==null?"null":requestDecision.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "execDecision", execDecision==null?"null":execDecision.toString(), indent+1);
		return sb.toString();
	}
}
