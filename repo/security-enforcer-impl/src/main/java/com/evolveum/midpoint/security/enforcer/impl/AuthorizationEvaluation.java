/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getValue;
import static com.evolveum.midpoint.schema.util.SchemaDebugUtil.shortDumpOrderConstraintsList;
import static com.evolveum.midpoint.security.api.AuthorizationConstants.AUTZ_ALL_URL;
import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.prettyActionUrl;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.security.enforcer.api.AbstractAuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ValueAuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.AuthorizationProcessingEvent;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.AuthorizationProcessingFinished;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.AuthorizationProcessingStarted;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
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

    /** Prefix for string {@link #id} for this operation (for tracing). */
    private static final String AUTZ_ID_PREFIX = "AUTZ.";

    /** Prefix for selector operation-level IDs invoked from this authorization-level operation (for tracing). */
    private static final String SEL_ID_PREFIX = "SEL.";

    /** Identifier for this operation (like `AUTZ.0`, `AUTZ.1`, ...) - for tracing. */
    @NotNull private final String id;

    /** The authorization being evaluated. */
    @NotNull final Authorization authorization;

    /** The human readable description of the authorization ({@link Authorization#getHumanReadableDesc()}). */
    @NotNull private final Lazy<String> lazyDescription;

    /** The whole operation we are part of. */
    @NotNull final EnforcerOperation op;

    @NotNull private final Beans b;
    @NotNull private final Task task;
    @NotNull final OperationResult result;

    AuthorizationEvaluation(
            int id,
            @NotNull Authorization authorization,
            @NotNull EnforcerOperation op,
            @NotNull OperationResult result) {
        this(AUTZ_ID_PREFIX + id, authorization, op, result);
    }

    private AuthorizationEvaluation(
            @Nullable String id,
            @NotNull Authorization authorization,
            @NotNull EnforcerOperation op,
            @NotNull OperationResult result) {
        this.id = Objects.requireNonNullElse(id, "");
        this.authorization = authorization;
        this.op = op;
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
        } else if (params instanceof ValueAuthorizationParameters<?> valueParams) {
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
        var applicability = isApplicableToObjectDeltaObjectInternal(odo);
        var anyObject = odo != null ? odo.getAnyObject() : null;
        traceAutzApplicabilityToObjectOrTarget("object", anyObject, applicability);
        return applicability.value;
    }

    private <O extends ObjectType> SelectorApplicabilityResult isApplicableToObjectDeltaObjectInternal(ObjectDeltaObject<O> odo)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        List<ValueSelector> objectSelectors = authorization.getParsedObjectSelectors();
        if (!objectSelectors.isEmpty()) {
            if (odo == null) {
                return SelectorApplicabilityResult.negative("null object operation info but selector(s) defined");
            }
            ObjectDelta<O> objectDelta = odo.getObjectDelta();
            if (objectDelta != null && objectDelta.isModify()) {
                var forOld = areSelectorsApplicable(objectSelectors, odo.getOldObjectRequired(), "object(old)");
                if (!forOld.value || !authorization.keepZoneOfControl()) {
                    return forOld;
                }
                var forNew = areSelectorsApplicable(objectSelectors, odo.getNewObjectRequired(), "object(new)");
                return SelectorApplicabilityResult.combined("old", forOld, "new", forNew);
            } else {
                // Old and new object should be the same. Or there is just one of them. Any one of them will do.
                return areSelectorsApplicable(objectSelectors, odo.getAnyObjectRequired(), "object");
            }
        } else {
            return SelectorApplicabilityResult.positive("no object selectors defined");
        }
    }

    private <O extends ObjectType> SelectorApplicabilityResult areSelectorsApplicable(
            @NotNull List<ValueSelector> selectors, @Nullable PrismObject<O> object, @NotNull String desc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return areSelectorsApplicable(selectors, getValue(object), desc);
    }

    private SelectorApplicabilityResult areSelectorsApplicable(
            @NotNull List<ValueSelector> selectors, @Nullable PrismValue value, @NotNull String desc)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (!selectors.isEmpty()) {
            if (value == null) {
                return SelectorApplicabilityResult.negative("null object but selector(s) defined");
            }
            int i = 0;
            for (ValueSelector selector : selectors) {
                if (isSelectorApplicable(selectorId(i++), selector, value, desc)) {
                    return SelectorApplicabilityResult.positive("a selector matched");
                }
            }
            return SelectorApplicabilityResult.negative("no selector matched");
        } else {
            return SelectorApplicabilityResult.positive("no selectors defined");
        }
    }

    boolean isApplicableToObject(PrismObject<? extends ObjectType> object)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var applicability = areSelectorsApplicable(authorization.getParsedObjectSelectors(), object, "object");
        traceAutzApplicabilityToObjectOrTarget("object", object, applicability);
        return applicability.value;
    }

    private boolean isApplicableToObjectValue(@Nullable PrismValue value)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var applicability = areSelectorsApplicable(authorization.getParsedObjectSelectors(), value, "object");
        traceAutzApplicabilityToObjectValue(value, applicability);
        return applicability.value;
    }

    <T extends ObjectType> boolean isApplicableToTarget(PrismObject<T> target)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var applicability = areSelectorsApplicable(authorization.getParsedTargetSelectors(), target, "target");
        traceAutzApplicabilityToObjectOrTarget("target", target, applicability);
        return applicability.value;
    }

    /** Returns `true` if the items specified by the authorization match the given parameters (delta or object). */
    ItemsMatchResult matchesOnItems(@NotNull AbstractAuthorizationParameters params) throws SchemaException {
        if (params instanceof AuthorizationParameters<?, ?> objectParams) {
            return matchesOnItems(getValue(objectParams.getOldObject()), objectParams.getDelta());
        } else if (params instanceof ValueAuthorizationParameters<?> valueParams) {
            return matchesOnItems(valueParams.getValue(), null);
        } else {
            throw new NotHereAssertionError();
        }
    }

    private ItemsMatchResult matchesOnItems(PrismValue value, ObjectDelta<? extends ObjectType> delta)
            throws SchemaException {
        var positiveItemPaths = authorization.getItems();
        if (positiveItemPaths.isEmpty()) {
            var negativeItemPaths = authorization.getExceptItems();
            if (negativeItemPaths.isEmpty()) {
                return ItemsMatchResult.positive("no item constraints -> applicable to all items");
            } else {
                return matchesOnItems(value, delta, negativeItemPaths, false);
            }
        } else {
            return matchesOnItems(value, delta, positiveItemPaths, true);
        }
    }

    /**
     * @param positive True if the `itemPaths` denote those _included_ (i.e. others are excluded); false if those paths
     * are _excluded_ (i.e. others are included).
     */
    private static ItemsMatchResult matchesOnItems(
            PrismValue value,
            ObjectDelta<? extends ObjectType> delta,
            PathSet itemPaths,
            boolean positive)
            throws SchemaException {
        for (ItemPath itemPath : itemPaths) {
            if (delta != null) {
                ItemDelta<?, ?> itemDelta = delta.findItemDelta(itemPath);
                if (itemDelta != null && !itemDelta.isEmpty()) {
                    if (positive) {
                        return ItemsMatchResult.positive("applicable delta item '%s'", itemPath);
                    } else {
                        return ItemsMatchResult.negative("excluded delta item '%s'", itemPath);
                    }
                }
            } else if (value != null) {
                if (containsItem(value, itemPath)) {
                    if (positive) {
                        return ItemsMatchResult.positive("applicable object item '%s'", itemPath);
                    } else {
                        return ItemsMatchResult.negative("excluded object item '%s'", itemPath);
                    }
                }
            }
        }
        if (positive) {
            return ItemsMatchResult.negative("no applicable item");
        } else {
            return ItemsMatchResult.positive("no excluded item");
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
            FocusType subject = op.getPrincipalFocus();
            if (subject != null) {
                variables.addVariableWithDeterminedDefinition(ExpressionConstants.VAR_SUBJECT, subject);
            } else {
                // ???
            }

            return ExpressionUtil.evaluateFilterExpressions(
                    filter, variables, MiscSchemaUtil.getExpressionProfile(), b.expressionFactory,
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
            @NotNull String desc)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return new SelectorEvaluation(id, selector, value, desc, this, result)
                .isSelectorApplicable();
    }

    //region Tracing
    void traceStart() {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingStarted(this));
        }
    }

    void traceEndNotApplicable(String message, Object... arguments) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingFinished(this, message, arguments));
        }
    }

    void traceEndNotApplicable() {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingFinished(this, "not applicable"));
        }
    }

    void traceEndApplied() {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingFinished(this, "applied"));
        }
    }

    void traceEndApplied(String message, Object... arguments) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingFinished(this, message, arguments));
        }
    }
    private void traceAutzNotApplicableToAction(@NotNull String operationUrl) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is not applicable for operation %s",
                            prettyActionUrl(operationUrl)));
        }
    }

    private void traceAutzApplicableToAction(@NotNull String operationUrl) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is applicable for operation %s (continuing evaluation)",
                            prettyActionUrl(operationUrl)));
        }
    }

    private void traceAutzApplicableToAnyAction() {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is applicable for all operations (continuing evaluation)"));
        }
    }

    private void traceAutzNotApplicableToActions(String[] requiredActions) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is not applicable for operation(s) %s",
                            prettyActionUrl(requiredActions)));
        }
    }

    private void traceAutzNotApplicableToPhase(@NotNull PhaseSelector phaseSelector) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is not applicable for '%s'",
                            phaseSelector));
        }
    }

    private void traceAutzApplicableToPhase(@NotNull PhaseSelector phaseSelector) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is applicable for '%s' (continuing evaluation)",
                            phaseSelector));
        }
    }

    private void traceAutzNotApplicableToLimitations(String[] operationUrls) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is limited to other action, not applicable for operation(s) %s",
                            prettyActionUrl(operationUrls)));
        }
    }

    private void traceAutzNotApplicableToOrderConstraints(List<OrderConstraintsType> paramOrderConstraints) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is not applicable for orderConstraints %s",
                            shortDumpOrderConstraintsList(paramOrderConstraints)));
        }
    }

    private void traceAutzNotApplicableToRelation(QName relation) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is not applicable for relation %s",
                            relation));
        }
    }

    private void traceAutzApplicabilityToObjectOrTarget(
            String desc, PrismObject<? extends ObjectType> object, SelectorApplicabilityResult applicability) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is %s for %s %s [%s]%s",
                            applicability.value ? "applicable" : "not applicable",
                            desc,
                            object,
                            applicability.message,
                            applicability.value ? " (continuing evaluation)" : ""));
        }
    }

    private void traceAutzApplicabilityToObjectValue(PrismValue value, SelectorApplicabilityResult applicability) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(
                            this,
                            "Authorization is %s for object [%s]%s: %s",
                            applicability.value ? "applicable" : "not applicable",
                            applicability.message,
                            applicability.value ? " (continuing evaluation)" : "",
                            MiscUtil.getDiagInfo(value)));
        }
    }

    @SuppressWarnings("SameParameterValue")
    void traceAutzProcessingNote(String message, Object... arguments) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingEvent(this, message, arguments));
        }
    }

    void traceAuthorizationAllow(@NotNull String operationUrl) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingFinished(
                            this,
                            "ALLOWED operation %s => but continuing evaluation of other authorizations",
                            prettyActionUrl(operationUrl)));
        }
    }

    void traceAuthorizationDenyIrrelevant(@NotNull String operationUrl, @NotNull ItemsMatchResult itemsMatchResult) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingFinished(
                            this,
                            "IRRELEVANT for operation %s (%s)"
                                    + " => continuing evaluation of other authorizations",
                            prettyActionUrl(operationUrl),
                            itemsMatchResult.getFormattedMessage()));
        }
    }

    void traceAuthorizationDenyRelevant(@NotNull String operationUrl, @NotNull ItemsMatchResult itemsMatchResult) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationProcessingFinished(
                            this,
                            "DENIED operation %s (%s) => continuing evaluation of other authorizations",
                            prettyActionUrl(operationUrl),
                            itemsMatchResult.getFormattedMessage()));
        }
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String selectorId(int local) {
        return id + "." + SEL_ID_PREFIX + local;
    }

    //endregion

    /** Tells whether selector was applicable; and the reason (for tracing purposes). */
    private record SelectorApplicabilityResult(boolean value, @NotNull String message) {

        static SelectorApplicabilityResult positive(@NotNull String message) {
            return new SelectorApplicabilityResult(true, message);
        }

        static SelectorApplicabilityResult negative(@NotNull String message) {
            return new SelectorApplicabilityResult(false, message);
        }

        @SuppressWarnings("SameParameterValue")
        static SelectorApplicabilityResult combined(
                String firstLabel, SelectorApplicabilityResult first, String secondLabel, SelectorApplicabilityResult second) {
            return new SelectorApplicabilityResult(
                    first.value && second.value,
                    first.message + " (" + firstLabel + "), " + second.message + " (" + secondLabel + ")");
        }
    }

    /** Tells whether item-limited authorization allows the operation; and the reason (for tracing purposes). */
    record ItemsMatchResult(boolean value, @NotNull String message, Object... objects) {

        static ItemsMatchResult positive(@NotNull String message, Object... objects) {
            return new ItemsMatchResult(true, message, objects);
        }

        static ItemsMatchResult negative(@NotNull String message, Object... objects) {
            return new ItemsMatchResult(false, message, objects);
        }

        String getFormattedMessage() {
            return message.formatted(objects);
        }
    }
}
