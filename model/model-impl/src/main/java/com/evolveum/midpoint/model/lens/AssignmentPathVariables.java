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
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;

/**
 * @author semancik
 *
 */
public class AssignmentPathVariables {
	
	private PrismContainerValue<AssignmentType> magicAssignment;
	private PrismContainerValue<AssignmentType> immediateAssignment;
	private PrismContainerValue<AssignmentType> thisAssignment;
	private PrismContainerValue<AssignmentType> focusAssignment;
	private PrismObject<? extends AbstractRoleType> immediateRole;
	
	public PrismContainerValue<AssignmentType> getMagicAssignment() {
		return magicAssignment;
	}
	
	public void setMagicAssignment(PrismContainerValue<AssignmentType> magicAssignment) {
		this.magicAssignment = magicAssignment;
	}
	
	public PrismContainerValue<AssignmentType> getImmediateAssignment() {
		return immediateAssignment;
	}
	
	public void setImmediateAssignment(PrismContainerValue<AssignmentType> immediateAssignment) {
		this.immediateAssignment = immediateAssignment;
	}
	
	public PrismContainerValue<AssignmentType> getThisAssignment() {
		return thisAssignment;
	}
	
	public void setThisAssignment(PrismContainerValue<AssignmentType> thisAssignment) {
		this.thisAssignment = thisAssignment;
	}
	
	public PrismContainerValue<AssignmentType> getFocusAssignment() {
		return focusAssignment;
	}

	public void setFocusAssignment(PrismContainerValue<AssignmentType> focusAssignment) {
		this.focusAssignment = focusAssignment;
	}

	public PrismObject<? extends AbstractRoleType> getImmediateRole() {
		return immediateRole;
	}
	
	public void setImmediateRole(PrismObject<? extends AbstractRoleType> immediateRole) {
		this.immediateRole = immediateRole;
	}

}
