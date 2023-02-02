/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.schema.util.ObjectReferenceTypeUtil.getTargetNameOrOid;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.simulation.PartitionScope;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Parsed analogy of {@link SimulationResultProcessedObjectType}.
 *
 * TODO decide on the purpose and implementation of this class - the duplication of properties and fragility when setting them
 *  becomes unbearable
 */
@SuppressWarnings("CommentedOutCode")
public class ProcessedObjectImpl<O extends ObjectType> implements ProcessedObject<O> {

    private Long recordId;
    @NotNull private final String transactionId;
    private final String oid; // TODO may be null?
    @NotNull private final Class<O> type;
    @Nullable private final ObjectReferenceType structuralArchetypeRef;
    @NotNull private final QName typeName;
    private final ShadowDiscriminatorType shadowDiscriminator;
    private final PolyStringType name;
    @NotNull private final ObjectProcessingStateType state;

    @NotNull private final ParsedMetricValues parsedMetricValues;

    /** Complete information on the tags (optional) */
    private Map<String, MarkType> eventMarksMap;

    private final Boolean focus;

    private Integer projectionRecords;

    private Long focusRecordId;

    @Nullable private final O before;
    @Nullable private final O after;
    @Nullable private final ObjectDelta<O> delta;

    private SimulationResultProcessedObjectType cachedBean;

    @NotNull private InternalState internalState;

    private ProcessedObjectImpl(
            @NotNull String transactionId,
            String oid,
            @NotNull Class<O> type,
            @Nullable ObjectReferenceType structuralArchetypeRef,
            ShadowDiscriminatorType shadowDiscriminator,
            PolyStringType name,
            @NotNull ObjectProcessingStateType state,
            @NotNull ParsedMetricValues parsedMetricValues,
            Boolean focus,
            @Nullable Long focusRecordId,
            @Nullable O before,
            @Nullable O after,
            @Nullable ObjectDelta<O> delta,
            @NotNull InternalState internalState) {
        this.transactionId = transactionId;
        this.oid = oid;
        this.type = type;
        this.typeName = ObjectTypes.getObjectType(type).getTypeQName();
        this.structuralArchetypeRef = structuralArchetypeRef;
        this.shadowDiscriminator = shadowDiscriminator;
        this.name = name;
        this.state = state;
        this.parsedMetricValues = parsedMetricValues;
        this.focus = focus;
        this.focusRecordId = focusRecordId;
        this.before = before;
        this.after = after;
        this.delta = delta;
        this.internalState = internalState;
    }

    @VisibleForTesting
    public static <O extends ObjectType> ProcessedObjectImpl<O> parse(@NotNull SimulationResultProcessedObjectType bean)
            throws SchemaException {
        Class<?> type = PrismContext.get().getSchemaRegistry().determineClassForTypeRequired(bean.getType());
        argCheck(ObjectType.class.isAssignableFrom(type), "Type is not an ObjectType: %s", type);
        //noinspection unchecked
        var obj = new ProcessedObjectImpl<>(
                bean.getTransactionId(),
                bean.getOid(),
                (Class<O>) type,
                bean.getStructuralArchetypeRef(),
                bean.getResourceObjectCoordinates(),
                bean.getName(),
                MiscUtil.argNonNull(bean.getState(), () -> "No processing state in " + bean),
                ParsedMetricValues.fromAll(bean.getMetricValue(), bean.getEventMarkRef()),
                bean.isFocus(),
                bean.getFocusRecordId(),
                (O) bean.getBefore(),
                (O) bean.getAfter(),
                DeltaConvertor.createObjectDeltaNullable(bean.getDelta()),
                InternalState.PARSED);
        obj.setRecordId(bean.getId());
        obj.setFocusRecordId(bean.getFocusRecordId());
        obj.setProjectionRecords(bean.getProjectionRecords());
        return obj;
    }

    private void addComputedMetricValues(@NotNull List<SimulationProcessedObjectMetricValueType> newValues) {
        assert internalState == InternalState.CREATING;
        parsedMetricValues.addMetricValues(newValues);
        internalState = InternalState.CREATED;
        invalidateCachedBean();
    }

