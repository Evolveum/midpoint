/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.objectReferenceListToPrismReferenceValues;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Finds link targets based on a collection of selectors.
 */
class LinkTargetFinder implements AutoCloseable {

    private static final Trace LOGGER = TraceManager.getTrace(LinkTargetFinder.class);

    private static final String OP_GET_TARGETS = LinkTargetFinder.class.getName() + ".getTargets";

    @NotNull private final PolicyRuleScriptExecutor beans;
    @NotNull private final LensContext<?> context;
    @NotNull private final EvaluatedPolicyRuleImpl rule;
    @NotNull private final OperationResult result;

    LinkTargetFinder(@NotNull PolicyRuleScriptExecutor policyRuleScriptExecutor,
            @NotNull LensContext<?> context, @NotNull EvaluatedPolicyRuleImpl rule,
            @NotNull OperationResult parentResult) {
        this.beans = policyRuleScriptExecutor;
        this.context = context;
        this.rule = rule;
        this.result = parentResult.createMinorSubresult(OP_GET_TARGETS);
    }

    List<PrismObject<? extends ObjectType>> getTargets(LinkTargetObjectSelectorType selector) {
        try {
            return getTargetsInternal(selector);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    private List<PrismObject<? extends ObjectType>> getTargetsInternal(LinkTargetObjectSelectorType selector) {
        LOGGER.trace("Selecting matching link targets for {} with rule={}", selector, rule);

        // We must create new set because we will remove links from it later.
        Set<PrismReferenceValue> links = new HashSet<>(getLinksInChangeSituation(selector));
        LOGGER.trace("Links matching change situation: {}", links);

        if (!selector.getRelation().isEmpty()) {
            applyRelationFilter(links, selector.getRelation());
            LOGGER.trace("Links matching also relation(s): {}", links);
        }

        if (isTrue(selector.isMatchesRuleAssignment())) {
            applyMatchingRuleAssignment(links);
            LOGGER.trace("Links matching also policy rule assignment: {}", links);
        }

        if (isTrue(selector.isMatchesConstraint())) {
            applyMatchingPolicyRuleConstraints(links);
            LOGGER.trace("Links matching also policy constraints: {}", links);
        }

        if (selector.getType() != null) {
            // This is just to avoid resolving objects with non-matching type
            applyObjectType(links, selector.getType());
            LOGGER.trace("Links matching also object type: {}", links);
        }

        Collection<PrismObject<? extends ObjectType>> resolvedObjects = resolveLinks(links);
        List<PrismObject<? extends ObjectType>> matchingObjects = getMatchingObjects(resolvedObjects, selector);

        LOGGER.trace("Final matching objects: {}", matchingObjects);
        return matchingObjects;
    }

    private List<PrismObject<? extends ObjectType>> getMatchingObjects(Collection<PrismObject<? extends ObjectType>> objects,
            ObjectSelectorType selector) {
        List<PrismObject<? extends ObjectType>> matching = new ArrayList<>();
        for (PrismObject<? extends ObjectType> object : objects) {
            try {
                if (beans.repositoryService.selectorMatches(selector, object, null, LOGGER, "")) {
                    matching.add(object);
                }
            } catch (CommonException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't match link target {} for {} when filtering link targets", e,
                        object, context.getFocusContextRequired().getObjectAny());
            }
        }
        return matching;
    }

    private Collection<PrismObject<? extends ObjectType>> resolveLinks(Set<PrismReferenceValue> links) {
        Map<String, PrismObject<? extends ObjectType>> objects = new HashMap<>();
        for (PrismReferenceValue link : links) {
            String oid = link.getOid();
            if (!objects.containsKey(oid)) {
                try {
                    Class<? extends ObjectType> clazz = getClassForType(link.getTargetType());
                    objects.put(oid, beans.repositoryService.getObject(clazz, oid, null, result));
                } catch (SchemaException | ObjectNotFoundException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve reference {} in {} when applying script on link targets",
                            e, link, context.getFocusContextRequired().getObjectAny());
                }
            }
        }
        return objects.values();
    }

