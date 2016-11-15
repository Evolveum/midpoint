/**
 * Copyright (c) 2015-2016 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * @author semancik
 *
 */
public class EvaluatedAssignmentTargetImpl implements EvaluatedAssignmentTarget {
	
	PrismObject<? extends FocusType> target;
	private boolean directlyAssigned;
	private boolean evaluateConstructions;
	private AssignmentType assignment;
	private Collection<ExclusionPolicyConstraintType> exclusions = null;

	@Override
	public PrismObject<? extends FocusType> getTarget() {
		return target;
	}

	public void setTarget(PrismObject<? extends FocusType> target) {
		this.target = target;
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
		return target.getOid();
	}

	public Collection<ExclusionPolicyConstraintType> getExclusions() {
		if (exclusions == null) {
			exclusions = new ArrayList<>();

			FocusType focusType = target.asObjectable();
			if (focusType instanceof AbstractRoleType) {
				AbstractRoleType roleType = (AbstractRoleType)focusType;
				
				// legacy (very old)
				for (ExclusionPolicyConstraintType exclusionType: roleType.getExclusion()) {
					exclusions.add(exclusionType);
				}
				
				// legacy
				PolicyConstraintsType constraints = roleType.getPolicyConstraints();
				if (constraints != null) {
					for (ExclusionPolicyConstraintType exclusionType: constraints.getExclusion()) {
						exclusions.add(exclusionType);
					}
				}
				
				for (AssignmentType assignmentInTarget: target.asObjectable().getAssignment()) {
					PolicyRuleType policyRule = assignmentInTarget.getPolicyRule();
					if (policyRule != null && policyRule.getPolicyConstraints() != null) {
						for (ExclusionPolicyConstraintType exclusionType: policyRule.getPolicyConstraints().getExclusion()) {
							exclusions.add(exclusionType);
						}
					}
				}
			}
		
		}
		return exclusions; 
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "EvaluatedAssignmentTarget", indent);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Target", target, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Assignment", String.valueOf(assignment), indent + 1);
		return sb.toString();
	}
	

}
