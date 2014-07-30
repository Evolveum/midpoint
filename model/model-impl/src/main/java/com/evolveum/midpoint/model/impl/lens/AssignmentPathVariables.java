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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author semancik
 *
 */
public class AssignmentPathVariables {
	
	private ItemDeltaItem<PrismContainerValue<AssignmentType>> magicAssignment;
	private ItemDeltaItem<PrismContainerValue<AssignmentType>> immediateAssignment;
	private ItemDeltaItem<PrismContainerValue<AssignmentType>> thisAssignment;
	private ItemDeltaItem<PrismContainerValue<AssignmentType>> focusAssignment;
	private PrismObject<? extends AbstractRoleType> immediateRole;

	public ItemDeltaItem<PrismContainerValue<AssignmentType>> getMagicAssignment() {
		return magicAssignment;
	}

	public void setMagicAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>> magicAssignment) {
		this.magicAssignment = magicAssignment;
	}

	public ItemDeltaItem<PrismContainerValue<AssignmentType>> getImmediateAssignment() {
		return immediateAssignment;
	}

	public void setImmediateAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>> immediateAssignment) {
		this.immediateAssignment = immediateAssignment;
	}

	public ItemDeltaItem<PrismContainerValue<AssignmentType>> getThisAssignment() {
		return thisAssignment;
	}

	public void setThisAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>> thisAssignment) {
		this.thisAssignment = thisAssignment;
	}

	public ItemDeltaItem<PrismContainerValue<AssignmentType>> getFocusAssignment() {
		return focusAssignment;
	}

	public void setFocusAssignment(ItemDeltaItem<PrismContainerValue<AssignmentType>> focusAssignment) {
		this.focusAssignment = focusAssignment;
	}

	public PrismObject<? extends AbstractRoleType> getImmediateRole() {
		return immediateRole;
	}

	public void setImmediateRole(PrismObject<? extends AbstractRoleType> immediateRole) {
		this.immediateRole = immediateRole;
	}
	
}