    private void applyObjectType(Set<PrismReferenceValue> links, QName type) {
        Class<?> clazz = getClassForType(type);
        links.removeIf(link -> beans.prismContext.getSchemaRegistry().isAssignableFrom(clazz, link.getTargetType()));
    }

    @NotNull
    private Class<? extends ObjectType> getClassForType(QName type) {
        Class<?> clazz = beans.prismContext.getSchemaRegistry().getCompileTimeClass(type);
        if (clazz == null) {
            throw new IllegalStateException("Unknown type " + type);
        } else if (!ObjectType.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Type " + type + " is not an ObjectType, it is " + clazz);
        } else {
            //noinspection unchecked
            return (Class<? extends ObjectType>) clazz;
        }
    }

    private void applyMatchingRuleAssignment(Set<PrismReferenceValue> links) {
        EvaluatedAssignmentImpl<?> evaluatedAssignment = rule.getEvaluatedAssignment();
        if (evaluatedAssignment == null) {
            throw new IllegalStateException("'matchesRuleAssignment' filter is selected but there's no evaluated assignment"
                    + " known for policy rule {}" + rule);
        }
        Set<String> oids = evaluatedAssignment.getRoles().stream()
                .filter(EvaluatedAssignmentTargetImpl::appliesToFocus)
                .map(EvaluatedAssignmentTargetImpl::getOid)
                .collect(Collectors.toSet());
        links.removeIf(link -> !oids.contains(link.getOid()));
    }

    private void applyMatchingPolicyRuleConstraints(Set<PrismReferenceValue> links) {
        Set<String> oids = rule.getAllTriggers().stream()
                .flatMap(trigger -> trigger.getTargetObjects().stream())
                .map(PrismObject::getOid)
                .collect(Collectors.toSet());
        links.removeIf(link -> !oids.contains(link.getOid()));
    }

    private void applyRelationFilter(Set<PrismReferenceValue> links, List<QName> relations) {
        links.removeIf(link -> !beans.prismContext.relationMatches(relations, link.getRelation()));
    }

    @NotNull
    private Set<PrismReferenceValue> getLinksInChangeSituation(LinkTargetObjectSelectorType selector) {
        switch (defaultIfNull(selector.getChangeSituation(), LinkChangeSituationType.ALWAYS)) {
            case ALWAYS:
                return SetUtils.union(getLinkedOld(), getLinkedNew());
            case ADDED:
                return SetUtils.difference(getLinkedNew(), getLinkedOld());
            case REMOVED:
                return SetUtils.difference(getLinkedOld(), getLinkedNew());
            case UNCHANGED:
                return SetUtils.intersection(getLinkedOld(), getLinkedNew());
            case CHANGED:
                return SetUtils.disjunction(getLinkedOld(), getLinkedNew());
            case IN_NEW:
                return getLinkedNew();
            case IN_OLD:
                return getLinkedOld();
            default:
                throw new AssertionError(selector.getChangeSituation());
        }
    }

    private Set<PrismReferenceValue> getLinkedOld() {
        ObjectType objectOld = asObjectable(context.getFocusContextRequired().getObjectOld());
        return objectOld instanceof AssignmentHolderType ?
                new HashSet<>(objectReferenceListToPrismReferenceValues(((AssignmentHolderType) objectOld).getRoleMembershipRef())) :
                emptySet();
    }

    private Set<PrismReferenceValue> getLinkedNew() {
        ObjectType objectNew = asObjectable(context.getFocusContextRequired().getObjectNew());
        return objectNew instanceof AssignmentHolderType ?
                new HashSet<>(objectReferenceListToPrismReferenceValues(((AssignmentHolderType) objectNew).getRoleMembershipRef())) :
                emptySet();
    }

    @Override
    public void close() {
        result.computeStatusIfUnknown();
    }
}
