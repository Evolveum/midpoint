/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import static com.evolveum.midpoint.schema.util.ProvenanceMetadataUtil.hasMappingSpecification;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastMinimal;

import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastNormal;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import static com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy.REAL_VALUE;

import java.io.Serializable;
import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.AbstractItemDeltaItem;
import com.evolveum.midpoint.schema.config.AbstractMappingConfigItem;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Evaluation of a mapping. It is non-recyclable single-use object. Once evaluated it should not be evaluated again.
 * It will retain its original inputs and outputs that can be read again and again. But these should not be
 * changed after evaluation.
 *
 * TODO document evaluation of time constraints ...
 *
 * Configuration properties are unmodifiable. They are to be set via Mapping.Builder.
 *
 * Serializability:
 *
 * The mapping is technically serializable. However, it is NOT expected to be evaluable after deserialization.
 * Only already computed results are to be fetched from such mapping object.
 *
 * @param <V> type of mapping output value
 * @param <D> type of mapping output value definition (property, container, ...)
 * @param <MBT> mapping bean type: MappingType or MetadataMappingType
 * @author Radovan Semancik
 */
public abstract class AbstractMappingImpl<V extends PrismValue, D extends ItemDefinition<?>, MBT extends AbstractMappingType>
        implements Mapping<V, D>, DebugDumpable, PrismValueDeltaSetTripleProducer<V, D> {

    static final Trace LOGGER = TraceManager.getTrace(AbstractMappingImpl.class);

    // TODO rename these op results when the right time comes
    private static final String OP_EVALUATE_PREPARED = MappingImpl.class.getName() + ".evaluatePrepared";
    private static final String OP_EVALUATE = MappingImpl.class.getName() + ".evaluate";
    private static final String OP_EVALUATE_TIME_VALIDITY = MappingImpl.class.getName() + ".evaluateTimeValidity";
    private static final String OP_PREPARE = MappingImpl.class.getName() + ".prepare";

    //region Configuration properties (almost unmodifiable)

    /** "Rich" definition of the mapping. */
    @NotNull final AbstractMappingConfigItem<MBT> mappingConfigItem;

    /** "Pure" definition of the mapping. Just for convenience. Derived from {@link #mappingConfigItem}. */
    @NotNull final MBT mappingBean;

    /** Classification of the mapping (for reporting and diagnostic purposes). */
    @Experimental
    private final MappingKindType mappingKind;

    /**
     * (Context) variables to be used during mapping evaluation.
     *
     * Transient. No evaluation will be possible after deserialization.
     */
    transient final VariablesMap variables;

    /**
     * The object-delta-object or item-delta-item for the default source context object/item.
     *
     * Default source context is the object/item that is used for resolution of paths that have no variable specification.
     * For example, traditional outbound and object template mappings have a default context of `$focus`.
     *
     * NOTE: This is different from {@link #defaultSource}. That one is the specific source (sub)item in the source context.
     */
    private final AbstractItemDeltaItem<?> defaultSourceContextIdi;

    /**
     * Typified version of {@link #defaultSourceContextIdi}. Lazily evaluated.
     */
    transient private TypedValue<AbstractItemDeltaItem<?>> typedDefaultSourceContextIdi;

    /**
     * One of the sources can be denoted as default.
     * See {@link ExpressionEvaluationContext#defaultSource}.
     *
     * Examples: attribute value for inbound mappings, "legal" information for existence mappings, etc.
     *
     * NOTE: Contrary to the use of defaultSource in expression evaluation context (where the default source
     * is always one of the sources), here the default source is an ADDITIONAL one, related to the other sources.
     * (If an explicit source of the same name is defined in the mapping, it overrides the default source.)
     */
    final Source<?, ?> defaultSource;

    /**
     * Information about the implicit source for a mapping. It is provided here for reporting and diagnostic purposes only.
     * An example: attributes/ri:name for inbound mapping for that attribute.
     */
    @Experimental
    private final ItemPath implicitSourcePath;

    /**
     * Definition of the target object or item. Used for resolution of paths that have no variable specification.
     *
     * @see #defaultSourceContextIdi
     */
    final PrismContainerDefinition<?> targetContextDefinition;

    /**
     * Information about the implicit target for a mapping. It is provided here for reporting and diagnostic purposes only.
     * An example: `$shadow/activation` for activation mapping.
     * Useful when {@link #defaultTargetPath} is not specified.
     */
    private final ItemPath implicitTargetPath;

    /**
     * Overrides the target path from the mapping bean. Used e.g. for associated objects mappings;
     * e.g. the bean says `targetRef` but the actual path will be `assignment/[123]/targetRef`.
     */
    @Nullable final ItemPath targetPathOverride;

    /**
     * Redirects the target to another item, for example `identities/identity[x]/personalNumber` instead
     * of `extension/personalNumber` when multi-source properties are enabled.
     *
     * This redirection takes place when the mapping is executed. So, e.g., the definition is still taken from the original path.
     */
    @Nullable final ItemPath targetPathExecutionOverride;

    /**
     * Default target path if "target" or "target/path" in the mapping bean is missing.
     * Used e.g. for outbound mappings.
     */
    final ItemPath defaultTargetPath;

    /**
     * Value for {@link #getOutputDefinition()} to be used when there's no target path specified.
     * (For some cases it perhaps could be derived using {@link #defaultTargetPath} but we currently
     * do not use this option.)
     */
    final D defaultTargetDefinition;

    /**
     * Original values of the mapping target. Currently used for range checking.
     */
    private final Collection<V> originalTargetValues;

    /**
     * Expression profile for tests, where archetype manager is not available, so they must be set explicitly.
     * NEVER use in production.
     */
    @VisibleForTesting
    @Nullable private final ExpressionProfile explicitExpressionProfile;

    /**
     * Expression profile to be used when evaluating various expressions (condition,
     * "main" expression, value set expressions, etc). Initialized right at the start of the evaluation.
     */
    @NotNull private final FreezableReference<ExpressionProfile> expressionProfileReference;

    /**
     * Information on the kind of mapping. (Partially overlaps with {@link #mappingKind}.)
     * It is put into output triples as an origin metadata. Deprecated. Most probably will
     * be replaced by provenance metadata.
     */
    private final OriginType originType;

    /**
     * Information on the object where the mapping is defined (e.g. role, resource, and so on).
     * Used for diagnostic and reporting purposes.
     *
     * Probably will be replaced by the origin in {@link #mappingConfigItem}, although - for resources - this currently points
     * to the actual (expanded) resource, not necessarily to the super-resource, where the mapping may be defined.
     */
    private final ObjectType originObject;

    /**
     * Provider of the value policy (used for "generate" expressions).
     * See {@link ExpressionEvaluationContext#valuePolicySupplier}.
     */
    transient final ConfigurableValuePolicySupplier valuePolicySupplier;

    /**
     * Mapping pre-expression is invoked just before main mapping expression.
     * Pre expression will get the same expression context as the main expression.
     * This is an opportunity to manipulate the context just before evaluation.
     * Or maybe evaluate additional expressions that set up environment for
     * main expression.
     */
    private final MappingPreExpression mappingPreExpression;

    /**
     * Additional clause for condition evaluation. If set to "false" then condition for old state
     * is considered to be false. Used to skip evaluation for old state if we know there's nothing
     * reasonable to be evaluated: e.g. when evaluating constructions for users being added (no "old"
     * state there).
     */
    private final boolean conditionMaskOld;

    /**
     * Additional clause for condition evaluation. If set to "false" then condition for new state
     * is considered to be false. Used to skip evaluation for new state if we know there's nothing
     * reasonable to be evaluated: e.g. when evaluating constructions for users being deleted (no "new"
     * state there).
     */
    private final boolean conditionMaskNew;

    /**
     * "System time" to be used when evaluating this mapping.
     */
    final XMLGregorianCalendar now;

    /**
     * Whether to record and display evaluation times.
     * Usually not used in production.
     */
    private final boolean profiling;

    /**
     * Free-form description of the context in which the mapping is evaluated.
     */
    private final String contextDescription;

    /**
     * Producer of extra variables. It is not directly used in mapping evaluation:
     * it is propagated to {@link ExpressionEvaluationContext#variableProducer}.
     *
     * Transient. No evaluation will be possible after deserialization.
     */
    transient private final VariableProducer variableProducer;

    /**
     * This is sometimes used to identify the element that mapping produces
     * if it is different from the one returned by {@link #getItemName()}.
     * Originally, this happened with associations.
     *
     * TODO reconsider if it's still necessary
     */
    private final QName targetItemName;

    /**
     * Mapping specification: name, containing object OID, object type (for resource), and assignment path.
     *
     * Provided either explicitly via builder or created by {@link #createDefaultSpecification()};
     * MUST be provided explicitly for inbound mappings (we need object type information there).
     */
    @NotNull private final MappingSpecificationType mappingSpecification;

    /**
     * Definition of ValueMetadataType.
     */
    @NotNull final PrismContainerDefinition<ValueMetadataType> valueMetadataDefinition;
    //endregion

    //region Working and output properties

    /**
     * Parses sources and targets. Holds partial results of this process.
     */
    final MappingParser<D, MBT> parser;

    /**
     * Sources for condition and expression evaluation.
     * These are created during mapping parsing.
     * (In rare cases, extra pre-parsed sources can be provided using builder.)
     */
    @NotNull final List<Source<?, ?>> sources = new ArrayList<>();

    /**
     * State of the mapping evaluation.
     */
    private MappingEvaluationState state = MappingEvaluationState.UNINITIALIZED;

    /**
     * Evaluated mapping expression. Once evaluated it is not used any more.
     */
    transient private Expression<V, D> expression;

    /**
     * Result of the mapping evaluation: values that will be added, deleted and kept in the target item.
     * (This is relative to the whole mapping and/or its assignment being added, deleted, or kept.)
     */
    private PrismValueDeltaSetTriple<V> outputTriple;

    /**
     * Whether we were requested to "push" (phantom) changes: source items that have a delta but their
     * real value has not changed.
     *
     * TODO move to "configuration" options and provide via builder.
     * (Current way of determining via lens context is more a hack than real solution.)
     */
    @Experimental
    private boolean pushChangesRequested;

    /**
     * Whether the conditions for pushing the changes at output were fulfilled,
     * so we instruct the consolidator (or analogous component) to do that.
     */
    @Experimental
    private boolean pushChanges;

    /**
     * Result of the condition evaluation in old vs. new state.
     */
    private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionOutputTriple;

    /**
     * Scalar result of the condition evaluation for "old" state. Non-null after evaluation.
     */
    private Boolean conditionResultOld;

    /**
     * Scalar result of the condition evaluation for "new" state. Non-null after evaluation.
     */
    private Boolean conditionResultNew;

    /**
     * Evaluation of time constraints.
     */
    private MappingTimeConstraintsEvaluation timeConstraintsEvaluation;

    /**
     * When the mapping evaluation started. Used only if profiling is turned on.
     */
    private Long evaluationStartTime;

    /**
     * When the mapping evaluation ended. Used only if profiling is turned on.
     */
    private Long evaluationEndTime;

    /**
     * Parent context description with added information about this mapping.
     * Lazily evaluated.
     */
    private String mappingContextDescription;

    /**
     * Trace for mapping evaluation, attached to the operation result.
     */
    private MappingEvaluationTraceType trace;

    /** The level of tracing requested for mapping evaluation. */
    private TracingLevelType tracingLevel;

    /**
     * Mapping state properties that are exposed to the expressions. They can be used by the expressions to "communicate".
     * E.g. one expression setting the property and other expression checking the property.
     */
    private Map<String, Serializable> stateProperties;

    /**
     * Task stored during the evaluation, removed afterwards.
     */
    transient Task task;

    /**
     * Value metadata computer to be used when expression is evaluated.
     */
    transient private TransformationValueMetadataComputer valueMetadataComputer;

    private List<MappingSpecificationType> mappingAliasSpecifications;
    //endregion

    //region Constructors and (relatively) simple getters
    protected AbstractMappingImpl(AbstractMappingBuilder<V, D, MBT, ?> builder) {
        variables = builder.getVariables();
        mappingConfigItem = Objects.requireNonNull(builder.getMappingConfigItem(), "Mapping definition cannot be null");
        mappingBean = mappingConfigItem.value();
        mappingKind = builder.getMappingKind();
        implicitSourcePath = builder.getImplicitSourcePath();
        implicitTargetPath = builder.getImplicitTargetPath();
        targetPathOverride = builder.getTargetPathOverride();
        targetPathExecutionOverride = builder.getTargetPathExecutionOverride();
        defaultSource = builder.defaultSource;
        defaultTargetDefinition = builder.getDefaultTargetDefinition();
        explicitExpressionProfile = builder.getExplicitExpressionProfile();
        expressionProfileReference = new FreezableReference<>();
        defaultTargetPath = builder.getDefaultTargetPath();
        originalTargetValues = builder.getOriginalTargetValues();
        defaultSourceContextIdi = builder.defaultSourceContextIdi;
        targetContextDefinition = builder.targetContextDefinition;
        originType = builder.getOriginType();
        originObject = builder.getOriginObject();
        valuePolicySupplier = builder.getValuePolicySupplier();
        variableProducer = builder.getVariableProducer();
        mappingPreExpression = builder.getMappingPreExpression();
        conditionMaskOld = builder.isConditionMaskOld();
        conditionMaskNew = builder.isConditionMaskNew();
        profiling = builder.isProfiling();
        contextDescription = builder.getContextDescription();
        targetItemName = builder.targetItemName;
        if (builder.getMappingSpecification() != null) {
            mappingSpecification = builder.getMappingSpecification();
        } else {
            stateCheck(mappingKind != MappingKindType.INBOUND,
                    "Inbound mappings must have an explicit mapping specification; in %s", originObject);
            mappingSpecification = createDefaultSpecification();
        }
        mappingAliasSpecifications = createMappingAliasSpecifications(mappingSpecification,mappingBean.getMappingAlias());
        now = builder.getNow();
        sources.addAll(builder.getAdditionalSources());
        parser = new MappingParser<>(this);
        valueMetadataDefinition = SchemaRegistry.get().findContainerDefinitionByCompileTimeClass(ValueMetadataType.class);
    }

    private MappingSpecificationType createDefaultSpecification() {
        MappingSpecificationType specification = new MappingSpecificationType();
        specification.setMappingName(mappingBean.getName());
        specification.setDefinitionObjectRef(ObjectTypeUtil.createObjectRef(originObject));
        return specification;
    }

    @SuppressWarnings("CopyConstructorMissesField") // TODO what about the other fields
    protected AbstractMappingImpl(AbstractMappingImpl<V, D, MBT> prototype) {
        this.mappingConfigItem = prototype.mappingConfigItem;
        this.mappingBean = prototype.mappingBean;
        this.mappingKind = prototype.mappingKind;
        this.implicitSourcePath = prototype.implicitSourcePath;
        this.implicitTargetPath = prototype.implicitTargetPath;
        this.targetPathOverride = prototype.targetPathOverride;
        this.targetPathExecutionOverride = prototype.targetPathExecutionOverride;
        this.sources.addAll(prototype.sources);
        this.variables = prototype.variables;

        this.defaultSourceContextIdi = prototype.defaultSourceContextIdi;
        // typedSourceContext as well?
        this.defaultSource = prototype.defaultSource;

        this.targetContextDefinition = prototype.targetContextDefinition;
        this.defaultTargetPath = prototype.defaultTargetPath;
        this.defaultTargetDefinition = prototype.defaultTargetDefinition;
        this.originalTargetValues = prototype.originalTargetValues;
        this.explicitExpressionProfile = prototype.explicitExpressionProfile;
        this.expressionProfileReference = prototype.expressionProfileReference;

        this.originType = prototype.originType;
        this.originObject = prototype.originObject;
        this.valuePolicySupplier = prototype.valuePolicySupplier;
        this.mappingPreExpression = prototype.mappingPreExpression;
        this.conditionMaskOld = prototype.conditionMaskOld;
        this.conditionMaskNew = prototype.conditionMaskNew;

        this.targetItemName = prototype.targetItemName;
        this.mappingSpecification = prototype.mappingSpecification;

        this.now = prototype.now;
        this.profiling = prototype.profiling;
        this.variableProducer = prototype.variableProducer;

        this.contextDescription = prototype.contextDescription;

        if (prototype.outputTriple != null) {
            this.outputTriple = prototype.outputTriple.clone();
        }
        if (prototype.conditionOutputTriple != null) {
            this.conditionOutputTriple = prototype.conditionOutputTriple.clone();
        }
        this.parser = prototype.parser;
        this.valueMetadataDefinition = prototype.valueMetadataDefinition;
    }

    public ObjectResolver getObjectResolver() {
        return ModelCommonBeans.get().objectResolver;
    }

    public QName getItemName() {
        D outputDefinition = getOutputDefinition();
        return outputDefinition != null ? outputDefinition.getItemName() : null;
    }

    public ObjectType getOriginObject() {
        return originObject;
    }

    public Source<?, ?> getDefaultSource() {
        return defaultSource;
    }

    @TestOnly
    AbstractItemDeltaItem<?> getDefaultSourceContextIdi() {
        return defaultSourceContextIdi;
    }

    public String getContextDescription() {
        return contextDescription;
    }

    TypedValue<AbstractItemDeltaItem<?>> getTypedDefaultSourceContextIdi() {
        if (defaultSourceContextIdi == null) {
            return null;
        }
        if (typedDefaultSourceContextIdi == null) {
            typedDefaultSourceContextIdi = new TypedValue<>(defaultSourceContextIdi, defaultSourceContextIdi.getDefinition());
        }
        return typedDefaultSourceContextIdi;
    }

    public String getMappingContextDescription() {
        if (mappingContextDescription == null) {
            StringBuilder sb = new StringBuilder("mapping ");
            if (mappingBean.getName() != null) {
                sb.append("'").append(mappingBean.getName()).append("' ");
            }
            sb.append("in ");
            sb.append(contextDescription);
            mappingContextDescription = sb.toString();
        }
        return mappingContextDescription;
    }

    @NotNull
    public MBT getMappingBean() {
        return mappingBean;
    }

    /** Should be called on prepared mapping. */
    public @NotNull ExpressionProfile getExpressionProfile() {
        return stateNonNull(
                expressionProfileReference.getValue(),
                "no expression profile; state = %s", state);
    }

    @Override
    public boolean isSourceless() {
        return sources.isEmpty();
    }

    @Override
    public @NotNull MappingStrengthType getStrength() {
        return getStrength(mappingBean);
    }

    public static MappingStrengthType getStrength(AbstractMappingType mappingBean) {
        if (mappingBean != null && mappingBean.getStrength() != null) {
            return mappingBean.getStrength();
        } else {
            return MappingStrengthType.NORMAL;
        }
    }

    @Override
    public boolean isAuthoritative() {
        return isNotFalse(mappingBean.isAuthoritative());
    }

    @Override
    public boolean isExclusive() {
        return isTrue(mappingBean.isExclusive());
    }

    public boolean hasTargetRange() {
        return mappingBean.getTarget().getSet() != null;
    }

    public static boolean isApplicableToChannel(AbstractMappingType mappingBean, String channelUri) {
        List<String> exceptChannel = mappingBean.getExceptChannel();
        if (!exceptChannel.isEmpty()) {
            return !exceptChannel.contains(channelUri);
        }
        List<String> applicableChannels = mappingBean.getChannel();
        return applicableChannels == null
                || applicableChannels.isEmpty()
                || applicableChannels.contains(channelUri);
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }

    public XMLGregorianCalendar getNextRecomputeTime() {
        return isEnabled() ?
                timeConstraintsEvaluation.getNextRecomputeTime() : null;
    }

    public boolean isTimeConstraintValid() {
        return isEnabled() && timeConstraintsEvaluation.isTimeConstraintValid();
    }

    public boolean isProfiling() {
        return profiling;
    }

    public boolean isEnabled() {
        return mappingConfigItem.isEnabled();
    }

    public Long getEtime() {
        if (evaluationStartTime == null || evaluationEndTime == null) {
            return null;
        }
        return evaluationEndTime - evaluationStartTime;
    }

    @Override
    public QName getTargetItemName() {
        return targetItemName;
    }

    @Override
    public <T extends Serializable> T getStateProperty(String propertyName) {
        //noinspection unchecked
        return stateProperties != null ? (T) stateProperties.get(propertyName) : null;
    }

    @Override
    public <T extends Serializable> T setStateProperty(String propertyName, T value) {
        if (stateProperties == null) {
            stateProperties = new HashMap<>();
        }
        //noinspection unchecked
        return (T) stateProperties.put(propertyName, value);
    }
    //endregion

    //region Evaluation

    /**
     * Evaluate the mapping. Can be called in UNINITIALIZED or PREPARED states only.
     */
    public void evaluate(Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        this.task = task;
        OperationResult result = createOpResultAndRecordStart(OP_EVALUATE, task, parentResult);
        try {
            assertUninitializedOrPrepared();
            prepare(result);
            evaluatePrepared(result);
            recordReturns(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            this.task = null;
        }
    }

    /**
     * Evaluate the time validity. Can be called in UNINITIALIZED or PREPARED states only.
     */
    public void evaluateTimeValidity(Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        this.task = task;
        OperationResult result = createOpResultAndRecordStart(OP_EVALUATE_TIME_VALIDITY, task, parentResult);
        try {
            assertUninitializedOrPrepared();
            prepare(result);
            recordSources();
            evaluateTimeConstraint(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            this.task = null;
        }
    }

    @NotNull
    private OperationResult createOpResultAndRecordStart(String opName, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(opName)
                .addArbitraryObjectAsContext("mapping", this)
                .addArbitraryObjectAsContext("context", getContextDescription())
                .addArbitraryObjectAsContext("task", task)
                .setMinor()
                .build();
        tracingLevel = result.getTracingLevel(MappingEvaluationTraceType.class);
        if (isAtLeastMinimal(tracingLevel)) {
            trace = new MappingEvaluationTraceType()
                    .mapping(mappingBean.clone())
                    .mappingKind(mappingKind)
                    .implicitSourcePath(implicitSourcePath != null ? new ItemPathType(implicitSourcePath) : null)
                    .implicitTargetPath(implicitTargetPath != null ? new ItemPathType(implicitTargetPath) : null)
                    .targetPathOverride(targetPathExecutionOverride != null ? new ItemPathType(targetPathExecutionOverride) : null)
                    .containingObjectRef(ObjectTypeUtil.createObjectRef(originObject));
            result.addTrace(trace);
        } else {
            trace = null;
        }
        return result;
    }

    private void recordReturns(OperationResult result) {
        result.addReturn("condition", conditionResultOld + " -> " + conditionResultNew);
        result.addReturn("outputTriple", getOutputTripleDiagRepresentation());
    }

    private String getOutputTripleDiagRepresentation() {
        if (outputTriple != null) {
            return "plus: " + outputTriple.getPlusSet().size() +
                    ", minus: " + outputTriple.getMinusSet().size() +
                    ", zero: " + outputTriple.getZeroSet().size();
        } else {
            return "null";
        }
    }

    private void assertUninitializedOrPrepared() {
        if (state != MappingEvaluationState.UNINITIALIZED && state != MappingEvaluationState.PREPARED) {
            throw new IllegalArgumentException("Expected mapping state UNINITIALIZED or PREPARED, but was " + state);
        }
    }

    /**
     * Prepare mapping for evaluation. Parse the values. After this call it can be checked if a mapping is
     * activated (i.e. if the input changes will "trigger" the mapping).
     */
    public void prepare(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        if (state == MappingEvaluationState.PREPARED) {
            return;
        }

        OperationResult result = parentResult.subresult(OP_PREPARE)
                .addArbitraryObjectAsContext("mapping", this)
                .addArbitraryObjectAsContext("task", task)
                .setMinor()
                .build();
        assertState(MappingEvaluationState.UNINITIALIZED);
        try {

            determineExpressionProfile(result);

            parser.parseSourcesAndTarget(result);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }

        transitionState(MappingEvaluationState.PREPARED);
        result.recordSuccess();
    }

    /** Determines and sets the expression profile in {@link #expressionProfileReference}. Callable only once. */
    private void determineExpressionProfile(OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        @NotNull ExpressionProfile profile;
        if (explicitExpressionProfile != null) {
            profile = explicitExpressionProfile;
        } else {
            profile = ModelCommonBeans.get().expressionProfileManager.determineExpressionProfileStrict(
                    mappingConfigItem.origin(), getTask(), result);
        }
        expressionProfileReference.setValue(profile);
        expressionProfileReference.freeze();
    }

    public boolean isActivated() {
        return sourcesChanged();
    }

    private void evaluatePrepared(OperationResult parentResult) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            CommunicationException {

        assertState(MappingEvaluationState.PREPARED);

        OperationResult result = parentResult.subresult(OP_EVALUATE_PREPARED)
                .addArbitraryObjectAsContext("mapping", this)
                .addArbitraryObjectAsContext("task", task)
                .setMinor()
                .build();

        noteEvaluationStart();

        try {
            recordSources();

            // We may need to re-parse the sources here

            evaluateTimeConstraint(result);

            // We have to evaluate condition even for mappings that are not time-valid. This is because we want
            // to skip trigger creation for mappings that do not satisfy the condition (see MID-6040).
            evaluateCondition(result);

            if (isTimeConstraintValid()) {
                setupValueMetadataComputer(result);

                if (isConditionSatisfied()) {
                    evaluateExpression(result);
                    applyDefinitionToOutputTriple();
                    recomputeValues();
                    setOrigin();
                    adjustForAuthoritative();
                } else {
                    outputTriple = null;
                }
                checkExistingTargetValues(result); // we check the range even for not-applicable mappings (MID-5953)
                transitionState(MappingEvaluationState.EVALUATED);

                result.recordSuccess();
                traceSuccess();
            } else {
                outputTriple = null;
                result.recordNotApplicableIfUnknown();
                traceDeferred();
            }
            recordOutput();
        } catch (Throwable e) {
            result.recordFatalError(e);
            traceFailure(e);
            throw e;
        } finally {
            noteEvaluationEnd();
        }
    }

    private void recordTimeConstraintValidity() {
        if (trace != null) {
            trace.setNextRecomputeTime(getNextRecomputeTime());
            trace.setTimeConstraintValid(isTimeConstraintValid());
        }
    }

    private void recordSources() throws SchemaException {
        if (trace != null && isAtLeastNormal(tracingLevel)) {
            for (Source<?, ?> source : sources) {
                trace.beginSource()
                        .name(source.getName())
                        .itemDeltaItem(source.toItemDeltaItemType());
            }
        }
    }

    private void recordOutput() {
        if (trace != null) {
            trace.setConditionResultOld(conditionResultOld);
            trace.setConditionResultNew(conditionResultNew);
            trace.setPushChangesRequested(pushChangesRequested);
            trace.setPushChanges(pushChanges);
            if (isAtLeastNormal(tracingLevel)) {
                if (outputTriple != null) {
                    trace.setOutput(DeltaSetTripleType.fromDeltaSetTriple(outputTriple));
                }
                trace.setStateProperties(createStatePropertiesTrace());
            }
        }
    }

    private @NotNull MappingStatePropertiesType createStatePropertiesTrace() {
        MappingStatePropertiesType properties = new MappingStatePropertiesType();
        if (stateProperties != null) {
            stateProperties.forEach((name, value) ->
                    properties.beginProperty()
                            .name(name)
                            .value(value != null ? value.toString() : null));
        }
        return properties;
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

    /**
     * Checks existing values. There are more reasons to do this:
     * <ol>
     *   <li>Range checking. We want to delete values that are in the range of this mapping but were not produced by it.</li>
     *   <li>Provenance handling.</li>
     * </ol>
     *
     * <p/>
     * The provenance handling is a bit more complex. It has three parts:
     * <ol>
     *    <li>For all negative values, we have to restrict deletion to our yield (only). This functionality can
     *        be provided by us or by the consolidator. Let us do it ourselves.</li>
     *    <li>For all non-negative values, We have to delete our own yield, except for cases where the metadata for
     *        the yield remained the same. This is to avoid accumulation of obsolete yields (although the
     *        consolidation could presumably treat this somehow). See {@link #deleteOwnYieldFromNonNegativeValues()} method.</li>
     *    <li>If the range is specified as "matchingProvenance", we remove our yield from all existing values.</li>
     * </ol>
     */
    private void checkExistingTargetValues(OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (valueMetadataComputer != null && valueMetadataComputer.supportsProvenance()) {
            restrictNegativeValuesToOwnYield();
            // Must come after the above method because this method creates some values in minus set.
            // We don't want these newly-added minus values to be processed by the above method.
            deleteOwnYieldFromNonNegativeValues();
        }

        VariableBindingDefinitionType target = mappingBean.getTarget();
        ValueSetDefinitionType explicitRangeSetDefBean = target != null ? target.getSet() : null;
        ValueSetDefinitionType effectiveRangeSetDefBean;
        // As of 4.9: Multivalues have by default provenance set mapping.
        if (explicitRangeSetDefBean == null && shouldUseMatchingProvenance()) {
            effectiveRangeSetDefBean = new ValueSetDefinitionType().predefined(ValueSetDefinitionPredefinedType.MATCHING_PROVENANCE);
        } else {
            effectiveRangeSetDefBean = explicitRangeSetDefBean;
        }
        if (effectiveRangeSetDefBean == null) {
            return;
        }
        String name;
        if (getOutputPath() != null) {
            name = getOutputPath().lastName().getLocalPart();
        } else {
            name = getOutputDefinition().getItemName().getLocalPart();
        }

        if (originalTargetValues == null) {
            if (explicitRangeSetDefBean != null) {
                throw new IllegalStateException(
                        "Couldn't check range for mapping in " + contextDescription + ", as original target values are not known.");
            } else {
                // This is mainly to cover corner cases like those in low-level tests
                // (TestMappingDomain, TestMappingDynamicSimple)
                return;
            }
        }

        ValueSetDefinition<V, D> rangeSetDef = new ValueSetDefinition<>(
                effectiveRangeSetDefBean,
                ValueSetDefinition.ExtraSetSpecification.fromBean(target),
                getOutputDefinition(),
                valueMetadataDefinition,
                getExpressionProfile(),
                ModelCommonBeans.get().expressionFactory,
                name,
                mappingSpecification,
                mappingAliasSpecifications,
                "range",
                "range of " + name + " in " + getMappingContextDescription(),
                task, result);
        rangeSetDef.init();
        rangeSetDef.setAdditionalVariables(variables);
        for (V originalValue : originalTargetValues) {
            // FIXME: Migrate legacy to new?

            if (rangeSetDef.contains(originalValue)) {
                addToMinusIfNecessary(originalValue, rangeSetDef, result);
            }
        }
    }

    /**
     * Special treatment of values present in both original item and zero/plus sets: values that were previously computed by this
     * mapping (and are computed also now) are removed.
     */
    private void deleteOwnYieldFromNonNegativeValues() throws SchemaException {
        if (outputTriple == null || originalTargetValues == null) {
            return;
        }

        for (V nonNegativeValue : outputTriple.getNonNegativeValues()) {
            if (nonNegativeValue != null) {
                List<V> matchingOriginalValues = originalTargetValues.stream()
                        .filter(originalValue ->
                                nonNegativeValue.equals(originalValue, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS))
                        .toList();
                for (V matchingOriginalValue : matchingOriginalValues) {

                    // Looking for metadata with the same mapping spec but not present in "new" value.
                    // What about the equivalence strategy?
                    //
                    // - One option is DATA: This will remove all values that are not exactly the same as the newly
                    //   computed one. It will provide "clean slate" to reflect even some minor non-real-value affecting changes.
                    //   The cost is that phantom adds could be generated.
                    //
                    // - Another option is REAL_VALUE. This is currently a little bit complicated because some of the metadata
                    //   that are important is now marked as operational. But we will gradually fix that.
                    List<PrismContainerValue<ValueMetadataType>> matchingMetadata =
                            matchingOriginalValue.<ValueMetadataType>getValueMetadataAsContainer().getValues().stream()
                                    .filter(md -> hasMappingSpecification(md.asContainerable(), mappingSpecification)
                                                    && !nonNegativeValue.<ValueMetadataType>getValueMetadataAsContainer().contains(md, REAL_VALUE))
                                    .collect(Collectors.toList());

                    if (!matchingMetadata.isEmpty()) {
                        PrismValue valueToDelete = matchingOriginalValue.clone();
                        valueToDelete.getValueMetadataAsContainer().clear();
                        valueToDelete.<ValueMetadataType>getValueMetadataAsContainer().addAll(CloneUtil.cloneCollectionMembers(matchingMetadata));
                        LOGGER.trace("Found an existing value with the metadata indicating it was created by this mapping: putting into minus set:\n{}\nMetadata:\n{}",
                                DebugUtil.debugDumpLazily(valueToDelete, 1),
                                DebugUtil.debugDumpLazily(valueToDelete.getValueMetadata(), 1));
                        //noinspection unchecked
                        outputTriple.addToMinusSet((V) valueToDelete);
                    }
                }
            }
        }
    }

    /**
     * For negative (minus) values we want to delete our yield only.
     */
    private void restrictNegativeValuesToOwnYield() throws SchemaException {
        if (outputTriple == null || originalTargetValues == null) {
            LOGGER.trace("restrictNegativeValuesToOwnYield: No output triple or no original target values, exiting.");
            return;
        }

        List<V> negativeValuesToKill = new ArrayList<>();

        for (V negativeValue : outputTriple.getMinusSet()) {
            LOGGER.trace("restrictNegativeValuesToOwnYield processing negative value of {}", negativeValue);
            if (negativeValue != null) {
                if (!negativeValue.hasValueMetadata()) {
                    // Negative value does not have metadata - should be removed?
                    negativeValue.hasValueMetadata();
                    //assert !negativeValue.hasValueMetadata();

                }
                List<V> matchingOriginalValues = originalTargetValues.stream()
                        .filter(originalValue ->
                                negativeValue.equals(originalValue, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS))
                        .toList();

                if (matchingOriginalValues.isEmpty()) {
                    // Huh? There's no original value corresponding to the one being deleted.
                    // We should maybe skip this negative value. But let's keep this dirty work to the consolidator instead.
                    LOGGER.trace("No original value corresponding to value in minus set. "
                            + "Keeping for consolidator to resolve. Value in minus set: {}", negativeValue);
                } else {
                    // We collect matching metadata from all equivalent existing values.
                    List<PrismContainerValue<ValueMetadataType>> matchingMetadataValues = new ArrayList<>();
                    boolean nakedValuePresent = false;
                    for (V matchingOriginalValue : matchingOriginalValues) {
                        List<PrismContainerValue<ValueMetadataType>> metadataValues =
                                matchingOriginalValue.<ValueMetadataType>getValueMetadataAsContainer().getValues();
                        if (metadataValues.isEmpty()) {
                            nakedValuePresent = true;
                        }
                        metadataValues.stream()
                                .filter(md -> hasMappingSpecification(md.asContainerable(), mappingSpecification))
                                .forEach(matchingMetadataValues::add);
                    }

                    if (nakedValuePresent) {
                        // No metadata found. This is a kind of transitional state, because if we work with metadata,
                        // each value should have some metadata. But OK, let us simply remove this value altogether.
                        LOGGER.trace("No metadata found for existing value(s), which is strange. "
                                + "Let us simply remove this value altogether.");
                    } else {
                        // Let us remove only own metadata.
                        if (matchingMetadataValues.isEmpty()) {
                            LOGGER.trace("Matching metadata not found for existing value(s). Preventing removal of this negative value.");
                            negativeValuesToKill.add(negativeValue);
                        } else {
                            LOGGER.trace("Restricting negative value to the following metadata values:\n{}",
                                    DebugUtil.debugDumpLazily(matchingMetadataValues, 1));
                            negativeValue.<ValueMetadataType>getValueMetadataAsContainer().addAll(CloneUtil.cloneCollectionMembers(matchingMetadataValues));
                        }
                    }
                }
            }
        }

        for (V negativeValueToKill : negativeValuesToKill) {
            outputTriple.getMinusSet().removeIf(value -> value == negativeValueToKill); // using == on purpose
        }
    }

    @SuppressWarnings("unchecked")
    private void addToMinusIfNecessary(
            @NotNull V originalValue,
            @NotNull ValueSetDefinition<V, D> rangeSetDef,
            @NotNull OperationResult result) {
        if (outputTriple != null
                && (outputTriple.presentInPlusSet(originalValue) || outputTriple.presentInZeroSet(originalValue))) {
            return;
        }
        if (expression != null && expression.doesVetoTargetValueRemoval(originalValue, result)) {
            LOGGER.trace("Expression vetoed removal of value: {}", originalValue);
            return;
        }
        // remove it!
        if (outputTriple == null) {
            outputTriple = PrismContext.get().deltaFactory().createPrismValueDeltaSetTriple();
        }

        V valueToDelete = (V) originalValue.clone();
        if (rangeSetDef.isYieldSpecific()) {
            LOGGER.trace("A yield of original value is in the mapping range (while not in mapping result), adding it to minus set: {}", originalValue);
            valueToDelete.<ValueMetadataType>getValueMetadataAsContainer()
                    .removeIf(md -> !rangeSetDef.hasMappingSpecification(md.asContainerable()));
            // TODO we could check if the minus set already contains the value we are going to remove
        } else {
            LOGGER.trace("Original value is in the mapping range (while not in mapping result), adding it to minus set: {}", originalValue);
        }
        outputTriple.addToMinusSet(valueToDelete);
    }

    public boolean isConditionSatisfied() {
        return conditionResultOld || conditionResultNew;
    }

    public PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> getConditionOutputTriple() {
        return conditionOutputTriple;
    }

    private void noteEvaluationStart() {
        if (profiling) {
            evaluationStartTime = System.currentTimeMillis();
        }
    }

    private void noteEvaluationEnd() {
        if (profiling) {
            evaluationEndTime = System.currentTimeMillis();
        }
    }

    private void traceSuccess() {
        if (!shouldCreateTextTrace()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mapping trace:\n");
        appendTraceHeader(sb);
        sb.append("\nCondition: ").append(conditionResultOld).append(" -> ").append(conditionResultNew);
        if (getNextRecomputeTime() != null) {
            sb.append("\nNext recompute: ");
            sb.append(getNextRecomputeTime());
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
        if (!shouldCreateTextTrace()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mapping trace:\n");
        appendTraceHeader(sb);
        sb.append("\nCondition: ").append(conditionResultOld).append(" -> ").append(conditionResultNew);

        sb.append("\nEvaluation ");
        if (!isConditionSatisfied()) {
            sb.append("WOULD BE ");
        }
        sb.append("DEFERRED to: ");
        if (getNextRecomputeTime() == null) {
            sb.append("null");
        } else {
            sb.append(getNextRecomputeTime());
        }
        if (profiling) {
            sb.append("\nEtime: ");
            sb.append(getEtime());
            sb.append(" ms");
        }
        appendTraceFooter(sb);
        trace(sb.toString());
    }

    private void traceFailure(Throwable e) {
        LOGGER.error("Error evaluating {}: {}", getMappingContextDescription(), e.getMessage(), e);
        if (!shouldCreateTextTrace()) {
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
    private boolean shouldCreateTextTrace() {
        return trace != null && isAtLeastNormal(tracingLevel) ||
                LOGGER.isTraceEnabled() ||
                mappingBean.isTrace() == Boolean.TRUE;
    }

    private void trace(String msg) {
        if (mappingBean.isTrace() == Boolean.TRUE) {
            LOGGER.info(msg);
        } else {
            LOGGER.trace(msg);
        }
        if (trace != null && isAtLeastNormal(tracingLevel)) {
            trace.setTextTrace(msg);
        }
    }

    private void appendTraceHeader(StringBuilder sb) {
        sb.append("---[ MAPPING ");
        if (mappingBean.getName() != null) {
            sb.append("'").append(mappingBean.getName()).append("' ");
        }
        sb.append(" in ");
        sb.append(contextDescription);
        sb.append("]---------------------------");
        sb.append("\nStrength: ").append(getStrength());
        if (!isAuthoritative()) {
            sb.append("\nNot authoritative");
        }
        for (Source<?, ?> source : sources) {
            sb.append("\n");
            source.mediumDump(sb);
        }
        sb.append("\nTarget: ").append(MiscUtil.toString(getOutputDefinition()));
        sb.append("\nTarget path: ").append(getOutputPath());
        if (targetPathExecutionOverride != null) {
            sb.append(" (specified as: ").append(parser.getOriginalOutputPath()).append(")");
        }

        sb.append("\nExpression: ");
        if (expression == null) {
            sb.append("null");
        } else {
            sb.append(expression.shortDebugDump());
        }

        sb.append("\nExpression profile: ").append(getExpressionProfile());
        sb.append("\nOrigin: ").append(mappingConfigItem.origin().fullDescription());

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
        return mappingBean.getCondition() == null || ExpressionUtil.computeConditionResult(booleanPropertyValues);
    }

    private void evaluateTimeConstraint(OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (timeConstraintsEvaluation == null) {
            timeConstraintsEvaluation = new MappingTimeConstraintsEvaluation(this);
            timeConstraintsEvaluation.evaluate(result);
        }
        timeConstraintsEvaluation.isTimeConstraintValid();
        recordTimeConstraintValidity();
    }

    private boolean sourcesChanged() {
        for (Source<?, ?> source : sources) {
            if (source.getDelta() != null) {
                return true;
            }
        }
        return false;
    }

    public D getOutputDefinition() {
        return parser.getOutputDefinition();
    }

    public ItemPath getOutputPath() {
        return parser.getOutputPath();
    }

    /**
     * Applies definition to the output if needed.
     */
    private void applyDefinitionToOutputTriple() throws SchemaException {
        if (outputTriple == null) {
            return;
        }
        if (outputTriple.isRaw()) {
            outputTriple.applyDefinition(getOutputDefinition());
        }
    }

    private void recomputeValues() {
        if (outputTriple == null) {
            return;
        }
        //noinspection rawtypes
        Visitor visitor = visitable -> {
            if (visitable instanceof PrismValue) {
                ((PrismValue) visitable).recompute(PrismContext.get());
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

    private void evaluateCondition(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        computeConditionTriple(result);

        boolean conditionOutputOld =
                computeConditionResult(conditionOutputTriple == null ? null : conditionOutputTriple.getNonPositiveValues());
        conditionResultOld = conditionOutputOld && conditionMaskOld;

        boolean conditionOutputNew =
                computeConditionResult(conditionOutputTriple == null ? null : conditionOutputTriple.getNonNegativeValues());
        conditionResultNew = conditionOutputNew && conditionMaskNew;
    }

    private void computeConditionTriple(OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            ExpressionEvaluationException,
            CommunicationException,
            ConfigurationException {
        ExpressionType conditionExpressionBean = mappingBean.getCondition();
        if (conditionExpressionBean == null) {
            // True -> True
            conditionOutputTriple = PrismContext.get().deltaFactory().createPrismValueDeltaSetTriple();
            conditionOutputTriple.addToZeroSet(PrismContext.get().itemFactory().createPropertyValue(Boolean.TRUE));
        } else {
            Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> expression =
                    ExpressionUtil.createCondition(
                            conditionExpressionBean,
                            getExpressionProfile(),
                            ModelCommonBeans.get().expressionFactory,
                            "condition in " + getMappingContextDescription(),
                            task, result);
            ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables,
                    "condition in " + getMappingContextDescription(), task);
            context.setValuePolicySupplier(valuePolicySupplier);
            context.setExpressionFactory(ModelCommonBeans.get().expressionFactory);
            context.setDefaultSource(defaultSource);
            context.setMappingQName(targetItemName);
            context.setVariableProducer(variableProducer);
            context.setLocalContextDescription("condition");
            conditionOutputTriple = expression.evaluate(context, result);
        }
    }

    private void evaluateExpression(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        expression = ModelCommonBeans.get().expressionFactory.makeExpression(
                mappingBean.getExpression(),
                getOutputDefinition(),
                getExpressionProfile(),
                "expression in " + getMappingContextDescription(),
                task,
                result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(
                sources, variables, "expression in " + getMappingContextDescription(), task);
        context.setDefaultSource(defaultSource);
        context.setSkipEvaluationMinus(!conditionResultOld);
        context.setSkipEvaluationPlus(!conditionResultNew);
        context.setValuePolicySupplier(valuePolicySupplier);
        context.setExpressionFactory(ModelCommonBeans.get().expressionFactory);
        context.setMappingQName(targetItemName);
        context.setTargetDefinitionBean(mappingBean.getTarget());
        context.setVariableProducer(variableProducer);
        context.setValueMetadataComputer(valueMetadataComputer);
        context.setLocalContextDescription("expression");

        if (mappingPreExpression != null) {
            mappingPreExpression.mappingPreExpression(context, result);
        }

        outputTriple = expression.evaluate(context, result);

        pushChangesRequested = determinePushChangesRequested();
        pushChanges = pushChangesRequested && sourcesChanged();

        if (outputTriple == null) {

            if (conditionResultNew) {
                // We need to return empty triple instead of null.
                // The condition was true (or there was not condition at all)
                // so the mapping is applicable.
                // Returning null would mean that the mapping is not applicable
                // at all.
                outputTriple = PrismContext.get().deltaFactory().createPrismValueDeltaSetTriple();
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

    private void setupValueMetadataComputer(OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        valueMetadataComputer = createValueMetadataComputer(result);
    }

    protected abstract TransformationValueMetadataComputer createValueMetadataComputer(OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException;

    protected abstract boolean determinePushChangesRequested();

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
        Item<V, D> output = (Item<V, D>) getOutputDefinition().instantiate();
        output.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));
        return output;
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
    public abstract AbstractMappingImpl<V, D, MBT> clone();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (conditionMaskNew ? 1231 : 1237);
        result = prime * result + (conditionMaskOld ? 1231 : 1237);
        result = prime * result + ((conditionOutputTriple == null) ? 0 : conditionOutputTriple.hashCode());
        result = prime * result + ((defaultSource == null) ? 0 : defaultSource.hashCode());
        result = prime * result + ((defaultTargetDefinition == null) ? 0 : defaultTargetDefinition.hashCode());
        result = prime * result + mappingBean.hashCode();
        result = prime * result + ((originObject == null) ? 0 : originObject.hashCode());
        result = prime * result + ((originType == null) ? 0 : originType.hashCode());
        result = prime * result + ((outputTriple == null) ? 0 : outputTriple.hashCode());
        result = prime * result + ((contextDescription == null) ? 0 : contextDescription.hashCode());
        result = prime * result + ((defaultSourceContextIdi == null) ? 0 : defaultSourceContextIdi.hashCode());
        result = prime * result + sources.hashCode();
        result = prime * result + ((targetContextDefinition == null) ? 0 : targetContextDefinition.hashCode());
        result = prime * result + ((variables == null) ? 0 : variables.hashCode());
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        AbstractMappingImpl<?, ?, ?> other = (MappingImpl<?, ?>) obj;
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
        if (!Objects.equals(expressionProfileReference, other.expressionProfileReference)) { return false; }
        if (!mappingBean.equals(other.mappingBean)) { return false; }
        if (originObject == null) {
            if (other.originObject != null) { return false; }
        } else if (!originObject.equals(other.originObject)) { return false; }
        if (originType != other.originType) { return false; }
        if (outputTriple == null) {
            if (other.outputTriple != null) { return false; }
        } else if (!outputTriple.equals(other.outputTriple)) { return false; }
        if (contextDescription == null) {
            if (other.contextDescription != null) { return false; }
        } else if (!contextDescription.equals(other.contextDescription)) { return false; }
        if (defaultSourceContextIdi == null) {
            if (other.defaultSourceContextIdi != null) { return false; }
        } else if (!defaultSourceContextIdi.equals(other.defaultSourceContextIdi)) { return false; }
        if (!sources.equals(other.sources)) { return false; }
        if (targetContextDefinition == null) {
            if (other.targetContextDefinition != null) { return false; }
        } else if (!targetContextDefinition.equals(other.targetContextDefinition)) { return false; }
        if (variables == null) {
            if (other.variables != null) { return false; }
        } else if (!variables.equals(other.variables)) { return false; }
        return true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this);
        return sb.toString();
    }

    @Override
    public String toString() {
        if (mappingBean.getName() != null) {
            return "M(" + mappingBean.getName() + ": " + getTargetItemPrettyName() + " = " + outputTriple + toStringStrength() + ")";
        } else {
            return "M(" + getTargetItemPrettyName() + " = " + outputTriple + toStringStrength() + ")";
        }
    }

    private String getTargetItemPrettyName() {
        if (targetItemName != null) {
            return SchemaDebugUtil.prettyPrint(targetItemName);
        }
        D outputDefinition = getOutputDefinition();
        if (outputDefinition != null) {
            return SchemaDebugUtil.prettyPrint(outputDefinition.getItemName());
        }
        return null;
    }

    private String toStringStrength() {
        return switch (getStrength()) {
            case NORMAL -> "";
            case WEAK -> ", weak";
            case STRONG -> ", strong";
        };
    }

    @Override
    public String getIdentifier() {
        return mappingBean.getName();
    }

    @Override
    public String toHumanReadableDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("mapping ");
        if (mappingBean.getName() != null) {
            sb.append("'").append(mappingBean.getName()).append("'");
        } else {
            sb.append(getTargetItemPrettyName());
        }
        if (originObject != null) {
            sb.append(" in ");
            sb.append(originObject);
        }
        return sb.toString();
    }

    /** Available only during mapping evaluation. */
    public @NotNull Task getTask() {
        return Objects.requireNonNull(task);
    }

    void recordTimeFrom(XMLGregorianCalendar timeFrom) {
        if (trace != null) {
            trace.setTimeFrom(timeFrom);
        }
    }

    void recordTimeTo(XMLGregorianCalendar timeTo) {
        if (trace != null) {
            trace.setTimeTo(timeTo);
        }
    }

    public @NotNull ModelCommonBeans getBeans() {
        return ModelCommonBeans.get();
    }

    public @NotNull MappingSpecificationType getMappingSpecification() {
        return mappingSpecification;
    }

    public List<QName> getSourceNames() {
        return sources.stream()
                .map(Source::getName)
                .collect(Collectors.toList());
    }

    @Experimental
    @Override
    public boolean isPushChanges() {
        return pushChanges;
    }

    boolean shouldUseMatchingProvenance() {
        return getOutputDefinition() != null && getOutputDefinition().isMultiValue() && mappingBean.getName() != null;
    }

    private static MappingSpecificationType createMappingAliasSpecification(MappingSpecificationType spec, String alias) {
        if (spec == null || alias == null || alias.isEmpty()) {
            return null;
        }
        return new MappingSpecificationType()
                .definitionObjectRef(CloneUtil.cloneCloneable(spec.getDefinitionObjectRef()))
                .objectType(CloneUtil.cloneCloneable(spec.getObjectType()))
                .mappingName(alias);
    }

    private static List<MappingSpecificationType> createMappingAliasSpecifications(MappingSpecificationType spec, List<String> alias) {
        if (spec == null || alias == null || alias.isEmpty()) {
            return null;
        }

        return alias.stream().map(s -> createMappingAliasSpecification(spec, s)).toList();
    }

    @Override
    public D getTargetItemDefinition() {
        return getOutputDefinition();
    }
}
