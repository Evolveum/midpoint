/*
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

import javax.xml.namespace.QName;

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
	// This is indexed by OID and relation only
	private DeltaTriple<Set<Key>> processedKeys;

	// Triple. Target processed for addition is not necessarily reusable for deletion
	// This is indexed by OID, relation and order
	private DeltaTriple<Set<OrderKey>> processedOrderKeys;


	public EvaluatedAssignmentTargetCache() {
		processedOrderKeys = new DeltaTriple<>(() -> new HashSet<>());
		processedKeys = new DeltaTriple<>(() -> new HashSet<>());
	}

	public void reset() {
		processedOrderKeys.foreach(set -> set.clear());
		processedKeys.foreach(set -> set.clear());
	}

	public void recordProcessing(AssignmentPathSegmentImpl segment, PlusMinusZero mode) {
		ObjectType targetType = segment.getTarget();
		if (!(targetType instanceof AbstractRoleType)) {
			return;
		}
		if (!isCacheable((AbstractRoleType)targetType)) {
			return;
		}
		processedOrderKeys.get(mode).add(new OrderKey(segment));

		if (targetType.getOid() != null) {
			processedKeys.get(mode).add(new Key(segment));
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
//			LOGGER.trace("Conservative idempotent and order is not matching: {}", target);
			return false;
		}
		if (idempotence == IdempotenceType.AGGRESSIVE) {
			// Aggressive idempotence implies that this is not a meta-role
			// Therefore we want to skip all evaluation except for order=1
			// We skip these evaluations even if we have NOT seen this role
			// before. If we do not skip this evaluation then the role will
			// be remembered as evaluation and it will not be re-evaluated
			// in non-meta context.
			if (!segment.getEvaluationOrder().isOrderOne()) {
//				LOGGER.trace("Aggressive idempotent and non-one order: {}: {}", segment.getEvaluationOrder(), target);
				return true;
			}
			return processedKeys.get(mode).contains(new Key(segment));
		} else {
			return processedOrderKeys.get(mode).contains(new OrderKey(segment));
		}
	}

	private class Key {
		private String targetOid;
		private QName relation;

		public Key(AssignmentPathSegmentImpl segment) {
			super();
			this.targetOid = segment.getTarget().getOid();
			this.relation = segment.getRelation();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((relation == null) ? 0 : relation.hashCode());
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
			if (relation == null) {
				if (other.relation != null) {
					return false;
				}
			} else if (!relation.equals(other.relation)) {
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
			return "Key(" + content() + ")";
		}


		protected String content() {
			return targetOid + "[" + relation + "]";
		}

	}

	private class OrderKey extends Key {
		private EvaluationOrder evaluationOrder;

		public OrderKey(AssignmentPathSegmentImpl segment) {
			super(segment);
			this.evaluationOrder = segment.getEvaluationOrder();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((evaluationOrder == null) ? 0 : evaluationOrder.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			OrderKey other = (OrderKey) obj;
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
			return true;
		}

		private EvaluatedAssignmentTargetCache getOuterType() {
			return EvaluatedAssignmentTargetCache.this;
		}

		@Override
		public String toString() {
			return "OrderKey("+ content() + ": order=" + evaluationOrder + ")";
		}

	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilder(EvaluatedAssignmentTargetCache.class, indent);
		sb.append("\n");
		DebugUtil.debugDumpLabelLn(sb, "processedKeys", indent + 1);
		processedKeys.debugDumpNoTitle(sb, indent + 2);
		sb.append("\n");
		DebugUtil.debugDumpLabelLn(sb, "processedOrderKeys", indent + 1);
		processedOrderKeys.debugDumpNoTitle(sb, indent + 2);
		return sb.toString();
	}

}
