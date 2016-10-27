/**
 * Copyright (c) 2015 Evolveum
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

import com.evolveum.midpoint.model.api.context.EvaluatedAbstractRole;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

/**
 * @author semancik
 *
 */
public class EvaluatedAbstractRoleImpl implements EvaluatedAbstractRole {
	
	PrismObject<? extends AbstractRoleType> role;
	private boolean directlyAssigned;
	private boolean evaluateConstructions;
	private AssignmentType assignment;

	@Override
	public PrismObject<? extends AbstractRoleType> getRole() {
		return role;
	}

	public void setRole(PrismObject<? extends AbstractRoleType> role) {
		this.role = role;
	}

	@Override
	public boolean isDirectlyAssigned() {
		return directlyAssigned;
	}

	public void setDirectlyAssigned(boolean directlyAssigned) {
		this.directlyAssigned = directlyAssigned;
	}

	@Override
	public boolean isEvaluateConstructions() {
		return evaluateConstructions;
	}

	public void setEvaluateConstructions(boolean evaluateConstructions) {
		this.evaluateConstructions = evaluateConstructions;
	}

	@Override
	public AssignmentType getAssignment() {
		return assignment;
	}

	public void setAssignment(AssignmentType assignment) {
		this.assignment = assignment;
	}
	
	public String getOid() {
		return role.getOid();
	}

	public PolicyConstraintsType getPolicyConstraints() {
		AbstractRoleType roleType = role.asObjectable();
		PolicyConstraintsType constraints = roleType.getPolicyConstraints();
		if (roleType.getExclusion().isEmpty()) {
			return constraints;
		}
		if (constraints == null) {
			constraints = new PolicyConstraintsType();
			roleType.setPolicyConstraints(constraints);
		}
		for (ExclusionPolicyConstraintType exclusion: roleType.getExclusion()) {
			constraints.getExclusion().add(exclusion.clone());
		}
		roleType.getExclusion().clear();
		return constraints;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "EvaluatedAbstractRole", indent);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Role", role, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Assignment", String.valueOf(assignment), indent + 1);
		return sb.toString();
	}
	

}
