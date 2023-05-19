/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.prettyActionUrl;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.emptySet;

/**
 * Evaluates given {@link Authorization} either for the applicability to the current situation, represented by action
 * (operation URL), phase, object, assignment target and related aspects, and so on; or to determine appropriate
 * security filter - see {@link AuthorizationFilterEvaluation}.
 *
 * It is a part of various {@link SecurityEnforcerImpl} operations.
 */
public class AuthorizationEvaluation {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull final Authorization authorization;
    @NotNull private final Lazy<String> lazyDescription;

    @NotNull final EnforcerOperation<?> op;
    @Nullable private final MidPointPrincipal principal;
    @NotNull private final Beans b;
    @NotNull private final Task task;
    @NotNull final OperationResult result;

    AuthorizationEvaluation(
            @NotNull Authorization authorization,
            @NotNull EnforcerOperation<?> op,
            @NotNull OperationResult result) {
        this.authorization = authorization;
        this.op = op;
        this.principal = op.principal;
        this.b = op.b;
        this.task = op.task;
        this.result = result;
        this.lazyDescription = Lazy.from(() -> this.authorization.getHumanReadableDesc());

        traceStart();
    }

    public @NotNull Authorization getAuthorization() {
        return authorization;
    }

    boolean isApplicableToAction(@NotNull String operationUrl) {
        List<String> autzActions = authorization.getAction();
        if (autzActions.contains(operationUrl) || autzActions.contains(AuthorizationConstants.AUTZ_ALL_URL)) {
            return true;
        }
        if (op.traceEnabled) {
            LOGGER.trace("      Authorization not applicable for operation {}", prettyActionUrl(operationUrl));
        }
        return false;
    }

    boolean isApplicableToActions(String[] requiredActions) {
        List<String> autzActions = authorization.getAction();
        if (autzActions.contains(AuthorizationConstants.AUTZ_ALL_URL)) {
            return true;
        }
        for (String requiredAction : requiredActions) {
            if (autzActions.contains(requiredAction)) {
                return true;
            }
        }
        if (op.traceEnabled) {
            LOGGER.trace("      Authorization not applicable for operation {}", prettyActionUrl(requiredActions));
        }
        return false;
    }

    /** No AUTZ_ALL exception here! */
    boolean isApplicableToActions(Collection<String> requiredActions) {
        List<String> autzActions = authorization.getAction();
        for (String requiredAction : requiredActions) {
            if (autzActions.contains(requiredAction)) {
                return true;
            }
        }
        if (op.traceEnabled) {
            LOGGER.trace("      Authorization not applicable for operation {}", prettyActionUrl(requiredActions));
        }
        return false;
    }

    boolean isApplicableToPhase(AuthorizationPhaseType phase, boolean includeNullPhase) {
        AuthorizationPhaseType autzPhase = authorization.getPhase();
        if (autzPhase == phase || (includeNullPhase && autzPhase == null)) {
            LOGGER.trace("      Authorization is applicable for phases {} (continuing evaluation)", phase);
            return true;
        } else {
            LOGGER.trace("      Authorization is not applicable for phase {} (includeNullPhase={})", phase, includeNullPhase);
            return false;
        }
    }

