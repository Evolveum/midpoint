/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.policy.PolicyRuleDumpUtil;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.util.HeteroEqualsChecker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility class related to policy rules, common to all layers (up to GUI).
 *
 * TODO consider splitting into more focused classes
 *
 * @see PolicyRuleDumpUtil
 */
public class PolicyRuleTypeUtil {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleTypeUtil.class);

    // TODO unused; consider removing
    public static void accept(List<EvaluatedPolicyRuleTriggerType> triggers, Consumer<EvaluatedPolicyRuleTriggerType> visitor) {
        for (EvaluatedPolicyRuleTriggerType trigger : triggers) {
            visitor.accept(trigger);
            if (trigger instanceof EvaluatedSituationTriggerType situationTrigger) {
                for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
                    accept(sourceRule.getTrigger(), visitor);
                }
            }
        }
    }

    // TODO unused; consider removing in the future
    public static boolean triggerCollectionsEqual(Collection<EvaluatedPolicyRuleTriggerType> triggers,
            Collection<EvaluatedPolicyRuleTriggerType> currentTriggersUnpacked) {
        HeteroEqualsChecker<EvaluatedPolicyRuleTriggerType, EvaluatedPolicyRuleTriggerType> equalsChecker = (t1, t2) -> {
            if (!(t1 instanceof EvaluatedSituationTriggerType st1) || !(t2 instanceof EvaluatedSituationTriggerType st2)) {
                return Objects.equals(t1, t2);
            }
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

    public static <T extends PolicyActionType> List<PolicyActionConfigItem<T>> filterActions(
            @NotNull Collection<? extends PolicyActionConfigItem<?>> actionsConfigItems, @NotNull Class<T> actionBeanClass) {
        //noinspection unchecked
        return actionsConfigItems.stream()
                .filter(a -> actionBeanClass.isAssignableFrom(a.value().getClass()))
                .map(a -> (PolicyActionConfigItem<T>) a)
                .collect(Collectors.toList());
    }

    @FunctionalInterface
    public interface ConstraintVisitor {
        /**
         * Visits given constraint.
         *
         * @param name {@link JAXBElement} name of the constraint (e.g. `minAssignees`, `assignment`, etc.)
         * @param constraint the constraint itself (bean with the value)
         * @return {@code false} if the visiting process is to be finished
         */
        boolean visit(QName name, AbstractPolicyConstraintType constraint);
    }

    /**
     * Visits all constraints in given {@link PolicyConstraintsType}.
     *
     * @param pc the constraints to visit
     * @param deep whether we want to recursely visit embedded constraints (and, or, not, transition)
     * @param alsoRoots whether we want to call the visitor also for the root (rootElementName must be set then)
     * @param ignoreRefs {@code false} if we want to visit referenced constraints (which must be resolved then)
     * @return {@code false} if the process was stopped by the {@link ConstraintVisitor}.
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
            rv = accept(pc, rootElementName, visitor);
        } else {
            rv = true;
        }
        rv = rv && accept(pc.getMinAssignees(), F_MIN_ASSIGNEES, visitor)
                && accept(pc.getMaxAssignees(), F_MAX_ASSIGNEES, visitor)
                && accept(pc.getObjectMinAssigneesViolation(), F_OBJECT_MIN_ASSIGNEES_VIOLATION, visitor)
                && accept(pc.getObjectMaxAssigneesViolation(), F_OBJECT_MAX_ASSIGNEES_VIOLATION, visitor)
                && accept(pc.getExclusion(), F_EXCLUSION, visitor)
                && accept(pc.getAssignment(), F_ASSIGNMENT, visitor)
                && accept(pc.getHasAssignment(), F_HAS_ASSIGNMENT, visitor)
                && accept(pc.getHasNoAssignment(), F_HAS_NO_ASSIGNMENT, visitor)
                && accept(pc.getRequirement(), F_REQUIREMENT, visitor)
                && accept(pc.getModification(), F_MODIFICATION, visitor)
                && accept(pc.getObjectTimeValidity(), F_OBJECT_TIME_VALIDITY, visitor)
                && accept(pc.getAssignmentTimeValidity(), F_ASSIGNMENT_TIME_VALIDITY, visitor)
                && accept(pc.getAssignmentState(), F_ASSIGNMENT_STATE, visitor)
                && accept(pc.getObjectState(), F_OBJECT_STATE, visitor)
                && accept(pc.getSituation(), F_SITUATION, visitor)
                && accept(pc.getCustom(), F_CUSTOM, visitor)
                && accept(pc.getTransition(), F_TRANSITION, visitor)
                && accept(pc.getAlwaysTrue(), F_ALWAYS_TRUE, visitor)
                && accept(pc.getOrphaned(), F_ORPHANED, visitor)
                && accept(pc.getAnd(), F_AND, visitor)
                && accept(pc.getOr(), F_OR, visitor)
                && accept(pc.getNot(), F_NOT, visitor)
                && accept(pc.getExecutionAttempts(), F_EXECUTION_ATTEMPTS, visitor)
                && accept(pc.getExecutionTime(), F_EXECUTION_TIME, visitor)
                && accept(pc.getItemProcessingResult(), F_ITEM_PROCESSING_RESULT, visitor);

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

    private static boolean accept(AbstractPolicyConstraintType constraint, QName name, ConstraintVisitor visitor) {
        return accept(MiscUtil.singletonOrEmptyList(constraint), name, visitor);
    }

    private static boolean accept(List<? extends AbstractPolicyConstraintType> constraints, QName name, ConstraintVisitor visitor) {
        for (AbstractPolicyConstraintType constraint : constraints) {
            if (!visitor.visit(name, constraint)) {
                return false;
            }
        }
        return true;
    }

    private static boolean accept(
            List<PolicyConstraintsType> constraintsList,
            ConstraintVisitor visitor,
            boolean alsoRoots,
            QName rootElementName,
            boolean ignoreRefs) {
        for (PolicyConstraintsType constraints : constraintsList) {
            if (!accept(constraints, visitor, true, alsoRoots, rootElementName, ignoreRefs)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the top-level list of constraints under a {@link PolicyConstraintsType}.
     *
     * @param ignoreRefs if {@code false}, the returned list will also contain referenced constraints (which must be resolved then)
     */
    public static List<JAXBElement<AbstractPolicyConstraintType>> toConstraintsList(PolicyConstraintsType pc, boolean ignoreRefs) {
        List<JAXBElement<AbstractPolicyConstraintType>> constraints = new ArrayList<>();
        accept(
                pc,
                (name, c) -> {
                    constraints.add(toConstraintJaxbElement(name, c));
                    return true;
                },
                false, false, null, ignoreRefs);
        return constraints;
    }

    @NotNull
    private static JAXBElement<AbstractPolicyConstraintType> toConstraintJaxbElement(QName name, AbstractPolicyConstraintType c) {
        return new JAXBElement<>(name, AbstractPolicyConstraintType.class, c);
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
            for (; ; ) {
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
        if (pc.getParent() instanceof PrismContainer<? extends AbstractPolicyConstraintType> container) {
            PrismValue containerParentValue = container.getParent();
            if (containerParentValue != null &&
                    ((PrismContainerValue) containerParentValue).asContainerable() instanceof AbstractPolicyConstraintType) {
                computePathToRoot(path, ((PrismContainerValue) container.getParent()));
            }
        }
    }

    /** FIXME what about the origin of the constraints? */
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
