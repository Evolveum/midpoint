/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.api;

import java.util.List;

import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ArchetypePolicyType;

/**
 * Data structure that contains information about all archetype-related interactions for a particular object.
 * This include archetype policies, assignments, relations, etc.
 * This data structure is supposed to hold all the archetype-related data that the user interface need to
 * display the object and to interact with the object. GUI should not need to to any other processing to
 * determine archetype-like information.
 * 
 * @author Radovan Semancik
 */
public class ArchetypeInteractionSpecification implements DebugDumpable {
	
	private ArchetypePolicyType archetypePolicy;
	private List<AssignmentTargetRelation> assignmentTargetRelations;

	public ArchetypePolicyType getArchetypePolicy() {
		return archetypePolicy;
	}

	public void setArchetypePolicy(ArchetypePolicyType archetypePolicy) {
		this.archetypePolicy = archetypePolicy;
	}

	/**
	 * Returns list of assignment target relation specifications. Simply speaking,
	 * those are object types that can be targets of assignments for this object
	 * and the respective relations. Simply speaking this means "what assignments can I have"
	 * or "what are the valid targets for relations that I hold".
	 * It is the reverse of assignmentRelation definition in AssignmentType in schema.
	 *  
	 * If null is returned then there is no applicable assignment target constraint.
	 * That means any assignment with any target and any relation is allowed.
	 * This is the default behavior.
	 * 
	 * If empty list is returned that means no assignments are allowed.
	 * I.e. there is no valid combination of target type and relation that could
	 * be applied. 
	 */
	public List<AssignmentTargetRelation> getAssignmentTargetRelations() {
		return assignmentTargetRelations;
	}

	public void setAssignmentTargetRelations(List<AssignmentTargetRelation> assignmentTargetRelations) {
		this.assignmentTargetRelations = assignmentTargetRelations;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ArchetypeInteractionSpecification.class, indent);
		PrismUtil.debugDumpWithLabelLn(sb, "archetypePolicy", archetypePolicy, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "assignmentTargetRelations", assignmentTargetRelations, indent + 1);
		return sb.toString();
	}

}
