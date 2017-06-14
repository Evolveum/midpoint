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
package com.evolveum.midpoint.model.impl.lens;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.prism.delta.DeltaTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdempotenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class EvaluatedAssignmentTargetCache implements DebugDumpable {
	
	private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignmentTargetCache.class);
	
	// Triple. Target processed for addition is not necessarily reusable for deletion
	// This is indexed by OID and order
	private DeltaTriple<Set<Key>> processedKeys;

	// This is indexed by OID only
	private DeltaTriple<Set<String>> processedOids;

	public EvaluatedAssignmentTargetCache() {
		processedKeys = new DeltaTriple<>(() -> new HashSet<>());
		processedOids = new DeltaTriple<>(() -> new HashSet<>());
	}
	
	public void reset() {
		processedKeys.foreach(set -> set.clear());
		processedOids.foreach(set -> set.clear());
	}

	public void recordProcessing(AssignmentPathSegmentImpl segment, PlusMinusZero mode) {
		ObjectType targetType = segment.getTarget();
		if (!(targetType instanceof AbstractRoleType)) {
			return;
		}
		if (!isCacheable((AbstractRoleType)targetType)) {
			return;
		}
		Key key = new Key(segment);
		Set<Key> keySet = processedKeys.get(mode);
		keySet.add(key);
		
		if (targetType.getOid() != null) {
			Set<String> oidSet = processedOids.get(mode);
			oidSet.add(targetType.getOid());
		}
	}
	
	private boolean isCacheable(AbstractRoleType targetType) {
		IdempotenceType idempotence = targetType.getIdempotence();
		return idempotence != null && idempotence != IdempotenceType.NONE;
	}

	public boolean canSkip(AssignmentPathSegmentImpl segment, PlusMinusZero mode) {
		ObjectType target = segment.getTarget();
		if (target == null || !(target instanceof AbstractRoleType)) {
//			LOGGER.trace("Non-skippable target: {}", target);
			return false;
		}
		IdempotenceType idempotence = ((AbstractRoleType)target).getIdempotence();
		if (idempotence == null || idempotence == IdempotenceType.NONE) {
//			LOGGER.trace("Not idempotent target: {}", target);
			return false;
		}
		if (idempotence == IdempotenceType.CONSERVATIVE && !segment.isMatchingOrder()) {
//			LOGGER.trace("Convervative idempotent and order is not matching: {}", target);
			return false;
		}
		if (idempotence == IdempotenceType.AGGRESSIVE) {
			Set<String> oidSet = processedOids.get(mode);
			return oidSet.contains(target.getOid());
		} else {
			Key key = new Key(segment);
			Set<Key> keySet = processedKeys.get(mode);
			return keySet.contains(key);
		}
	}
	
	private class Key {
		private EvaluationOrder evaluationOrder;
		private String targetOid;
		
		public Key(AssignmentPathSegmentImpl segment) {
			super();
			this.evaluationOrder = segment.getEvaluationOrder();
			this.targetOid = segment.getTarget().getOid();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((evaluationOrder == null) ? 0 : evaluationOrder.hashCode());
			result = prime * result + ((targetOid == null) ? 0 : targetOid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Key other = (Key) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (evaluationOrder == null) {
				if (other.evaluationOrder != null) {
					return false;
				}
			} else if (!evaluationOrder.equals(other.evaluationOrder)) {
				return false;
			}
			if (targetOid == null) {
				if (other.targetOid != null) {
					return false;
				}
			} else if (!targetOid.equals(other.targetOid)) {
				return false;
			}
			return true;
		}

		private EvaluatedAssignmentTargetCache getOuterType() {
			return EvaluatedAssignmentTargetCache.this;
		}

		@Override
		public String toString() {
			return "Key("+ targetOid + ": order=" + evaluationOrder + ")";
		}
		
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilder(EvaluatedAssignmentTargetCache.class, indent);
		sb.append("\n");
		DebugUtil.debugDumpLabelLn(sb, "processedKeys", indent + 1);
		processedKeys.debugDumpNoTitle(sb, indent + 2);
		sb.append("\n");
		DebugUtil.debugDumpLabelLn(sb, "processedOids", indent + 1);
		processedOids.debugDumpNoTitle(sb, indent + 2);
		return sb.toString();
	}
	
}
