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

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedMultiplicityTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.PLUS;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class MultiplicityConstraintEvaluator implements PolicyConstraintEvaluator<MultiplicityPolicyConstraintType> {

	private static final String CONSTRAINT_KEY_PREFIX = "multiplicityConstraint.";
	private static final String KEY_MIN = "min.";
	private static final String KEY_MAX = "max.";
	private static final String KEY_OBJECT = "object";
	private static final String KEY_TARGET = "target";

	@Autowired private ConstraintEvaluatorHelper evaluatorHelper;
	@Autowired private PrismContext prismContext;
	@Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<MultiplicityPolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		if (rctx instanceof ObjectPolicyRuleEvaluationContext) {
			return evaluateForObject(constraint, (ObjectPolicyRuleEvaluationContext<F>) rctx, result);
		} else if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
			return evaluateForAssignment(constraint, (AssignmentPolicyRuleEvaluationContext<F>) rctx, result);
		} else {
			return null;
		}
	}

	// TODO shouldn't we return all triggers?
	private <F extends FocusType> EvaluatedPolicyRuleTrigger evaluateForObject(
			JAXBElement<MultiplicityPolicyConstraintType> constraint,
			ObjectPolicyRuleEvaluationContext<F> ctx, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		PrismObject<? extends ObjectType> target = ctx.focusContext.getObjectAny();
		if (target == null || !(target.asObjectable() instanceof AbstractRoleType)) {
			return null;
		}
		List<QName> relationsToCheck = constraint.getValue().getRelation().isEmpty()
				? Collections.singletonList(SchemaConstants.ORG_DEFAULT) : constraint.getValue().getRelation();

		AbstractRoleType targetRole = (AbstractRoleType) target.asObjectable();
		boolean isMin = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MIN_ASSIGNEES)
				|| QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_OBJECT_MIN_ASSIGNEES_VIOLATION);
		boolean isMax = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MAX_ASSIGNEES)
				|| QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_OBJECT_MAX_ASSIGNEES_VIOLATION);
		assert isMin || isMax;
		// TODO cache repository call results
		if (isMin) {
			Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getValue().getMultiplicity());
			if (requiredMultiplicity <= 0) {
				return null;            // unbounded or 0
			}
			for (QName relationToCheck : relationsToCheck) {
				int currentAssignees = getNumberOfAssigneesExceptMyself(targetRole, null, relationToCheck, result);
				if (currentAssignees < requiredMultiplicity) {
					return new EvaluatedMultiplicityTrigger(PolicyConstraintKindType.MIN_ASSIGNEES_VIOLATION,
							constraint.getValue(),
							getMessage(constraint.getValue(), ctx, result, KEY_MIN, KEY_OBJECT, target,
									requiredMultiplicity, relationToCheck.getLocalPart()),
							getShortMessage(constraint.getValue(), ctx, result, KEY_MIN, KEY_OBJECT, target,
									requiredMultiplicity, relationToCheck.getLocalPart()));
				}
			}
			return null;
		} else {
			Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getValue().getMultiplicity());
			if (requiredMultiplicity < 0) {
				return null;			// unbounded
			}
			for (QName relationToCheck : relationsToCheck) {
				int currentAssigneesExceptMyself = getNumberOfAssigneesExceptMyself(targetRole, null, relationToCheck, result);
				if (currentAssigneesExceptMyself >= requiredMultiplicity) {
					return new EvaluatedMultiplicityTrigger(PolicyConstraintKindType.MAX_ASSIGNEES_VIOLATION,
							constraint.getValue(),
							getMessage(constraint.getValue(), ctx, result, KEY_MAX, KEY_OBJECT, target,
									requiredMultiplicity, relationToCheck.getLocalPart()),
							getShortMessage(constraint.getValue(), ctx, result, KEY_MAX, KEY_OBJECT, target,
									requiredMultiplicity, relationToCheck.getLocalPart()));
				}
			}
			return null;
		}
	}

	private <F extends FocusType> EvaluatedPolicyRuleTrigger evaluateForAssignment(
			JAXBElement<MultiplicityPolicyConstraintType> constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		if (!ctx.isDirect) {
			return null;
		}
		if (ctx.inPlus) {
			if (!ctx.evaluatedAssignment.isPresentInCurrentObject()) {
				return checkAssigneeConstraints(constraint, ctx.lensContext, ctx.evaluatedAssignment, PLUS, ctx, result);		// only really new assignments
			}
		} else if (ctx.inMinus) {
			if (ctx.evaluatedAssignment.isPresentInCurrentObject()) {
				return checkAssigneeConstraints(constraint, ctx.lensContext, ctx.evaluatedAssignment, PlusMinusZero.MINUS, ctx, result);		// only assignments that are really deleted
			}
		}
		return null;
	}

	private <F extends FocusType> EvaluatedPolicyRuleTrigger<MultiplicityPolicyConstraintType> checkAssigneeConstraints(JAXBElement<MultiplicityPolicyConstraintType> constraint,
			LensContext<F> context, EvaluatedAssignment<F> assignment, PlusMinusZero plusMinus,
			AssignmentPolicyRuleEvaluationContext<F> ctx, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		PrismObject<?> target = assignment.getTarget();
		if (target == null || !(target.asObjectable() instanceof AbstractRoleType)) {
			return null;
		}
		AbstractRoleType targetRole = (AbstractRoleType) target.asObjectable();
		QName relation = ObjectTypeUtil.normalizeRelation(assignment.getRelation());
		if (!containsRelation(constraint.getValue(), relation)) {
			return null;
		}
		String focusOid = context.getFocusContext() != null ? context.getFocusContext().getOid() : null;
		boolean isMin = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MIN_ASSIGNEES);
		boolean isMax = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MAX_ASSIGNEES);
		assert isMin || isMax;
		// TODO cache repository call results
		if (isMin) {
			Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getValue().getMultiplicity());
			if (requiredMultiplicity <= 0) {
				return null;            // unbounded or 0
			}
			// Complain only if the situation is getting worse
			int currentAssigneesExceptMyself = getNumberOfAssigneesExceptMyself(targetRole, focusOid, relation, result);
			if (currentAssigneesExceptMyself < requiredMultiplicity && plusMinus == PlusMinusZero.MINUS) {
				return new EvaluatedMultiplicityTrigger(PolicyConstraintKindType.MIN_ASSIGNEES_VIOLATION,
						constraint.getValue(),
						getMessage(constraint.getValue(), ctx, result, KEY_MIN, KEY_TARGET, targetRole.asPrismObject(),
								requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself),
						getShortMessage(constraint.getValue(), ctx, result, KEY_MIN, KEY_TARGET, targetRole.asPrismObject(),
								requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself));
			} else {
				return null;
			}
		} else {
			Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getValue().getMultiplicity());
			if (requiredMultiplicity < 0) {
				return null;			// unbounded
			}
			// Complain only if the situation is getting worse
			int currentAssigneesExceptMyself = getNumberOfAssigneesExceptMyself(targetRole, focusOid, relation, result);
			if (currentAssigneesExceptMyself >= requiredMultiplicity && plusMinus == PLUS) {
				return new EvaluatedMultiplicityTrigger(PolicyConstraintKindType.MAX_ASSIGNEES_VIOLATION,
						constraint.getValue(),
						getMessage(constraint.getValue(), ctx, result, KEY_MAX, KEY_TARGET, targetRole.asPrismObject(),
								requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself+1),
						getShortMessage(constraint.getValue(), ctx, result, KEY_MAX, KEY_TARGET, targetRole.asPrismObject(),
								requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself+1));
			}
		}
		return null;
	}

	private boolean containsRelation(MultiplicityPolicyConstraintType constraint, QName relation) {
		return getConstraintRelations(constraint).stream()
				.anyMatch(constraintRelation -> ObjectTypeUtil.relationMatches(constraintRelation, relation));
	}

	private List<QName> getConstraintRelations(MultiplicityPolicyConstraintType constraint) {
		return !constraint.getRelation().isEmpty() ?
				constraint.getRelation() :
				Collections.singletonList(SchemaConstants.ORG_DEFAULT);
	}

	/**
	 * Returns numbers of assignees with the given relation name.
	 */
	private int getNumberOfAssigneesExceptMyself(AbstractRoleType target, String selfOid, QName relation, OperationResult result)
			throws SchemaException {
		if (target.getOid() == null) {
			return 0;
		}
		S_AtomicFilterExit q = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(
						new PrismReferenceValue(target.getOid()).relation(relation));
		if (selfOid != null) {
			q = q.and().not().id(selfOid);
		}
		ObjectQuery query = q.build();
		return repositoryService.countObjects(FocusType.class, query, null, result);
	}

	private <F extends FocusType> LocalizableMessage getMessage(MultiplicityPolicyConstraintType constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result, String key1, String key2,
			PrismObject<?> target, Object... args)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + key1 + key2)
				.arg(ObjectTypeUtil.createTechnicalObjectSpecification(target))
				.args(args)
				.build();
		return evaluatorHelper.createLocalizableMessage(constraint, rctx, builtInMessage, result);
	}

	private <F extends FocusType> LocalizableMessage getShortMessage(MultiplicityPolicyConstraintType constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result, String key1, String key2,
			PrismObject<?> target, Object... args)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + key1 + key2)
				.arg(ObjectTypeUtil.createObjectSpecification(target))
				.args(args)
				.build();
		return evaluatorHelper.createLocalizableShortMessage(constraint, rctx, builtInMessage, result);
	}
}
