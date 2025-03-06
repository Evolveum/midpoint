/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.schema.util.ObjectReferenceTypeUtil.getTargetNameOrOid;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.simulation.SimulationMetricReference;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection;
import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.deleg.ItemDeltaDelegator;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.api.ShadowSimulationData;
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
import com.evolveum.midpoint.schema.util.delta.ItemDeltaFilter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Default (and the only) implementation of {@link ProcessedObject}.
 */
public class ProcessedObjectImpl<O extends ObjectType> implements ProcessedObject<O> {

    /** To store parsed version along with {@link SimulationResultProcessedObjectType} bean. */
    public static final String KEY_PARSED = ProcessedObjectImpl.class.getName() + ".parsed";

    /** See {@link SimulationResultProcessedObjectType#getId()}. */
    private Long recordId;

    /** See {@link SimulationResultProcessedObjectType#getTransactionId()}. */
    @NotNull private final String transactionId;

    /** See {@link ProcessedObject#getOid()}. */
    private final String oid;

    /** See {@link ProcessedObject#getType()}. */
    @NotNull private final Class<O> type;

    /** {@link QName} variant of {@link #type}. */
    @NotNull private final QName typeName;

    @Nullable private final ObjectReferenceType structuralArchetypeRef;

    private final ShadowDiscriminatorType shadowDiscriminator;

    /** See {@link ProcessedObject#getName()}. */
    private final PolyStringType name;

    /** See {@link ProcessedObject#getState()}. */
    @NotNull private final ObjectProcessingStateType state;

    /** See {@link ProcessedObject#getResultStatus()}. */
    @Nullable private OperationResultStatus resultStatus;

    /** See {@link ProcessedObject#getResult()}. */
    @Nullable private OperationResult result;

    /** Readily-accessible values of metrics related to this object. Currently does not include built-in metrics. */
    @NotNull private final ParsedMetricValues parsedMetricValues;

    /** Complete information on the tags (optional). For dumping purposes. */
    @VisibleForTesting
    private Map<String, MarkType> eventMarksMap;

    /** See {@link SimulationResultProcessedObjectType#isFocus()}. */
    private final Boolean focus;

    /**
     * See {@link SimulationResultProcessedObjectType#getProjectionRecords()}.
     *
     * Not final, as its creation requires processing a set of records (focus/projections).
     */
    private Integer projectionRecords;

    /**
     * See {@link SimulationResultProcessedObjectType#getFocusRecordId()}.
     *
     * Not final, as its creation requires processing a set of records (focus/projections).
     */
    private Long focusRecordId;

    /** See {@link ProcessedObject#getBefore()}. */
    @Nullable private final O before;

    /** See {@link ProcessedObject#getAfter()}. */
    @Nullable private final O after;

    /** See {@link ProcessedObject#getDelta()}. */
    @Nullable private final ObjectDelta<O> delta;

    /** Stores externalized form of this object, to avoid repeated processing e.g. during metrics filters evaluation. */
    private SimulationResultProcessedObjectType cachedBean;

    /** Aids with creating instances of this class. See {@link InternalState}. */
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

