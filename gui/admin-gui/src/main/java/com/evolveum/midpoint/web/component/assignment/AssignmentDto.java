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
package com.evolveum.midpoint.web.component.assignment;

import java.io.Serializable;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

public class AssignmentDto extends Selectable<AssignmentDto> implements Comparable<AssignmentDto>, Serializable{

	private static final long serialVersionUID = 1L;

	private AssignmentType assignment;
	private UserDtoStatus status;
	private AssignmentType oldAssignment;
//	private RelationTypes relationType;

	public static final String F_VALUE = "assignment";
	public static final String F_RELATION_TYPE = "relationType";

	public AssignmentDto(AssignmentType assignment, UserDtoStatus status) {
		this.assignment = assignment;
		this.oldAssignment = assignment.clone();
		this.status = status;
	}

	public Collection<? extends ItemDelta> computeAssignmentDelta() {
		Collection<? extends ItemDelta> deltas = oldAssignment.asPrismContainerValue().diff(assignment.asPrismContainerValue());
		return deltas;
	}

	public void revertChanges() {
		assignment = oldAssignment.clone();
	}

	public QName getRelation() {

		//TODO: what kind of rlation should be returned for the PERSONA CONSTRUCTION?

		if (assignment.getConstruction() != null) {
			return SchemaConstants.ORG_DEFAULT;
		}

		if (assignment.getTargetRef() == null) {
			return null;
		}

		return assignment.getTargetRef().getRelation();

	}

	public RelationTypes getRelationType() {
		return RelationTypes.getRelationType(getRelation());
	}

	public void setRelationType(RelationTypes relationType){
		if (assignment.getTargetRef() == null) {
			return;
		}

		assignment.getTargetRef().setRelation(relationType.getRelation());
	}


	public AssignmentType getAssignment() {
		return assignment;
	}

	@Override
	public int compareTo(AssignmentDto other) {
		Validate.notNull(other, "Can't compare assignment editor dto with null.");

		String name1 = "";//getName() != null ? getName() : "";
		String name2 = "";//other.getName() != null ? other.getName() : "";

		return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
	}

	public UserDtoStatus getStatus() {
		return status;
	}

	public void setStatus(UserDtoStatus status) {
		this.status = status;
	}

}
