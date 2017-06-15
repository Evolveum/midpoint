/**
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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.internals.TestingPaths;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Smart set of assignment values that keep track whether the assignment is new, old or changed.
 * 
 * This information is used for various reasons. We specifically distinguish between assignments in objectCurrent and objectOld
 * to be able to reliably detect phantom adds: a phantom add is an assignment that is both in OLD and CURRENT objects. This is
 * important in waves greater than 0, where objectCurrent is already updated with existing assignments. (See MID-2422.)
 * 
 * @author Radovan Semancik
 */
public class SmartAssignmentCollection<F extends FocusType> implements Iterable<SmartAssignmentElement>, DebugDumpable {
	
	private Map<SmartAssignmentKey,SmartAssignmentElement> aMap = null;
	private Map<Long,SmartAssignmentElement> idMap;

	public void collect(PrismObject<F> objectCurrent, PrismObject<F> objectOld, ContainerDelta<AssignmentType> assignmentDelta) throws SchemaException {
		PrismContainer<AssignmentType> assignmentContainerCurrent = null;
		if (objectCurrent != null) {
			assignmentContainerCurrent = objectCurrent.findContainer(FocusType.F_ASSIGNMENT);
		}
		
		if (aMap == null) {
			int initialCapacity = computeInitialCapacity(assignmentContainerCurrent, assignmentDelta);
			aMap = new HashMap<>(initialCapacity);
			idMap = new HashMap<>(initialCapacity);
		}
		
		collectAssignments(assignmentContainerCurrent, Mode.CURRENT);
		
		if (objectOld != null) {
			collectAssignments(objectOld.findContainer(FocusType.F_ASSIGNMENT), Mode.OLD);
		}
		
		collectAssignments(assignmentDelta);
	}

	private void collectAssignments(PrismContainer<AssignmentType> assignmentContainer, Mode mode) throws SchemaException {
		if (assignmentContainer == null) {
			return;
		}
		for (PrismContainerValue<AssignmentType> assignmentCVal: assignmentContainer.getValues()) {
			collectAssignment(assignmentCVal, mode);
		}
	}

	private void collectAssignments(ContainerDelta<AssignmentType> assignmentDelta) throws SchemaException {
		if (assignmentDelta == null) {
			return;
		}
		collectAssignmentsDeltaSet(assignmentDelta.getValuesToReplace());
		collectAssignmentsDeltaSet(assignmentDelta.getValuesToAdd());
		collectAssignmentsDeltaSet(assignmentDelta.getValuesToDelete());
	}
	
	
	private void collectAssignmentsDeltaSet(Collection<PrismContainerValue<AssignmentType>> deltaSet) throws SchemaException {
		if (deltaSet == null) {
			return;
		}
		for (PrismContainerValue<AssignmentType> assignmentCVal: deltaSet) {
			collectAssignment(assignmentCVal, Mode.CHANGED);
		}
	}

	private void collectAssignment(PrismContainerValue<AssignmentType> assignmentCVal, Mode mode) throws SchemaException {
		
		SmartAssignmentElement element = null;
		
		// Special lookup for empty elements.
		// Changed assignments may be "light", i.e. they may contain just the identifier. 
		// Make sure that we always have the full assignment data.
		if (assignmentCVal.isEmpty()) {
			if (assignmentCVal.getId() != null) {
				element = idMap.get(assignmentCVal.getId());
				if (element == null) {
					// deleting non-existing assignment. Safe to ignore?
					return;
				}
			} else {
				// Empty assignment without ID
				throw new SchemaException("Attempt to change empty assignment without ID");
			}
		}
		
		if (element == null) {
			element = lookup(assignmentCVal);
		}
		
		if (element == null) {
			 element = put(assignmentCVal);
		}
		
		switch (mode) {
			case CURRENT: 
				element.setCurrent(true);
				break;
			case OLD:
				element.setOld(true);
				break;
			case CHANGED:
				element.setChanged(true);
				break;
		}
	}

	private SmartAssignmentElement put(PrismContainerValue<AssignmentType> assignmentCVal) {
		SmartAssignmentElement element = new SmartAssignmentElement(assignmentCVal);
		aMap.put(element.getKey(), element);
		if (assignmentCVal.getId() != null) {
			idMap.put(assignmentCVal.getId(), element);
		}
		return element;
	}

	private SmartAssignmentElement lookup(PrismContainerValue<AssignmentType> assignmentCVal) {
		if (assignmentCVal.getId() != null) {
			// shortcut. But also important for deltas that specify correct id, but the value
			// does not match exactly (e.g. small differences in relation, e.g. null vs default)
			return idMap.get(assignmentCVal.getId());
		}
		SmartAssignmentKey key = new SmartAssignmentKey(assignmentCVal);
		return aMap.get(key);
	}

	private int computeInitialCapacity(PrismContainer<AssignmentType> assignmentContainerCurrent, ContainerDelta<AssignmentType> assignmentDelta) {
		int capacity = 0;
		if (assignmentContainerCurrent != null) {
			capacity += assignmentContainerCurrent.size();
		}
		if (assignmentDelta != null) {
			capacity += assignmentDelta.size();
		}
		return capacity;
	}

	@Override
	public Iterator<SmartAssignmentElement> iterator() {
		if (InternalsConfig.getTestingPaths() == TestingPaths.REVERSED) {
			Collection<SmartAssignmentElement> values = aMap.values();
			List<SmartAssignmentElement> valuesList = new ArrayList<>(values.size());
			valuesList.addAll(values);
			Collections.reverse(valuesList);
			return valuesList.iterator();
		} else {
			return aMap.values().iterator();
		}
	}
		
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("SmartAssignmentCollection: ");
		if (aMap == null) {
			sb.append("uninitialized");
		} else {
			sb.append(aMap.size()).append(" items");
			for (SmartAssignmentElement element: aMap.values()) {
				sb.append("\n");
				sb.append(element.debugDump(indent + 1));
			}
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return "SmartAssignmentCollection(" + aMap.values() + ")";
	}

	private enum Mode {
		CURRENT, OLD, CHANGED;
	}
	
}