    /**
     * Creates {@link ProcessedObjectImpl} from pre-existing {@link SimulationResultProcessedObjectType} bean.
     * For example, during testing or report creation.
     *
     * @see InternalState#PARSED
     */
    public static <O extends ObjectType> @NotNull ProcessedObjectImpl<O> parse(@NotNull SimulationResultProcessedObjectType bean)
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
                (O) ObjectTypeUtil.fix(bean.getBefore()),
                (O) ObjectTypeUtil.fix(bean.getAfter()),
                DeltaConvertor.createObjectDeltaNullable(bean.getDelta()),
                InternalState.PARSED);
        obj.setRecordId(bean.getId());
        obj.setFocusRecordId(bean.getFocusRecordId());
        obj.setProjectionRecords(bean.getProjectionRecords());
        obj.setResultAndStatus(bean.getResult(), bean.getResultStatus());
        return obj;
    }

    /**
     * Creates {@link ProcessedObjectImpl} for the {@link LensElementContext}. This is the classic (audit-like) simulation.
     *
     * Limitations:
     *
     * - We ignore the fact that sometimes we don't have full shadow loaded. (Although we do our best to load such full
     * shadows during clockwork processing.) The deltas applied to "shadow-only" state may be misleading.
     */
    public static <O extends ObjectType> @Nullable ProcessedObjectImpl<O> create(
            @NotNull LensElementContext<O> elementContext,
            @NotNull SimulationTransactionImpl simulationTransaction,
            @NotNull Task task,
            @NotNull OperationResult result) throws CommonException {
        return createInternal(elementContext, null, simulationTransaction, task, result);
    }

    /**
     * Creates {@link ProcessedObjectImpl} for "single delta" scenarios. Used to implement "compare-mode" mappings/items.
     *
     * @see SingleDeltaSimulationDataImpl
     */
    @Experimental
    static <O extends ObjectType> ProcessedObjectImpl<O> createSingleDelta(
            @NotNull LensElementContext<O> elementContext,
            @NotNull ObjectDelta<O> simulationDelta,
            @NotNull SimulationTransactionImpl simulationTransaction,
            @NotNull Task task,
            @NotNull OperationResult result) throws CommonException {
        return createInternal(elementContext, simulationDelta, simulationTransaction, task, result);
    }

    private static <O extends ObjectType> @Nullable ProcessedObjectImpl<O> createInternal(
            @NotNull LensElementContext<O> elementContext,
            @Nullable ObjectDelta<O> singleDelta,
            @NotNull SimulationTransactionImpl simulationTransaction,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {

        Class<O> type = elementContext.getObjectTypeClass();
        @Nullable O stateBefore = asObjectable(elementContext.getStateBeforeSimulatedOperation());
        @Nullable ObjectDelta<O> delta = singleDelta != null ? singleDelta : elementContext.getSummaryExecutedDelta();

        // We intentionally do not use "object new" because it does not contain e.g. linkRefs added.
        @Nullable O stateAfter;
        if (delta == null) {
            stateAfter = stateBefore;
        } else {
            stateAfter = asObjectable(
                    delta.computeChangedObject(
                            asPrismObject(stateBefore)));
        }

        // We intentionally prefer the state before. The reason is that e.g. for object name we want to display the record
        // and report on it under the OLD name - it is the name of the object that is currently valid in the real world.
        // The same is true for the shadow discriminator. See MID-8610.
        @Nullable O anyState = MiscUtil.getFirstNonNull(stateBefore, stateAfter);

        if (anyState == null) {
            return null;
        }

        var processedObject = new ProcessedObjectImpl<>(
                simulationTransaction.getTransactionId(),
                elementContext.getOid(),
                type,
                determineStructuralArchetypeRef(anyState, result),
                determineShadowDiscriminator(anyState),
                anyState.getName(),
                delta != null ?
                        ProcessedObject.DELTA_TO_PROCESSING_STATE.get(delta.getChangeType()) :
                        ObjectProcessingStateType.UNMODIFIED,
                singleDelta == null ?
                        ParsedMetricValues.fromEventMarks(
                                elementContext.getMatchingEventMarksOids(), elementContext.getAllConsideredEventMarksOids()) :
                        new ParsedMetricValues(Map.of()), // Ignoring metrics in single-delta mode
                elementContext instanceof LensFocusContext<?>,
                null, // provided later
                stateBefore,
                stateAfter,
                delta,
                InternalState.CREATING);

        if (singleDelta == null) {
            processedObject.addComputedMetricValues(
                    ObjectMetricsComputation.computeAll(
                            processedObject,
                            elementContext,
                            simulationTransaction.getSimulationResult(),
                            ModelBeans.get().simulationResultManager.getExplicitMetricDefinitions(),
                            task,
                            result));
        } else {
            // Ignoring metrics in single-delta mode
            processedObject.addComputedMetricValues(List.of());
        }

        return processedObject;
    }

    private void addComputedMetricValues(@NotNull List<SimulationProcessedObjectMetricValueType> newValues) {
        assert internalState == InternalState.CREATING;
        parsedMetricValues.addMetricValues(newValues);
        internalState = InternalState.CREATED;
        invalidateCachedBean();
    }

    /**
     * Creates {@link ProcessedObjectImpl} for low-level shadow simulation scenarios.
     */
    @Experimental
    static @NotNull ProcessedObjectImpl<ShadowType> createForShadow(
            @NotNull ShadowSimulationData data, @NotNull SimulationTransactionImpl simulationTransaction)
            throws CommonException {

        ShadowType shadowBefore = data.getShadowBefore();
        ObjectDelta<ShadowType> delta = data.getDelta();
        ShadowType shadowAfter = shadowBefore.clone();
        delta.applyTo(shadowAfter.asPrismObject());

        List<String> marks = determineShadowEventMarks(shadowBefore, shadowAfter);

        var processedObject = new ProcessedObjectImpl<>(
                simulationTransaction.getTransactionId(),
                shadowBefore.getOid(),
                ShadowType.class,
                null,
                determineShadowDiscriminator(shadowAfter), // or from "before" state?
                shadowAfter.getName(),
                ProcessedObject.DELTA_TO_PROCESSING_STATE.get(delta.getChangeType()),
                ParsedMetricValues.fromEventMarks(
                        marks,
                        List.of(SystemObjectsType.MARK_SHADOW_CLASSIFICATION_CHANGED.value(),
                                SystemObjectsType.MARK_SHADOW_CORRELATION_STATE_CHANGED.value())),
                false,
                null,
                shadowBefore,
                shadowAfter,
                delta,
                InternalState.CREATING);

        processedObject.addComputedMetricValues(List.of()); // Ignoring custom metrics in this mode

        // TODO we should somehow record errors during low-level shadow management (classification, correlation)
        processedObject.setResultAndStatus(null, OperationResultStatusType.SUCCESS);

        return processedObject;
    }

    private static List<String> determineShadowEventMarks(ShadowType before, ShadowType after) {
        List<String> marks = new ArrayList<>();
        if (isClassificationChanged(before, after)) {
            marks.add(SystemObjectsType.MARK_SHADOW_CLASSIFICATION_CHANGED.value());
        }
        if (isCorrelationStateChanged(before, after)) {
            marks.add(SystemObjectsType.MARK_SHADOW_CORRELATION_STATE_CHANGED.value());
        }
        return marks;
    }

    private static boolean isClassificationChanged(ShadowType before, ShadowType after) {
        return before.getKind() != after.getKind()
                || !Objects.equals(before.getIntent(), after.getIntent())
                || !Objects.equals(before.getTag(), after.getTag());
    }

    private static boolean isCorrelationStateChanged(ShadowType before, ShadowType after) {
        ShadowCorrelationStateType cBefore = before.getCorrelation();
        ShadowCorrelationStateType cAfter = after.getCorrelation();
        CorrelationSituationType situationBefore = cBefore != null ? cBefore.getSituation() : null;
        CorrelationSituationType situationAfter = cAfter != null ? cAfter.getSituation() : null;
        if (situationBefore != situationAfter) {
            return true;
        }
        String resultingOwnerOidBefore = Referencable.getOid(cBefore != null ? cBefore.getResultingOwner() : null);
        String resultingOwnerOidAfter = Referencable.getOid(cAfter != null ? cAfter.getResultingOwner() : null);
        if (!Objects.equals(resultingOwnerOidBefore, resultingOwnerOidAfter)) {
            return true;
        }
        ResourceObjectOwnerOptionsType ownerOptionsBefore = cBefore != null ? cBefore.getOwnerOptions() : null;
        ResourceObjectOwnerOptionsType ownerOptionsAfter = cAfter != null ? cAfter.getOwnerOptions() : null;
        return !Objects.equals(ownerOptionsBefore, ownerOptionsAfter);
    }

    private static <O extends ObjectType> @Nullable ObjectReferenceType determineStructuralArchetypeRef(
            @NotNull O object, @NotNull OperationResult result) throws SchemaException {
        if (!(object instanceof AssignmentHolderType assignmentHolder)) {
            return null;
        }
        var archetype = ModelBeans.get().archetypeManager.determineStructuralArchetype(assignmentHolder, result);
        return ObjectTypeUtil.createObjectRef(archetype);
    }

    private static <O extends ObjectType> ShadowDiscriminatorType determineShadowDiscriminator(O object) {
        if (!(object instanceof ShadowType shadow)) {
            return null;
        }
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

    public @Nullable String getResourceOid() {
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
    public @NotNull Collection<String> getMatchingEventMarksOids() {
        return parsedMetricValues.getMatchingEventMarks();
    }

    @Override
    public @NotNull Collection<ObjectReferenceType> getEffectiveObjectMarksRefs() {
        return ObjectTypeUtil.getReallyEffectiveMarkRefs(
                getBeforeOrAfterRequired());
    }

    @Override
    public boolean isFocus() {
        return Boolean.TRUE.equals(focus);
    }

    void setProjectionRecords(Integer projectionRecords) {
        this.projectionRecords = projectionRecords;
        invalidateCachedBean();
    }

    private void invalidateCachedBean() {
        this.cachedBean = null;
    }

    @Override
    public @Nullable OperationResultStatus getResultStatus() {
        return resultStatus;
    }

    @Override
    public @Nullable OperationResult getResult() {
        return result;
    }

    void setResultAndStatus(@Nullable OperationResult result) {
        assert result == null || result.isClosed();
        this.result = result;
        this.resultStatus = result != null ? result.getStatus() : null;
        invalidateCachedBean();
    }

    private void setResultAndStatus(@Nullable OperationResultType resultBean, @Nullable OperationResultStatusType statusBean) {
        this.result = OperationResult.createOperationResult(resultBean);
        this.resultStatus = OperationResultStatus.parseStatusType(statusBean);
        invalidateCachedBean();
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

    @Override
    public @NotNull SimulationResultProcessedObjectType toBean() {
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
                    .result(result != null ? result.createBeanRootOnly() : null)
                    .resultStatus(OperationResultStatus.createStatusType(resultStatus))
                    .before(prepareObjectForStorage(before))
                    .after(prepareObjectForStorage(after))
                    .delta(DeltaConvertor.toObjectDeltaType(delta));

            // Event marks
            List<ObjectReferenceType> eventMarkRef = bean.getEventMarkRef();
            parsedMetricValues.getMatchingEventMarks().forEach(
                    oid -> eventMarkRef.add(ObjectTypeUtil.createObjectRef(oid, ObjectTypes.MARK)));

            List<ObjectReferenceType> consideredEventMarkRef = bean.getConsideredEventMarkRef();
            parsedMetricValues.getAllConsideredEventMarks().forEach(
                    oid -> consideredEventMarkRef.add(ObjectTypeUtil.createObjectRef(oid, ObjectTypes.MARK)));

            // Explicit marks
            bean.getMetricValue().addAll(
                    parsedMetricValues.getExplicitMetricValueBeans());

            // Built-in marks are not stored into the bean

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
        sb.append("; status=");
        sb.append(resultStatus);
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
        DebugUtil.debugDumpWithLabelLn(sb, "result", String.valueOf(result), indent + 1);
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
     * Counts modifications based on the specified path, excluding changes in ADD/DELETE deltas.
     * <p>
     * The item path must exactly match the configuration.
     * </p>
     * Sub-item modifications are not counted.
     *
     * @param itemPath the path to count modifications for
     * @return the number of attribute modifications.
     */
    @SuppressWarnings("unused") // used in scripts
    public int getItemModificationsCount(@NotNull ItemPath itemPath) {
        return getItemModificationsCount(itemPath, false);
    }

    /**
     * Counts modifications based on the specified path, excluding changes in ADD/DELETE deltas.
     * <p>
     * The item path must exactly match the configuration.
     * </p>
     * If {@code allowSubPaths} is true, sub-item modifications are also counted.
     *
     * @param itemPath the path to count modifications for
     * @param allowSubPaths if true, sub-item modifications are allowed
     * @return the number of attribute modifications
     */
    @SuppressWarnings("unused") // used in scripts
    public int getItemModificationsCount(@NotNull ItemPath itemPath, boolean allowSubPaths) {
        if (delta == null || !delta.isModify()) {
            return 0;
        }

        return (int) delta.getModifications().stream()
                .filter(itemDelta -> {
                    ItemPath deltaPath = itemDelta.getPath();
                    return allowSubPaths
                            ? deltaPath.isSuperPathOrEquivalent(itemPath)
                            : deltaPath.equivalent(itemPath);
                })
                .count();
    }

    /**
     * Returns the number of VALUES of associations added or deleted.
     *
     * Not counting changes in ADD/DELETE deltas.
     * Assuming that there are no REPLACE modifications of associations.
     * Assuming that there are no ADD/DELETE modifications of the whole associations container.
     */
    @SuppressWarnings("unused") // used in scripts
    public int getAssociationValuesChanged() throws SchemaException {
        if (delta == null || !delta.isModify()) {
            return 0;
        }
        int sum = 0;
        for (ItemDelta<?, ?> mod : delta.getModifications()) {
            sum += ShadowAssociationsCollection.ofDelta(mod).getNumberOfValues();
        }
        return sum;
    }

    @Nullable MetricValue getMetricValue(@NotNull SimulationMetricReference ref) {
        assert internalState == InternalState.CREATED;
        if (ref instanceof SimulationMetricReference.BuiltIn) {
            return matches(((SimulationMetricReference.BuiltIn) ref).getBuiltIn()) ? MetricValue.ONE : MetricValue.ZERO;
        } else {
            return parsedMetricValues.getMetricValue(ref);
        }
    }

    private boolean matches(@NotNull BuiltInSimulationMetricType builtIn) {
        switch (builtIn) {
            case ADDED:
                return isAddition();
            case MODIFIED:
                return isModification();
            case DELETED:
                return isDeletion();
            case ERRORS:
                return resultStatus != null && resultStatus.isError();
            default:
                throw new AssertionError(builtIn);
        }
    }

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

    @Override
    public boolean hasEventMark(@NotNull String eventMarkOid) {
        return parsedMetricValues.getMatchingEventMarks().contains(eventMarkOid);
    }

    @Override
    public boolean hasNoEventMarks() {
        return parsedMetricValues.getMatchingEventMarks().isEmpty();
    }

    PartitionScope partitionScope() {
        return new PartitionScope(
                getTypeName(), getStructuralArchetypeOid(), getResourceOid(), getKind(), getIntent(), ALL_DIMENSIONS);
    }

    void propagateRecordId() {
        recordId = Objects.requireNonNull(
                cachedBean != null ? cachedBean.getId() : null,
                () -> "No cached bean or no ID in it: " + cachedBean);
    }

    private Set<?> getRealValuesBefore(@NotNull ItemPath path) {
        return getRealValues(before, path);
    }

    private Set<? extends PrismValue> getPrismValuesBefore(@NotNull ItemPath path) {
        return getPrismValues(before, path);
    }

    private Set<?> getRealValuesAfter(@NotNull ItemPath path) {
        return getRealValues(after, path);
    }

    private Set<? extends PrismValue> getPrismValuesAfter(@NotNull ItemPath path) {
        return getPrismValues(after, path);
    }

    @Override
    public @NotNull Collection<ProcessedObjectItemDelta<?, ?>> getItemDeltas(
            @Nullable Object pathsToInclude, @Nullable Object pathsToExclude, @Nullable Boolean includeOperationalItems) {
        if (delta != null && delta.isModify()) {
            ItemDeltaFilter filter = ItemDeltaFilter.create(pathsToInclude, pathsToExclude, includeOperationalItems);
            return delta.getModifications().stream()
                    .map(itemDelta -> new ProcessedObjectItemDeltaImpl<>(itemDelta))
                    .filter(itemDelta -> filter.matches(itemDelta))
                    .filter(itemDelta -> !itemDelta.isPhantom())
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    private Set<?> getRealValues(O object, ItemPath path) {
        return getPrismValues(object, path).stream()
                .map(v -> v.getRealValue())
                .collect(Collectors.toSet());
    }

    private Set<? extends PrismValue> getPrismValues(O object, ItemPath path) {
        if (object == null) {
            return Set.of();
        }
        Item<?, ?> item = object.asPrismContainerValue().findItem(path);
        return item != null ?
                Set.copyOf(item.getValues()) :
                Set.of();
    }

    @Override
    public @NotNull Collection<Metric> getMetrics(@Nullable Boolean showEventMarks, @Nullable Boolean showExplicitMetrics) {
        List<Metric> metrics = new ArrayList<>();
        if (!Boolean.FALSE.equals(showEventMarks)) {
            metrics.addAll(
                    parsedMetricValues.getValuesForEventMarks());
        }
        if (!Boolean.FALSE.equals(showExplicitMetrics)) {
            metrics.addAll(
                    parsedMetricValues.getValuesForExplicitMetrics());
        }
        return metrics;
    }

    @Override
    public void applyDefinitions(@NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException {
        if (!ShadowType.class.equals(type)) {
            return;
        }
        ShadowType shadow = (ShadowType) getAfterOrBefore();
        if (shadow == null) {
            throw new IllegalStateException("No object? In: " + this);
        }
        if (delta != null && !delta.isEmpty()) {
            ModelBeans.get().provisioningService.applyDefinition(delta, shadow, task, result);
        }
        if (before != null) {
            ModelBeans.get().provisioningService.applyDefinition(before.asPrismObject(), task, result);
        }
        if (after != null) {
            ModelBeans.get().provisioningService.applyDefinition(after.asPrismObject(), task, result);
        }
    }

    @Override
    public void fixEstimatedOldValuesInDelta() {
        if (!ObjectDelta.isModify(delta) || before == null) {
            return;
        }

        for (ItemDelta<?, ?> modification : delta.getModifications()) {
            if (modification.getEstimatedOldValues() == null) {
                ItemPath path = modification.getPath();
                var item = before.asPrismContainerValue().findItem(path);
                //noinspection unchecked
                modification.setEstimatedOldValuesWithCloning(
                        item != null ? item.getValues() : List.of());
            }
        }
    }

    class ProcessedObjectItemDeltaImpl<V extends PrismValue, D extends ItemDefinition<?>>
            implements ItemDeltaDelegator<V, D>, ProcessedObjectItemDelta<V, D> {

        @NotNull private final ItemDelta<V, D> delegate;

        ProcessedObjectItemDeltaImpl(@NotNull ItemDelta<V, D> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ItemDelta<V, D> delegate() {
            return delegate;
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public ItemDelta<V, D> clone() {
            return new ProcessedObjectItemDeltaImpl<>(delegate);
        }

        @Override
        public @NotNull Set<?> getRealValuesBefore() {
            return ProcessedObjectImpl.this.getRealValuesBefore(getPath());
        }

        @Override
        public @NotNull Set<? extends PrismValue> getPrismValuesBefore() {
            return ProcessedObjectImpl.this.getPrismValuesBefore(getPath());
        }

        @Override
        public @NotNull Set<?> getRealValuesAfter() {
            return ProcessedObjectImpl.this.getRealValuesAfter(getPath());
        }

        @Override
        public @NotNull Set<? extends PrismValue> getPrismValuesAfter() {
            return ProcessedObjectImpl.this.getPrismValuesAfter(getPath());
        }

        @Override
        public @NotNull Set<?> getRealValuesAdded() {
            return PrismValueCollectionsUtil.getRealValuesOfCollection(
                    PrismValueCollectionsUtil.differenceConsideringIds(
                            getPrismValuesAfter(),
                            getPrismValuesBefore(),
                            ParameterizedEquivalenceStrategy.REAL_VALUE));
        }

        @Override
        public @NotNull Set<?> getRealValuesDeleted() {
            return PrismValueCollectionsUtil.getRealValuesOfCollection(
                    PrismValueCollectionsUtil.differenceConsideringIds(
                            getPrismValuesBefore(),
                            getPrismValuesAfter(),
                            ParameterizedEquivalenceStrategy.REAL_VALUE));
        }

        @Override
        public @NotNull Set<?> getRealValuesModified() {
            return PrismValueCollectionsUtil.getRealValuesOfCollection(
                    PrismValueCollectionsUtil.sameIdDifferentContent(
                            getPrismValuesBefore(),
                            getPrismValuesAfter(),
                            ParameterizedEquivalenceStrategy.REAL_VALUE));
        }

        @Override
        public @NotNull Set<?> getRealValuesUnchanged() {
            return PrismValueCollectionsUtil.getRealValuesOfCollection(
                    PrismValueCollectionsUtil.intersection(
                            getPrismValuesBefore(),
                            getPrismValuesAfter(),
                            ParameterizedEquivalenceStrategy.REAL_VALUE));
        }

        /** Either we have only operational data changed, or the delta is phantom one indeed. */
        public boolean isPhantom() {
            return getValuesWithStates().isEmpty();
        }

        @Override
        public @NotNull Collection<ValueWithState> getValuesWithStates() {
            List<ValueWithState> all = new ArrayList<>();
            getRealValuesAdded().forEach(v -> all.add(new ValueWithState(v, ValueWithState.State.ADDED)));
            getRealValuesDeleted().forEach(v -> all.add(new ValueWithState(v, ValueWithState.State.DELETED)));
            if (isReplace()) {
                // We provide the information about modified values only if the delta is "REPLACE".
                // The reason is that for ADD/DELETE deltas, all modifications should be covered by separate sub-item deltas.
                getRealValuesModified().forEach(v -> all.add(new ValueWithState(v, ValueWithState.State.MODIFIED)));
            }
            //getRealValuesUnchanged().forEach(v -> all.add(new ValueWithState(v, ValueWithState.State.UNCHANGED)));
            return all;
        }

        @Override
        public @Nullable AssignmentType getRelatedAssignment() {
            ItemPath path = getPath();
            if (!path.startsWith(AssignmentHolderType.F_ASSIGNMENT)) {
                return null;
            }
            ItemPath rest = path.rest();
            Long id = rest.firstToIdOrNull();
            if (id == null) {
                return null;
            }
            O any = MiscUtil.getFirstNonNull(after, before);
            if (!(any instanceof AssignmentHolderType)) {
                return null; // should not occur
            }
            return ((AssignmentHolderType) any).getAssignment().stream()
                    .filter(a -> id.equals(a.getId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String toString() {
            return "ProcessedObjectItemDelta{" + delegate + '}';
        }
    }

    static class MetricValue implements Serializable {

        public static final MetricValue ZERO = new MetricValue(BigDecimal.ZERO, false);
        static final MetricValue ONE = new MetricValue(BigDecimal.ONE, true);

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
        PARSED
    }

    static class ParsedMetricValues implements DebugDumpable, Serializable {

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
                        SimulationMetricReference.forExplicit(valueBean.getIdentifier()),
                        new MetricValue(valueBean.getValue(), BooleanUtils.toBooleanDefaultIfNull(valueBean.isSelected(), false)));
            }
        }

        @NotNull Collection<String> getMatchingEventMarks() {
            return valueMap.entrySet().stream()
                    .filter(e -> e.getValue().inSelection)
                    .filter(e -> e.getKey() instanceof SimulationMetricReference.Mark)
                    .map(e -> ((SimulationMetricReference.Mark) e.getKey()).getOid())
                    .collect(Collectors.toSet());
        }

        @NotNull Collection<String> getAllConsideredEventMarks() {
            return valueMap.keySet().stream()
                    .filter(ref -> ref instanceof SimulationMetricReference.Mark)
                    .map(ref -> ((SimulationMetricReference.Mark) ref).getOid())
                    .collect(Collectors.toSet());
        }

        @NotNull Collection<SimulationProcessedObjectMetricValueType> getExplicitMetricValueBeans() {
            return valueMap.entrySet().stream()
                    .filter(e -> e.getKey() instanceof SimulationMetricReference.Explicit)
                    .map(e -> new SimulationProcessedObjectMetricValueType()
                            .identifier(((SimulationMetricReference.Explicit) e.getKey()).getIdentifier())
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

        @Nullable MetricValue getMetricValue(@NotNull SimulationMetricReference ref) {
            return valueMap.get(ref);
        }

        @NotNull Collection<? extends Metric> getValuesForEventMarks() {
            return valueMap.entrySet().stream()
                    .filter(e -> e.getKey() instanceof SimulationMetricReference.Mark)
                    .map(e -> MetricImpl.of(e))
                    .collect(Collectors.toList());
        }

        @NotNull Collection<? extends Metric> getValuesForExplicitMetrics() {
            return valueMap.entrySet().stream()
                    .filter(e -> e.getKey() instanceof SimulationMetricReference.Explicit)
                    .map(e -> MetricImpl.of(e))
                    .collect(Collectors.toList());
        }
    }

    static class MetricImpl implements Metric {

        @NotNull private final SimulationMetricReference reference;
        @NotNull private final MetricValue value;

        MetricImpl(@NotNull SimulationMetricReference reference, @NotNull MetricValue value) {
            this.reference = reference;
            this.value = value;
        }

        public static MetricImpl of(Map.Entry<SimulationMetricReference, MetricValue> entry) {
            return new MetricImpl(entry.getKey(), entry.getValue());
        }

        @Override
        public @Nullable ObjectReferenceType getEventMarkRef() {
            return reference instanceof SimulationMetricReference.Mark ?
                    ((SimulationMetricReference.Mark) reference).getRef() : null;
        }

        @Override
        public @Nullable String getId() {
            return reference instanceof SimulationMetricReference.Explicit ?
                    ((SimulationMetricReference.Explicit) reference).getIdentifier() : null;
        }

        @Override
        public boolean isSelected() {
            return value.inSelection;
        }

        @Override
        public BigDecimal getValue() {
            return value.value;
        }
    }
}
