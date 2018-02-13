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

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

/**
 * @author semancik
 *
 */
public class PhasedConstraints implements DebugDumpable {

	private ItemSecurityConstraintsImpl requestConstraints = new ItemSecurityConstraintsImpl();
	private ItemSecurityConstraintsImpl execConstraints = new ItemSecurityConstraintsImpl();

	protected ItemSecurityConstraintsImpl getRequestConstraints() {
		return requestConstraints;
	}

	protected ItemSecurityConstraintsImpl getExecConstraints() {
		return execConstraints;
	}

	public ItemSecurityConstraintsImpl get(AuthorizationPhaseType phase) {
		switch (phase) {
			case REQUEST:
				return requestConstraints;
			case EXECUTION:
				return execConstraints;
			default:
				return null;
		}
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(PhasedConstraints.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "request", requestConstraints, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "execution", execConstraints, indent+1);
		return sb.toString();
	}

}
