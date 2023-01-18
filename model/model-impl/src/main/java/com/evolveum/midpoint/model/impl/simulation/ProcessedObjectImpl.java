/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ItemDelta;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractSimulationMetricReferenceTypeUtil;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.VisibleForTesting;

import javax.xml.namespace.QName;

/**
 * Parsed analogy of {@link SimulationResultProcessedObjectType}.
 */
public class ProcessedObjectImpl<O extends ObjectType> implements ProcessedObject<O> {

    private final String oid; // TODO may be null?
    @NotNull private final Class<O> type;
    @NotNull private final QName typeName;
    private final ShadowDiscriminatorType shadowDiscriminator;
    private final PolyStringType name;
    @NotNull private final ObjectProcessingStateType state;
    @NotNull private final Collection<String> eventTags;

    /** Present only when parsed from the bean. */
    @NotNull private final Map<String, BigDecimal> metricValues;

    /** Complete information on the tags (optional) */
    private Map<String, TagType> eventTagsMap;

    @Nullable private final O before;
    @Nullable private final O after;
    @Nullable private final ObjectDelta<O> delta;

    /**
     * Sometimes, it is convenient to have the original (bean) form accessible as well.
     *
     * FIXME this approach is a bit fragile and unsafe; but let us keep it for the time being
     */
    @Nullable private final SimulationResultProcessedObjectType originalBean;

    private ProcessedObjectImpl(
            String oid,
            @NotNull Class<O> type,
            ShadowDiscriminatorType shadowDiscriminator,
            PolyStringType name,
            @NotNull ObjectProcessingStateType state,
            @NotNull Collection<String> eventTags,
            @NotNull Map<String, BigDecimal> metricValues,
            @Nullable O before,
            @Nullable O after,
            @Nullable ObjectDelta<O> delta,
            @Nullable SimulationResultProcessedObjectType originalBean) {
        this.oid = oid;
        this.type = type;
        this.typeName = ObjectTypes.getObjectType(type).getTypeQName();
        this.shadowDiscriminator = shadowDiscriminator;
        this.name = name;
        this.state = state;
        this.eventTags = eventTags;
        this.metricValues = metricValues;
        this.before = before;
        this.after = after;
        this.delta = delta;
        this.originalBean = originalBean;
    }

    public static <O extends ObjectType> ProcessedObjectImpl<O> parse(@NotNull SimulationResultProcessedObjectType bean)
            throws SchemaException {
        Class<?> type = PrismContext.get().getSchemaRegistry().determineClassForTypeRequired(bean.getType());
        argCheck(ObjectType.class.isAssignableFrom(type), "Type is not an ObjectType: %s", type);
        //noinspection unchecked
        return new ProcessedObjectImpl<>(
                bean.getOid(),
                (Class<O>) type,
                bean.getResourceObjectCoordinates(),
                bean.getName(),
                MiscUtil.argNonNull(bean.getState(), () -> "No processing state in " + bean),
                getEventTagsOids(bean),
                getMetricValues(bean),
                (O) bean.getBefore(),
                (O) bean.getAfter(),
                DeltaConvertor.createObjectDeltaNullable(bean.getDelta()),
                bean);
    }

    private static Map<String, BigDecimal> getMetricValues(SimulationResultProcessedObjectType bean) {
        return bean.getMetricValue().stream()
                .collect(Collectors.toMap(
                        ProcessedObjectSimulationMetricValueType::getIdentifier,
                        ProcessedObjectSimulationMetricValueType::getValue));
    }

