/**
 * Copyright (c) 2018-2019 Evolveum
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

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Data structure that contains information about possible assignment targets or holders for a particular object.
 * 
 * This data structure is used in two related, but slight distinct cases: looking for assignment targets
 * and looking for assignment holders. In both cases this structure describes candidate objects on the
 * "other side" of the assignment.
 * 
 * @author Radovan Semancik
 */
public class AssignmentCandidatesSpecification implements DebugDumpable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private boolean supportGenericAssignment;
	private List<AssignmentObjectRelation> assignmentObjectRelations;

	// TODO: add properties from RoleSelectionSpecification here
	
	/**
	 * If set to true then the holder object can support "generic" assignment.
	 * This means that any object type can be assigned (constrained by authorizations).
	 * This usually means that GUI should render "add assignment" button that is not
	 * constrained to specific target type or archetype.
	 */
	public boolean isSupportGenericAssignment() {
		return supportGenericAssignment;
	}

	public void setSupportGenericAssignment(boolean supportGenericAssignment) {
		this.supportGenericAssignment = supportGenericAssignment;
	}

	/**
	 * Returns list of assignment target relation specifications. Simply speaking,
	 * those are object types that can be targets of assignments for this object
	 * and the respective relations. Simply speaking this means "what assignments can I have"
	 * or "what are the valid targets for relations that I hold".
	 * It is the reverse of assignmentRelation definition in AssignmentType in schema.
	 *  
	 * If empty list is returned that means no assignments are allowed.
	 * I.e. there is no valid combination of target type and relation that could
	 * be applied. However, generic assignments may still be allowed.
	 * See supportGenericAssignment.
	 */
	public List<AssignmentObjectRelation> getAssignmentObjectRelations() {
		return assignmentObjectRelations;
	}

	public void setAssignmentObjectRelations(List<AssignmentObjectRelation> assignmentTargetRelations) {
		this.assignmentObjectRelations = assignmentTargetRelations;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(AssignmentCandidatesSpecification.class, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "supportGenericAssignment", supportGenericAssignment, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "assignmentObjectRelations", assignmentObjectRelations, indent + 1);
		return sb.toString();
	}

}
