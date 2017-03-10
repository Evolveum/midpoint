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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

/**
 * Primary duty of this class is to be a part of assignment path. (This is what is visible through its interface,
 * AssignmentPathSegment.) However, it also serves as a place where auxiliary information about assignment evaluation
 * is stored.
 *
 * @author semancik
 *
 */
public class AssignmentPathSegmentImpl implements AssignmentPathSegment {

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentPathSegmentImpl.class);

	// "assignment path segment" information

	final ObjectType source;					// we avoid "getter" notation for some final fields to simplify client code
	private final ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi;
	private final boolean isAssignment;			// false means inducement
	private QName relation;
	private ObjectType target;

	// assignment evaluation information

	final String sourceDescription;					// Human readable text describing the source (for error messages)
	private boolean pathToSourceValid;				// Is the whole path to *source* valid, i.e. enabled (meaning activation.effectiveStatus)?
	private boolean validityOverride = false;		// Should we evaluate content of the assignment even if it's not valid i.e. enabled?
													// This is set to true on the first assignment in the chain.

	/**
	 *  Constructions, focus mappings, and focus policy rules (or "assignment content") should be collected only
	 *  from assignments that belong directly to the focus; i.e. not from assignments attached to roles, meta roles,
	 *  meta-meta roles, etc.
	 *
	 *  Content belonging to roles should be collected if it's attached to them via inducements of order 1.
	 *  Content belonging to meta-roles should be collected if it's attached to them via inducements of order 2.
	 *  And so on.
	 *
	 *  For each segment (i.e. assignment/inducement), we know we can use its content if its "isMatchingOrder" attribute is true.
	 *
	 *  But how we compute this attribute?
	 *
	 *  Each segment in the assignment path has an evaluation order. First assignment has an evaluation order of 1, second
	 *  assignment has an order of 2, etc. Order of a segment can be seen as the number of assignments segments in the path
	 *  (including itself). And we collect content from assignment segments of order 1.
	 *
	 *  But what about inducements? There are two (somewhat related) questions:
	 *
	 *  1. How we compute isMatchingOrder for inducement segments?
	 *  2. How we compute evaluation order for inducement segments?
	 *
	 *  As for #1: To compute isMatchingOrder, we must take evaluation order of the _previous_ segment, and compare
	 *  it with the inducement order (or, more generally, inducement order constraints). If they match, we say that inducement
	 *  has matching order.
	 *
	 *  As for #2: For some inducements we can determine resulting order, while for others we can not. Problematic inducements
	 *  are those that do not have strict order, but an interval of orders instead. For the former we can compute the
	 *  resulting order as previous - (N-1), where N is the order of the inducement (unspecified means 1). For the latter
	 *  we consider the resulting order as undefined.
	 *
	 *  TODO relations
	 *
	 *  Special consideration must be given when collecting target policy rules, i.e. rules that are attached to
	 *  assignment targets. Such rules are typically attached to roles that are being assigned. So let's consider this:
	 *
	 *  rule1 (e.g. assignment approval policy rule)
	 *    A
	 *    |
	 *    |
	 *  Pirate
	 *    A
	 *    |
	 *    |
	 *   jack
	 *
	 *   When evaluating jack->Pirate assignment, rule1 would not be normally taken into account, because its assignment
	 *   (Pirate->rule1) has an order of 2. However, we want to collect it - but not as an item related to focus, but
	 *   as an item related to evaluated assignment's target. Therefore besides isMatchingOrder we maintain isMatchingOrderPlusOne
	 *   that marks all segments (assignments/inducements) that contain policy rules relevant to the evaluated assignment's target.
	 *
	 *   TODO how exactly do we compute it
	 */
	private Boolean isMatchingOrder = null;
	private EvaluationOrder evaluationOrder;

	private Boolean isMatchingOrderPlusOne = null;

	private boolean processMembership = false;

	private ObjectType varThisObject;

	AssignmentPathSegmentImpl(ObjectType source, String sourceDescription,
			ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
			boolean isAssignment) {
		this.source = source;
		this.sourceDescription = sourceDescription;
		this.assignmentIdi = assignmentIdi;
		this.isAssignment = isAssignment;
	}

	@Override
	public boolean isAssignment() {
		return isAssignment;
	}

	public ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> getAssignmentIdi() {
		return assignmentIdi;
	}

	@Override
	public AssignmentType getAssignment() {
		if (assignmentIdi == null || assignmentIdi.getItemNew() == null || assignmentIdi.getItemNew().isEmpty()) {
			return null;
		}
		return ((PrismContainer<AssignmentType>) assignmentIdi.getItemNew()).getValue().asContainerable();
	}

	@Override
	public QName getRelation() {
		return relation;
	}

	public void setRelation(QName relation) {
		this.relation = relation;
	}

	@Override
	public ObjectType getTarget() {
		return target;
	}

	public void setTarget(ObjectType target) {
		this.target = target;
	}
	
	@Override
	public ObjectType getSource() {
		return source;
	}

	public String getSourceDescription() {
		return sourceDescription;
	}

	public boolean isPathToSourceValid() {
		return pathToSourceValid;
	}

	public void setPathToSourceValid(boolean pathToSourceValid) {
		this.pathToSourceValid = pathToSourceValid;
	}

	public boolean isValidityOverride() {
		return validityOverride;
	}

	public void setValidityOverride(boolean validityOverride) {
		this.validityOverride = validityOverride;
	}

	public EvaluationOrder getEvaluationOrder() {
		return evaluationOrder;
	}

	public void setEvaluationOrder(EvaluationOrder evaluationOrder) {
		setEvaluationOrder(evaluationOrder, null, null);
	}

	public void setEvaluationOrder(EvaluationOrder evaluationOrder, Boolean matchingOrder, Boolean matchingOrderPlusOne) {
		this.evaluationOrder = evaluationOrder;
		this.isMatchingOrder = matchingOrder;
		this.isMatchingOrderPlusOne = matchingOrderPlusOne;
	}

	public ObjectType getOrderOneObject() {
		return varThisObject;
	}

	public void setOrderOneObject(ObjectType varThisObject) {
		this.varThisObject = varThisObject;
	}
	
	public boolean isProcessMembership() {
		return processMembership;
	}

	public void setProcessMembership(boolean processMembership) {
		this.processMembership = processMembership;
	}

	/**
	 *  Whether this assignment/inducement matches the focus level, i.e. if we should collect constructions,
	 *  focus mappings, and focus policy rules from it.
	 */
	public boolean isMatchingOrder() {
		if (isMatchingOrder == null) {
			isMatchingOrder = computeMatchingOrder(getAssignment(), evaluationOrder, 0);
		}
		return isMatchingOrder;
	}
	
	public boolean isMatchingOrderPlusOne() {
		if (isMatchingOrderPlusOne == null) {
			isMatchingOrderPlusOne = computeMatchingOrder(getAssignment(), evaluationOrder, 1);
		}
		return isMatchingOrderPlusOne;
	}

	static boolean computeMatchingOrder(AssignmentType assignmentType, EvaluationOrder evaluationOrder, int offset) {
		boolean rv;
		if (assignmentType.getOrder() == null && assignmentType.getOrderConstraint().isEmpty()) {
			// compatibility
			rv = evaluationOrder.getSummaryOrder() - offset == 1;
		} else {
			rv = true;
			if (assignmentType.getOrder() != null) {
				if (evaluationOrder.getSummaryOrder() - offset != assignmentType.getOrder()) {
					rv = false;
				}
			}
			for (OrderConstraintsType orderConstraint : assignmentType.getOrderConstraint()) {
				if (!isMatchingConstraint(orderConstraint, evaluationOrder, offset)) {
					rv = false;
					break;
				}
			}
		}
		LOGGER.trace("computeMatchingOrder => {}, for offset={}; assignment.order={}, assignment.orderConstraint={}, evaluationOrder={} ... assignment = {}",
				rv, offset, assignmentType.getOrder(), assignmentType.getOrderConstraint(), evaluationOrder);
		return rv;
	}

	private static boolean isMatchingConstraint(OrderConstraintsType orderConstraint, EvaluationOrder evaluationOrder, int offset) {
		int evaluationOrderInt = evaluationOrder.getMatchingRelationOrder(orderConstraint.getRelation()) - offset;
		if (orderConstraint.getOrder() != null) {
			return orderConstraint.getOrder() == evaluationOrderInt;
		} else {
			int orderMin = 1;
			int orderMax = 1;
			if (orderConstraint.getOrderMin() != null) {
				orderMin = XsdTypeMapper.multiplicityToInteger(orderConstraint.getOrderMin());
			}
			if (orderConstraint.getOrderMax() != null) {
				orderMax = XsdTypeMapper.multiplicityToInteger(orderConstraint.getOrderMax());
			}
			return XsdTypeMapper.isMatchingMultiplicity(evaluationOrderInt, orderMin, orderMax);
		}
	}
	
	@Override
	public boolean isDelegation() {
		return DeputyUtils.isDelegationRelation(relation);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assignmentIdi == null) ? 0 : assignmentIdi.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssignmentPathSegmentImpl other = (AssignmentPathSegmentImpl) obj;
		if (assignmentIdi == null) {
			if (other.assignmentIdi != null)
				return false;
		} else if (!assignmentIdi.equals(other.assignmentIdi))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("AssignmentPathSegment(");
		sb.append(evaluationOrder);
		if (isMatchingOrder()) {			// here is a side effect but most probably it's harmless
			sb.append("(match)");
		}
		if (isMatchingOrderPlusOne()) {
			sb.append("(match+1)");
		}
		sb.append(": ");
		sb.append(source).append(" ");
		PrismContainer<AssignmentType> assignment = (PrismContainer<AssignmentType>) assignmentIdi.getAnyItem();
		AssignmentType assignmentType = assignment != null ? assignment.getValue().asContainerable() : null;
		if (assignmentType != null && assignmentType.getConstruction() != null) {
			sb.append("Constr '").append(assignmentType.getConstruction().getDescription()).append("' ");
		}
		ObjectReferenceType targetRef = assignmentType != null ? assignmentType.getTargetRef() : null;
		if (target != null || targetRef != null) {
			sb.append("-[");
			if (relation != null) {
				sb.append(relation.getLocalPart());
			}
			sb.append("]-> ");
			if (target != null) {
				sb.append(target);
			} else {
				sb.append(ObjectTypeUtil.toShortString(targetRef, true));
			}
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "AssignmentPathSegment", indent);
		sb.append("\n");
		DebugUtil.debugDumpWithLabelLn(sb, "source", source==null?"null":source.toString(), indent + 1);
		String assignmentOrInducement = isAssignment ? "assignment" : "inducement";
		if (assignmentIdi != null) {
			DebugUtil.debugDumpWithLabelLn(sb, assignmentOrInducement + " old", String.valueOf(assignmentIdi.getItemOld()), indent + 1);
			DebugUtil.debugDumpWithLabelLn(sb, assignmentOrInducement + " delta", String.valueOf(assignmentIdi.getDelta()), indent + 1);
			DebugUtil.debugDumpWithLabelLn(sb, assignmentOrInducement + " new", String.valueOf(assignmentIdi.getItemNew()), indent + 1);
		} else {
			DebugUtil.debugDumpWithLabelLn(sb, assignmentOrInducement, "null", indent + 1);
		}
		DebugUtil.debugDumpWithLabelLn(sb, "target", target==null?"null":target.toString(), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "evaluationOrder", evaluationOrder, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "isMatchingOrder", isMatchingOrder, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "isMatchingOrderPlusOne", isMatchingOrderPlusOne, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "relation", relation, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "pathToSourceValid", pathToSourceValid, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "validityOverride", validityOverride, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "processMembership", processMembership, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "varThisObject", varThisObject==null?"null":varThisObject.toString(), indent + 1);
		return sb.toString();
	}

	@NotNull
	@Override
	public AssignmentPathSegmentType toAssignmentPathSegmentType() {
		AssignmentPathSegmentType rv = new AssignmentPathSegmentType();
		AssignmentType assignment = getAssignment();
		if (assignment != null) {
			rv.setAssignment(assignment);
			rv.setAssignmentId(assignment.getId());
		}
		if (source != null) {
			rv.setSourceRef(ObjectTypeUtil.createObjectRef(source));
			rv.setSourceDisplayName(ObjectTypeUtil.getDisplayName(source));
		}
		if (target != null) {
			rv.setTargetRef(ObjectTypeUtil.createObjectRef(target));
			rv.setTargetDisplayName(ObjectTypeUtil.getDisplayName(target));
		}
		rv.setMatchingOrder(isMatchingOrder());
		return rv;
	}
}