    boolean isApplicableToLimitations(String limitAuthorizationAction, String[] operationUrls) {
        if (limitAuthorizationAction == null) {
            return true;
        }
        AuthorizationLimitationsType autzLimitations = authorization.getLimitations();
        if (autzLimitations == null) {
            return true;
        }
        List<String> autzLimitationsActions = autzLimitations.getAction();
        if (autzLimitationsActions.isEmpty() || autzLimitationsActions.contains(limitAuthorizationAction)) {
            return true;
        }
        if (op.traceEnabled) {
            LOGGER.trace("      Authorization is limited to other action, not applicable for operation {}",
                    prettyActionUrl(operationUrls));
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isApplicableToOrderConstraints(List<OrderConstraintsType> paramOrderConstraints) {
        var applies = getOrderConstraintsApplicability(paramOrderConstraints);
        if (!applies && op.traceEnabled) {
            LOGGER.trace("      Authorization not applicable for orderConstraints {}",
                    SchemaDebugUtil.shortDumpOrderConstraintsList(paramOrderConstraints));
        }
        return applies;
    }

    private boolean getOrderConstraintsApplicability(List<OrderConstraintsType> paramOrderConstraints) {
        if (authorization.getAction().contains(AuthorizationConstants.AUTZ_ALL_URL)) {
            // #all is always applicable
            // Compatibility note: in fact, this not really correct. We should not make
            // any special case for #all action - except for the fact that it applies to
            // all actions. Even for #all, the object and target specification should
            // still be processed. But orderConstraint is a bit different. For all other
            // authorization clauses holds that empty clause means that everything is
            // applicable. But it is different for orderConstraints. Due to compatibility
            // with midPoint 3.8 empty orderConstraints means min=0,max=0, i.e. it applies
            // only to assignment (not inducements). Therefore we need this exception for
            // #all, otherwise #all won't be applicable to inducements.
            return true;
        }
        OrderConstraintsType autzOrderConstraints = authorization.getOrderConstraints();
        if (paramOrderConstraints == null || paramOrderConstraints.isEmpty()) {
            return autzOrderConstraints == null;
        }
        for (OrderConstraintsType paramOrderConstraint : paramOrderConstraints) {
            if (!isSubset(paramOrderConstraint, autzOrderConstraints)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSubset(OrderConstraintsType paramOrderConstraint, OrderConstraintsType autzOrderConstraints) {
        int autzOrderMin;
        int autzOrderMax;

        if (autzOrderConstraints == null) {
            autzOrderMin = 0;
            autzOrderMax = 0;
        } else {
            if (autzOrderConstraints.getRelation() != null) {
                throw new UnsupportedOperationException("Complex order constraints with relation not supported in authorizations");
            }
            if (autzOrderConstraints.getResetOrder() != null) {
                throw new UnsupportedOperationException("Complex order constraints with resetOrder not supported in authorizations");
            }

            int autzOrder = or0(autzOrderConstraints.getOrder());
            autzOrderMin = Objects.requireNonNullElse(
                    XsdTypeMapper.multiplicityToInteger(autzOrderConstraints.getOrderMin()),
                    autzOrder);
            autzOrderMax = Objects.requireNonNullElse(
                    XsdTypeMapper.multiplicityToInteger(autzOrderConstraints.getOrderMax()),
                    autzOrder);
        }

        Integer paramOrder = paramOrderConstraint.getOrder();
        Integer paramOrderMin = XsdTypeMapper.multiplicityToInteger(paramOrderConstraint.getOrderMin());
        if (paramOrderMin == null) {
            paramOrderMin = paramOrder;
        }
        Integer paramOrderMax = XsdTypeMapper.multiplicityToInteger(paramOrderConstraint.getOrderMax());
        if (paramOrderMax == null) {
            paramOrderMax = paramOrder;
        }

        if (autzOrderMin < 0 || paramOrderMin < 0) {
            // minimum set to infinity, should not really happen
            return false;
        }

        if (paramOrderMin < autzOrderMin) {
            return false;
        }

        if (autzOrderMax < 0) {
            // required maximum set to infinity, everything allowed
            return true;
        }

        if (paramOrderMax < 0) {
            // parameter maximum set to infinity. You cannot pass now.
            return false;
        }

        return paramOrderMax <= autzOrderMax;
    }

    boolean isApplicableToRelation(QName relation) {
        List<QName> autzRelation = authorization.getRelation();
        if (autzRelation.isEmpty() || QNameUtil.contains(autzRelation, relation)) {
            return true;
        } else {
            LOGGER.trace("      Authorization is not applicable for relation {}", relation);
            return false;
        }
    }

    <O extends ObjectType> boolean isApplicableToObject(ObjectDeltaObject<O> odo)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        var anyObject = odo != null ? odo.getAnyObject() : null;
        if (isApplicableToObjectDeltaObjectInternal(odo)) {
            LOGGER.trace("    Authorization is applicable for object {} (continuing evaluation)", anyObject);
            return true;
        } else {
            LOGGER.trace("    Authorization is not applicable for object {}, none of the object specifications match (breaking evaluation)",
                    anyObject);
            return false;
        }
    }

    private <O extends ObjectType> boolean isApplicableToObjectDeltaObjectInternal(ObjectDeltaObject<O> odo)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        List<? extends OwnedObjectSelectorType> objectSelectors = authorization.getObjectSelectors();
        if (!objectSelectors.isEmpty()) {
            if (odo == null) {
                if (op.traceEnabled) {
                    LOGGER.trace("  object not applicable for null {}", getDesc());
                }
                return false;
            }
            ObjectDelta<O> objectDelta = odo.getObjectDelta();
            if (objectDelta != null && objectDelta.isModify()) {
                if (authorization.keepZoneOfControl()) {
                    PrismObject<O> oldObject = odo.getOldObject();
                    if (oldObject == null) {
                        throw new IllegalStateException("No old object in odo " + odo);
                    }
                    if (!areSelectorsApplicable(objectSelectors, oldObject, "object(old)")) {
                        return false;
                    }
                    PrismObject<O> newObject = odo.getNewObject();
                    if (newObject == null) {
                        throw new IllegalStateException("No new object in odo " + odo);
                    }
                    return areSelectorsApplicable(objectSelectors, newObject, "object(new)");
                } else {
                    PrismObject<O> object = odo.getOldObject();
                    if (object == null) {
                        throw new IllegalStateException("No old object in odo " + odo);
                    }
                    return areSelectorsApplicable(objectSelectors, object, "object(old)");
                }
            } else {
                // Old and new object should be the same. Or there is just one of them. Any one of them will do.
                PrismObject<O> object = odo.getAnyObject();
                if (object == null) {
                    throw new IllegalStateException("No object in odo " + odo);
                }
                return areSelectorsApplicable(objectSelectors, object, "object");
            }
        } else {
            LOGGER.trace("    {}: No object specification in authorization (authorization is applicable)", getDesc());
            return true;
        }
    }

    private <O extends ObjectType> boolean areSelectorsApplicable(
            @NotNull List<? extends OwnedObjectSelectorType> selectors, @Nullable PrismObject<O> object, @NotNull String desc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (!selectors.isEmpty()) {
            if (object == null) {
                if (op.traceEnabled) {
                    LOGGER.trace("  Authorization is not applicable for null {}", desc);
                }
                return false;
            }
            for (OwnedObjectSelectorType selector : selectors) {
                if (isSelectorApplicable(selector, object, emptySet(), desc)) {
                    return true;
                }
            }
            return false;
        } else {
            LOGGER.trace("    Authorization: No {} specification in authorization (authorization is applicable)", desc);
            return true;
        }
    }

    <T extends ObjectType> boolean isApplicableToObject(PrismObject<T> object)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (areSelectorsApplicable(authorization.getObjectSelectors(), object, "object")) {
            LOGGER.trace("    applicable for object {} (continuing evaluation)", object);
            return true;
        } else {
            LOGGER.trace("    not applicable for object {}, none of the object specifications match (breaking evaluation)",
                    object);
            return false;
        }
    }

    <T extends ObjectType> boolean isApplicableToTarget(PrismObject<T> target)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (areSelectorsApplicable(authorization.getTargetSelectors(), target, "target")) {
            LOGGER.trace("    applicable for target {} (continuing evaluation)", target);
            return true;
        } else {
            LOGGER.trace("    not applicable for target {}, none of the target specifications match (breaking evaluation)",
                    target);
            return false;
        }
    }

    // TODO name
    <O extends ObjectType> boolean matchesItems(PrismObject<O> object, ObjectDelta<O> delta) throws SchemaException {
        List<ItemPathType> itemPaths = authorization.getItem();
        if (itemPaths.isEmpty()) {
            List<ItemPathType> exceptItems = authorization.getExceptItem();
            if (exceptItems.isEmpty()) {
                // No item constraints. Applicable for all items.
                LOGGER.trace("  items empty");
                return true;
            } else {
                return matchesItems(object, delta, exceptItems, false);
            }
        } else {
            return matchesItems(object, delta, itemPaths, true);
        }
    }

    private static <O extends ObjectType> boolean matchesItems(
            PrismObject<O> object, ObjectDelta<O> delta, List<ItemPathType> itemPaths, boolean positive)
            throws SchemaException {
        for (ItemPathType itemPathType : itemPaths) {
            ItemPath itemPath = itemPathType.getItemPath();
            if (delta == null) {
                if (object != null) {
                    if (object.containsItem(itemPath, false)) {
                        if (positive) {
                            LOGGER.trace("  applicable object item {}", itemPath);
                            return true;
                        } else {
                            LOGGER.trace("  excluded object item {}", itemPath);
                            return false;
                        }
                    }
                }
            } else {
                ItemDelta<?, ?> itemDelta = delta.findItemDelta(itemPath);
                if (itemDelta != null && !itemDelta.isEmpty()) {
                    if (positive) {
                        LOGGER.trace("  applicable delta item {}", itemPath);
                        return true;
                    } else {
                        LOGGER.trace("  excluded delta item {}", itemPath);
                        return false;
                    }
                }
            }
        }
        if (positive) {
            LOGGER.trace("  no applicable item");
            return false;
        } else {
            LOGGER.trace("  no excluded item");
            return true;
        }
    }

    ObjectFilterExpressionEvaluator createFilterEvaluator(String desc) {
        return filter -> {
            if (filter == null) {
                return null;
            }
            VariablesMap variables = new VariablesMap();
            PrismObject<? extends FocusType> subject = principal != null ? principal.getFocus().asPrismObject() : null;
            PrismObjectDefinition<? extends FocusType> def;
            if (subject != null) {
                def = subject.getDefinition();
                if (def == null) {
                    def = b.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(subject.asObjectable().getClass());
                }
                variables.addVariableDefinition(ExpressionConstants.VAR_SUBJECT, subject, def);
            } else {
                // ???
            }

            return ExpressionUtil.evaluateFilterExpressions(
                    filter, variables, MiscSchemaUtil.getExpressionProfile(), b.expressionFactory, b.prismContext,
                    "expression in " + desc + " in authorization " + getDesc(), task, result);
        };
    }

    String getDesc() {
        return lazyDescription.get();
    }

    /**
     * TODO move elsewhere?
     *
     * @param otherSelfOids Which OIDs should match "self" in addition to the current principal OID. Usually these could be
     * some or all of delegators' OIDs, i.e. people that delegated privileges to the current principal.
     * The reason is that if we want to match assignee or requestor (probably targetObject and owner as well)
     * we want to give appropriate privileges also to assignee/requestor delegates.
     */
    public boolean isSelectorApplicable(
            @NotNull SubjectedObjectSelectorType selector,
            @Nullable PrismObject<? extends ObjectType> object,
            @NotNull Collection<String> otherSelfOids,
            @NotNull String desc)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        var value = object != null ? object.getValue() : null;
        return new ObjectSelectorEvaluation<>(selector, value, otherSelfOids, desc, this, result)
                .isSelectorApplicable();
    }

    public boolean isSelectorApplicable(
            @NotNull SubjectedObjectSelectorType selector,
            @Nullable PrismValue value,
            @NotNull Collection<String> otherSelfOids,
            @NotNull String desc)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return new ObjectSelectorEvaluation<>(selector, value, otherSelfOids, desc, this, result)
                .isSelectorApplicable();
    }

    private void traceStart() {
        if (op.traceEnabled) {
            LOGGER.trace("    Evaluating {}", getDesc());
        }
    }
}
