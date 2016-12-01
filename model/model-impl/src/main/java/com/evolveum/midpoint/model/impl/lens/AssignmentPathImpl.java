/*
 * Copyright (c) 2010-2016 Evolveum
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
import java.util.List;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class AssignmentPathImpl implements AssignmentPath {
	
	private List<AssignmentPathSegmentImpl> segments;

	public AssignmentPathImpl() {
		segments = createNewSegments();
	}
	
	AssignmentPathImpl(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi) {
		this.segments = createNewSegments();
		segments.add(new AssignmentPathSegmentImpl(assignmentIdi, true));
	}

	private List<AssignmentPathSegmentImpl> createNewSegments() {
		return new ArrayList<>();
	}

	@Override
	public List<AssignmentPathSegmentImpl> getSegments() {
		return segments;
	}
	
	public void add(AssignmentPathSegmentImpl segment) {
		segments.add(segment);
	}
	
	public void remove(AssignmentPathSegment segment) {
		segments.remove(segment);
	}

	
	@Override
	public AssignmentPathSegmentImpl getFirstAssignmentSegment() {
		return segments.get(0);
	}
	
	@Override
	public boolean isEmpty() {
		return segments.isEmpty();
	}

	@Override
	public int size() { return segments.size(); }

	@Override
	public EvaluationOrder getEvaluationOrder() {
		if (isEmpty()) {
			return EvaluationOrderImpl.ZERO;
		} else {
			return segments.get(segments.size()-1).getEvaluationOrder();
		}
	}
	
	@Override
	public AssignmentPathSegment last() {
		if (isEmpty()) {
			return null;
		} else {
			return segments.get(segments.size()-1);
		}
	}
	
	@Override
	public boolean containsTarget(ObjectType target) {
		if (target == null) {
			return false;
		}
		for (AssignmentPathSegment segment: segments) {
			ObjectType segmentTarget = segment.getTarget();
			if (segmentTarget != null) {
				if (segmentTarget.getOid() != null && target.getOid() != null && 
						segmentTarget.getOid().equals(target.getOid())) {
					return true;
				}
				if (segmentTarget.getOid() == null && target.getOid() == null &&
						segmentTarget.equals(target)) {
					return true;
				}
			}
		}
		return false;
	}

	
	/**
	 * Shallow clone.
	 */
	public AssignmentPathImpl clone() {
		AssignmentPathImpl clone = new AssignmentPathImpl();
		clone.segments.addAll(this.segments);
		return clone;
	}

	@Override
	public String toString() {
		return "AssignmentPath(" + segments + ")";
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "AssignmentPath", indent);
		if (segments == null || segments.isEmpty()) {
			sb.append(" (empty)");
		} else {
			sb.append(" (").append(segments.size()).append(")");
			if (DebugUtil.isDetailedDebugDump()) {
				sb.append("\n");
				DebugUtil.debugDump(sb, segments, indent + 1, false);
			} else {
				for (AssignmentPathSegment segment: segments) {
					sb.append("\n");
					DebugUtil.indentDebugDump(sb, indent + 1);
					sb.append(segment.toString());
				}
			}
		}
		return sb.toString();
	}

}
