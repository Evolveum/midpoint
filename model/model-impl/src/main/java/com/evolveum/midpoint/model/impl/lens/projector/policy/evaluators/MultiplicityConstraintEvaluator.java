/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.PLUS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedMultiplicityTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@Component
public class MultiplicityConstraintEvaluator
        implements PolicyConstraintEvaluator<MultiplicityPolicyConstraintType, EvaluatedMultiplicityTrigger> {

    private static final String OP_EVALUATE = MultiplicityConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY_PREFIX = "multiplicityConstraint.";
    private static final String KEY_MIN = "min.";
    private static final String KEY_MAX = "max.";
    private static final String KEY_OBJECT = "object";
    private static final String KEY_TARGET = "target";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @Override
    public @NotNull <O extends ObjectType> Collection<EvaluatedMultiplicityTrigger> evaluate(
            @NotNull JAXBElement<MultiplicityPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<O> rctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (rctx instanceof ObjectPolicyRuleEvaluationContext) {
                return evaluateForObject(constraint, (ObjectPolicyRuleEvaluationContext<?>) rctx, result);
            } else if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
                return evaluateForAssignment(constraint, (AssignmentPolicyRuleEvaluationContext<?>) rctx, result);
            } else {
                return List.of();
            }
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private @NotNull Collection<EvaluatedMultiplicityTrigger> evaluateForObject(
            JAXBElement<MultiplicityPolicyConstraintType> constraint,
            ObjectPolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        PrismObject<? extends ObjectType> target = ctx.elementContext.getObjectAny();
        if (target == null || !(target.asObjectable() instanceof AbstractRoleType)) {
            return List.of();
        }
        List<QName> relationsToCheck = constraint.getValue().getRelation().isEmpty()
                ? Collections.singletonList(prismContext.getDefaultRelation()) : constraint.getValue().getRelation();

        AbstractRoleType targetRole = (AbstractRoleType) target.asObjectable();
        boolean isMin = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MIN_ASSIGNEES)
                || QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_OBJECT_MIN_ASSIGNEES_VIOLATION);
        boolean isMax = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MAX_ASSIGNEES)
                || QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_OBJECT_MAX_ASSIGNEES_VIOLATION);
        if (!isMin && !isMax) {
            throw new AssertionError("!isMin and !isMax");
        }
        // TODO cache repository call results
        Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getValue().getMultiplicity());
        if (requiredMultiplicity == null) {
            return List.of();
        }
        if (isMin) {
            if (requiredMultiplicity <= 0) {
                return List.of(); // unbounded or 0
            }
            List<EvaluatedMultiplicityTrigger> triggers = new ArrayList<>();
            for (QName relationToCheck : relationsToCheck) {
                int currentAssignees = getNumberOfAssigneesExceptMyself(targetRole, null, relationToCheck, result);
                if (currentAssignees < requiredMultiplicity) {
                    triggers.add(new EvaluatedMultiplicityTrigger(
                            PolicyConstraintKindType.MIN_ASSIGNEES_VIOLATION,
                            constraint.getValue(),
                            getMessage(constraint, ctx, result, KEY_MIN, KEY_OBJECT, target,
                                    requiredMultiplicity, relationToCheck.getLocalPart()),
                            getShortMessage(constraint, ctx, result, KEY_MIN, KEY_OBJECT, target,
                                    requiredMultiplicity, relationToCheck.getLocalPart())));
                }
            }
            return triggers;
        } else {
            if (requiredMultiplicity < 0) {
                return List.of(); // unbounded
            }
            List<EvaluatedMultiplicityTrigger> triggers = new ArrayList<>();
            for (QName relationToCheck : relationsToCheck) {
                int currentAssigneesExceptMyself = getNumberOfAssigneesExceptMyself(targetRole, null, relationToCheck, result);
                if (currentAssigneesExceptMyself > requiredMultiplicity) {
                    triggers.add(new EvaluatedMultiplicityTrigger(
                            PolicyConstraintKindType.MAX_ASSIGNEES_VIOLATION,
                            constraint.getValue(),
                            getMessage(constraint, ctx, result, KEY_MAX, KEY_OBJECT, target,
                                    requiredMultiplicity, relationToCheck.getLocalPart()),
                            getShortMessage(constraint, ctx, result, KEY_MAX, KEY_OBJECT, target,
                                    requiredMultiplicity, relationToCheck.getLocalPart())));
                }
            }
            return triggers;
        }
    }

    private <AH extends AssignmentHolderType> @NotNull Collection<EvaluatedMultiplicityTrigger> evaluateForAssignment(
            JAXBElement<MultiplicityPolicyConstraintType> constraint,
            AssignmentPolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (!ctx.isDirect()) {
            return List.of();
        }
        if (ctx.isAdded) {
            if (!ctx.evaluatedAssignment.isPresentInCurrentObject()) {
                // only really new assignments
                return checkAssigneeConstraints(constraint, ctx.evaluatedAssignment, PLUS, ctx, result);
            }
        } else if (ctx.isDeleted) {
            if (ctx.evaluatedAssignment.isPresentInCurrentObject()) {
                // only assignments that are really deleted
                return checkAssigneeConstraints(constraint, ctx.evaluatedAssignment, PlusMinusZero.MINUS, ctx, result);
            }
        }
        return List.of();
    }

    private <AH extends AssignmentHolderType> @NotNull Collection<EvaluatedMultiplicityTrigger> checkAssigneeConstraints(
            JAXBElement<MultiplicityPolicyConstraintType> constraint,
            EvaluatedAssignment assignment,
            PlusMinusZero plusMinus,
            AssignmentPolicyRuleEvaluationContext<AH> ctx,
            OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<?> target = assignment.getTarget();
        if (target == null || !(target.asObjectable() instanceof AbstractRoleType)) {
            return List.of();
        }
        AbstractRoleType targetRole = (AbstractRoleType) target.asObjectable();
        QName relation = assignment.getNormalizedRelation();
        if (relation == null || !containsRelation(constraint.getValue(), relation)) {
            return List.of();
        }
        String focusOid = ctx.getFocusContext().getOid();
        boolean isMin = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MIN_ASSIGNEES);
        boolean isMax = QNameUtil.match(constraint.getName(), PolicyConstraintsType.F_MAX_ASSIGNEES);
        if (!isMin && !isMax) {
            throw new AssertionError("!isMin and !isMax");
        }
        // TODO cache repository call results
        Integer requiredMultiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getValue().getMultiplicity());
        if (isMin) {
            if (requiredMultiplicity <= 0) {
                return List.of(); // unbounded or 0
            }
            // Complain only if the situation is getting worse
            int currentAssigneesExceptMyself = getNumberOfAssigneesExceptMyself(targetRole, focusOid, relation, result);
            if (currentAssigneesExceptMyself < requiredMultiplicity && plusMinus == PlusMinusZero.MINUS) {
                return List.of(new EvaluatedMultiplicityTrigger(
                        PolicyConstraintKindType.MIN_ASSIGNEES_VIOLATION,
                        constraint.getValue(),
                        getMessage(constraint, ctx, result, KEY_MIN, KEY_TARGET, targetRole.asPrismObject(),
                                requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself),
                        getShortMessage(constraint, ctx, result, KEY_MIN, KEY_TARGET, targetRole.asPrismObject(),
                                requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself)));
            } else {
                return List.of();
            }
        } else {
            if (requiredMultiplicity < 0) {
                return List.of(); // unbounded
            }
            // Complain only if the situation is getting worse
            int currentAssigneesExceptMyself = getNumberOfAssigneesExceptMyself(targetRole, focusOid, relation, result);
            if (currentAssigneesExceptMyself >= requiredMultiplicity && plusMinus == PLUS) {
                return List.of(new EvaluatedMultiplicityTrigger(
                        PolicyConstraintKindType.MAX_ASSIGNEES_VIOLATION,
                        constraint.getValue(),
                        getMessage(constraint, ctx, result, KEY_MAX, KEY_TARGET, targetRole.asPrismObject(),
                                requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself + 1),
                        getShortMessage(constraint, ctx, result, KEY_MAX, KEY_TARGET, targetRole.asPrismObject(),
                                requiredMultiplicity, relation.getLocalPart(), currentAssigneesExceptMyself + 1)));
            }
        }
        return List.of();
    }

    private boolean containsRelation(MultiplicityPolicyConstraintType constraint, QName relation) {
        return getConstraintRelations(constraint).stream()
                .anyMatch(constraintRelation -> prismContext.relationMatches(constraintRelation, relation));
    }

    private List<QName> getConstraintRelations(MultiplicityPolicyConstraintType constraint) {
        return !constraint.getRelation().isEmpty() ?
                constraint.getRelation() :
                Collections.singletonList(prismContext.getDefaultRelation());
    }

    /**
     * Returns numbers of assignees with the given relation name.
     */
    private int getNumberOfAssigneesExceptMyself(AbstractRoleType target, String selfOid, QName relation, OperationResult result)
            throws SchemaException {
        if (target.getOid() == null) {
            return 0;
        }
        S_FilterExit q = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(
                        prismContext.itemFactory().createReferenceValue(target.getOid()).relation(relation));
        if (selfOid != null) {
            q = q.and().not().id(selfOid);
        }
        ObjectQuery query = q.build();
        return repositoryService.countObjects(FocusType.class, query, null, result);
    }

    private LocalizableMessage getMessage(
            JAXBElement<MultiplicityPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> rctx,
            OperationResult result,
            String key1,
            String key2,
            PrismObject<?> target,
            Object... args)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + key1 + key2)
                .arg(ObjectTypeUtil.createDisplayInformation(target, true))
                .args(args)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, rctx, builtInMessage, result);
    }

    private LocalizableMessage getShortMessage(
            JAXBElement<MultiplicityPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> rctx,
            OperationResult result,
            String key1,
            String key2,
            PrismObject<?> target,
            Object... args)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + key1 + key2)
                .arg(ObjectTypeUtil.createDisplayInformation(target, false))
                .args(args)
                .build();
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, rctx, builtInMessage, result);
    }
}
