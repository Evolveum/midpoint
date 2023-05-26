/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.HeteroEqualsChecker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.*;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

public class PolicyRuleTypeUtil {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleTypeUtil.class);

    private static final char JOIN_AND = '*';
    private static final char JOIN_OR = '|';

    private static final Map<String, String> CONSTRAINT_NAMES = new HashMap<>();

    private static final String SYMBOL_MIN = "min";
    private static final String SYMBOL_MAX = "max";
    private static final String SYMBOL_ASSIGNMENT_MODIFICATION = "amod";
    private static final String SYMBOL_OBJECT_MODIFICATION = "omod";
    private static final String SYMBOL_OBJECT_STATE = "ostate";
    private static final String SYMBOL_ASSIGNMENT_STATE = "astate";
    private static final String SYMBOL_EXCLUSION = "exc";
    private static final String SYMBOL_HAS_ASSIGNMENT = "hasass";
    private static final String SYMBOL_NO_ASSIGNMENT = "noass";
    private static final String SYMBOL_OBJECT_TIME_VALIDITY = "otime";
    private static final String SYMBOL_ASSIGNMENT_TIME_VALIDITY = "atime";
    private static final String SYMBOL_SITUATION = "sit";
    private static final String SYMBOL_CUSTOM = "custom";
    private static final String SYMBOL_TRANSITION = "trans";
    private static final String SYMBOL_ALWAYS_TRUE = "true";
    private static final String SYMBOL_ORPHANED = "orphaned";

    static {
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MIN_ASSIGNEES.getLocalPart(), SYMBOL_MIN);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MAX_ASSIGNEES.getLocalPart(), SYMBOL_MAX);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ASSIGNMENT.getLocalPart(), SYMBOL_ASSIGNMENT_MODIFICATION);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_MODIFICATION.getLocalPart(), SYMBOL_OBJECT_MODIFICATION);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_OBJECT_STATE.getLocalPart(), SYMBOL_OBJECT_STATE);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ASSIGNMENT_STATE.getLocalPart(), SYMBOL_ASSIGNMENT_STATE);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_EXCLUSION.getLocalPart(), SYMBOL_EXCLUSION);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_HAS_ASSIGNMENT.getLocalPart(), SYMBOL_HAS_ASSIGNMENT);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_HAS_NO_ASSIGNMENT.getLocalPart(), SYMBOL_NO_ASSIGNMENT);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_OBJECT_TIME_VALIDITY.getLocalPart(), SYMBOL_OBJECT_TIME_VALIDITY);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ASSIGNMENT_TIME_VALIDITY.getLocalPart(), SYMBOL_ASSIGNMENT_TIME_VALIDITY);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_SITUATION.getLocalPart(), SYMBOL_SITUATION);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_CUSTOM.getLocalPart(), SYMBOL_CUSTOM);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_TRANSITION.getLocalPart(), SYMBOL_TRANSITION);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ALWAYS_TRUE.getLocalPart(), SYMBOL_ALWAYS_TRUE);
        CONSTRAINT_NAMES.put(PolicyConstraintsType.F_ORPHANED.getLocalPart(), SYMBOL_ORPHANED);
    }

    public static String toShortString(PolicyRuleType rule) {
        if (rule != null) {
            return toShortString(rule.getPolicyConstraints()) + "→" + toShortString(rule.getPolicyActions());
        } else {
            return null;
        }
    }

    public static String toShortString(JAXBElement<? extends AbstractPolicyConstraintType> constraint) {
        StringBuilder sb = new StringBuilder();
        toShortString(sb, constraint, ' ');
        return sb.toString();
    }

    public static String toShortString(PolicyConstraintsType constraints) {
        return toShortString(constraints, JOIN_AND);
    }

    public static String toShortString(PolicyConstraintsType constraints, char join) {
        if (constraints == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // we ignore refs to be able to dump even unresolved policy rules
        for (JAXBElement<AbstractPolicyConstraintType> constraint : toConstraintsList(constraints, false, true)) {
            toShortString(sb, constraint, join);
        }
        for (PolicyConstraintReferenceType ref : constraints.getRef()) {
            if (sb.length() > 0) {
                sb.append(join);
            }
            sb.append("ref:").append(ref.getName());
        }
        return sb.toString();
    }

    private static void toShortString(StringBuilder sb, JAXBElement<? extends AbstractPolicyConstraintType> constraint, char join) {
        QName name = constraint.getName();
        String abbreviation = CONSTRAINT_NAMES.get(name.getLocalPart());
        if (sb.length() > 0) {
            sb.append(join);
        }
        if (QNameUtil.match(name, PolicyConstraintsType.F_AND)) {
            sb.append('(');
            sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), JOIN_AND));
            sb.append(')');
        } else if (QNameUtil.match(name, PolicyConstraintsType.F_OR)) {
            sb.append('(');
            sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), JOIN_OR));
            sb.append(')');
        } else if (QNameUtil.match(name, PolicyConstraintsType.F_NOT)) {
            sb.append('(');
            sb.append(toShortString((PolicyConstraintsType) constraint.getValue(), JOIN_AND));
            sb.append(')');
        } else if (QNameUtil.match(name, PolicyConstraintsType.F_TRANSITION)) {
            TransitionPolicyConstraintType trans = (TransitionPolicyConstraintType) constraint.getValue();
            sb.append(SYMBOL_TRANSITION);
            sb.append(toTransSymbol(trans.isStateBefore()));
            sb.append(toTransSymbol(trans.isStateAfter()));
            sb.append('(');
            sb.append(toShortString(trans.getConstraints(), JOIN_AND));
            sb.append(')');
        } else {
            sb.append(abbreviation != null ? abbreviation : name.getLocalPart());
        }
    }

    private static String toTransSymbol(Boolean state) {
        if (state != null) {
            return state ? "1" : "0";
        } else {
            return "x";
        }
    }

    public static String toShortString(PolicyActionsType actions) {
        return toShortString(actions, null);
    }

    /**
     * @param enabledActions if null we don't consider action enabled/disabled state
     */
    public static String toShortString(PolicyActionsType actions, List<PolicyActionType> enabledActions) {
        if (actions == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (actions.getEnforcement() != null) {
            sb.append("enforce");
            if (enabledActions != null && filterActions(enabledActions, EnforcementPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (!actions.getApproval().isEmpty()) {
            sb.append(" approve");
            if (enabledActions != null && filterActions(enabledActions, ApprovalPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getRemediation() != null) {
            sb.append(" remedy");
            if (enabledActions != null && filterActions(enabledActions, RemediationPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getPrune() != null) {
            sb.append(" prune");
            if (enabledActions != null && filterActions(enabledActions, PrunePolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getCertification() != null) {
            sb.append(" certify");
            if (enabledActions != null && filterActions(enabledActions, CertificationPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (!actions.getNotification().isEmpty()) {
            sb.append(" notify");
            if (enabledActions != null && filterActions(enabledActions, NotificationPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getRecord() != null) {
            sb.append(" record");
            if (enabledActions != null && filterActions(enabledActions, RecordPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (!actions.getScriptExecution().isEmpty()) {
            sb.append(" execute");
            if (enabledActions != null && filterActions(enabledActions, ScriptExecutionPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        if (actions.getSuspendTask() != null) {
            sb.append(" suspend");
            if (enabledActions != null && filterActions(enabledActions, SuspendTaskPolicyActionType.class).isEmpty()) {
                sb.append("X");
            }
        }
        return sb.toString().trim();
    }

//    public static String toShortString(AbstractPolicyConstraintType constraint) {
//        if (constraint == null) {
//            return "null";
//        }
//        StringBuilder sb = new StringBuilder();
//        sb.append(getConstraintClassShortcut(constraint.getClass()));
//        if (constraint.getName() != null) {
//            sb.append(":").append(constraint.getName());
//        }
//        return sb.toString();
//    }

    public static String toDiagShortcut(PolicyConstraintKindType constraintKind) {
        if (constraintKind == null) {
            return "null";
        }
        switch (constraintKind) {
            case ASSIGNMENT_MODIFICATION: return SYMBOL_ASSIGNMENT_MODIFICATION;
            case OBJECT_MODIFICATION: return SYMBOL_OBJECT_MODIFICATION;
            case EXCLUSION: return SYMBOL_EXCLUSION;
            case MAX_ASSIGNEES_VIOLATION: return SYMBOL_MAX;
            case MIN_ASSIGNEES_VIOLATION: return SYMBOL_MIN;
            case OBJECT_STATE: return SYMBOL_OBJECT_STATE;
            case ASSIGNMENT_STATE: return SYMBOL_ASSIGNMENT_STATE;
            case HAS_ASSIGNMENT: return SYMBOL_HAS_ASSIGNMENT;
            case HAS_NO_ASSIGNMENT: return SYMBOL_NO_ASSIGNMENT;
            case OBJECT_TIME_VALIDITY: return SYMBOL_OBJECT_TIME_VALIDITY;
            case ASSIGNMENT_TIME_VALIDITY: return SYMBOL_ASSIGNMENT_TIME_VALIDITY;
            case SITUATION: return SYMBOL_SITUATION;
            default: return constraintKind.toString();
        }
    }

    public static void visit(List<EvaluatedPolicyRuleTriggerType> triggers, Consumer<EvaluatedPolicyRuleTriggerType> visitor) {
        for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
            visitor.accept(trigger);
            if (trigger instanceof EvaluatedSituationTriggerType) {
                EvaluatedSituationTriggerType situationTrigger = (EvaluatedSituationTriggerType) trigger;
                for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
                    visit(sourceRule.getTrigger(), visitor);
                }
            }
        }
    }

    public static boolean triggerCollectionsEqual(Collection<EvaluatedPolicyRuleTriggerType> triggers,
            Collection<EvaluatedPolicyRuleTriggerType> currentTriggersUnpacked) {
        HeteroEqualsChecker<EvaluatedPolicyRuleTriggerType, EvaluatedPolicyRuleTriggerType> equalsChecker = (t1, t2) -> {
            if (!(t1 instanceof EvaluatedSituationTriggerType) || !(t2 instanceof EvaluatedSituationTriggerType)) {
                return Objects.equals(t1, t2);
            }
            EvaluatedSituationTriggerType st1 = (EvaluatedSituationTriggerType) t1;
            EvaluatedSituationTriggerType st2 = (EvaluatedSituationTriggerType) t2;
            // TODO deduplicate this (against standard equals) somehow
            // Until that is done, update after each change to triggers beans structure
            return Objects.equals(st1.getRef(), st2.getRef())
                    && Objects.equals(st1.getTriggerId(), st2.getTriggerId())
                    && Objects.equals(st1.getRuleName(), st2.getRuleName())
                    && Objects.equals(st1.getConstraintName(), st2.getConstraintName())
                    && Objects.equals(st1.getConstraintKind(), st2.getConstraintKind())
                    && Objects.equals(st1.getMessage(), st2.getMessage())
                    && Objects.equals(st1.getShortMessage(), st2.getShortMessage())
                    && Objects.equals(st1.getPresentationOrder(), st2.getPresentationOrder())
                    && MiscUtil.unorderedCollectionEquals(st1.getSourceRule(), st2.getSourceRule());
        };
        return MiscUtil.unorderedCollectionEquals(currentTriggersUnpacked, triggers, equalsChecker);
    }

    public static List<PolicyActionType> getAllActions(PolicyActionsType actions) {
        List<PolicyActionType> rv = new ArrayList<>();
        if (actions == null) {
            return rv;
        }
        addIgnoreNull(rv, actions.getEnforcement());
        rv.addAll(actions.getApproval());
        addIgnoreNull(rv, actions.getRecord());
        rv.addAll(actions.getNotification());
        rv.addAll(actions.getScriptExecution());
        addIgnoreNull(rv, actions.getCertification());
        addIgnoreNull(rv, actions.getPrune());
        addIgnoreNull(rv, actions.getRemediation());
        addIgnoreNull(rv, actions.getSuspendTask());
        return rv;
    }

    public static <T extends PolicyActionType> List<T> filterActions(List<PolicyActionType> actions, Class<T> clazz) {
        //noinspection unchecked
        return actions.stream()
                .filter(a -> clazz.isAssignableFrom(a.getClass()))
                .map(a -> (T) a)
                .collect(Collectors.toList());
    }

    public static @NotNull List<TimeValidityPolicyConstraintType> getTimeValidityConstraints(PolicyRuleType policyRule) {
        return policyRule != null && policyRule.getPolicyConstraints() != null ?
                policyRule.getPolicyConstraints().getObjectTimeValidity() : List.of();
    }

    public static boolean hasNotificationActions(PolicyRuleType policyRule) {
        return policyRule != null && policyRule.getPolicyActions() != null &&
                !policyRule.getPolicyActions().getNotification().isEmpty();
    }

    @FunctionalInterface
    interface ConstraintVisitor {
        /**
         * Returns false if the process is to be finished.
         */
        boolean visit(QName name, AbstractPolicyConstraintType constraint);
    }

    /**
     * Returns false if the process was stopped by the consumer.
     * All references should be resolved.
     */
    public static boolean accept(
            PolicyConstraintsType pc,
            ConstraintVisitor visitor,
            boolean deep,
            boolean alsoRoots,
            QName rootElementName,
            boolean ignoreRefs) {
        if (pc == null) {
            return true;
        }
        boolean rv;
        if (alsoRoots) {
            assert rootElementName != null;
            rv = visit(Collections.singletonList(pc), rootElementName, visitor);
        } else {
            rv = true;
        }
        rv = rv && visit(pc.getMinAssignees(), F_MIN_ASSIGNEES, visitor)
                && visit(pc.getMaxAssignees(), F_MAX_ASSIGNEES, visitor)
                && visit(pc.getObjectMinAssigneesViolation(), F_OBJECT_MIN_ASSIGNEES_VIOLATION, visitor)
                && visit(pc.getObjectMaxAssigneesViolation(), F_OBJECT_MAX_ASSIGNEES_VIOLATION, visitor)
                && visit(pc.getExclusion(), F_EXCLUSION, visitor)
                && visit(pc.getAssignment(), F_ASSIGNMENT, visitor)
                && visit(pc.getHasAssignment(), F_HAS_ASSIGNMENT, visitor)
                && visit(pc.getHasNoAssignment(), F_HAS_NO_ASSIGNMENT, visitor)
                && visit(pc.getModification(), F_MODIFICATION, visitor)
                && visit(pc.getObjectTimeValidity(), F_OBJECT_TIME_VALIDITY, visitor)
                && visit(pc.getAssignmentTimeValidity(), F_ASSIGNMENT_TIME_VALIDITY, visitor)
                && visit(pc.getAssignmentState(), F_ASSIGNMENT_STATE, visitor)
                && visit(pc.getObjectState(), F_OBJECT_STATE, visitor)
                && visit(pc.getSituation(), F_SITUATION, visitor)
                && visit(pc.getCustom(), F_CUSTOM, visitor)
                && visit(pc.getTransition(), F_TRANSITION, visitor)
                && visit(pc.getAlwaysTrue(), F_ALWAYS_TRUE, visitor)
                && visit(pc.getOrphaned(), F_ORPHANED, visitor)
                && visit(pc.getAnd(), F_AND, visitor)
                && visit(pc.getOr(), F_OR, visitor)
                && visit(pc.getNot(), F_NOT, visitor);

        if (!ignoreRefs && !pc.getRef().isEmpty()) {
            throw new IllegalStateException("Unresolved constraint reference (" + pc.getRef() + ").");
        }

        if (deep) {
            for (TransitionPolicyConstraintType transitionConstraint : pc.getTransition()) {
                rv = rv && accept(transitionConstraint.getConstraints(), visitor, true, alsoRoots, F_AND, ignoreRefs);
            }
            rv = rv
                    && accept(pc.getAnd(), visitor, alsoRoots, F_AND, ignoreRefs)
                    && accept(pc.getOr(), visitor, alsoRoots, F_OR, ignoreRefs)
                    && accept(pc.getNot(), visitor, alsoRoots, F_AND, ignoreRefs);
        }
        return rv;
    }

    private static boolean visit(List<? extends AbstractPolicyConstraintType> constraints, QName name, ConstraintVisitor visitor) {
        for (AbstractPolicyConstraintType constraint : constraints) {
            if (!visitor.visit(name, constraint)) {
                return false;
            }
        }
        return true;
    }

    private static boolean accept(
            List<PolicyConstraintsType> constraintsList,
            ConstraintVisitor matcher,
            boolean alsoRoots,
            QName rootElementName,
            boolean ignoreRefs) {
        for (PolicyConstraintsType constraints : constraintsList) {
            if (!accept(constraints, matcher, true, alsoRoots, rootElementName, ignoreRefs)) {
                return false;
            }
        }
        return true;
    }

    public static List<JAXBElement<AbstractPolicyConstraintType>> toConstraintsList(
            PolicyConstraintsType pc, boolean deep, boolean ignoreRefs) {
        List<JAXBElement<AbstractPolicyConstraintType>> rv = new ArrayList<>();
        accept(
                pc,
                (name, c) -> {
                    rv.add(toConstraintJaxbElement(name, c));
                    return true;
                },
                deep, false, null, ignoreRefs);
        return rv;
    }

    @NotNull
    private static JAXBElement<AbstractPolicyConstraintType> toConstraintJaxbElement(QName name, AbstractPolicyConstraintType c) {
        return new JAXBElement<>(name, AbstractPolicyConstraintType.class, c);
    }

    /**
     * Returns true if this policy rule can be applied to an assignment.
     * By default, rules that have only object-related constraints, are said to be applicable to objects only
     * (even if technically they could be applied to assignments as well).
     */
    public static boolean isApplicableToAssignment(PolicyRuleType rule) {
        PolicyRuleEvaluationTargetType evaluationTarget = rule.getEvaluationTarget();
        if (evaluationTarget != null) {
            return evaluationTarget == PolicyRuleEvaluationTargetType.ASSIGNMENT;
        } else {
            return hasAssignmentOnlyConstraint(rule) || !hasObjectRelatedConstraint(rule);
        }
    }

    /**
     * Returns true if this policy rule can be applied to an object as a whole.
     */
    public static boolean isApplicableToObject(PolicyRuleType rule) {
        PolicyRuleEvaluationTargetType evaluationTarget = rule.getEvaluationTarget();
        if (evaluationTarget != null) {
            return evaluationTarget == PolicyRuleEvaluationTargetType.OBJECT;
        } else {
            return !hasAssignmentOnlyConstraint(rule);
        }
    }

    /**
     * Returns true if this policy rule can be applied to a projection.
     */
    public static boolean isApplicableToProjection(PolicyRuleType rule) {
        return rule.getEvaluationTarget() == PolicyRuleEvaluationTargetType.PROJECTION;
    }

    private static boolean hasAssignmentOnlyConstraint(PolicyRuleType rule) {
        // 'accept' continues until isNotAssignmentOnly is false; and returns false then --> so we return true in that case (i.e. we have found assignmentOnly-constraint)
        return !accept(rule.getPolicyConstraints(), PolicyRuleTypeUtil::isNotAssignmentOnly, true, true, F_AND, false);
    }

    // do we have a constraint that indicates a use against object?
    private static boolean hasObjectRelatedConstraint(PolicyRuleType rule) {
        // 'accept' continues until isNotObjectRelated is false; and returns false then --> so we return true in that case (i.e. we have found object-related constraint)
        return !accept(
                rule.getPolicyConstraints(),
                PolicyRuleTypeUtil::isNotObjectRelated,
                true, true, F_AND, false);
    }

    private static final Set<Class<? extends AbstractPolicyConstraintType>> ASSIGNMENTS_ONLY_CONSTRAINTS_CLASSES =
            new HashSet<>(Arrays.asList(AssignmentModificationPolicyConstraintType.class, ExclusionPolicyConstraintType.class));

    private static boolean isNotAssignmentOnly(QName name, AbstractPolicyConstraintType c) {
        boolean assignmentOnly = ASSIGNMENTS_ONLY_CONSTRAINTS_CLASSES.contains(c.getClass())
                || QNameUtil.match(name, PolicyConstraintsType.F_ASSIGNMENT_STATE)
                || QNameUtil.match(name, PolicyConstraintsType.F_ASSIGNMENT_TIME_VALIDITY)
                || QNameUtil.match(name, PolicyConstraintsType.F_MIN_ASSIGNEES)     // these can be evaluated also on object level
                || QNameUtil.match(name, PolicyConstraintsType.F_MAX_ASSIGNEES)     // but it requires evaluationTarget=object set (to maintain backwards compatibility)
                ;
        //System.out.println("isAssignmentOnly: " + name.getLocalPart() + "/" + c.getClass().getSimpleName() + " -> " + rv);
        return !assignmentOnly;
    }

    private static final Set<Class<? extends AbstractPolicyConstraintType>> OBJECT_RELATED_CONSTRAINTS_CLASSES =
            new HashSet<>(Collections.singletonList(HasAssignmentPolicyConstraintType.class));

    private static boolean isNotObjectRelated(QName name, AbstractPolicyConstraintType c) {
        boolean objectRelated = OBJECT_RELATED_CONSTRAINTS_CLASSES.contains(c.getClass())
                || QNameUtil.match(name, PolicyConstraintsType.F_MODIFICATION)
                || QNameUtil.match(name, PolicyConstraintsType.F_OBJECT_STATE)
                || QNameUtil.match(name, PolicyConstraintsType.F_OBJECT_TIME_VALIDITY)
                || QNameUtil.match(name, PolicyConstraintsType.F_OBJECT_MIN_ASSIGNEES_VIOLATION)
                || QNameUtil.match(name, PolicyConstraintsType.F_OBJECT_MAX_ASSIGNEES_VIOLATION)
                ;
        //System.out.println("isObjectRelated: " + name.getLocalPart() + "/" + c.getClass().getSimpleName() + " -> " + rv);
        return !objectRelated;
    }

    interface ConstraintResolver {
        @NotNull
        JAXBElement<? extends AbstractPolicyConstraintType> resolve(@NotNull String name)
                throws ObjectNotFoundException, SchemaException;
    }

    public static class LazyMapConstraintsResolver implements ConstraintResolver {
        // the supplier provides list of entries instead of a map because we want to make duplicate checking at one place
        // (in this class)
        @NotNull private final List<Supplier<List<Map.Entry<String, JAXBElement<? extends AbstractPolicyConstraintType>>>>> constraintsSuppliers;
        @NotNull private final Map<String, JAXBElement<? extends AbstractPolicyConstraintType>> constraintsMap = new HashMap<>();
        private int usedSuppliers = 0;

        @SafeVarargs
        private LazyMapConstraintsResolver(
                @NotNull Supplier<List<Map.Entry<String, JAXBElement<? extends AbstractPolicyConstraintType>>>>... constraintsSuppliers) {
            this.constraintsSuppliers = Arrays.asList(constraintsSuppliers);
        }

        @NotNull
        @Override
        public JAXBElement<? extends AbstractPolicyConstraintType> resolve(@NotNull String name)
                throws ObjectNotFoundException, SchemaException {
            for (;;) {
                JAXBElement<? extends AbstractPolicyConstraintType> rv = constraintsMap.get(name);
                if (rv != null) {
                    return rv;
                }
                if (usedSuppliers >= constraintsSuppliers.size()) {
                    throw new ObjectNotFoundException(
                            String.format(
                                    "No policy constraint named '%s' could be found. Known constraints: %s",
                                    name, constraintsMap.keySet()),
                            AbstractPolicyConstraintType.class, name);
                }
                List<Map.Entry<String, JAXBElement<? extends AbstractPolicyConstraintType>>> newEntries =
                        constraintsSuppliers.get(usedSuppliers++).get();
                for (Map.Entry<String, JAXBElement<? extends AbstractPolicyConstraintType>> newEntry : newEntries) {
                    JAXBElement<? extends AbstractPolicyConstraintType> existingElement = constraintsMap.get(newEntry.getKey());
                    JAXBElement<? extends AbstractPolicyConstraintType> newElement = newEntry.getValue();
                    if (existingElement != null) {
                        if (!QNameUtil.match(existingElement.getName(), newElement.getName())
                                || !existingElement.getValue().equals(newElement.getValue())) {
                            LOGGER.error("Conflicting definitions of '{}' found:\n>>> new:\n{}\n>>> existing:\n{}",
                                    newEntry.getKey(),
                                    PrismContext.get().xmlSerializer().serialize(newElement),
                                    PrismContext.get().xmlSerializer().serialize(existingElement));
                            throw new SchemaException("Conflicting definitions of '" + newEntry.getKey() + "' found.");
                        }
                    } else {
                        constraintsMap.put(newEntry.getKey(), newElement);
                    }
                }
            }
        }
    }

    private static void resolveConstraintReferences(PolicyConstraintsType pc, ConstraintResolver resolver) {
        // This works even on chained rules because on any PolicyConstraintsType the visitor is called on a root
        // (thus resolving the references) before it is called on children. And those children are already resolved;
        // so, any references contained within them get also resolved.
        accept(pc, (name, c) -> {
            if (c instanceof PolicyConstraintsType) {
                try {
                    resolveLocalReferences((PolicyConstraintsType) c, resolver);
                } catch (ObjectNotFoundException | SchemaException e) {
                    MiscUtil.throwExceptionAsUnchecked(e);
                }
            }
            return true;
        }, true, true, F_AND, true);
    }

    @SuppressWarnings("unchecked")
    private static void resolveLocalReferences(PolicyConstraintsType pc, ConstraintResolver resolver)
            throws ObjectNotFoundException, SchemaException {
        for (PolicyConstraintReferenceType ref : pc.getRef()) {
            String refName = ref != null ? ref.getName() : null;
            if (StringUtils.isEmpty(refName)) {
                throw new SchemaException("Illegal empty reference: " + ref);
            }
            List<String> pathToRoot = getPathToRoot(pc);
            if (pathToRoot.contains(refName)) {
                throw new SchemaException("Trying to resolve cyclic reference to constraint '" + refName + "'. Contained in: " + pathToRoot);
            }
            JAXBElement<? extends AbstractPolicyConstraintType> resolved = resolver.resolve(refName);
            QName constraintName = resolved.getName();
            AbstractPolicyConstraintType constraintValue = resolved.getValue();
            PrismContainer<? extends AbstractPolicyConstraintType> container = pc.asPrismContainerValue().findOrCreateContainer(constraintName);
            container.add(constraintValue.asPrismContainerValue().clone());
        }
        pc.getRef().clear();
    }

    // ugly hacking, but should work
    @SuppressWarnings("unchecked")
    private static List<String> getPathToRoot(PolicyConstraintsType pc) {
        List<String> rv = new ArrayList<>();
        computePathToRoot(rv, pc.asPrismContainerValue());
        return rv;
    }

    @SuppressWarnings("unchecked")
    private static void computePathToRoot(List<String> path, PrismContainerValue<? extends AbstractPolicyConstraintType> pc) {
        path.add(pc.asContainerable().getName());
        if (pc.getParent() instanceof PrismContainer) {
            PrismContainer<? extends AbstractPolicyConstraintType> container =
                    (PrismContainer<? extends AbstractPolicyConstraintType>) pc.getParent();
            PrismValue containerParentValue = container.getParent();
            if (containerParentValue != null &&
                    ((PrismContainerValue) containerParentValue).asContainerable() instanceof AbstractPolicyConstraintType) {
                computePathToRoot(path, ((PrismContainerValue) container.getParent()));
            }
        }
    }

    public static void resolveConstraintReferences(
            List<PolicyRuleType> rules, Collection<? extends PolicyRuleType> otherRules) {
        LazyMapConstraintsResolver resolver =
                new LazyMapConstraintsResolver(createConstraintsSupplier(rules), createConstraintsSupplier(otherRules));
        for (PolicyRuleType rule : rules) {
            resolveConstraintReferences(rule.getPolicyConstraints(), resolver);
        }
    }

    @NotNull
    private static Supplier<List<Map.Entry<String, JAXBElement<? extends AbstractPolicyConstraintType>>>> createConstraintsSupplier(
            Collection<? extends PolicyRuleType> rules) {
        return () -> {
            List<Map.Entry<String, JAXBElement<? extends AbstractPolicyConstraintType>>> constraints = new ArrayList<>();
            for (PolicyRuleType rule : rules) {
                accept(rule.getPolicyConstraints(), (elementName, c) -> {
                    if (StringUtils.isNotEmpty(c.getName())) {
                        constraints.add(new AbstractMap.SimpleEntry<>(c.getName(), toConstraintJaxbElement(elementName, c)));
                    }
                    return true;
                }, true, true, F_AND, true);
            }
            return constraints;
        };
    }

    @NotNull
    public static List<EvaluatedExclusionTriggerType> getAllExclusionTriggers(List<EvaluatedPolicyRuleType> rules) {
        List<EvaluatedExclusionTriggerType> rv = new ArrayList<>();
        getExclusionTriggersFromRules(rv, rules);
        return rv;
    }

    private static void getExclusionTriggersFromRules(List<EvaluatedExclusionTriggerType> rv, List<EvaluatedPolicyRuleType> rules) {
        for (EvaluatedPolicyRuleType rule : rules) {
            getExclusionTriggersFromRule(rv, rule);
        }
    }

    private static void getExclusionTriggersFromRule(List<EvaluatedExclusionTriggerType> rv, EvaluatedPolicyRuleType rule) {
        getExclusionTriggersFromTriggers(rv, rule.getTrigger());
    }

    private static void getExclusionTriggersFromTriggers(List<EvaluatedExclusionTriggerType> rv, List<EvaluatedPolicyRuleTriggerType> triggers) {
        for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
            if (trigger instanceof EvaluatedExclusionTriggerType) {
                rv.add((EvaluatedExclusionTriggerType) trigger);
            } else if (trigger instanceof EvaluatedEmbeddingTriggerType) {
                getExclusionTriggersFromTriggers(rv, ((EvaluatedEmbeddingTriggerType) trigger).getEmbedded());
            } else if (trigger instanceof EvaluatedSituationTriggerType) {
                getExclusionTriggersFromRules(rv, ((EvaluatedSituationTriggerType) trigger).getSourceRule());
            }
        }
    }

    public static String createId(String containingObjectOid, Long containerId) {
        return containingObjectOid + ":" + containerId;
    }

    public static String createId(String containingObjectOid) {
        return containingObjectOid + ":rule";
    }
}
