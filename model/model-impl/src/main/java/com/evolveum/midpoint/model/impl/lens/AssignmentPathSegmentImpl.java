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

import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Primary duty of this class is to be a part of assignment path. (This is what is visible through its interface,
 * AssignmentPathSegment.) However, it also serves as a place where auxiliary information about assignment evaluation
 * is stored.
 *
 * @author semancik
 *
 */
@SuppressWarnings("WeakerAccess")
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

	// Last segment with the same evaluation order. Used for higher-order (summary-rewriting) inducements.
	// See e.g. TestAssignmentProcessor2.test600.

	private Integer lastEqualOrderSegmentIndex;

	/**
	 *  Assignments and inducements can carry constructions, focus mappings, and policy rules.
	 *  We can call these "assignment/inducement payload", or "payload" for short.
	 *
	 *  When looking at assignments/inducements in assignment path, payload of some assignments/inducements will be collected
	 *  to focus, while payload from others will be not. How we know what to collect?
	 *
	 *  For assignments/inducements belonging directly to the focus, we take payload from all the assignments. Not from inducements.
	 *  For assignments/inducements belonging to roles (assigned to focus), we take payload from all the inducements of order 1.
	 *  For assignments/inducements belonging to meta-roles (assigned to roles), we take payload from all the inducements of order 2.
	 *  And so on. (It is in fact a bit more complicated, as described below when discussing relations. But OK for the moment.)
	 *
	 *  To know whether to collect payload from assignment/inducement, i.e. from assignment path segment, we
	 *  define "isMatchingOrder" attribute - and collect only if value of this attribute is true.
	 *
	 *  How we compute this attribute?
	 *
	 *  Each assignment path segment has an evaluation order. First assignment has an evaluation order of 1, second
	 *  assignment has an order of 2, etc. Order of a segment can be seen as the number of assignments segments in the path
	 *  (including itself). And, for "real" assignments (i.e. not inducements), we collect payload from assignment segments
	 *  of order 1.
	 *
	 *  But what about inducements? There are two - somewhat related - questions:
	 *
	 *  1. How we compute isMatchingOrder for inducement segments?
	 *  2. How we compute evaluation order for inducement segments?
	 *
	 *  As for #1: To compute isMatchingOrder, we must take evaluation order of the _previous_ segment, and compare
	 *  it with the order (or, more generally, order constraints) of the inducement. If they match, we say that inducement
	 *  has matching order.
	 *
	 *  As for #2: It is not usual that inducements have targets (roles) with another assignments, i.e. that evaluation continues
	 *  after an inducement segment. But it definitely could happen. We can look at it this way: inducement is something like
	 *  a "shortcut" that creates an assignment where no assignment was before. E.g. if we have R1 -A-> MR1 -I-> MR2,
	 *  the "newly created" assignment is R1 -A-> MR2. I.e. as if the " -A-> MR1 -I-> " part was just replaced by " -A-> ".
	 *  If the inducement is of higher order, even more assignments are "cut out". From R1 -A-> MR1 -A-> MMR1 -(I2)-> MR2
	 *  we have R1 -A-> MR2, i.e. we cut out " -A-> MR1 -A-> MMR1 -(I2)-> " and replaced it by " -A-> ".
	 *  So it looks like that when computing new evaluation order of an inducement, we have to "go back" few steps
	 *  through the assignment path.
	 *
	 *  Such situation can also easily occur when org structures are used. As depicted in TestAssignmentProcessor2.test500
	 *  and later, imagine this:
	 *
	 *           Org1 -----I----+                                              Org2 -----I----+
	 *             ^            | (orderConstraints 1..N)                        ^            | (orderConstraints: manager: 1)
	 *             |            |                                                |            |
	 *             |            V                                                |            V
	 *           Org11        Admin                                            Org21        Admin
	 *             ^                                                             ^
	 *             |                                                         (manager)
	 *             |                                                             |
	 *            jack                                                          jack
	 *
	 *  In order to resolve such cases, we created the "resetOrder" attribute. It can be applied either to
	 *  summary order, or to one or more specific relations (please, don't specify it for both summary order
	 *  and specific relations!)
	 *
	 *  In the above examples, we could specify e.g. resetOrder=1 for summary order (for both left and right situation).
	 *  For the right one, we could instead specify resetOrder=0 for org:manager relation; although the former solution
	 *  is preferable.
	 *
	 *  By simply specifying inducement order greater than 1 (e.g. 5) without any specific order constraints,
	 *  we implicitly provide resetOrder instruction for summary order that points order-1 levels back (i.e. 4 levels back
	 *  for order=5).
	 *
	 *  Generally, it is preferred to use resetOrder for summary order. It works well with both normal and target
	 *  evaluation order. When resetting individual components, target evaluation order can have problems, as shown
	 *  in TestEvaluationProcessor2.test520.
	 *
	 *  ----
	 *
	 *  Because evaluation order can "increase" and "decrease", it is possible that it goes to zero or below, and then
	 *  increase back to positive numbers. Is that OK? Imagine this:
	 *
	 *  (Quite an ugly example, but such things might exist.)
	 *
	 *  Metarole:CrewMember ----I----+  Metarole:Sailors
	 *           A                   |  A
	 *           |                   |  |
	 *           |                   V  |
	 *         Pirate               Sailor
	 *           A
	 *           |
	 *           |
	 *          jack
	 *
	 *  When evaluating jack->Pirate assignment, it is OK to collect from everything (Pirate, CrewMember, Sailor, Sailors).
	 *
	 *  But when evaluating Pirate as a focal object (forget about jack for the moment), we have Pirate->CrewMember assignment.
	 *  For this assignment we should ignore payload from Sailor (obviously, because the order is not matching), but from
	 *  Metarole:Sailors as well. Payload from Sailors is not connected to Pirate in any meaningful way. (For example, if
	 *  Sailors prescribes an account construction for Sailor, it is of no use to collect this construction when evaluating
	 *  Pirate as focal object!)
	 *
	 *  Evaluating relations
	 *  ====================
	 *
	 *  With the arrival of various kinds of relations (deputy, manager, approver, owner) things got a bit complicated.
	 *  For instance, the deputy relation cannot be used to determine evaluation order in a usual way, because if
	 *  we have this situation:
	 *
	 *  Pirate -----I-----> Sailor
	 *    A
	 *    | (default)
	 *    |
	 *   jack
	 *    A
	 *    | (deputy)
	 *    |
	 *  barbossa
	 *
	 *  we obviously want to have barbossa to obtain all payload from roles Pirate and Sailor: exactly as jack does.
	 *  So, the evaluation order of " barbossa -A-> jack -A-> Pirate " should be 1, not two. So deputy is a very special
	 *  kind of relation, that does _not_ increase the traditional evaluation order. But we really want to record
	 *  the fact that the deputy is on the assignment path; therefore, besides traditional "scalar" evaluation order
	 *  (called "summaryOrder") we maintain evaluation orders for each relation separately. In the above example,
	 *  the evaluation orders would be:
	 *     barbossa--->jack     summary: 0, deputy: 1, default: 0
	 *     jack--->Pirate       summary: 1, deputy: 1, default: 1
	 *     Pirate-I->Sailor     summary: 1, deputy: 1, default: 1 (because the inducement has a default order of 1)
	 *
	 *  When we determine matchingOrder for an inducement (question #1 above), we can ask about summary order,
	 *  as well as about individual components (deputy and default, in this case).
	 *
	 *  Actually, we have three categories of relations (see MID-3581):
	 *
	 *  - membership relations: apply membership references, payload, authorizations, gui config
	 *  - delegation relations: similar to membership, bud different handling of order
	 *  - other relations: apply membership references but in limited way; payload, authorizations and gui config
	 *    are - by default - not applied.
	 *
	 *  Currently, membership relations are: default (i.e. member), manager, and meta.
	 *  Delegation: deputy.
	 *  Other: approver, owner, and custom ones.
	 *
	 *  As for the "other" relations: they bring membership information, but limited to the target that is directly
	 *  assigned. So if jack is an approver for role Landluber, his roleMembershipRef will contain ref to Landluber
	 *  but not e.g. to role Earthworm that is induced by Landluber. In a similar way, payload from targets assigned
	 *  by "other" relations is not collected.
	 *
	 *  Both of this can be overridden by using specific orderConstraints on particular inducement.
	 *  Set of order constraint is considered to match evaluation order with "other" relations, if for each such "other"
	 *  relation it contains related constraint. So, if one explicitly wants an inducement to be applied when
	 *  "approver" relation is encountered, he may do so.
     *
	 *  Note: authorizations and gui config information are not considered to be a payload, because they are not
	 *  part of an assignment/inducement - they are part of a role. In the future we might move them into
	 *  assignments/inducements.
	 *
	 *  Collecting target policy rules
	 *  ==============================
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
	 *   as an item related to evaluated assignment's target. Therefore besides isMatchingOrder we maintain isMatchingOrderForTarget
	 *   that marks all segments (assignments/inducements) that contain policy rules relevant to the evaluated assignment's target.
	 *
	 *   The computation is done by maintaining two evaluationOrders:
	 *    - plain evaluationOrder that is related to the focus object [starts from ZERO.advance(first assignment relation)]
	 *    - special evaluationOrderForTarget that is related to the target of the assignment being evaluated [starts from ZERO]
	 *
	 *   Finally, how we distinguish between "this target" and "other targets" policy rules? Current approach
	 *   is like this: count the number of times the evaluation order for target reached ZERO level. First encounter with
	 *   that level is on "this target". And we assume that each subsequent marks a target that is among "others".
	 *
	 *   @see AssignmentEvaluator#appliesDirectly
	 */
	private Boolean isMatchingOrder = null;
	private EvaluationOrder evaluationOrder;
	private Boolean isMatchingOrderForTarget = null;
	private EvaluationOrder evaluationOrderForTarget;

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

	@SuppressWarnings("unused")
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

	@SuppressWarnings("SameParameterValue")
	public void setValidityOverride(boolean validityOverride) {
		this.validityOverride = validityOverride;
	}

	public EvaluationOrder getEvaluationOrder() {
		return evaluationOrder;
	}

	public void setEvaluationOrder(EvaluationOrder evaluationOrder) {
		setEvaluationOrder(evaluationOrder, null);
	}

	public void setEvaluationOrder(EvaluationOrder evaluationOrder, Boolean matchingOrder) {
		this.evaluationOrder = evaluationOrder;
		this.isMatchingOrder = matchingOrder;
	}

	public EvaluationOrder getEvaluationOrderForTarget() {
		return evaluationOrderForTarget;
	}

	public void setEvaluationOrderForTarget(EvaluationOrder evaluationOrder) {
		setEvaluationOrderForTarget(evaluationOrder, null);
	}

	public void setEvaluationOrderForTarget(EvaluationOrder evaluationOrderForTarget, Boolean matching) {
		this.evaluationOrderForTarget = evaluationOrderForTarget;
		this.isMatchingOrderForTarget = matching;
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

	@Override
	public boolean isMatchingOrder() {
		if (isMatchingOrder == null) {
			isMatchingOrder = computeMatchingOrder(evaluationOrder, getAssignment());
		}
		return isMatchingOrder;
	}

	@Override
	public boolean isMatchingOrderForTarget() {
		if (isMatchingOrderForTarget == null) {
			isMatchingOrderForTarget = computeMatchingOrder(evaluationOrderForTarget, getAssignment());
		}
		return isMatchingOrderForTarget;
	}

	static boolean computeMatchingOrder(EvaluationOrder evaluationOrder, AssignmentType assignmentType) {
		return computeMatchingOrder(evaluationOrder, assignmentType.getOrder(), assignmentType.getOrderConstraint());
	}

	static boolean computeMatchingOrder(EvaluationOrder evaluationOrder, Integer assignmentOrder,
			List<OrderConstraintsType> assignmentOrderConstraint) {
		boolean rv;
		List<QName> extraRelations = new ArrayList<>(evaluationOrder.getExtraRelations());
		if (assignmentOrder == null && assignmentOrderConstraint.isEmpty()) {
			// compatibility
			rv = evaluationOrder.getSummaryOrder() == 1;
		} else {
			rv = true;
			if (assignmentOrder != null) {
				if (evaluationOrder.getSummaryOrder() != assignmentOrder) {
					rv = false;
				}
			}
			for (OrderConstraintsType orderConstraint : assignmentOrderConstraint) {
				if (!isMatchingConstraint(orderConstraint, evaluationOrder)) {
					rv = false;
					break;
				}
				extraRelations.removeIf(r -> QNameUtil.match(r, orderConstraint.getRelation()));
			}
		}
		if (!extraRelations.isEmpty()) {
			rv = false;
		}
		LOGGER.trace("computeMatchingOrder => {}, for assignment.order={}, assignment.orderConstraint={}, evaluationOrder={}, remainingExtraRelations={}",
				rv, assignmentOrder, assignmentOrderConstraint, evaluationOrder, extraRelations);
		return rv;
	}

	private static boolean isMatchingConstraint(OrderConstraintsType orderConstraint, EvaluationOrder evaluationOrder) {
		int evaluationOrderInt = evaluationOrder.getMatchingRelationOrder(orderConstraint.getRelation());
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
		return ObjectTypeUtil.isDelegationRelation(relation);
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
		if (isMatchingOrderForTarget()) {		// the same here
			sb.append("(match-target)");
		}
		sb.append(": ");
		sb.append(source).append(" ");
		if (!isAssignment) {
			sb.append("inducement ");
		}
		PrismContainer<AssignmentType> assignment = (PrismContainer<AssignmentType>) assignmentIdi.getAnyItem();
		AssignmentType assignmentType = assignment != null ? assignment.getValue().asContainerable() : null;
		if (assignmentType != null) {
			sb.append("id:").append(assignmentType.getId()).append(" ");
			if (assignmentType.getConstruction() != null) {
				sb.append("Constr '").append(assignmentType.getConstruction().getDescription()).append("' ");
			}
			if (assignmentType.getFocusMappings() != null) {
				sb.append("FMappings (").append(assignmentType.getFocusMappings().getMapping().size()).append(") ");
			}
			if (assignmentType.getPolicyRule() != null) {
				sb.append("Rule '").append(assignmentType.getPolicyRule().getName()).append("' ");
			}
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
		// TODO eventually remove this
		if (lastEqualOrderSegmentIndex != null) {
			sb.append(", lastEqualOrder: ").append(lastEqualOrderSegmentIndex);
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
		DebugUtil.debugDumpWithLabelLn(sb, "isMatchingOrderForTarget", isMatchingOrderForTarget, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "relation", relation, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "pathToSourceValid", pathToSourceValid, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "validityOverride", validityOverride, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "processMembership", processMembership, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "lastEqualOrderSegmentIndex", lastEqualOrderSegmentIndex, indent + 1);
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

	public Integer getLastEqualOrderSegmentIndex() {
		return lastEqualOrderSegmentIndex;
	}

	public void setLastEqualOrderSegmentIndex(Integer lastEqualOrderSegmentIndex) {
		this.lastEqualOrderSegmentIndex = lastEqualOrderSegmentIndex;
	}
}
