/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import java.util.*;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Mapping is non-recyclable single-use object. Once evaluated it should not be evaluated again. It will retain its original
 * inputs and outputs that can be read again and again. But these should not be changed after evaluation.
 * <p>
 * Configuration properties are unmodifiable. They are to be set via Mapping.Builder.
 *
 * @author Radovan Semancik
 */
public class MappingImpl<V extends PrismValue, D extends ItemDefinition>
        implements Mapping<V, D>, DebugDumpable, PrismValueDeltaSetTripleProducer<V, D> {

    private static final String OP_EVALUATE_PREPARED = MappingImpl.class.getName() + ".evaluatePrepared";
    private static final String OP_EVALUATE = MappingImpl.class.getName() + ".evaluate";
    private static final String OP_PREPARE = MappingImpl.class.getName() + ".prepare";

    // configuration properties (unmodifiable)
    private final MappingType mappingType;
    private final ExpressionFactory expressionFactory;
    private final ExpressionVariables variables;
    private final PrismContext prismContext;

    private final ObjectDeltaObject<?> sourceContext;
    private TypedValue<ObjectDeltaObject<?>> typedSourceContext; // cached
    private final Collection<Source<?, ?>> sources;
    private final Source<?, ?> defaultSource;

    private final PrismObjectDefinition<?> targetContext;
    private final ItemPath defaultTargetPath;
    private final D defaultTargetDefinition;
    private final Collection<V> originalTargetValues;
    private final ExpressionProfile expressionProfile;

    private final ObjectResolver objectResolver;
    private final SecurityContextManager securityContextManager;          // in order to get c:actor variable
    private final OriginType originType;
    private final ObjectType originObject;
    private final ValuePolicyResolver stringPolicyResolver;
    private final boolean conditionMaskOld;
    private final boolean conditionMaskNew;
    private final XMLGregorianCalendar defaultReferenceTime;
    private final XMLGregorianCalendar now;
    private final boolean profiling;
    private final String contextDescription;

    private final QName mappingQName;                        // This is sometimes used to identify the element that mapping produces
    // if it is different from itemName. E.g. this happens with associations.
    private final RefinedObjectClassDefinition refinedObjectClassDefinition;

    // working and output properties
    private D outputDefinition;
    private ItemPath outputPath;
    private MappingEvaluationState state = MappingEvaluationState.UNINITIALIZED;

    private PrismValueDeltaSetTriple<V> outputTriple;
    private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionOutputTriple;
    private Boolean timeConstraintValid;
    private XMLGregorianCalendar nextRecomputeTime;
    private Long evaluationStartTime;
    private Long evaluationEndTime;

    private String mappingContextDescription;

    private VariableProducer variableProducer;

    private MappingEvaluationTraceType trace;

    /**
     * Mapping pre-expression is invoked just before main mapping expression.
     * Pre expression will get the same expression context as the main expression.
     * This is an opportunity to manipulate the context just before evaluation.
     * Or maybe evaluate additional expressions that set up environment for
     * main expression.
     */
    private MappingPreExpression mappingPreExpression;

    // This is single-use only. Once evaluated it is not used any more
    // it is remembered only for tracing purposes.
    private Expression<V, D> expression;

    // Mapping state properties that are exposed to the expressions. They can be used by the expressions to "communicate".
    // E.g. one expression seting the property and other expression checking the property.
    private Map<String, Object> stateProperties;

    private static final Trace LOGGER = TraceManager.getTrace(MappingImpl.class);

    private MappingImpl(Builder<V, D> builder) {
        prismContext = builder.prismContext;
        expressionFactory = builder.expressionFactory;
        variables = builder.variables;
        mappingType = builder.mappingType;
        objectResolver = builder.objectResolver;
        securityContextManager = builder.securityContextManager;
        defaultSource = builder.defaultSource;
        defaultTargetDefinition = builder.defaultTargetDefinition;
        expressionProfile = builder.expressionProfile;
        defaultTargetPath = builder.defaultTargetPath;
        originalTargetValues = builder.originalTargetValues;
        sourceContext = builder.sourceContext;
        targetContext = builder.targetContext;
        sources = builder.sources;
        originType = builder.originType;
        originObject = builder.originObject;
        stringPolicyResolver = builder.valuePolicyResolver;
        variableProducer = builder.variableProducer;
        mappingPreExpression = builder.mappingPreExpression;
        conditionMaskOld = builder.conditionMaskOld;
        conditionMaskNew = builder.conditionMaskNew;
        defaultReferenceTime = builder.defaultReferenceTime;
        profiling = builder.profiling;
        contextDescription = builder.contextDescription;
        mappingQName = builder.mappingQName;
        refinedObjectClassDefinition = builder.refinedObjectClassDefinition;
        now = builder.now;
    }

    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public QName getItemName() {
        if (outputDefinition != null) {
            return outputDefinition.getItemName();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public OriginType getOriginType() {
        return originType;
    }

    public ObjectType getOriginObject() {
        return originObject;
    }

    public Source<?, ?> getDefaultSource() {
        return defaultSource;
    }

    @SuppressWarnings("unused")
    public D getDefaultTargetDefinition() {
        return defaultTargetDefinition;
    }

    @SuppressWarnings("unused")
    public ItemPath getDefaultTargetPath() {
        return defaultTargetPath;
    }

    public ObjectDeltaObject<?> getSourceContext() {
        return sourceContext;
    }

    @SuppressWarnings("WeakerAccess")
    public PrismObjectDefinition<?> getTargetContext() {
        return targetContext;
    }

    public String getContextDescription() {
        return contextDescription;
    }

    private TypedValue<ObjectDeltaObject<?>> getTypedSourceContext() {
        if (sourceContext == null) {
            return null;
        }
        if (typedSourceContext == null) {
            typedSourceContext = new TypedValue<>(sourceContext, sourceContext.getDefinition());
        }
        return typedSourceContext;
    }

    public String getMappingContextDescription() {
        if (mappingContextDescription == null) {
            StringBuilder sb = new StringBuilder("mapping ");
            if (mappingType.getName() != null) {
                sb.append("'").append(mappingType.getName()).append("' ");
            }
            sb.append("in ");
            sb.append(contextDescription);
            mappingContextDescription = sb.toString();
        }
        return mappingContextDescription;
    }

    public MappingType getMappingType() {
        return mappingType;
    }

    @SuppressWarnings("unused")
    public MappingPreExpression getMappingPreExpression() {
        return mappingPreExpression;
    }

    public void setMappingPreExpression(MappingPreExpression mappingPreExpression) {
        this.mappingPreExpression = mappingPreExpression;
    }

    @Override
    public boolean isSourceless() {
        return sources.isEmpty();
    }

    @Override
    public MappingStrengthType getStrength() {
        return getStrength(mappingType);
    }

    public static MappingStrengthType getStrength(MappingType mappingType) {
        if (mappingType == null) {
            return MappingStrengthType.NORMAL;
        }
        MappingStrengthType value = mappingType.getStrength();
        if (value == null) {
            value = MappingStrengthType.NORMAL;
        }
        return value;
    }

    @Override
    public boolean isAuthoritative() {
        if (mappingType == null) {
            return true;
        }
        Boolean value = mappingType.isAuthoritative();
        if (value == null) {
            value = true;
        }
        return value;
    }

    @Override
    public boolean isExclusive() {
        if (mappingType == null) {
            return false;
        }
        Boolean value = mappingType.isExclusive();
        if (value == null) {
            value = false;
        }
        return value;
    }

    public boolean hasTargetRange() {
        return mappingType.getTarget().getSet() != null;
    }

    @SuppressWarnings("unused")
    public boolean isConditionMaskOld() {
        return conditionMaskOld;
    }

    @SuppressWarnings("unused")
    public boolean isConditionMaskNew() {
        return conditionMaskNew;
    }

    private PrismContext getPrismContext() {
        return prismContext;
    }

    @SuppressWarnings("unused")
    public ValuePolicyResolver getStringPolicyResolver() {
        return stringPolicyResolver;
    }

    @SuppressWarnings("unused")
    public boolean isApplicableToChannel(String channelUri) {
        return isApplicableToChannel(mappingType, channelUri);
    }

    public static boolean isApplicableToChannel(MappingType mappingType, String channelUri) {
        List<String> exceptChannel = mappingType.getExceptChannel();
        if (exceptChannel != null && !exceptChannel.isEmpty()) {
            return !exceptChannel.contains(channelUri);
        }
        List<String> applicableChannels = mappingType.getChannel();
        return applicableChannels == null || applicableChannels.isEmpty() || applicableChannels.contains(channelUri);
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }

    @SuppressWarnings("unused")
    public XMLGregorianCalendar getDefaultReferenceTime() {
        return defaultReferenceTime;
    }

    public XMLGregorianCalendar getNextRecomputeTime() {
        return nextRecomputeTime;
    }

    public boolean isProfiling() {
        return profiling;
    }

    @SuppressWarnings("unused")
    public Long getEvaluationStartTime() {
        return evaluationStartTime;
    }

    @SuppressWarnings("unused")
    public Long getEvaluationEndTime() {
        return evaluationEndTime;
    }

    public Long getEtime() {
        if (evaluationStartTime == null || evaluationEndTime == null) {
            return null;
        }
        return evaluationEndTime - evaluationStartTime;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer#getMappingQName()
     */
    @Override
    public QName getMappingQName() {
        return mappingQName;
    }

    @SuppressWarnings("WeakerAccess")
    public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
        return refinedObjectClassDefinition;
    }

    @Override
    public <T> T getStateProperty(String propertyName) {
        if (stateProperties == null) {
            return null;
        }
        //noinspection unchecked
        return (T) stateProperties.get(propertyName);
    }

    @Override
    public <T> T setStateProperty(String propertyName, T value) {
        if (stateProperties == null) {
            stateProperties = new HashMap<>();
        }
        //noinspection unchecked
        return (T) stateProperties.put(propertyName, value);
    }

    // TODO: rename to evaluateAll
    public void evaluate(Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .addArbitraryObjectAsContext("mapping", this)
                .addArbitraryObjectAsContext("context", getContextDescription())
                .addArbitraryObjectAsContext("task", task)
                .setMinor()
                .build();
        if (result.isTracingNormal(MappingEvaluationTraceType.class)) {
            // temporary solution - to avoid checking level at too many places
            trace = new MappingEvaluationTraceType(prismContext);
            trace.setMapping(mappingType.clone());
            result.addTrace(trace);
        } else {
            trace = null;
        }
        try {
            prepare(task, result);

            //        if (!isActivated()) {
            //            outputTriple = null;
            //            LOGGER.debug("Skipping evaluation of mapping {} in {} because it is not activated",
            //                    mappingType.getName() == null?null:mappingType.getName(), contextDescription);
            //            return;
            //        }

            evaluateBody(task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Prepare mapping for evaluation.  Parse the values
     * After this call it can be checked if a mapping is activated (i.e. if the input changes will "trigger" the mapping).
     */
    public void prepare(Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        OperationResult result = parentResult.subresult(OP_PREPARE)
                .addArbitraryObjectAsContext("mapping", this)
                .addArbitraryObjectAsContext("task", task)
                .setMinor()
                .build();
        assertState(MappingEvaluationState.UNINITIALIZED);
        try {

            parseSources(task, result);

            parseTarget();
            if (outputPath != null && outputDefinition == null) {
                throw new IllegalArgumentException("No output definition, cannot evaluate " + getMappingContextDescription());
            }

        } catch (ExpressionEvaluationException | ObjectNotFoundException | RuntimeException | SchemaException |
                CommunicationException | SecurityViolationException | ConfigurationException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        transitionState(MappingEvaluationState.PREPARED);
        result.recordSuccess();
    }

    private void traceSources() throws SchemaException {
        for (Source<?, ?> source : sources) {
            MappingSourceEvaluationTraceType sourceTrace = new MappingSourceEvaluationTraceType(prismContext);
            sourceTrace.setName(source.getName());
            sourceTrace.setItemDeltaItem(source.toItemDeltaItemType());
            trace.getSource().add(sourceTrace);
        }
    }

    public boolean isActivated() {
        // TODO
//        return isActivated;
        return sourcesChanged();
    }

    // TODO: rename to evaluate -- or evaluatePrepared?
    private void evaluateBody(Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        assertState(MappingEvaluationState.PREPARED);

        OperationResult result = parentResult.subresult(OP_EVALUATE_PREPARED)
                .addArbitraryObjectAsContext("mapping", this)
                .addArbitraryObjectAsContext("task", task)
                .setMinor()
                .build();

        traceEvaluationStart();

        try {

            if (trace != null) {
                traceSources();
            }

            // We may need to re-parse the sources here

            evaluateTimeConstraintValid(task, result);
            if (trace != null) {
                trace.setNextRecomputeTime(nextRecomputeTime);
                trace.setTimeConstraintValid(timeConstraintValid);
            }

            if (!timeConstraintValid) {
                outputTriple = null;
                result.recordNotApplicableIfUnknown();
                traceDeferred();
                return;
            }

            evaluateCondition(task, result);

            boolean conditionOutputOld = computeConditionResult(conditionOutputTriple == null ? null : conditionOutputTriple.getNonPositiveValues());
            boolean conditionResultOld = conditionOutputOld && conditionMaskOld;

            boolean conditionOutputNew = computeConditionResult(conditionOutputTriple == null ? null : conditionOutputTriple.getNonNegativeValues());
            boolean conditionResultNew = conditionOutputNew && conditionMaskNew;

            if (trace != null) {
                trace.setConditionResultOld(conditionResultOld);
                trace.setConditionResultNew(conditionResultNew);
            }

            boolean applicable = conditionResultOld || conditionResultNew;
            if (applicable) {
                // TODO trace source and target values ... and range processing
                evaluateExpression(task, result, conditionResultOld, conditionResultNew);
                fixDefinition();
                recomputeValues();
                setOrigin();
                adjustForAuthoritative();
            } else {
                outputTriple = null;
            }
            checkRange(task, result); // we check the range even for not-applicable mappings (MID-5953)
            transitionState(MappingEvaluationState.EVALUATED);

            if (applicable) {
                result.recordSuccess();
                traceSuccess(conditionResultOld, conditionResultNew);
            } else {
                result.recordNotApplicableIfUnknown();
                traceNotApplicable("condition is false");
            }

            if (trace != null) {
                traceOutput();
            }

        } catch (Throwable e) {
            result.recordFatalError(e);
            traceFailure(e);
            throw e;
        }
    }

    private void traceOutput() {
        if (outputTriple != null) {
            trace.setOutput(DeltaSetTripleType.fromDeltaSetTriple(outputTriple));
        }
    }

    private void adjustForAuthoritative() {
        if (isAuthoritative()) {
            return;
        }
        if (outputTriple == null) {
            return;
        }
        // Non-authoritative mappings do not remove values. Simply eliminate any values from the
        // minus set to do that.
        // However, we need to do this before we process range. Non-authoritative mappings may
        // still remove values if range is set. We do not want to ignore minus values from
        // range processing.
        outputTriple.clearMinusSet();
    }

    private void checkRange(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        VariableBindingDefinitionType target = mappingType.getTarget();
        if (target != null && target.getSet() != null) {
            checkRangeTarget(task, result);
        }
    }

    private void checkRangeTarget(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        String name;
        if (outputPath != null) {
            name = outputPath.lastName().getLocalPart();
        } else {
            name = outputDefinition.getItemName().getLocalPart();
        }
        if (originalTargetValues == null) {
            throw new IllegalStateException("Couldn't check range for mapping in " + contextDescription + ", as original target values are not known.");
        }
        ValueSetDefinitionType rangetSetDefType = mappingType.getTarget().getSet();
        ValueSetDefinition<V, D> setDef = new ValueSetDefinition<>(rangetSetDefType, outputDefinition, expressionProfile, name, "range of " + name + " in " + getMappingContextDescription(), task, result);
        setDef.init(expressionFactory);
        setDef.setAdditionalVariables(variables);
        for (V originalValue : originalTargetValues) {
            if (!setDef.contains(originalValue)) {
                continue;
            }
            addToMinusIfNecessary(originalValue);
        }
    }

    @SuppressWarnings("unchecked")
    private void addToMinusIfNecessary(V originalValue) {
        if (outputTriple != null && (outputTriple.presentInPlusSet(originalValue) || outputTriple.presentInZeroSet(originalValue))) {
            return;
        }
        // remove it!
        if (outputTriple == null) {
            outputTriple = getPrismContext().deltaFactory().createPrismValueDeltaSetTriple();
        }
        LOGGER.trace("Original value is in the mapping range (while not in mapping result), adding it to minus set: {}", originalValue);
        outputTriple.addToMinusSet((V) originalValue.clone());
    }

    @SuppressWarnings("unused")         // todo is this externally used?
    public boolean isSatisfyCondition() {
        if (conditionOutputTriple == null) {
            return true;
        }
        boolean conditionOutputOld = computeConditionResult(conditionOutputTriple.getNonPositiveValues());
        boolean conditionResultOld = conditionOutputOld && conditionMaskOld;

        boolean conditionOutputNew = computeConditionResult(conditionOutputTriple.getNonNegativeValues());
        boolean conditionResultNew = conditionOutputNew && conditionMaskNew;
        return (conditionResultOld || conditionResultNew);
    }

    public PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> getConditionOutputTriple() {
        return conditionOutputTriple;
    }

    private void traceEvaluationStart() {
        if (profiling) {
            evaluationStartTime = System.currentTimeMillis();
        }
    }

    private void traceEvaluationEnd() {
        if (profiling) {
            evaluationEndTime = System.currentTimeMillis();
        }
    }

    private void traceSuccess(boolean conditionResultOld, boolean conditionResultNew) {
        traceEvaluationEnd();
        if (!isTrace()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mapping trace:\n");
        appendTraceHeader(sb);
        sb.append("\nCondition: ").append(conditionResultOld).append(" -> ").append(conditionResultNew);
        if (nextRecomputeTime != null) {
            sb.append("\nNext recompute: ");
            sb.append(nextRecomputeTime);
        }
        sb.append("\nResult: ");
        if (outputTriple == null) {
            sb.append("null");
        } else {
            sb.append(outputTriple.toHumanReadableString());
        }
        if (profiling) {
            sb.append("\nEtime: ");
            sb.append(getEtime());
            sb.append(" ms");
        }
        appendTraceFooter(sb);
        trace(sb.toString());
    }

    private void traceDeferred() {
        traceEvaluationEnd();
        if (!isTrace()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mapping trace:\n");
        appendTraceHeader(sb);
        sb.append("\nEvaluation DEFERRED to: ");
        if (nextRecomputeTime == null) {
            sb.append("null");
        } else {
            sb.append(nextRecomputeTime);
        }
        if (profiling) {
            sb.append("\nEtime: ");
            sb.append(getEtime());
            sb.append(" ms");
        }
        appendTraceFooter(sb);
        trace(sb.toString());
    }

    @SuppressWarnings("SameParameterValue")
    private void traceNotApplicable(String reason) {
        traceEvaluationEnd();
        if (!isTrace()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mapping trace:\n");
        appendTraceHeader(sb);
        sb.append("\nEvaluation is NOT APPLICABLE because ").append(reason);
        if (profiling) {
            sb.append("\nEtime: ");
            sb.append(getEtime());
            sb.append(" ms");
        }
        appendTraceFooter(sb);
        trace(sb.toString());
    }

    private void traceFailure(Throwable e) {
        LOGGER.error("Error evaluating {}: {}-{}", getMappingContextDescription(), e.getMessage(), e);
        traceEvaluationEnd();
        if (!isTrace()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mapping FAILURE:\n");
        appendTraceHeader(sb);
        sb.append("\nERROR: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        if (profiling) {
            sb.append("\nEtime: ");
            sb.append(getEtime());
            sb.append(" ms");
        }
        appendTraceFooter(sb);
        trace(sb.toString());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isTrace() {
        return trace != null || LOGGER.isTraceEnabled() || (mappingType != null && mappingType.isTrace() == Boolean.TRUE);
    }

    private void trace(String msg) {
        if (mappingType != null && mappingType.isTrace() == Boolean.TRUE) {
            LOGGER.info(msg);
        } else {
            LOGGER.trace(msg);
        }
        if (trace != null) {
            trace.setTextTrace(msg);
        }
    }

    private void appendTraceHeader(StringBuilder sb) {
        sb.append("---[ MAPPING ");
        if (mappingType.getName() != null) {
            sb.append("'").append(mappingType.getName()).append("' ");
        }
        sb.append(" in ");
        sb.append(contextDescription);
        sb.append("]---------------------------");
        MappingStrengthType strength = getStrength();
        if (strength != null) {
            sb.append("\nStrength: ").append(strength);
        }
        if (!isAuthoritative()) {
            sb.append("\nNot authoritative");
        }
        for (Source<?, ?> source : sources) {
            sb.append("\n");
            source.mediumDump(sb);
        }
        sb.append("\nTarget: ").append(MiscUtil.toString(outputDefinition));
        sb.append("\nExpression: ");
        if (expression == null) {
            sb.append("null");
        } else {
            sb.append(expression.shortDebugDump());
        }
        if (stateProperties != null) {
            sb.append("\nState:\n");
            DebugUtil.debugDumpMapMultiLine(sb, stateProperties, 1);
        }
    }

    private void appendTraceFooter(StringBuilder sb) {
        sb.append("\n------------------------------------------------------");
    }

    private boolean computeConditionResult(Collection<PrismPropertyValue<Boolean>> booleanPropertyValues) {
        // If condition is not present at all consider it to be true
        return mappingType.getCondition() == null || ExpressionUtil.computeConditionResult(booleanPropertyValues);
    }

    public boolean evaluateTimeConstraintValid(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (timeConstraintValid == null) {
            timeConstraintValid = parseTimeConstraints(task, result);
        }
        return timeConstraintValid;
    }

    // Sets nextRecomputeTime as a side effect.
    private boolean parseTimeConstraints(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        MappingTimeDeclarationType timeFromType = mappingType.getTimeFrom();
        MappingTimeDeclarationType timeToType = mappingType.getTimeTo();
        if (timeFromType == null && timeToType == null) {
            return true;
        }

        XMLGregorianCalendar timeFrom = parseTime(timeFromType, task, result);
        if (trace != null) {
            trace.setTimeFrom(timeFrom);
        }
        if (timeFrom == null && timeFromType != null) {
            // Time is specified but there is no value for it.
            // This means that event that should start validity haven't happened yet
            // therefore the mapping is not yet valid.
            return false;
        }
        XMLGregorianCalendar timeTo = parseTime(timeToType, task, result);
        if (trace != null) {
            trace.setTimeTo(timeTo);
        }

        if (timeFrom != null && timeFrom.compare(now) == DatatypeConstants.GREATER) {
            // before timeFrom
            nextRecomputeTime = timeFrom;
            return false;
        }

        if (timeTo == null && timeToType != null) {
            // Time is specified but there is no value for it.
            // This means that event that should stop validity haven't happened yet
            // therefore the mapping is still valid.
            return true;
        }

        if (timeTo != null && timeTo.compare(now) == DatatypeConstants.GREATER) {
            // between timeFrom and timeTo (also no timeFrom and before timeTo)
            nextRecomputeTime = timeTo;
            return true;
        }

        // If timeTo is null, we are "in range"
        // Otherwise it is less than now (so we are after it), i.e. we are "out of range"
        // In both cases there is nothing to recompute in the future
        return timeTo == null;
    }

    private XMLGregorianCalendar parseTime(MappingTimeDeclarationType timeType, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (timeType == null) {
            return null;
        }
        XMLGregorianCalendar referenceTime;
        ExpressionType expressionType = timeType.getExpression();
        VariableBindingDefinitionType referenceTimeType = timeType.getReferenceTime();
        if (referenceTimeType == null) {
            if (defaultReferenceTime == null) {
                if (expressionType == null) {
                    throw new SchemaException("No reference time specified, there is also no default and no expression; in time specification in " + getMappingContextDescription());
                } else {
                    referenceTime = null;
                }
            } else {
                referenceTime = defaultReferenceTime;
            }
        } else {
            referenceTime = parseTimeSource(referenceTimeType, task, result);
        }

        XMLGregorianCalendar time;
        if (expressionType == null) {
            if (referenceTime == null) {
                return null;
            } else {
                time = (XMLGregorianCalendar) referenceTime.clone();
            }
        } else {
            MutablePrismPropertyDefinition<XMLGregorianCalendar> timeDefinition = prismContext.definitionFactory().createPropertyDefinition(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, PrimitiveType.XSD_DATETIME);
            timeDefinition.setMaxOccurs(1);

            ExpressionVariables timeVariables = new ExpressionVariables();
            timeVariables.addVariableDefinitions(variables);
            timeVariables.addVariableDefinition(ExpressionConstants.VAR_REFERENCE_TIME, referenceTime, timeDefinition);

            PrismPropertyValue<XMLGregorianCalendar> timePropVal = ExpressionUtil.evaluateExpression(sources, timeVariables, timeDefinition, expressionType, expressionProfile, expressionFactory, "time expression in " + contextDescription, task, result);

            if (timePropVal == null) {
                return null;
            }

            time = timePropVal.getValue();
        }
        Duration offset = timeType.getOffset();
        if (offset != null) {
            time.add(offset);
        }
        return time;
    }

    private XMLGregorianCalendar parseTimeSource(VariableBindingDefinitionType source, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ItemPath path = getSourcePath(source);

        Object sourceObject = ExpressionUtil.resolvePathGetValue(path, variables, false, getTypedSourceContext(), objectResolver, getPrismContext(), "reference time definition in " + getMappingContextDescription(), task, result);
        if (sourceObject == null) {
            return null;
        }
        PrismProperty<XMLGregorianCalendar> timeProperty;
        if (sourceObject instanceof ItemDeltaItem<?, ?>) {
            //noinspection unchecked
            timeProperty = (PrismProperty<XMLGregorianCalendar>) ((ItemDeltaItem<?, ?>) sourceObject).getItemNew();
        } else if (sourceObject instanceof Item<?, ?>) {
            //noinspection unchecked
            timeProperty = (PrismProperty<XMLGregorianCalendar>) sourceObject;
        } else {
            throw new IllegalStateException("Unknown resolve result " + sourceObject);
        }
        return timeProperty != null ? timeProperty.getRealValue() : null;
    }

    private void parseSources(Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        List<VariableBindingDefinitionType> sourceDefinitions = mappingType.getSource();
        if (defaultSource != null) {
            defaultSource.recompute();
            this.sources.add(defaultSource);
            defaultSource.recompute();
        }
        if (sourceDefinitions != null) {
            for (VariableBindingDefinitionType sourceDefinition : sourceDefinitions) {
                Source<?, ?> source = parseSource(sourceDefinition, task, result);
                source.recompute();

                // Override existing sources (e.g. default source)
                this.sources.removeIf(next -> next.getName().equals(source.getName()));
                this.sources.add(source);
            }
        }
    }

    private <IV extends PrismValue, ID extends ItemDefinition> Source<IV, ID> parseSource(
            VariableBindingDefinitionType sourceDefinition, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ItemPath path = getSourcePath(sourceDefinition);
        QName sourceQName = sourceDefinition.getName() != null ? sourceDefinition.getName() : ItemPath.toName(path.last());
        String variableName = sourceQName.getLocalPart();

        TypedValue<?> typedSourceObject = ExpressionUtil.resolvePathGetTypedValue(path, variables, true,
                getTypedSourceContext(), objectResolver, getPrismContext(),
                "source definition in " + getMappingContextDescription(), task, result);

        Object sourceObject = typedSourceObject != null ? typedSourceObject.getValue() : null;
        Item<IV, ID> itemOld = null;
        ItemDelta<IV, ID> delta = null;
        Item<IV, ID> itemNew = null;
        ItemPath resolvePath = path;
        ItemPath residualPath = null;
        Collection<? extends ItemDelta<?, ?>> subItemDeltas = null;
        if (sourceObject != null) {
            if (sourceObject instanceof ItemDeltaItem<?, ?>) {
                //noinspection unchecked
                itemOld = ((ItemDeltaItem<IV, ID>) sourceObject).getItemOld();
                //noinspection unchecked
                delta = ((ItemDeltaItem<IV, ID>) sourceObject).getDelta();
                //noinspection unchecked
                itemNew = ((ItemDeltaItem<IV, ID>) sourceObject).getItemNew();
                //noinspection unchecked
                residualPath = ((ItemDeltaItem<IV, ID>) sourceObject).getResidualPath();
                //noinspection unchecked
                resolvePath = ((ItemDeltaItem<IV, ID>) sourceObject).getResolvePath();
                //noinspection unchecked
                subItemDeltas = ((ItemDeltaItem<IV, ID>) sourceObject).getSubItemDeltas();
            } else if (sourceObject instanceof Item<?, ?>) {
                //noinspection unchecked
                itemOld = (Item<IV, ID>) sourceObject;
                //noinspection unchecked
                itemNew = (Item<IV, ID>) sourceObject;
            } else {
                throw new IllegalStateException("Unknown resolve result " + sourceObject);
            }
        }

        ID sourceItemDefinition = typedSourceObject != null ? typedSourceObject.getDefinition() : null;

        // apply domain
        ValueSetDefinitionType domainSetType = sourceDefinition.getSet();
        if (domainSetType != null) {
            ValueSetDefinition<IV, ID> setDef = new ValueSetDefinition<>(
                    domainSetType, sourceItemDefinition, expressionProfile, variableName,
                    "domain of " + variableName + " in " + getMappingContextDescription(),
                    task, result);
            setDef.init(expressionFactory);
            setDef.setAdditionalVariables(variables);
            try {

                if (itemOld != null) {
                    //noinspection unchecked
                    itemOld = itemOld.clone();
                    itemOld.filterValues(val -> setDef.containsTunnel(val));
                }

                if (itemNew != null) {
                    //noinspection unchecked
                    itemNew = itemNew.clone();
                    itemNew.filterValues(val -> setDef.containsTunnel(val));
                }

                if (delta != null) {
                    delta = delta.clone();
                    delta.filterValues(val -> setDef.containsTunnel(val));
                }

            } catch (TunnelException te) {
                Throwable cause = te.getCause();
                if (cause instanceof SchemaException) {
                    throw (SchemaException) cause;
                } else if (cause instanceof ExpressionEvaluationException) {
                    throw (ExpressionEvaluationException) cause;
                } else if (cause instanceof ObjectNotFoundException) {
                    throw (ObjectNotFoundException) cause;
                } else if (cause instanceof CommunicationException) {
                    throw (CommunicationException) cause;
                } else if (cause instanceof ConfigurationException) {
                    throw (ConfigurationException) cause;
                } else if (cause instanceof SecurityViolationException) {
                    throw (SecurityViolationException) cause;
                }
            }
        }

        Source<IV, ID> source = new Source<>(itemOld, delta, itemNew, sourceQName, sourceItemDefinition);
        source.setResidualPath(residualPath);
        source.setResolvePath(resolvePath);
        source.setSubItemDeltas(subItemDeltas);
        return source;
    }

    @NotNull
    private ItemPath getSourcePath(VariableBindingDefinitionType sourceType) throws SchemaException {
        ItemPathType itemPathType = sourceType.getPath();
        if (itemPathType == null) {
            throw new SchemaException("No path in source definition in " + getMappingContextDescription());
        }
        ItemPath path = itemPathType.getItemPath();
        if (path.isEmpty()) {
            throw new SchemaException("Empty source path in " + getMappingContextDescription());
        }
        return path;
    }

    private boolean sourcesChanged() {
        for (Source<?, ?> source : sources) {
            if (source.getDelta() != null) {
                return true;
            }
        }
        return false;
    }

    private void parseTarget() throws SchemaException {
        VariableBindingDefinitionType targetType = mappingType.getTarget();
        if (targetType == null) {
            outputDefinition = defaultTargetDefinition;
            outputPath = defaultTargetPath;
        } else {
            ItemPathType itemPathType = targetType.getPath();
            if (itemPathType == null) {
                outputDefinition = defaultTargetDefinition;
                outputPath = defaultTargetPath;
            } else {
                ItemPath path = itemPathType.getItemPath();
                outputDefinition = ExpressionUtil.resolveDefinitionPath(
                        path, variables, targetContext,
                        "target definition in " + getMappingContextDescription());
                if (outputDefinition == null) {
                    throw new SchemaException("No target item that would conform to the path "
                            + path + " in " + getMappingContextDescription());
                }
                outputPath = path.stripVariableSegment();
            }
        }
        if (stringPolicyResolver != null) {
            stringPolicyResolver.setOutputDefinition(outputDefinition);
            stringPolicyResolver.setOutputPath(outputPath);
        }
    }

    public D getOutputDefinition() throws SchemaException {
        if (outputDefinition == null) {
            parseTarget();
        }
        return outputDefinition;
    }

    public ItemPath getOutputPath() throws SchemaException {
        if (outputDefinition == null) {
            parseTarget();
        }
        return outputPath;
    }

    /**
     * Applies definition to the output if needed.
     */
    private void fixDefinition() throws SchemaException {
        if (outputTriple == null) {
            return;
        }
        if (outputTriple.isRaw()) {
            outputTriple.applyDefinition(outputDefinition);
        }
    }

    private void recomputeValues() {
        if (outputTriple == null) {
            return;
        }
        Visitor visitor = visitable -> {
            if (visitable instanceof PrismValue) {
                ((PrismValue) visitable).recompute(getPrismContext());
            }
        };
        outputTriple.accept(visitor);
    }

    private void setOrigin() {
        if (outputTriple == null) {
            return;
        }
        if (originType != null) {
            outputTriple.setOriginType(originType);
        }
        if (originObject != null) {
            outputTriple.setOriginObject(originObject);
        }
    }

    private void evaluateCondition(Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionType conditionExpressionType = mappingType.getCondition();
        if (conditionExpressionType == null) {
            // True -> True
            conditionOutputTriple = getPrismContext().deltaFactory().createPrismValueDeltaSetTriple();
            conditionOutputTriple.addToZeroSet(getPrismContext().itemFactory().createPropertyValue(Boolean.TRUE));
            return;
        }
        Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> expression =
                ExpressionUtil.createCondition(conditionExpressionType, expressionProfile, expressionFactory,
                        "condition in " + getMappingContextDescription(), task, result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables,
                "condition in " + getMappingContextDescription(), task);
        context.setValuePolicyResolver(stringPolicyResolver);
        context.setExpressionFactory(expressionFactory);
        context.setDefaultSource(defaultSource);
        context.setDefaultTargetContext(getTargetContext());
        context.setRefinedObjectClassDefinition(getRefinedObjectClassDefinition());
        context.setMappingQName(mappingQName);
        context.setVariableProducer(variableProducer);
        conditionOutputTriple = expression.evaluate(context, result);
    }

    private void evaluateExpression(Task task, OperationResult result,
            boolean conditionResultOld, boolean conditionResultNew)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionType expressionType = null;
        if (mappingType != null) {
            expressionType = mappingType.getExpression();
        }
        expression = expressionFactory.makeExpression(expressionType, outputDefinition, expressionProfile,
                "expression in " + getMappingContextDescription(), task, result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables,
                "expression in " + getMappingContextDescription(), task);
        context.setDefaultSource(defaultSource);
        context.setSkipEvaluationMinus(!conditionResultOld);
        context.setSkipEvaluationPlus(!conditionResultNew);
        context.setValuePolicyResolver(stringPolicyResolver);
        context.setExpressionFactory(expressionFactory);
        context.setDefaultTargetContext(getTargetContext());
        context.setRefinedObjectClassDefinition(getRefinedObjectClassDefinition());
        context.setMappingQName(mappingQName);
        context.setVariableProducer(variableProducer);

        if (mappingPreExpression != null) {
            mappingPreExpression.mappingPreExpression(context, result);
        }

        outputTriple = expression.evaluate(context, result);

        if (outputTriple == null) {

            if (conditionResultNew) {
                // We need to return empty triple instead of null.
                // The condition was true (or there was not condition at all)
                // so the mapping is applicable.
                // Returning null would mean that the mapping is not applicable
                // at all.
                outputTriple = getPrismContext().deltaFactory().createPrismValueDeltaSetTriple();
            }

        } else {

            // reflect condition change
            if (!conditionResultOld && conditionResultNew) {
                // Condition change false -> true
                outputTriple.addAllToPlusSet(outputTriple.getZeroSet());
                outputTriple.clearZeroSet();
                outputTriple.clearMinusSet();
            }
            if (conditionResultOld && !conditionResultNew) {
                // Condition change true -> false
                outputTriple.addAllToMinusSet(outputTriple.getZeroSet());
                outputTriple.clearZeroSet();
                outputTriple.clearPlusSet();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer#getOutputTriple()
     */
    @Override
    public PrismValueDeltaSetTriple<V> getOutputTriple() {
        if (outputTriple != null && InternalsConfig.consistencyChecks) {
            try {
                outputTriple.checkNoParent();
            } catch (IllegalStateException e) {
                throw new IllegalStateException(e.getMessage() + " in output triple in " + getContextDescription(), e);
            }
        }
        return outputTriple;
    }

    public Item<V, D> getOutput() throws SchemaException {
        if (outputTriple == null) {
            return null;
        }
        //noinspection unchecked
        Item<V, D> output = outputDefinition.instantiate();
        output.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));
        return output;
    }

    public ItemDelta<V, D> createEmptyDelta(ItemPath path) {
        //noinspection unchecked
        return outputDefinition.createEmptyDelta(path);
    }

    private void transitionState(MappingEvaluationState newState) {
        state = newState;
    }

    private void assertState(MappingEvaluationState expectedState) {
        if (state != expectedState) {
            throw new IllegalArgumentException("Expected mapping state " + expectedState + ", but was " + state);
        }
    }

    /**
     * Shallow clone. Only the output is cloned deeply.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public PrismValueDeltaSetTripleProducer<V, D> clone() {
        MappingImpl<V, D> clone = new Builder<V, D>()
                .mappingType(mappingType)
                .contextDescription(contextDescription)
                .expressionFactory(expressionFactory)
                .securityContextManager(securityContextManager)
                .variables(variables)
                .conditionMaskNew(conditionMaskNew)
                .conditionMaskOld(conditionMaskOld)
                .defaultSource(defaultSource)
                .defaultTargetDefinition(defaultTargetDefinition)
                .expressionProfile(expressionProfile)
                .objectResolver(objectResolver)
                .originObject(originObject)
                .originType(originType)
                .sourceContext(sourceContext)
                .sources(sources)
                .targetContext(targetContext)
                .build();

        clone.outputDefinition = outputDefinition;
        clone.outputPath = outputPath;

        if (this.outputTriple != null) {
            clone.outputTriple = this.outputTriple.clone();
        }
        if (this.conditionOutputTriple != null) {
            clone.conditionOutputTriple = this.conditionOutputTriple.clone();
        }
        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (conditionMaskNew ? 1231 : 1237);
        result = prime * result + (conditionMaskOld ? 1231 : 1237);
        result = prime * result + ((conditionOutputTriple == null) ? 0 : conditionOutputTriple.hashCode());
        result = prime * result + ((defaultSource == null) ? 0 : defaultSource.hashCode());
        result = prime * result + ((defaultTargetDefinition == null) ? 0 : defaultTargetDefinition.hashCode());
        result = prime * result + ((expressionProfile == null) ? 0 : expressionProfile.hashCode());
        result = prime * result + ((expressionFactory == null) ? 0 : expressionFactory.hashCode());
        result = prime * result + ((mappingType == null) ? 0 : mappingType.hashCode());
        result = prime * result + ((objectResolver == null) ? 0 : objectResolver.hashCode());
        result = prime * result + ((originObject == null) ? 0 : originObject.hashCode());
        result = prime * result + ((originType == null) ? 0 : originType.hashCode());
        result = prime * result + ((outputDefinition == null) ? 0 : outputDefinition.hashCode());
        result = prime * result + ((outputTriple == null) ? 0 : outputTriple.hashCode());
        result = prime * result + ((contextDescription == null) ? 0 : contextDescription.hashCode());
        result = prime * result + ((sourceContext == null) ? 0 : sourceContext.hashCode());
        result = prime * result + ((sources == null) ? 0 : sources.hashCode());
        result = prime * result + ((targetContext == null) ? 0 : targetContext.hashCode());
        result = prime * result + ((variables == null) ? 0 : variables.hashCode());
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        MappingImpl other = (MappingImpl) obj;
        if (conditionMaskNew != other.conditionMaskNew) { return false; }
        if (conditionMaskOld != other.conditionMaskOld) { return false; }
        if (conditionOutputTriple == null) {
            if (other.conditionOutputTriple != null) { return false; }
        } else if (!conditionOutputTriple.equals(other.conditionOutputTriple)) { return false; }
        if (defaultSource == null) {
            if (other.defaultSource != null) { return false; }
        } else if (!defaultSource.equals(other.defaultSource)) { return false; }
        if (defaultTargetDefinition == null) {
            if (other.defaultTargetDefinition != null) { return false; }
        } else if (!defaultTargetDefinition.equals(other.defaultTargetDefinition)) { return false; }
        if (expressionProfile == null) {
            if (other.expressionProfile != null) { return false; }
        } else if (!expressionProfile.equals(other.expressionProfile)) { return false; }
        if (expressionFactory == null) {
            if (other.expressionFactory != null) { return false; }
        } else if (!expressionFactory.equals(other.expressionFactory)) { return false; }
        if (mappingType == null) {
            if (other.mappingType != null) { return false; }
        } else if (!mappingType.equals(other.mappingType)) { return false; }
        if (objectResolver == null) {
            if (other.objectResolver != null) { return false; }
        } else if (!objectResolver.equals(other.objectResolver)) { return false; }
        if (originObject == null) {
            if (other.originObject != null) { return false; }
        } else if (!originObject.equals(other.originObject)) { return false; }
        if (originType != other.originType) { return false; }
        if (outputDefinition == null) {
            if (other.outputDefinition != null) { return false; }
        } else if (!outputDefinition.equals(other.outputDefinition)) { return false; }
        if (outputTriple == null) {
            if (other.outputTriple != null) { return false; }
        } else if (!outputTriple.equals(other.outputTriple)) { return false; }
        if (contextDescription == null) {
            if (other.contextDescription != null) { return false; }
        } else if (!contextDescription.equals(other.contextDescription)) { return false; }
        if (sourceContext == null) {
            if (other.sourceContext != null) { return false; }
        } else if (!sourceContext.equals(other.sourceContext)) { return false; }
        if (sources == null) {
            if (other.sources != null) { return false; }
        } else if (!sources.equals(other.sources)) { return false; }
        if (targetContext == null) {
            if (other.targetContext != null) { return false; }
        } else if (!targetContext.equals(other.targetContext)) { return false; }
        if (variables == null) {
            if (other.variables != null) { return false; }
        } else if (!variables.equals(other.variables)) { return false; }
        return true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(toString());
        return sb.toString();
    }

    @Override
    public String toString() {
        if (mappingType != null && mappingType.getName() != null) {
            return "M(" + mappingType.getName() + ": " + getMappingDisplayName() + " = " + outputTriple + toStringStrength() + ")";
        } else {
            return "M(" + getMappingDisplayName() + " = " + outputTriple + toStringStrength() + ")";
        }
    }

    private String getMappingDisplayName() {
        if (mappingQName != null) {
            return SchemaDebugUtil.prettyPrint(mappingQName);
        }
        if (outputDefinition == null) {
            return null;
        }
        return SchemaDebugUtil.prettyPrint(outputDefinition.getItemName());
    }

    private String toStringStrength() {
        switch (getStrength()) {
            case NORMAL:
                return "";
            case WEAK:
                return ", weak";
            case STRONG:
                return ", strong";
        }
        return null;
    }

    @Override
    public String getIdentifier() {
        return mappingType != null ? mappingType.getName() : null;
    }

    @Override
    public String toHumanReadableDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("mapping ");
        if (mappingType != null && mappingType.getName() != null) {
            sb.append("'").append(mappingType.getName()).append("'");
        } else {
            sb.append(getMappingDisplayName());
        }
        if (originObject != null) {
            sb.append(" in ");
            sb.append(originObject);
        }
        return sb.toString();
    }

    /**
     * Builder is used to construct a configuration of Mapping object, which - after building - becomes
     * immutable.
     * <p>
     * In order to provide backward-compatibility with existing use of Mapping object, the builder has
     * also traditional setter methods. Both setters and "builder-style" methods MODIFY existing Builder
     * object (i.e. they do not create a new one).
     * <p>
     * TODO decide on which style of setters to keep (setters vs builder-style).
     */
    @SuppressWarnings({ "unused", "BooleanMethodIsAlwaysInverted", "UnusedReturnValue" })
    public static final class Builder<V extends PrismValue, D extends ItemDefinition> {
        private ExpressionFactory expressionFactory;
        private ExpressionVariables variables = new ExpressionVariables();
        private MappingType mappingType;
        private ObjectResolver objectResolver;
        private SecurityContextManager securityContextManager;
        private Source<?, ?> defaultSource;
        private D defaultTargetDefinition;
        private ExpressionProfile expressionProfile;
        private ItemPath defaultTargetPath;
        private Collection<V> originalTargetValues;
        private ObjectDeltaObject<?> sourceContext;
        private PrismObjectDefinition<?> targetContext;
        private Collection<Source<?, ?>> sources = new ArrayList<>();
        private OriginType originType;
        private ObjectType originObject;
        private ValuePolicyResolver valuePolicyResolver;
        private VariableProducer variableProducer;
        private MappingPreExpression mappingPreExpression;
        private boolean conditionMaskOld = true;
        private boolean conditionMaskNew = true;
        private XMLGregorianCalendar now;
        private XMLGregorianCalendar defaultReferenceTime;
        private boolean profiling;
        private String contextDescription;
        private QName mappingQName;
        private RefinedObjectClassDefinition refinedObjectClassDefinition;
        private PrismContext prismContext;

        public Builder<V, D> expressionFactory(ExpressionFactory val) {
            expressionFactory = val;
            return this;
        }

        public Builder<V, D> variables(ExpressionVariables val) {
            variables = val;
            return this;
        }

        public Builder<V, D> mappingType(MappingType val) {
            mappingType = val;
            return this;
        }

        public Builder<V, D> objectResolver(ObjectResolver val) {
            objectResolver = val;
            return this;
        }

        public Builder<V, D> securityContextManager(SecurityContextManager val) {
            securityContextManager = val;
            return this;
        }

        public Builder<V, D> defaultSource(Source<?, ?> val) {
            defaultSource = val;
            return this;
        }

        public Builder<V, D> defaultTargetDefinition(D val) {
            defaultTargetDefinition = val;
            return this;
        }

        public Builder<V, D> expressionProfile(ExpressionProfile val) {
            expressionProfile = val;
            return this;
        }

        public Builder<V, D> defaultTargetPath(ItemPath val) {
            defaultTargetPath = val;
            return this;
        }

        public Builder<V, D> originalTargetValues(Collection<V> values) {
            originalTargetValues = values;
            return this;
        }

        public Builder<V, D> sourceContext(ObjectDeltaObject<?> val) {
            if (val.getDefinition() == null) {
                throw new IllegalArgumentException("Attempt to set mapping source context without a definition");
            }
            sourceContext = val;
            return this;
        }

        public Builder<V, D> targetContext(PrismObjectDefinition<?> val) {
            targetContext = val;
            return this;
        }

        public Builder<V, D> sources(Collection<Source<?, ?>> val) {
            sources = val;
            return this;
        }

        public Builder<V, D> originType(OriginType val) {
            originType = val;
            return this;
        }

        public Builder<V, D> originObject(ObjectType val) {
            originObject = val;
            return this;
        }

        public Builder<V, D> valuePolicyResolver(ValuePolicyResolver val) {
            valuePolicyResolver = val;
            return this;
        }

        public Builder<V, D> variableResolver(VariableProducer<V> variableProducer) {
            this.variableProducer = variableProducer;
            return this;
        }

        public Builder<V, D> mappingPreExpression(MappingPreExpression mappingPreExpression) {
            this.mappingPreExpression = mappingPreExpression;
            return this;
        }

        public Builder<V, D> conditionMaskOld(boolean val) {
            conditionMaskOld = val;
            return this;
        }

        public Builder<V, D> conditionMaskNew(boolean val) {
            conditionMaskNew = val;
            return this;
        }

        public Builder<V, D> now(XMLGregorianCalendar val) {
            now = val;
            return this;
        }

        public Builder<V, D> defaultReferenceTime(XMLGregorianCalendar val) {
            defaultReferenceTime = val;
            return this;
        }

        public Builder<V, D> profiling(boolean val) {
            profiling = val;
            return this;
        }

        public Builder<V, D> contextDescription(String val) {
            contextDescription = val;
            return this;
        }

        public Builder<V, D> mappingQName(QName val) {
            mappingQName = val;
            return this;
        }

        public Builder<V, D> refinedObjectClassDefinition(RefinedObjectClassDefinition val) {
            refinedObjectClassDefinition = val;
            return this;
        }

        public Builder<V, D> prismContext(PrismContext val) {
            prismContext = val;
            return this;
        }

        public MappingImpl<V, D> build() {
            return new MappingImpl<>(this);
        }

        public ExpressionFactory getExpressionFactory() {
            return expressionFactory;
        }

        public ExpressionVariables getVariables() {
            return variables;
        }

        public MappingType getMappingType() {
            return mappingType;
        }

        public ObjectResolver getObjectResolver() {
            return objectResolver;
        }

        public SecurityContextManager getSecurityContextManager() {
            return securityContextManager;
        }

        public Source<?, ?> getDefaultSource() {
            return defaultSource;
        }

        public D getDefaultTargetDefinition() {
            return defaultTargetDefinition;
        }

        public ItemPath getDefaultTargetPath() {
            return defaultTargetPath;
        }

        public Collection<V> getOriginalTargetValues() {
            return originalTargetValues;
        }

        public ObjectDeltaObject<?> getSourceContext() {
            return sourceContext;
        }

        public PrismObjectDefinition<?> getTargetContext() {
            return targetContext;
        }

        public Collection<Source<?, ?>> getSources() {
            return sources;
        }

        public OriginType getOriginType() {
            return originType;
        }

        public ObjectType getOriginObject() {
            return originObject;
        }

        public ValuePolicyResolver getValuePolicyResolver() {
            return valuePolicyResolver;
        }

        public VariableProducer getVariableProducer() {
            return variableProducer;
        }

        public boolean isConditionMaskOld() {
            return conditionMaskOld;
        }

        public boolean isConditionMaskNew() {
            return conditionMaskNew;
        }

        public XMLGregorianCalendar getNow() {
            return now;
        }

        public XMLGregorianCalendar getDefaultReferenceTime() {
            return defaultReferenceTime;
        }

        public boolean isProfiling() {
            return profiling;
        }

        public String getContextDescription() {
            return contextDescription;
        }

        public QName getMappingQName() {
            return mappingQName;
        }

        public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
            return refinedObjectClassDefinition;
        }

        public Builder<V, D> rootNode(ObjectReferenceType objectRef) {
            return addVariableDefinition(null, objectRef);
        }

        public Builder<V, D> rootNode(ObjectDeltaObject<?> odo) {
            return addVariableDefinition(null, odo);
        }

        public <O extends ObjectType> Builder<V, D> rootNode(O objectType, PrismObjectDefinition<O> definition) {
            variables.put(null, objectType, definition);
            return this;
        }

        public <O extends ObjectType> Builder<V, D> rootNode(PrismObject<? extends ObjectType> mpObject, PrismObjectDefinition<O> definition) {
            variables.put(null, mpObject, definition);
            return this;
        }

        public PrismContext getPrismContext() {
            return prismContext;
        }

        public Builder<V, D> addVariableDefinition(ExpressionVariableDefinitionType varDef) throws SchemaException {
            if (varDef.getObjectRef() != null) {
                ObjectReferenceType ref = varDef.getObjectRef();
                ref.setType(getPrismContext().getSchemaRegistry().qualifyTypeName(ref.getType()));
                return addVariableDefinition(varDef.getName().getLocalPart(), ref);
            } else if (varDef.getValue() != null) {
                // This is raw value. We do have definition here. The best we can do is autodetect.
                // Expression evaluation code will do that as a fallback behavior.
                return addVariableDefinition(varDef.getName().getLocalPart(), varDef.getValue(), Object.class);
            } else {
                LOGGER.warn("Empty definition of variable {} in {}, ignoring it", varDef.getName(), getContextDescription());
                return this;
            }
        }

        public Builder<V, D> addVariableDefinition(String name, ObjectReferenceType objectRef) {
            return addVariableDefinition(name, objectRef, objectRef.asReferenceValue().getDefinition());
        }

        public <O extends ObjectType> Builder<V, D> addVariableDefinition(String name, O objectType, Class<O> expectedClass) {
            // Maybe determine definition from schema registry here in case that object is null. We can do that here.
            variables.putObject(name, objectType, expectedClass);
            return this;
        }

        public <O extends ObjectType> Builder<V, D> addVariableDefinition(String name, PrismObject<O> midpointObject, Class<O> expectedClass) {
            // Maybe determine definition from schema registry here in case that object is null. We can do that here.
            variables.putObject(name, midpointObject, expectedClass);
            return this;
        }

        public Builder<V, D> addVariableDefinition(String name, String value) {
            MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                    new QName(SchemaConstants.NS_C, name), PrimitiveType.STRING.getQname());
            return addVariableDefinition(name, value, def);
        }

        public Builder<V, D> addVariableDefinition(String name, boolean value) {
            MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                    new QName(SchemaConstants.NS_C, name), PrimitiveType.BOOLEAN.getQname());
            return addVariableDefinition(name, value, def);
        }

        public Builder<V, D> addVariableDefinition(String name, int value) {
            MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                    new QName(SchemaConstants.NS_C, name), PrimitiveType.INT.getQname());
            return addVariableDefinition(name, value, def);
        }

//        public Builder<V, D> addVariableDefinition(String name, Element value) {
//            return addVariableDefinition(name, (Object)value);
//        }

        public Builder<V, D> addVariableDefinition(String name, PrismValue value) {
            return addVariableDefinition(name, value, value.getParent().getDefinition());
        }

        public Builder<V, D> addVariableDefinition(String name, ObjectDeltaObject<?> value) {
            PrismObjectDefinition<?> definition = value.getDefinition();
            if (definition == null) {
                throw new IllegalArgumentException("Attempt to set variable '" + name + "' as ODO without a definition: " + value);
            }
            return addVariableDefinition(name, value, definition);
        }

        public Builder<V, D> addAliasRegistration(String alias, String mainVariable) {
            variables.registerAlias(alias, mainVariable);
            return this;
        }

        public Builder<V, D> addVariableDefinitions(VariablesMap extraVariables) {
            variables.putAll(extraVariables);
            return this;
        }

        public Builder<V, D> addVariableDefinition(String name, Object value, ItemDefinition definition) {
            variables.put(name, value, definition);
            return this;
        }

        public Builder<V, D> addVariableDefinition(String name, Object value, Class<?> typeClass) {
            variables.put(name, value, typeClass);
            return this;
        }

        public boolean hasVariableDefinition(String varName) {
            return variables.containsKey(varName);
        }

        public boolean isApplicableToChannel(String channel) {
            return MappingImpl.isApplicableToChannel(mappingType, channel);
        }

        public Builder<V, D> addSource(Source<?, ?> source) {
            sources.add(source);
            return this;
        }

        // traditional setters are also here, to avoid massive changes to existing code

        @Deprecated
        public void setExpressionFactory(ExpressionFactory expressionFactory) {
            this.expressionFactory = expressionFactory;
        }

        @Deprecated
        public void setVariables(ExpressionVariables variables) {
            this.variables = variables;
        }

        @Deprecated
        public void setMappingType(MappingType mappingType) {
            this.mappingType = mappingType;
        }

        @Deprecated
        public void setObjectResolver(ObjectResolver objectResolver) {
            this.objectResolver = objectResolver;
        }

        @Deprecated
        public void setDefaultSource(Source<?, ?> defaultSource) {
            this.defaultSource = defaultSource;
        }

        @Deprecated
        public void setDefaultTargetDefinition(D defaultTargetDefinition) {
            this.defaultTargetDefinition = defaultTargetDefinition;
        }

        @Deprecated
        public void setDefaultTargetPath(ItemPath defaultTargetPath) {
            this.defaultTargetPath = defaultTargetPath;
        }

        @Deprecated
        public void setSourceContext(ObjectDeltaObject<?> sourceContext) {
            this.sourceContext = sourceContext;
        }

        @Deprecated
        public void setTargetContext(PrismObjectDefinition<?> targetContext) {
            this.targetContext = targetContext;
        }

        @Deprecated
        public void setSources(Collection<Source<?, ?>> sources) {
            this.sources = sources;
        }

        @Deprecated
        public void setOriginType(OriginType originType) {
            this.originType = originType;
        }

        @Deprecated
        public void setOriginObject(ObjectType originObject) {
            this.originObject = originObject;
        }

        @Deprecated
        public void setStringPolicyResolver(
                ValuePolicyResolver stringPolicyResolver) {
            this.valuePolicyResolver = stringPolicyResolver;
        }

        @Deprecated
        public void setConditionMaskOld(boolean conditionMaskOld) {
            this.conditionMaskOld = conditionMaskOld;
        }

        @Deprecated
        public void setConditionMaskNew(boolean conditionMaskNew) {
            this.conditionMaskNew = conditionMaskNew;
        }

        @Deprecated
        public void setNow(XMLGregorianCalendar now) {
            this.now = now;
        }

        @Deprecated
        public void setDefaultReferenceTime(XMLGregorianCalendar defaultReferenceTime) {
            this.defaultReferenceTime = defaultReferenceTime;
        }

        @Deprecated
        public void setProfiling(boolean profiling) {
            this.profiling = profiling;
        }

        @Deprecated
        public void setContextDescription(String contextDescription) {
            this.contextDescription = contextDescription;
        }

        @Deprecated
        public void setMappingQName(QName mappingQName) {
            this.mappingQName = mappingQName;
        }

        @Deprecated
        public void setRefinedObjectClassDefinition(
                RefinedObjectClassDefinition refinedObjectClassDefinition) {
            this.refinedObjectClassDefinition = refinedObjectClassDefinition;
        }

        @Deprecated
        public void setPrismContext(PrismContext prismContext) {
            this.prismContext = prismContext;
        }

        public MappingStrengthType getStrength() {
            return MappingImpl.getStrength(mappingType);
        }
    }
}