    /**
     * Creates {@link ProcessedObjectImpl} for the {@link LensElementContext}.
     *
     * Limitations:
     *
     * - We ignore the fact that sometimes we don't have full shadow loaded. The deltas applied to "shadow-only" state
     * may be misleading.
     */
    public static <O extends ObjectType> @Nullable ProcessedObjectImpl<O> create(
            @NotNull LensElementContext<O> elementContext,
            @NotNull SimulationTransactionImpl simulationTransaction,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {

        Class<O> type = elementContext.getObjectTypeClass();
        boolean isFocus = elementContext instanceof LensFocusContext<?>;
        @Nullable O stateBefore;
        if (isFocus) {
            // Focus is computed iteratively, so "current" represents some intermediate state.
            // We are sure that "old" is OK for focus.
            stateBefore = asObjectable(elementContext.getObjectOld());
        } else {
            // Projections are never computed iteratively (except for already-exists exceptions, that cannot occur during
            // simulations). Moreover, if full shadow is loaded during processing, it is put into "current", not into "old".
            // Hence it's best to take "current" for projections.
            stateBefore = asObjectable(elementContext.getObjectCurrent());
        }
        @Nullable O stateAfter = asObjectable(elementContext.getObjectNew());
        @Nullable O anyState = MiscUtil.getFirstNonNull(stateAfter, stateBefore);
        @Nullable ObjectDelta<O> delta = elementContext.getSummaryExecutedDelta();
        if (anyState == null || stateBefore == null && delta == null) {
            // Nothing to report on here. Note that for shadows it's possible that stateBefore and delta are null but
            // stateAfter is "PCV(null)" - due to the way of computation of objectNew for shadows
            return null;
        }

        // We may consider returning null if anyState is null (meaning that the delta is MODIFY/DELETE with null stateBefore)

        var processedObject = new ProcessedObjectImpl<>(
                simulationTransaction.getTransactionId(),
                elementContext.getOid(),
                type,
                elementContext instanceof LensFocusContext<?> ?
                        ((LensFocusContext<?>) elementContext).getStructuralArchetypeRef() : null,
                determineShadowDiscriminator(anyState),
                anyState.getName(),
                delta != null ?
                        ProcessedObject.DELTA_TO_PROCESSING_STATE.get(delta.getChangeType()) :
                        ObjectProcessingStateType.UNMODIFIED,
                ParsedMetricValues.fromEventMarks(
                        elementContext.getMatchingEventMarks(), elementContext.getAllConsideredEventMarks()),
                isFocus,
                null, // provided later
                stateBefore,
                stateAfter,
                delta,
                InternalState.CREATING);

        processedObject.addComputedMetricValues(
                ObjectMetricsComputation.computeAll(
                        processedObject,
                        elementContext,
                        simulationTransaction.getSimulationResult(),
                        ModelBeans.get().simulationResultManager.getMetricDefinitions(),
                        task,
                        result));

        return processedObject;
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

    Long getRecordId() {
        return recordId;
    }

    private void setRecordId(Long recordId) {
        this.recordId = recordId;
        invalidateCachedBean();
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

    private @Nullable String getStructuralArchetypeOid() {
        return Referencable.getOid(structuralArchetypeRef);
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
    public @NotNull Collection<String> getMatchingEventMarks() {
        return parsedMetricValues.getMatchingEventMarks();
    }

    @Override
    public @Nullable Map<String, MarkType> getEventMarksMap() {
        return eventMarksMap;
    }

    @Override
    public void setEventMarksMap(Map<String, MarkType> eventMarksMap) {
        this.eventMarksMap = eventMarksMap;
    }

    public Boolean getFocus() {
        return focus;
    }

    void setProjectionRecords(Integer projectionRecords) {
        this.projectionRecords = projectionRecords;
        invalidateCachedBean();
    }

    private void invalidateCachedBean() {
        this.cachedBean = null;
    }

    void setFocusRecordId(Long focusRecordId) {
        this.focusRecordId = focusRecordId;
        invalidateCachedBean();
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

    public SimulationResultProcessedObjectType toBean() {
        if (cachedBean != null) {
            return cachedBean;
        }
        try {
            SimulationResultProcessedObjectType bean = new SimulationResultProcessedObjectType()
                    .transactionId(transactionId)
                    .oid(oid)
                    .type(
                            PrismContext.get().getSchemaRegistry().determineTypeForClass(type))
                    .structuralArchetypeRef(structuralArchetypeRef)
                    .resourceObjectCoordinates(shadowDiscriminator != null ? shadowDiscriminator.clone() : null)
                    .name(name)
                    .state(state)
                    .focus(focus)
                    .focusRecordId(focusRecordId)
                    .projectionRecords(projectionRecords)
                    .before(prepareObjectForStorage(before))
                    .after(prepareObjectForStorage(after))
                    .delta(DeltaConvertor.toObjectDeltaType(delta));
            List<ObjectReferenceType> eventMarkRef = bean.getEventMarkRef();
            parsedMetricValues.getMatchingEventMarks().forEach(
                    oid -> eventMarkRef.add(ObjectTypeUtil.createObjectRef(oid, ObjectTypes.MARK)));
            List<ObjectReferenceType> consideredEventMarkRef = bean.getConsideredEventMarkRef();
            parsedMetricValues.getAllConsideredEventMarks().forEach(
                    oid -> consideredEventMarkRef.add(ObjectTypeUtil.createObjectRef(oid, ObjectTypes.MARK)));
            bean.getMetricValue().addAll(parsedMetricValues.getMetricValueBeans());
            cachedBean = bean;
            return bean;
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "while creating SimulationResultProcessedObjectType instance");
        }
    }

    static @NotNull Collection<SimulationResultProcessedObjectType> toBeans(
            @NotNull Collection<? extends ProcessedObjectImpl<?>> processedObjects) {
        return processedObjects.stream()
                .map(o -> o.toBean())
                .collect(Collectors.toList());
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
        if (structuralArchetypeRef != null) {
            sb.append("/").append(getTargetNameOrOid(structuralArchetypeRef));
        }
        sb.append(" ").append(oid);
        sb.append(" (").append(name).append("): ");
        sb.append(state);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "id", recordId + " in tx '" + transactionId + "'", indent + 1);
        DebugUtil.debugDumpLabel(sb, "context", indent + 1);
        sb.append(" ");
        if (focus == null) {
            sb.append("No 'is focus' information! (It seems like a problem.)\n");
            DebugUtil.debugDumpWithLabelLn(sb, "projection records", projectionRecords, indent + 2);
            DebugUtil.debugDumpWithLabel(sb, "focus record ID", focusRecordId, indent + 2);
        } else if (focus) {
            sb.append("This is a focus with ").append(projectionRecords).append(" projection(s).");
        } else {
            sb.append("This is a projection; focus record ID: ").append(focusRecordId).append(".");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "metrics", parsedMetricValues, indent + 1);
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
                ", metrics=" + parsedMetricValues +
                ", before=" + before +
                ", after=" + after +
                ", delta=" + delta +
                '}';
    }

//    private Collection<String> getEventMarksDebugDump() {
//        if (eventMarksMap != null) {
//            return eventMarksMap.entrySet().stream()
//                    .map(e -> getTagDebugDump(e))
//                    .collect(Collectors.toList());
//        } else {
//            return eventMarks;
//        }
//    }
//
//    private String getTagDebugDump(Map.Entry<String, MarkType> tagEntry) {
//        String tagOid = tagEntry.getKey();
//        MarkType tag = tagEntry.getValue();
//        if (tag != null) {
//            return getOrig(tag.getName()) + " (" + tagOid + ")";
//        } else {
//            return tagOid;
//        }
//    }

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

    @Nullable MetricValue getMetricValue(@NotNull SimulationMetricReferenceType ref) {
        assert internalState == InternalState.CREATED;
        return parsedMetricValues.getMetricValue(ref);
    }

    @Override
    public boolean matches(@NotNull SimulationObjectPredicateType predicate, @NotNull Task task, @NotNull OperationResult result)
            throws CommonException {
        SearchFilterType filter = predicate.getFilter();
        if (filter != null && !matchesFilter(filter)) {
            return false;
        }
        ExpressionType expression = predicate.getExpression();
        return expression == null || matchesExpression(expression, task, result);
    }

    private boolean matchesFilter(@NotNull SearchFilterType filterBean) throws SchemaException {
        //noinspection unchecked
        PrismContainerValue<SimulationResultProcessedObjectType> originalPcv = toBean().asPrismContainerValue();
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

    @VisibleForTesting
    @Override
    public void resolveEventMarks(OperationResult result) {
        if (eventMarksMap != null) {
            return;
        }
        eventMarksMap = ModelCommonBeans.get().markManager.resolveMarkNames(
                parsedMetricValues.getMatchingEventMarks(), result);
    }

    PartitionScope partitionScope() {
        return new PartitionScope(getTypeName(), getStructuralArchetypeOid(), getResourceOid(), getKind(), getIntent(), ALL_DIMENSIONS);
    }

    void propagateRecordId() {
        recordId = Objects.requireNonNull(
                cachedBean != null ? cachedBean.getId() : null,
                () -> "No cached bean or no ID in it: " + cachedBean);
    }

    static class ParsedMetricValues implements DebugDumpable {
        @NotNull private final Map<SimulationMetricReference, MetricValue> valueMap;

        private ParsedMetricValues(@NotNull Map<SimulationMetricReference, MetricValue> valueMap) {
            this.valueMap = valueMap;
        }

        static @NotNull ParsedMetricValues fromEventMarks(
                @NotNull Collection<String> matching,
                @NotNull Collection<String> allConsidered) {
            Map<SimulationMetricReference, MetricValue> valueMap = new HashMap<>();
            for (String tagOid : allConsidered) {
                boolean matches = matching.contains(tagOid);
                valueMap.put(
                        SimulationMetricReference.forMark(tagOid),
                        new MetricValue(matches ? BigDecimal.ONE : BigDecimal.ZERO, matches));
            }
            return new ParsedMetricValues(valueMap);
        }

        // We don't have "all considered event marks" here
        static ParsedMetricValues fromAll(
                @NotNull List<SimulationProcessedObjectMetricValueType> metricValue,
                @NotNull List<ObjectReferenceType> matchingMarkRefs) {
            var matchingTagOids = matchingMarkRefs.stream()
                    .map(ref -> ref.getOid())
                    .collect(Collectors.toSet());
            ParsedMetricValues parsedMetricValues = fromEventMarks(matchingTagOids, matchingTagOids);
            parsedMetricValues.addMetricValues(metricValue);
            return parsedMetricValues;
        }

        void addMetricValues(List<SimulationProcessedObjectMetricValueType> values) {
            for (SimulationProcessedObjectMetricValueType valueBean : values) {
                valueMap.put(
                        SimulationMetricReference.forMetricId(valueBean.getIdentifier()),
                        new MetricValue(valueBean.getValue(), valueBean.isSelected()));
            }
        }

        @NotNull Collection<String> getMatchingEventMarks() {
            return valueMap.entrySet().stream()
                    .filter(e -> e.getKey().isMark())
                    .filter(e -> e.getValue().inSelection)
                    .map(e -> e.getKey().getMarkOid())
                    .collect(Collectors.toSet());
        }

        @NotNull Collection<String> getAllConsideredEventMarks() {
            return valueMap.keySet().stream()
                    .filter(ref -> ref.isMark())
                    .map(ref -> ref.getMarkOid())
                    .collect(Collectors.toSet());
        }

        @NotNull Collection<SimulationProcessedObjectMetricValueType> getMetricValueBeans() {
            return valueMap.entrySet().stream()
                    .filter(e -> e.getKey().isCustomMetric())
                    .map(e -> new SimulationProcessedObjectMetricValueType()
                            .identifier(e.getKey().getMetricIdentifier())
                            .selected(e.getValue().inSelection)
                            .value(e.getValue().value))
                    .collect(Collectors.toList());
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ParsedMetricValues.class, indent);
            DebugUtil.debugDumpWithLabel(sb, "values", valueMap, indent + 1);
            return sb.toString();
        }

        @Nullable MetricValue getMetricValue(@NotNull SimulationMetricReferenceType ref) {
            return valueMap.get(
                    SimulationMetricReference.fromBean(ref));
        }
    }

    static class MetricValue {
        @NotNull final BigDecimal value;
        final boolean inSelection;

        MetricValue(@NotNull BigDecimal value, boolean inSelection) {
            this.value = value;
            this.inSelection = inSelection;
        }

        @Override
        public String toString() {
            return value + " (" + (inSelection ? "in" : "out") + ")";
        }
    }

    private enum InternalState {
        /** Instance is being created. Metric values are not available. */
        CREATING,

        /** Instance is fully created, all metric values are available. */
        CREATED,

        /** Instance is re-parsed. "All considered event marks" information is not available. */
        @VisibleForTesting
        PARSED
    }
}
