/*
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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

/**
 * @author semancik
 *
 */
public class PersonaConstruction<F extends FocusType> extends AbstractConstruction<F, PersonaConstructionType> {

	public PersonaConstruction(PersonaConstructionType constructionType, ObjectType source) {
		super(constructionType, source);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "PersonaConstruction", indent);
		PersonaConstructionType constructionType = getConstructionType();
		if (constructionType != null) {
			DebugUtil.debugDumpWithLabelLn(sb, "targetType", constructionType.getTargetType(), indent + 1);
			DebugUtil.debugDumpWithLabelLn(sb, "subtype", constructionType.getTargetSubtype(), indent + 1);
			DebugUtil.debugDumpWithLabelToStringLn(sb, "strength", constructionType.getStrength(), indent + 1);
		}
		DebugUtil.debugDumpWithLabelLn(sb, "isValid", isValid(), indent + 1);
		sb.append("\n");
		if (getConstructionType() != null && getConstructionType().getDescription() != null) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "description", indent + 1);
			sb.append(" ").append(getConstructionType().getDescription());
		}
		if (getAssignmentPath() != null) {
			sb.append("\n");
			sb.append(getAssignmentPath().debugDump(indent + 1));
		}
		return sb.toString();

	}

}
