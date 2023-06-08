/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static java.util.Collections.emptySet;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getValue;
import static com.evolveum.midpoint.schema.util.SchemaDebugUtil.shortDumpOrderConstraintsList;
import static com.evolveum.midpoint.security.api.AuthorizationConstants.AUTZ_ALL_URL;
import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.prettyActionUrl;
import static com.evolveum.midpoint.security.enforcer.impl.TracingUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.AbstractAuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ValueAuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;

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

    /** TODO describe, decide */
    @NotNull private final String id;

    @NotNull final Authorization authorization;
    @NotNull private final Lazy<String> lazyDescription;

    @NotNull final EnforcerOperation op;
    @Nullable private final MidPointPrincipal principal;
    @NotNull private final Beans b;
    @NotNull private final Task task;
    @NotNull final OperationResult result;

    AuthorizationEvaluation(
            @Nullable String id,
            @NotNull Authorization authorization,
            @NotNull EnforcerOperation op,
            @NotNull OperationResult result) {
        this.id = Objects.requireNonNullElse(id, "");
        this.authorization = authorization;
        this.op = op;
        this.principal = op.principal;
        this.b = op.b;
        this.task = op.task;
        this.result = result;
        this.lazyDescription = Lazy.from(() -> this.authorization.getHumanReadableDesc());
    }

    public @NotNull Authorization getAuthorization() {
        return authorization;
    }

    boolean isApplicableToAction(@NotNull String operationUrl) {
        List<String> autzActions = authorization.getAction();
        if (autzActions.contains(operationUrl) || autzActions.contains(AUTZ_ALL_URL)) {
            traceAutzApplicableToAction(operationUrl);
            return true;
        } else {
            traceAutzNotApplicableToAction(operationUrl);
            return false;
        }
    }

    boolean isApplicableToActions(String[] requiredActions) {
        List<String> autzActions = authorization.getAction();
        if (autzActions.contains(AUTZ_ALL_URL)) {
            traceAutzApplicableToAnyAction();
            return true;
        }
        for (String requiredAction : requiredActions) {
            if (autzActions.contains(requiredAction)) {
                traceAutzApplicableToAction(requiredAction);
                return true;
            }
        }
        traceAutzNotApplicableToActions(requiredActions);
        return false;
    }

    boolean isApplicableToPhase(@NotNull PhaseSelector phaseSelector) {
        if (phaseSelector.matches(authorization.getPhase())) {
            traceAutzApplicableToPhase(phaseSelector);
            return true;
        } else {
            traceAutzNotApplicableToPhase(phaseSelector);
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
        traceAutzNotApplicableToLimitations(operationUrls);
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isApplicableToOrderConstraints(List<OrderConstraintsType> paramOrderConstraints) {
        var applies = getOrderConstraintsApplicability(paramOrderConstraints);
        if (!applies) {
            traceAutzNotApplicableToOrderConstraints(paramOrderConstraints);
        }
        return applies;
    }

    private boolean getOrderConstraintsApplicability(List<OrderConstraintsType> paramOrderConstraints) {
        if (authorization.getAction().contains(AUTZ_ALL_URL)) {
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

    boolean isApplicableToParameters(@NotNull AbstractAuthorizationParameters params)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (params instanceof AuthorizationParameters<?, ?> objectParams) {
            return isApplicableToRelation(objectParams.getRelation())
                    && isApplicableToOrderConstraints(objectParams.getOrderConstraints())
                    && isApplicableToObjectOperation(objectParams.getOdo())
                    && isApplicableToTarget(objectParams.getTarget());
        } else if (params instanceof ValueAuthorizationParameters<?> valueParams){
            return isApplicableToObjectValue(valueParams.getValue());
        } else {
            throw new NotHereAssertionError();
        }
    }

    private boolean isApplicableToRelation(QName relation) {
        List<QName> autzRelation = authorization.getRelation();
        if (autzRelation.isEmpty() || QNameUtil.contains(autzRelation, relation)) {
            return true;
        } else {
            traceAutzNotApplicableToRelation(relation);
            return false;
        }
    }

    private boolean isApplicableToObjectOperation(ObjectDeltaObject<? extends ObjectType> odo)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        var anyObject = odo != null ? odo.getAnyObject() : null;
        if (isApplicableToObjectDeltaObjectInternal(odo)) {
            traceAutzApplicableToObjectOperation(anyObject);
            return true;
        } else {
            traceAutzNotApplicableToObjectOperation(anyObject);
            return false;
        }
    }

    private <O extends ObjectType> boolean isApplicableToObjectDeltaObjectInternal(ObjectDeltaObject<O> odo)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        List<ValueSelector> objectSelectors = authorization.getParsedObjectSelectors();
        if (!objectSelectors.isEmpty()) {
            if (odo == null) {
                traceAutzNotApplicableToNullOperation();
                return false;
            }
            ObjectDelta<O> objectDelta = odo.getObjectDelta();
            if (objectDelta != null && objectDelta.isModify()) {
                if (authorization.keepZoneOfControl()) {
                    return areSelectorsApplicable(objectSelectors, odo.getOldObjectRequired(), "object(old)")
                            && areSelectorsApplicable(objectSelectors, odo.getNewObjectRequired(), "object(new)");
                } else {
                    return areSelectorsApplicable(objectSelectors, odo.getOldObjectRequired(), "object(old)");
                }
            } else {
                // Old and new object should be the same. Or there is just one of them. Any one of them will do.
                return areSelectorsApplicable(objectSelectors, odo.getAnyObjectRequired(), "object");
            }
        } else {
            traceAutzApplicableBecauseNoObjectSpecification();
            return true;
        }
    }

    private <O extends ObjectType> boolean areSelectorsApplicable(
            @NotNull List<ValueSelector> selectors, @Nullable PrismObject<O> object, @NotNull String desc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return areSelectorsApplicable(
                selectors, getValue(object), desc);
    }

    private boolean areSelectorsApplicable(
            @NotNull List<ValueSelector> selectors, @Nullable PrismValue value, @NotNull String desc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (!selectors.isEmpty()) {
            if (value == null) {
                traceSelectorsNotApplicableForNullObject(desc);
                return false;
            }
            int i = 0;
            for (ValueSelector selector : selectors) {
                if (isSelectorApplicable(String.valueOf(i++), selector, value, emptySet(), desc)) {
                    return true;
                }
            }
            return false;
        } else {
            traceNoSelectorsPresent(desc);
            return true;
        }
    }

    boolean isApplicableToObject(PrismObject<? extends ObjectType> object)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (areSelectorsApplicable(authorization.getParsedObjectSelectors(), object, "object")) {
            traceAutzApplicableToObject(object);
            return true;
        } else {
            traceAutzNotApplicableToObject(object);
            return false;
        }
    }

    private boolean isApplicableToObjectValue(@Nullable PrismValue value)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (areSelectorsApplicable(authorization.getParsedObjectSelectors(), value, "object")) {
            traceAutzApplicableToValue(value);
            return true;
        } else {
            traceAutzNotApplicableToValue(value);
            return false;
        }
    }

    <T extends ObjectType> boolean isApplicableToTarget(PrismObject<T> target)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (areSelectorsApplicable(authorization.getParsedTargetSelectors(), target, "target")) {
            traceAutzApplicableToTarget(target);
            return true;
        } else {
            traceAutzNotApplicableToTarget(target);
            return false;
        }
    }

    // TODO name
    boolean matchesItems(AbstractAuthorizationParameters params) throws SchemaException {
        if (params instanceof AuthorizationParameters<?, ?> objectParams) {
            return matchesItems(getValue(objectParams.getOldObject()), objectParams.getDelta());
        } else if (params instanceof ValueAuthorizationParameters<?> valueParams) {
            return matchesItems(valueParams.getValue(), null);
        } else {
            throw new NotHereAssertionError();
        }
    }

    private boolean matchesItems(PrismValue value, ObjectDelta<? extends ObjectType> delta)
            throws SchemaException {
        var positive = authorization.getItems();
        if (positive.isEmpty()) {
            var negative = authorization.getExceptItems();
            if (negative.isEmpty()) {
                // No item constraints. Applicable for all items.
                LOGGER.trace("  items empty");
                return true;
            } else {
                return matchesItems(value, delta, negative, false);
            }
        } else {
            return matchesItems(value, delta, positive, true);
        }
    }

    private static boolean matchesItems(
            PrismValue value,
            ObjectDelta<? extends ObjectType> delta,
            PathSet itemPaths,
            boolean positive)
            throws SchemaException {
        for (ItemPath itemPath : itemPaths) {
            if (delta == null) {
                if (value != null) {
                    if (containsItem(value, itemPath)) {
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

    private static boolean containsItem(@NotNull PrismValue value, @NotNull ItemPath itemPath) throws SchemaException {
        if (itemPath.isEmpty()) {
            return true;
        } else if (value instanceof PrismContainerValue<?> pcv) {
            return pcv.containsItem(itemPath, false);
        } else {
            return false;
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
                    def = b.prismContext.getSchemaRegistry()
                            .findObjectDefinitionByCompileTimeClass(subject.asObjectable().getClass());
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

    boolean shouldSkipSubObjectSelectors() {
        if (op instanceof CompileConstraintsOperation<?> cop) {
            return cop.getOptions().isSkipSubObjectSelectors();
        } else {
            return false;
        }
    }

    public boolean isSelectorApplicable(
            @NotNull String id,
            @NotNull ValueSelector selector,
            @NotNull PrismValue value,
            @NotNull Collection<String> otherSelfOids,
            @NotNull String desc)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return new SelectorEvaluation(id, selector, value, otherSelfOids, desc, this, result)
                .isSelectorApplicable();
    }

    void traceStart() {
        if (op.traceEnabled) {
            LOGGER.trace("{} Evaluating {}", start(), getDesc());
        }
    }

    void traceEndNotApplicable() {
        if (op.traceEnabled) {
            LOGGER.trace("{} Result: {}: not applicable", end(), getDesc());
        }
    }

    private void traceAutzNotApplicableToAction(@NotNull String operationUrl) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization not applicable for operation {}", cont(), prettyActionUrl(operationUrl));
        }
    }

    private void traceAutzApplicableToAction(@NotNull String operationUrl) {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is applicable for operation {} (continuing evaluation)",
                    cont(), prettyActionUrl(operationUrl));
        }
    }

    private void traceAutzApplicableToAnyAction() {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is applicable for all operations (continuing evaluation)", cont());
        }
    }

    private void traceAutzNotApplicableToActions(String[] requiredActions) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization not applicable for operation(s) {}", cont(), prettyActionUrl(requiredActions));
        }
    }

    private void traceAutzNotApplicableToPhase(@NotNull PhaseSelector phaseSelector) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is not applicable for phase filter '{}'", cont(), phaseSelector);
        }
    }

    private void traceAutzApplicableToPhase(@NotNull PhaseSelector phaseSelector) {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is applicable for phase filter '{}' (continuing evaluation)",
                    cont(), phaseSelector);
        }
    }

    private void traceAutzNotApplicableToLimitations(String[] operationUrls) {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is limited to other action, not applicable for operation {}",
                    cont(), prettyActionUrl(operationUrls));
        }
    }

    private void traceAutzNotApplicableToOrderConstraints(List<OrderConstraintsType> paramOrderConstraints) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization not applicable for orderConstraints {}",
                    cont(), shortDumpOrderConstraintsList(paramOrderConstraints));
        }
    }

    private void traceAutzNotApplicableToRelation(QName relation) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is not applicable for relation {}", cont(), relation);
        }
    }

    private void traceAutzNotApplicableToObjectOperation(PrismObject<? extends ObjectType> anyObject) {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is not applicable for object {}, none of the object specifications match",
                    cont(), anyObject);
        }
    }

    private void traceAutzApplicableToObjectOperation(PrismObject<? extends ObjectType> anyObject) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is applicable for object {} (continuing evaluation)", cont(), anyObject);
        }
    }

    private void traceAutzNotApplicableToNullOperation() {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is not applicable for null object operation info", cont());
        }
    }

    private void traceAutzApplicableBecauseNoObjectSpecification() {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is applicable, because there is no object specification (continuing evaluation)",
                    cont());
        }
    }

    private void traceAutzApplicableToObject(PrismObject<? extends ObjectType> object) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is applicable to object {} (continuing evaluation)", cont(), object);
        }
    }

    private void traceAutzNotApplicableToObject(PrismObject<? extends ObjectType> object) {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is not applicable to object {}, none of the object specifications match",
                    cont(), object);
        }
    }

    private void traceAutzApplicableToValue(PrismValue value) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is applicable to object (continuing evaluation): {}",
                    cont(), MiscUtil.getDiagInfo(value));
        }
    }

    private void traceAutzNotApplicableToValue(PrismValue value) {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is not applicable to object; none of the object specifications match: {}",
                    cont(), MiscUtil.getDiagInfo(value));
        }
    }

    private void traceAutzNotApplicableToTarget(PrismObject<? extends ObjectType> target) {
        if (op.traceEnabled) {
            LOGGER.trace(
                    "{} Authorization is not applicable to target {}, none of the target specifications match",
                    cont(), target);
        }
    }

    private void traceAutzApplicableToTarget(PrismObject<? extends ObjectType> target) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is applicable to target {} (continuing evaluation)", cont(), target);
        }
    }

    private void traceSelectorsNotApplicableForNullObject(@NotNull String desc) {
        if (op.traceEnabled) {
            LOGGER.trace("{} Authorization is not applicable for null {}", SELECTORS, desc);
        }
    }

    private void traceNoSelectorsPresent(@NotNull String desc) {
        if (op.traceEnabled) {
            LOGGER.trace("{} No {} selectors in authorization (authorization is applicable)", SELECTORS, desc);
        }
    }

    private String start() {
        return AUTZ + id + START;
    }

    private String cont() {
        return AUTZ + id + CONT;
    }

    String end() {
        return AUTZ + id + END;
    }
}
