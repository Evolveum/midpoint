/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * TODO better name ;)
 *
 * @author mederly
 */
public class DelegatorWithOtherPrivilegesLimitations implements DebugDumpable {

	@NotNull private final UserType delegator;
	@NotNull final private List<OtherPrivilegesLimitationType> limitations;

	public DelegatorWithOtherPrivilegesLimitations(@NotNull UserType delegator,
			@NotNull List<OtherPrivilegesLimitationType> limitations) {
		this.delegator = delegator;
		this.limitations = limitations;
	}

	@NotNull
	public UserType getDelegator() {
		return delegator;
	}

	@NotNull
	public List<OtherPrivilegesLimitationType> getLimitations() {
		return limitations;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "DelegatorWithOtherPrivilegesLimitations", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Delegator", ObjectTypeUtil.toShortString(delegator), indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "Limitations", limitations, indent + 1);
		return sb.toString();	}
}
