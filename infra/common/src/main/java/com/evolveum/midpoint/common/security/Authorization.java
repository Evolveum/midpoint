/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationType;

/**
 * @author semancik
 *
 */
public class Authorization implements GrantedAuthority, Dumpable, DebugDumpable {
	
	AuthorizationType authorizationType;

	public Authorization(AuthorizationType authorizationType) {
		super();
		this.authorizationType = authorizationType;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.GrantedAuthority#getAuthority()
	 */
	@Override
	public String getAuthority() {
		// this is complex authority. Just return null
		return null;
	}

	public String getDescription() {
		return authorizationType.getDescription();
	}

	public AuthorizationDecisionType getDecision() {
		 AuthorizationDecisionType decision = authorizationType.getDecision();
		 if (decision == null) {
			 return AuthorizationDecisionType.ALLOW;
		 }
		 return decision;
	}

	public List<String> getAction() {
		return authorizationType.getAction();
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.Dumpable#dump()
	 */
	@Override
	public String dump() {
		return debugDump();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
	 */
	@Override
	public String debugDump() {
		// TODO Auto-generated method stub
		return debugDump(0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "Authorization(", indent);
		sb.append(authorizationType);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Authorization(" + authorizationType == null ? "null" : authorizationType.getAction() + ")";
	}

}