    private static Set<String> getEventTagsOids(SimulationResultProcessedObjectType bean) {
        return bean.getEventTagRef().stream()
                .map(ref -> ref.getOid())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static <O extends ObjectType> ProcessedObjectImpl<O> create(
            O stateBefore, ObjectDelta<O> delta, Collection<String> eventTags) throws SchemaException {

        Class<O> type;
        if (stateBefore != null) {
            //noinspection unchecked
            type = (Class<O>) stateBefore.getClass();
        } else if (delta != null) {
            type = delta.getObjectTypeClass();
        } else {
            return null;
        }

        @Nullable O stateAfter = computeStateAfter(stateBefore, delta);
        @Nullable O anyState = MiscUtil.getFirstNonNull(stateAfter, stateBefore);

        // We may consider returning null if anyState is null (meaning that the delta is MODIFY/DELETE with null stateBefore)

        return new ProcessedObjectImpl<>(
                determineOid(anyState, delta),
                type,
                determineShadowDiscriminator(anyState),
                anyState != null ? anyState.getName() : null,
                delta != null ?
                        ProcessedObject.DELTA_TO_PROCESSING_STATE.get(delta.getChangeType()) :
                        ObjectProcessingStateType.UNMODIFIED,
                eventTags,
                Map.of(),
                stateBefore,
                stateAfter,
                delta,
                null);
    }

    private static <O extends ObjectType> ShadowDiscriminatorType determineShadowDiscriminator(O object) {
        if (!(object instanceof ShadowType)) {
            return null;
        }
        ShadowType shadow = (ShadowType) object;
        ObjectReferenceType resourceRef = shadow.getResourceRef();
        return new ShadowDiscriminatorType()
                .resourceRef(resourceRef != null ? resourceRef.clone() : null)
                .kind(shadow.getKind())
                .intent(shadow.getIntent())
                .tag(shadow.getTag())
                .objectClassName(shadow.getObjectClass());
    }

    @Override
    public String getOid() {
        return oid;
    }

    @Override
    public @NotNull Class<O> getType() {
        return type;
    }

    public @NotNull QName getTypeName() {
        return typeName;
    }

    public ShadowDiscriminatorType getShadowDiscriminator() {
        return shadowDiscriminator;
    }

    public String getResourceOid() {
        return shadowDiscriminator != null ?
                Referencable.getOid(shadowDiscriminator.getResourceRef()) : null;
    }

    public ShadowKindType getKind() {
        return shadowDiscriminator != null ? shadowDiscriminator.getKind() : null;
    }

    public String getIntent() {
        return shadowDiscriminator != null ? shadowDiscriminator.getIntent() : null;
    }

    @Override
    public @Nullable PolyStringType getName() {
        return name;
    }

    @Override
    public @NotNull ObjectProcessingStateType getState() {
        return state;
    }

    @Override
    public @NotNull Collection<String> getEventTags() {
        return eventTags;
    }

    @Override
    public @Nullable Map<String, TagType> getEventTagsMap() {
        return eventTagsMap;
    }

    @Override
    public void setEventTagsMap(Map<String, TagType> eventTagsMap) {
        this.eventTagsMap = eventTagsMap;
    }

    @Override
    @Nullable
    public O getBefore() {
        return before;
    }

    @Override
    @Nullable
    public O getAfter() {
        return after;
    }

    @Override
    @Nullable
    public ObjectDelta<O> getDelta() {
        return delta;
    }

    private static <O extends ObjectType> O computeStateAfter(O stateBefore, ObjectDelta<O> delta) throws SchemaException {
        if (stateBefore == null) {
            if (delta == null) {
                return null;
            } else if (delta.isAdd()) {
                return delta.getObjectToAdd().asObjectable();
            } else {
                // We may relax this before release - we may still store the delta
                throw new IllegalStateException("No initial state and MODIFY/DELETE delta? Delta: " + delta);
            }
        } else if (delta != null) {
            //noinspection unchecked
            PrismObject<O> clone = (PrismObject<O>) stateBefore.asPrismObject().clone();
            delta.applyTo(clone);
            return clone.asObjectable();
        } else {
            return stateBefore;
        }
    }

    private static <O extends ObjectType> @Nullable String determineOid(O anyState, ObjectDelta<O> delta) {
        if (anyState != null) {
            String oid = anyState.getOid();
            if (oid != null) {
                return oid;
            }
        }
        if (delta != null) {
            return delta.getOid();
        }
        return null;
    }

    public SimulationResultProcessedObjectType toBean() throws SchemaException {
        SimulationResultProcessedObjectType processedObject = new SimulationResultProcessedObjectType()
                .oid(oid)
                .type(
                        PrismContext.get().getSchemaRegistry().determineTypeForClass(type))
                .resourceObjectCoordinates(shadowDiscriminator != null ? shadowDiscriminator.clone() : null)
                .name(name)
                .state(state)
                .before(prepareObjectForStorage(before))
                .after(prepareObjectForStorage(after))
                .delta(DeltaConvertor.toObjectDeltaType(delta));
        List<ObjectReferenceType> eventTagRef = processedObject.getEventTagRef();
        eventTags.forEach(
                oid -> eventTagRef.add(ObjectTypeUtil.createObjectRef(oid, ObjectTypes.TAG)));
        List<ProcessedObjectSimulationMetricValueType> metricValues = processedObject.getMetricValue();
        this.metricValues.forEach(
                (id, value) -> metricValues.add(
                        new ProcessedObjectSimulationMetricValueType()
                                .identifier(id)
                                .value(value)));
        return processedObject;
    }

    private ObjectType prepareObjectForStorage(@Nullable O original) {
        if (original != null) {
            ObjectType clone = original.clone();
            clone.setOid(null); // this is currently not parseable
            clone.setVersion(null); // this is currently not parseable
            clone.setFetchResult(null); // can contain a lot of garbage
            return clone;
        } else {
            return null;
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilder(getClass(), indent);
        sb.append(" of ").append(type.getSimpleName());
        sb.append(" ").append(oid);
        sb.append(" (").append(name).append("): ");
        sb.append(state);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "tags", getEventTagsDebugDump(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "metrics", metricValues, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "before", before, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "delta", delta, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ProcessedObject{" +
                "oid='" + oid + '\'' +
                ", type=" + type +
                ", name=" + name +
                ", state=" + state +
                ", eventTags=" + getEventTagsDebugDump() +
                ", metrics=" + metricValues +
                ", before=" + before +
                ", after=" + after +
                ", delta=" + delta +
                '}';
    }

    private Collection<String> getEventTagsDebugDump() {
        if (eventTagsMap != null) {
            return eventTagsMap.entrySet().stream()
                    .map(e -> getTagDebugDump(e))
                    .collect(Collectors.toList());
        } else {
            return eventTags;
        }
    }

    private String getTagDebugDump(Map.Entry<String, TagType> tagEntry) {
        String tagOid = tagEntry.getKey();
        TagType tag = tagEntry.getValue();
        if (tag != null) {
            return getOrig(tag.getName()) + " (" + tagOid + ")";
        } else {
            return tagOid;
        }
    }

    @Override
    public @Nullable O getAfterOrBefore() {
        return MiscUtil.getFirstNonNull(after, before);
    }

    /**
     * Returns the number of modifications of non-operational items.
     *
     * Currently the implementation is primitive: we simply count the modifications (item deltas) in the object MODIFY delta.
     * Other kinds of deltas (add, delete) are ignored. We also count a container modification as one item modification, even
     * if there are multiple properties changed in the container.
     */
    @SuppressWarnings("unused") // used in scripts
    @VisibleForTesting
    public int getNonOperationalItemModificationsCount() {
        if (delta == null || !delta.isModify()) {
            return 0;
        }
        return (int) delta.getModifications().stream()
                .filter(mod -> !mod.isOperational())
                .count();
    }

    /**
     * Returns the number of attribute modifications.
     *
     * Not counting changes in ADD/DELETE deltas.
     */
    @SuppressWarnings("unused") // used in scripts
    @VisibleForTesting
    public int getAttributeModificationsCount() {
        if (delta == null || !delta.isModify()) {
            return 0;
        }
        return (int) delta.getModifications().stream()
                .filter(this::isAttributeModification)
                .count();
    }

    private boolean isAttributeModification(@NotNull ItemDelta<?, ?> modification) {
        return ShadowType.F_ATTRIBUTES.equivalent(modification.getParentPath());
    }

    /**
     * Returns the number of VALUES of associations added or deleted.
     *
     * Not counting changes in ADD/DELETE deltas.
     * Assuming that there are no REPLACE modifications of associations.
     */
    @SuppressWarnings("unused") // used in scripts
    @VisibleForTesting
    public int getAssociationValuesChanged() {
        if (delta == null || !delta.isModify()) {
            return 0;
        }
        return delta.getModifications().stream()
                .filter(mod -> ShadowType.F_ASSOCIATION.equivalent(mod.getPath()))
                .mapToInt(mod -> getChangedValues(mod))
                .sum();
    }

    private int getChangedValues(ItemDelta<?, ?> mod) {
        return emptyIfNull(mod.getValuesToAdd()).size()
                + emptyIfNull(mod.getValuesToDelete()).size();
    }

    @SuppressWarnings("unused") // used in scripts
    public boolean isOfFocusType() {
        return FocusType.class.isAssignableFrom(type);
    }

    @SuppressWarnings("unused") // used in scripts
    public boolean isShadow() {
        return ShadowType.class.isAssignableFrom(type);
    }

    @NotNull BigDecimal getMetricValueRequired(@NotNull AbstractSimulationMetricReferenceType sourceRef) {
        return MiscUtil.stateNonNull(
                getMetricValue(sourceRef),
                () -> String.format("No value for %s in %s", AbstractSimulationMetricReferenceTypeUtil.describe(sourceRef), this));
    }

    @Nullable BigDecimal getMetricValue(@NotNull AbstractSimulationMetricReferenceType ref) {
        String identifier = ref.getIdentifier();
        if (identifier != null) {
            return metricValues.get(identifier);
        }
        String tagOid = Referencable.getOid(ref.getEventTagRef());
        if (tagOid != null) {
            return eventTags.contains(tagOid) ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        throw new IllegalArgumentException("Neither metric identifier nor event tag OID present in metric reference: " + ref);
    }

    @Override
    public boolean matches(SimulationResultProcessedObjectPredicateType predicate, Task task, OperationResult result)
            throws CommonException {
        SearchFilterType filter = predicate.getFilter();
        if (filter != null && !matchesFilter(filter)) {
            return false;
        }
        ExpressionType expression = predicate.getExpression();
        return expression == null || matchesExpression(expression, task, result);
    }

    private boolean matchesFilter(@NotNull SearchFilterType filterBean) throws SchemaException {
        stateCheck(originalBean != null,
                "Trying to evaluate a filter but the original bean is not present: %s", this);
        //noinspection unchecked
        PrismContainerValue<SimulationResultProcessedObjectType> originalPcv = originalBean.asPrismContainerValue();
        ObjectFilter filter =
                PrismContext.get().getQueryConverter().parseFilter(filterBean, SimulationResultProcessedObjectType.class);
        return filter.match(originalPcv, SchemaService.get().matchingRuleRegistry());
    }

    private boolean matchesExpression(@NotNull ExpressionType expression, Task task, OperationResult result)
            throws CommonException {
        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_PROCESSED_OBJECT, this, ProcessedObject.class);
        return ExpressionUtil.evaluateConditionDefaultFalse(
                variables,
                expression,
                MiscSchemaUtil.getExpressionProfile(),
                ModelCommonBeans.get().expressionFactory,
                "matching expression",
                task,
                result);
    }

    // ugly heuristics to decide if this event is applicable to given type - TODO decide what to do with this funny method
    boolean isProbablyApplicable(TagType eventTag) {
        Set<PolicyRuleEvaluationTargetType> targets = eventTag.getPolicyRule().stream()
                .map(rule -> rule.getEvaluationTarget())
                .collect(Collectors.toSet());
        if (targets.size() != 1) {
            return true;
        }
        PolicyRuleEvaluationTargetType target = targets.iterator().next();
        if (target == PolicyRuleEvaluationTargetType.PROJECTION) {
            return ShadowType.class.equals(type);
        } else if (target == PolicyRuleEvaluationTargetType.OBJECT) {
            return !ShadowType.class.equals(type);
        } else {
            return true;
        }
    }
}
