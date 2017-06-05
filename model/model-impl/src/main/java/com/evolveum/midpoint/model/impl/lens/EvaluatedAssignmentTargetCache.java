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
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class EvaluatedAssignmentTargetCache implements DebugDumpable {
	
	// Three sets. Target processed for addition is not necessarily reusable for deletion
	private Set<Key> processedKeysPlus = new HashSet<>();
	private Set<Key> processedKeysZero = new HashSet<>();
	private Set<Key> processedKeysMinus = new HashSet<>();
	
	public void reset() {
		processedKeysPlus = new HashSet<>();
		processedKeysZero = new HashSet<>();
		processedKeysMinus = new HashSet<>();
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
		Set<Key> set = getSet(mode);
		set.add(key);
	}
	
	private boolean isCacheable(AbstractRoleType targetType) {
		return BooleanUtils.isTrue(targetType.isIdempotent());
	}

	public boolean canSkip(AssignmentPathSegmentImpl segment, PlusMinusZero mode) {
		if (!segment.isMatchingOrder()) {
			// Not sure about this. Just playing safe.
			return false;
		}
		Key key = new Key(segment);
		Set<Key> set = getSet(mode);
		return set.contains(key);
	}
	
	private Set<Key> getSet(PlusMinusZero mode) {
		switch (mode) {
			case PLUS: return processedKeysPlus;
			case ZERO: return processedKeysZero;
			case MINUS: return processedKeysMinus;
		}
		throw new IllegalArgumentException("Wrong mode "+mode);
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
		DebugUtil.debugDumpWithLabelLn(sb, "plus", processedKeysPlus, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "zero", processedKeysZero, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "minus", processedKeysMinus, indent + 1);
		return sb.toString();
	}
	
}
