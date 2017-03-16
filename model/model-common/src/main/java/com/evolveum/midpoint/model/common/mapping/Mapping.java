/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.common.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.expression.ValueSetDefinition;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * 
 * Mapping is non-recyclable single-use object. Once evaluated it should not be evaluated again. It will retain its original
 * inputs and outputs that can be read again and again. But these should not be changed after evaluation.
 *
 * Configuration properties are unmodifiable. They are to be set via Mapping.Builder.
 * 
 * @author Radovan Semancik
 *
 */
public class Mapping<V extends PrismValue,D extends ItemDefinition> implements DebugDumpable, PrismValueDeltaSetTripleProducer<V, D> {
	
	// configuration properties (unmodifiable)
	private final MappingType mappingType;
	private final ExpressionFactory expressionFactory;
	private final ExpressionVariables variables;

	private final ObjectDeltaObject<?> sourceContext;
	private final Collection<Source<?,?>> sources;
	private final Source<?,?> defaultSource;

	private final PrismObjectDefinition<?> targetContext;
	private final ItemPath defaultTargetPath;
	private final D defaultTargetDefinition;
	private final Collection<V> originalTargetValues;

	private final ObjectResolver objectResolver;
    private final SecurityEnforcer securityEnforcer;          // in order to get c:actor variable
	private final OriginType originType;
	private final ObjectType originObject;
	private final FilterManager<Filter> filterManager;
	private final StringPolicyResolver stringPolicyResolver;
	private final boolean conditionMaskOld;
	private final boolean conditionMaskNew;
	private final XMLGregorianCalendar defaultReferenceTime;
	private final XMLGregorianCalendar now;
	private final boolean profiling;
	private final String contextDescription;

	private final QName mappingQName;						// This is sometimes used to identify the element that mapping produces
															// if it is different from itemName. E.g. this happens with associations.
	private final RefinedObjectClassDefinition refinedObjectClassDefinition;

	// working and output properties
	private D outputDefinition;
	private ItemPath outputPath;

	private PrismValueDeltaSetTriple<V> outputTriple;
	private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionOutputTriple;
	private Boolean timeConstraintValid = null;
	private XMLGregorianCalendar nextRecomputeTime = null;
	private Long evaluationStartTime = null;
	private Long evaluationEndTime = null;

	private String mappingContextDescription = null;

	// This is single-use only. Once evaluated it is not used any more
	// it is remembered only for tracing purposes.
	private Expression<V,D> expression;
	
	private static final Trace LOGGER = TraceManager.getTrace(Mapping.class);

	private Mapping(Builder<V,D> builder) {
		expressionFactory = builder.expressionFactory;
		variables = builder.variables;
		mappingType = builder.mappingType;
		objectResolver = builder.objectResolver;
		securityEnforcer = builder.securityEnforcer;
		defaultSource = builder.defaultSource;
		defaultTargetDefinition = builder.defaultTargetDefinition;
		defaultTargetPath = builder.defaultTargetPath;
		originalTargetValues = builder.originalTargetValues;
		sourceContext = builder.sourceContext;
		targetContext = builder.targetContext;
		sources = builder.sources;
		originType = builder.originType;
		originObject = builder.originObject;
		filterManager = builder.filterManager;
		stringPolicyResolver = builder.stringPolicyResolver;
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
			return outputDefinition.getName();
		}
		return null;
	}
	
	public OriginType getOriginType() {
		return originType;
	}

	public ObjectType getOriginObject() {
		return originObject;
	}

	public Source<?,?> getDefaultSource() {
		return defaultSource;
	}

	public D getDefaultTargetDefinition() {
		return defaultTargetDefinition;
	}

	public ItemPath getDefaultTargetPath() {
		return defaultTargetPath;
	}

	public ObjectDeltaObject<?> getSourceContext() {
		return sourceContext;
	}

	public PrismObjectDefinition<?> getTargetContext() {
		return targetContext;
	}

	public String getContextDescription() {
		return contextDescription;
	}

	public String getMappingContextDescription() {
		if (mappingContextDescription == null) {
			StringBuilder sb = new StringBuilder("mapping ");
			if (mappingType.getName() != null) {
				sb.append("'").append(mappingType.getName()).append("' ");
			}
			sb.append("in ");
			sb.append(contextDescription);
			mappingContextDescription  = sb.toString();
		}
		return mappingContextDescription;
	}

	public MappingType getMappingType() {
		return mappingType;
	}
	
	@Override
	public boolean isSourceless() {
		return sources.isEmpty();
	}

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
	
	public Boolean isTolerant() {
		if (mappingType == null) {
			return null;
		}
		return mappingType.isTolerant();
	}
	
	public boolean isConditionMaskOld() {
		return conditionMaskOld;
	}

	public boolean isConditionMaskNew() {
		return conditionMaskNew;
	}

	private PrismContext getPrismContext() {
		return outputDefinition.getPrismContext();
	}

	public FilterManager<Filter> getFilterManager() {
		return filterManager;
	}

	public StringPolicyResolver getStringPolicyResolver() {
		return stringPolicyResolver;
	}

	public boolean isApplicableToChannel(String channelUri) {
		return isApplicableToChannel(mappingType, channelUri);
	}

	public static boolean isApplicableToChannel(MappingType mappingType, String channelUri) {
		List<String> exceptChannel = mappingType.getExceptChannel();
		if (exceptChannel != null &&  !exceptChannel.isEmpty()){
			return !exceptChannel.contains(channelUri);
		}
		List<String> applicableChannels = mappingType.getChannel();
		if (applicableChannels == null || applicableChannels.isEmpty()) {
			return true;
		}
		return applicableChannels.contains(channelUri);
	}

	public XMLGregorianCalendar getNow() {
		return now;
	}

	public XMLGregorianCalendar getDefaultReferenceTime() {
		return defaultReferenceTime;
	}

	public XMLGregorianCalendar getNextRecomputeTime() {
		return nextRecomputeTime;
	}

	public boolean isProfiling() {
		return profiling;
	}

	public Long getEvaluationStartTime() {
		return evaluationStartTime;
	}

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

	public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
		return refinedObjectClassDefinition;
	}

	public void evaluate(Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		OperationResult result = parentResult.createMinorSubresult(Mapping.class.getName()+".evaluate");

        traceEvaluationStart();
		
		try {
			evaluateTimeConstraintValid(task, result);
			
			if (!timeConstraintValid) {
				outputTriple = null;
				result.recordNotApplicableIfUnknown();
				traceDeferred();
				return;
			}

			parseSources(task, result);
			parseTarget();

			if (outputPath != null && outputDefinition == null) {
				throw new IllegalArgumentException("No output definition, cannot evaluate "+getMappingContextDescription());
			}
			
			evaluateCondition(task, result);
			
			boolean conditionOutputOld = computeConditionResult(conditionOutputTriple.getNonPositiveValues());
			boolean conditionResultOld = conditionOutputOld && conditionMaskOld;
			
			boolean conditionOutputNew = computeConditionResult(conditionOutputTriple.getNonNegativeValues());
			boolean conditionResultNew = conditionOutputNew && conditionMaskNew;
			
			if (conditionResultOld || conditionResultNew) {
				// TODO: input filter
				evaluateExpression(task, result, conditionResultOld, conditionResultNew);
				fixDefinition();
				recomputeValues();
				setOrigin();
				// TODO: output filter

				checkRange(task, result);
			}

			result.recordSuccess();
			traceSuccess(conditionResultOld, conditionResultNew);
			
		} catch (ExpressionEvaluationException | ObjectNotFoundException | RuntimeException | SchemaException | Error e) {
			result.recordFatalError(e);
			traceFailure(e);
			throw e;
		}
	}

	private void checkRange(Task task, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		VariableBindingDefinitionType target = mappingType.getTarget();
		if (target != null && target.getSet() != null) {
			checkRangeTarget(task, result);
		}
		if (mappingType.getRange() != null) {
			checkRangeLegacy(task, result);
		}
	}

	private void checkRangeTarget(Task task, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (originalTargetValues == null) {
			throw new IllegalStateException("Couldn't check range for mapping in " + contextDescription + ", as original target values are not known.");
		}
		ValueSetDefinitionType rangetSetDefType = mappingType.getTarget().getSet();
		QName name = outputPath.lastNamed().getName();
		ValueSetDefinition setDef = new ValueSetDefinition(rangetSetDefType, name, "range of "+name.getLocalPart()+" in "+getMappingContextDescription(), task, result);
		setDef.init(expressionFactory);
		for (V originalValue : originalTargetValues) {
			if (!setDef.contains(originalValue)) {
				continue;
			}
			addToMinusIfNecessary(originalValue);
		}
	}

	private void addToMinusIfNecessary(V originalValue) {
		if (outputTriple != null && (outputTriple.presentInPlusSet(originalValue) || outputTriple.presentInZeroSet(originalValue))) {
			return;
		}
		// remove it!
		if (outputTriple == null) {
			outputTriple = new PrismValueDeltaSetTriple<>();
		}
		LOGGER.trace("Original value is in the mapping range (while not in mapping result), adding it to minus set: {}", originalValue);
		outputTriple.addToMinusSet(originalValue);
	}

	private void checkRangeLegacy(Task task, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (originalTargetValues == null) {
			throw new IllegalStateException("Couldn't check range for mapping in " + contextDescription + ", as original target values are not known.");
		}
		for (V originalValue : originalTargetValues) {
			if (!isInRange(originalValue, task, result)) {
				continue;
			}
			addToMinusIfNecessary(originalValue);
		}
	}

	private boolean isInRange(V value, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		@NotNull ValueSetSpecificationType range = mappingType.getRange();
		if (range.getIsInSetExpression() == null) {
			return false;
		}
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(this.variables);			// TODO is this ok?

		if (value instanceof PrismContainerValue) {
			// artifically create parent for PCV in order to pass it to expression
			PrismContainer.createParentIfNeeded((PrismContainerValue) value, outputDefinition);
		}
		variables.addVariableDefinition(ExpressionConstants.VAR_VALUE, value);

		PrismPropertyDefinition<Boolean> outputDef = new PrismPropertyDefinitionImpl<Boolean>(SchemaConstantsGenerated.C_VALUE, DOMUtil.XSD_BOOLEAN, getPrismContext(), null, false);
		PrismPropertyValue<Boolean> rv = ExpressionUtil.evaluateExpression(variables, outputDef, range.getIsInSetExpression(), expressionFactory, "isInSet expression in " + contextDescription, task, result);

		// but now remove the parent!
		if (value.getParent() != null) {
			value.setParent(null);
		}

		return rv != null && rv.getValue() != null ? rv.getValue() : Boolean.FALSE;
	}

	public boolean isSatisfyCondition() {
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
	
	private void traceFailure(Throwable e) {
		LOGGER.error("Error evaluating {}: {}", new Object[]{getMappingContextDescription(), e.getMessage(), e});
		traceEvaluationEnd();
		if (!isTrace()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Mapping failure:\n");
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
	
	private boolean isTrace() {
		return LOGGER.isTraceEnabled() || (mappingType != null && mappingType.isTrace() == Boolean.TRUE); 
	}
	
	private void trace(String msg) {
		if (mappingType != null && mappingType.isTrace() == Boolean.TRUE) {
			LOGGER.info(msg);
		} else {
			LOGGER.trace(msg);
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
		for (Source<?,?> source: sources) {
			sb.append("\nSource: ");
			sb.append(source.shortDebugDump());
		}
		sb.append("\nTarget: ").append(MiscUtil.toString(outputDefinition));
		sb.append("\nExpression: ");
		if (expression == null) {
			sb.append("null");
		} else {
			sb.append(expression.shortDebugDump());
		}
	}

	private void appendTraceFooter(StringBuilder sb) {
		sb.append("\n------------------------------------------------------");
	}
	
	private boolean computeConditionResult(Collection<PrismPropertyValue<Boolean>> booleanPropertyValues) {
		if (mappingType.getCondition() == null) {
			// If condition is not present at all consider it to be true
			return true;
		}
		return ExpressionUtil.computeConditionResult(booleanPropertyValues);
	}

	public Boolean evaluateTimeConstraintValid(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (timeConstraintValid == null) {
			parseTimeConstraints(task, result);
		}
		return timeConstraintValid;
	}

	private void parseTimeConstraints(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		MappingTimeDeclarationType timeFromType = mappingType.getTimeFrom();
		MappingTimeDeclarationType timeToType = mappingType.getTimeTo();
		if (timeFromType == null && timeToType == null) {
			timeConstraintValid = true;
			return;
		}
		
		XMLGregorianCalendar timeFrom = parseTime(timeFromType, task, result);
		if (timeFrom == null && timeFromType != null) {
			// Time is specified but there is no value for it.
			// This means that event that should start validity haven't happened yet
			// therefore the mapping is not yet valid. 
			timeConstraintValid = false;
			return;
		}
		XMLGregorianCalendar timeTo = parseTime(timeToType, task, result);
		
		if (timeFrom != null && timeFrom.compare(now) == DatatypeConstants.GREATER) {
			// before timeFrom
			nextRecomputeTime = timeFrom;
			timeConstraintValid = false;
			return;
		}
		
		if (timeTo == null && timeToType != null) {
			// Time is specified but there is no value for it.
			// This means that event that should stop validity haven't happened yet
			// therefore the mapping is still valid. 
			timeConstraintValid = true;
			return;
		}
		
		if (timeTo != null && timeTo.compare(now) == DatatypeConstants.GREATER) {
			// between timeFrom and timeTo (also no timeFrom and before timeTo)
			nextRecomputeTime = timeTo;
			timeConstraintValid = true;
			return;
		}

		if (timeTo == null) {
			// after timeFrom and no timeTo
			// no nextRecomputeTime set, there is nothing to recompute in the future
			timeConstraintValid = true;
			return;
			
		} else {
			// after timeTo
			// no nextRecomputeTime set, there is nothing to recompute in the future
			timeConstraintValid = false;
			return;
		}
		
	}
	
	private XMLGregorianCalendar parseTime(MappingTimeDeclarationType timeType, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (timeType == null) {
			return null;
		}
		XMLGregorianCalendar time;
		VariableBindingDefinitionType referenceTimeType = timeType.getReferenceTime();
		if (referenceTimeType == null) {
			if (defaultReferenceTime == null) {
				throw new SchemaException("No reference time specified (and there is also no default) in time specification in "+getMappingContextDescription());
			} else {
				time = (XMLGregorianCalendar) defaultReferenceTime.clone();
			}
		} else {
			time = parseTimeSource(referenceTimeType, task, result);
			if (time == null) {
				// Reference time is specified but the value is not present.
				return null;
			}
			time = (XMLGregorianCalendar) time.clone();
		}
		Duration offset = timeType.getOffset();
		if (offset != null) {
			time.add(offset);
		}
		return time;
	}

	private XMLGregorianCalendar parseTimeSource(VariableBindingDefinitionType sourceType, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		ItemPathType itemPathType = sourceType.getPath();
		if (itemPathType == null) {
			throw new SchemaException("No path in source definition in "+getMappingContextDescription());
		}
		ItemPath path = itemPathType.getItemPath();
		if (path.isEmpty()) {
			throw new SchemaException("Empty source path in "+getMappingContextDescription());
		}
		
		Object sourceObject = ExpressionUtil.resolvePath(path, variables, sourceContext, objectResolver, "reference time definition in "+getMappingContextDescription(), task, result);
		if (sourceObject == null) {
			return null;
		}
		PrismProperty<XMLGregorianCalendar> timeProperty;
		if (sourceObject instanceof ItemDeltaItem<?,?>) {
			timeProperty = (PrismProperty<XMLGregorianCalendar>) ((ItemDeltaItem<?,?>)sourceObject).getItemNew();
		} else if (sourceObject instanceof Item<?,?>) {
			timeProperty = (PrismProperty<XMLGregorianCalendar>) sourceObject;
		} else {
			throw new IllegalStateException("Unknown resolve result "+sourceObject);
		}
		if (timeProperty == null) {
			return null;
		}
		return timeProperty.getRealValue();
	}

	private Collection<Source<?,?>> parseSources(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		List<VariableBindingDefinitionType> sourceTypes = mappingType.getSource();
		if (defaultSource != null) {
			defaultSource.recompute();
			sources.add(defaultSource);
			defaultSource.recompute();
		}
		if (sourceTypes != null) {
			for (VariableBindingDefinitionType sourceType: sourceTypes) {
				Source<?,?> source = parseSource(sourceType, task, result);
				source.recompute();
				
				// Override existing sources (e.g. default source)
				Iterator<Source<?,?>> iterator = sources.iterator();
				while (iterator.hasNext()) {
					Source<?,?> next = iterator.next();
					if (next.getName().equals(source.getName())) {
						iterator.remove();
					}
				}
				
				sources.add(source);
			}
		}
		return sources;
	}

	private <IV extends PrismValue, ID extends ItemDefinition> Source<IV,ID> parseSource(VariableBindingDefinitionType sourceType, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ItemPathType itemPathType = sourceType.getPath();
		if (itemPathType == null) {
			throw new SchemaException("No path in source definition in "+getMappingContextDescription());
		}
		ItemPath path = itemPathType.getItemPath();
		if (path.isEmpty()) {
			throw new SchemaException("Empty source path in "+getMappingContextDescription());
		}
		QName name = sourceType.getName();
		if (name == null) {
			name = ItemPath.getName(path.last());
		}
		ItemPath resolvePath = path;
		Object sourceObject = ExpressionUtil.resolvePath(path, variables, sourceContext, objectResolver, "source definition in "+getMappingContextDescription(), task, result);
		Item<IV,ID> itemOld = null;
		ItemDelta<IV,ID> delta = null;
		Item<IV,ID> itemNew = null;
		ItemPath residualPath = null;
		Collection<? extends ItemDelta<?,?>> subItemDeltas = null;
		if (sourceObject != null) {
			if (sourceObject instanceof ItemDeltaItem<?,?>) {
				itemOld = ((ItemDeltaItem<IV,ID>)sourceObject).getItemOld();
				delta = ((ItemDeltaItem<IV,ID>)sourceObject).getDelta();
				itemNew = ((ItemDeltaItem<IV,ID>)sourceObject).getItemNew();
				residualPath = ((ItemDeltaItem<IV,ID>)sourceObject).getResidualPath();
				resolvePath = ((ItemDeltaItem<IV,ID>)sourceObject).getResolvePath();
				subItemDeltas = ((ItemDeltaItem<IV,ID>)sourceObject).getSubItemDeltas();
			} else if (sourceObject instanceof Item<?,?>) {
				itemOld = (Item<IV,ID>) sourceObject;
				itemNew = (Item<IV,ID>) sourceObject;
			} else {
				throw new IllegalStateException("Unknown resolve result "+sourceObject);
			}
		}
		
		// apply domain
		ValueSetDefinitionType domainSetType = sourceType.getSet();
		if (domainSetType != null) {
			ValueSetDefinition setDef = new ValueSetDefinition(domainSetType, name, "domain of "+name.getLocalPart()+" in "+getMappingContextDescription(), task, result);
			setDef.init(expressionFactory);
			try {
				
				if (itemOld != null) {
					itemOld = itemOld.clone();
					itemOld.filterValues(val -> setDef.containsTunnel(val));
				}
				
				if (itemNew != null) {
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
					throw (SchemaException)cause;
				} else if (cause instanceof ExpressionEvaluationException) {
					throw (ExpressionEvaluationException)cause;
				} else if (cause instanceof ObjectNotFoundException) {
					throw (ObjectNotFoundException)cause;
				}
			}
		}
		
		Source<IV,ID> source = new Source<>(itemOld, delta, itemNew, name);
		source.setResidualPath(residualPath);
		source.setResolvePath(resolvePath);
		source.setSubItemDeltas(subItemDeltas);
		return source;
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
				outputDefinition = ExpressionUtil.resolveDefinitionPath(path, variables, targetContext, "target definition in "+getMappingContextDescription());
				if (outputDefinition == null) {
					throw new SchemaException("No target item that would conform to the path "+path+" in "+getMappingContextDescription());
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
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (visitable instanceof PrismValue) {
					((PrismValue)visitable).recompute(getPrismContext());
				}
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
	
	private void evaluateCondition(Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ExpressionType conditionExpressionType = mappingType.getCondition();
		if (conditionExpressionType == null) {
			// True -> True
			conditionOutputTriple = new PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>>();
			conditionOutputTriple.addToZeroSet(new PrismPropertyValue<Boolean>(Boolean.TRUE));
			return;
		}
		Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = 
				ExpressionUtil.createCondition(conditionExpressionType, expressionFactory, 
				"condition in "+getMappingContextDescription(), task, result);
		ExpressionEvaluationContext params = new ExpressionEvaluationContext(sources, variables, 
				"condition in "+getMappingContextDescription(), task, result);
		params.setStringPolicyResolver(stringPolicyResolver);
		params.setExpressionFactory(expressionFactory);
		params.setDefaultSource(defaultSource);
		params.setDefaultTargetContext(getTargetContext());
		params.setRefinedObjectClassDefinition(getRefinedObjectClassDefinition());
		params.setMappingQName(mappingQName);
		conditionOutputTriple = expression.evaluate(params);
	}

	
	private void evaluateExpression(Task task, OperationResult result, boolean conditionResultOld, boolean conditionResultNew) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ExpressionType expressionType = null;
		if (mappingType != null) {
			expressionType = mappingType.getExpression();
		}
		expression = expressionFactory.makeExpression(expressionType, outputDefinition, 
				"expression in "+getMappingContextDescription(), task, result);
		ExpressionEvaluationContext params = new ExpressionEvaluationContext(sources, variables, 
				"expression in "+getMappingContextDescription(), task, result);
		params.setDefaultSource(defaultSource);
		params.setSkipEvaluationMinus(!conditionResultOld);
		params.setSkipEvaluationPlus(!conditionResultNew);
		params.setStringPolicyResolver(stringPolicyResolver);
		params.setExpressionFactory(expressionFactory);
		params.setDefaultTargetContext(getTargetContext());
		params.setRefinedObjectClassDefinition(getRefinedObjectClassDefinition());
		params.setMappingQName(mappingQName);
		outputTriple = expression.evaluate(params);

		if (outputTriple == null) {
			
			if (conditionResultNew) {
				// We need to return empty triple instead of null.
				// The condition was true (or there was not condition at all)
				// so the mapping is applicable.
				// Returning null would mean that the mapping is not applicable
				// at all.
				outputTriple = new PrismValueDeltaSetTriple<>();
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

	public Item<V,D> getOutput() throws SchemaException {
		if (outputTriple == null) {
			return null;
		}
		Item<V,D> output = outputDefinition.instantiate();
		output.addAll(PrismValue.cloneCollection(outputTriple.getNonNegativeValues()));
		return output;
	}
	
	public ItemDelta<V,D> createEmptyDelta(ItemPath path) {
		return outputDefinition.createEmptyDelta(path);
	}
	
	private <T> PrismPropertyValue<T> filterValue(PrismPropertyValue<T> propertyValue, List<ValueFilterType> filters) {
        PrismPropertyValue<T> filteredValue = propertyValue.clone();
        filteredValue.setOriginType(OriginType.INBOUND);

        if (filters == null || filters.isEmpty()) {
            return filteredValue;
        }

        for (ValueFilterType filter : filters) {
            Filter filterInstance = filterManager.getFilterInstance(filter.getType(), filter.getAny());
            filterInstance.apply(filteredValue);
        }

        return filteredValue;
    }
	
	/**
	 * Shallow clone. Only the output is cloned deeply.
	 */
	public PrismValueDeltaSetTripleProducer<V, D> clone() {
		Mapping<V, D> clone = new Builder<V, D>()
				.mappingType(mappingType)
				.contextDescription(contextDescription)
				.expressionFactory(expressionFactory)
				.securityEnforcer(securityEnforcer)
				.variables(variables)
				.conditionMaskNew(conditionMaskNew)
				.conditionMaskOld(conditionMaskOld)
				.defaultSource(defaultSource)
				.defaultTargetDefinition(defaultTargetDefinition)
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
		result = prime * result
				+ ((defaultTargetDefinition == null) ? 0 : defaultTargetDefinition.hashCode());
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mapping other = (Mapping) obj;
		if (conditionMaskNew != other.conditionMaskNew)
			return false;
		if (conditionMaskOld != other.conditionMaskOld)
			return false;
		if (conditionOutputTriple == null) {
			if (other.conditionOutputTriple != null)
				return false;
		} else if (!conditionOutputTriple.equals(other.conditionOutputTriple))
			return false;
		if (defaultSource == null) {
			if (other.defaultSource != null)
				return false;
		} else if (!defaultSource.equals(other.defaultSource))
			return false;
		if (defaultTargetDefinition == null) {
			if (other.defaultTargetDefinition != null)
				return false;
		} else if (!defaultTargetDefinition.equals(other.defaultTargetDefinition))
			return false;
		if (expressionFactory == null) {
			if (other.expressionFactory != null)
				return false;
		} else if (!expressionFactory.equals(other.expressionFactory))
			return false;
		if (mappingType == null) {
			if (other.mappingType != null)
				return false;
		} else if (!mappingType.equals(other.mappingType))
			return false;
		if (objectResolver == null) {
			if (other.objectResolver != null)
				return false;
		} else if (!objectResolver.equals(other.objectResolver))
			return false;
		if (originObject == null) {
			if (other.originObject != null)
				return false;
		} else if (!originObject.equals(other.originObject))
			return false;
		if (originType != other.originType)
			return false;
		if (outputDefinition == null) {
			if (other.outputDefinition != null)
				return false;
		} else if (!outputDefinition.equals(other.outputDefinition))
			return false;
		if (outputTriple == null) {
			if (other.outputTriple != null)
				return false;
		} else if (!outputTriple.equals(other.outputTriple))
			return false;
		if (contextDescription == null) {
			if (other.contextDescription != null)
				return false;
		} else if (!contextDescription.equals(other.contextDescription))
			return false;
		if (sourceContext == null) {
			if (other.sourceContext != null)
				return false;
		} else if (!sourceContext.equals(other.sourceContext))
			return false;
		if (sources == null) {
			if (other.sources != null)
				return false;
		} else if (!sources.equals(other.sources))
			return false;
		if (targetContext == null) {
			if (other.targetContext != null)
				return false;
		} else if (!targetContext.equals(other.targetContext))
			return false;
		if (variables == null) {
			if (other.variables != null)
				return false;
		} else if (!variables.equals(other.variables))
			return false;
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append(toString());
		return sb.toString();
	}

	@Override
	public String toString() {
		if (mappingType != null && mappingType.getName() != null) {
			return "M(" + mappingType.getName()+ ": "+ getMappingDisplayName() + " = " + outputTriple + toStringStrength() + ")";
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
		return SchemaDebugUtil.prettyPrint(outputDefinition.getName());
	}

	private String toStringStrength() {
		switch (getStrength()) {
			case NORMAL: return "";
			case WEAK: return ", weak";
			case STRONG: return ", strong";
		}
		return null;
	}

	/**
	 * Builder is used to construct a configuration of Mapping object, which - after building - becomes
	 * immutable.
	 *
	 * In order to provide backward-compatibility with existing use of Mapping object, the builder has
	 * also traditional setter methods. Both setters and "builder-style" methods MODIFY existing Builder
	 * object (i.e. they do not create a new one).
	 *
	 * TODO decide on which style of setters to keep (setters vs builder-style).
	 */
	public static final class Builder<V extends PrismValue, D extends ItemDefinition> {
		private ExpressionFactory expressionFactory;
		private ExpressionVariables variables = new ExpressionVariables();
		private MappingType mappingType;
		private ObjectResolver objectResolver;
		private SecurityEnforcer securityEnforcer;
		private Source<?, ?> defaultSource;
		private D defaultTargetDefinition;
		private ItemPath defaultTargetPath;
		private Collection<V> originalTargetValues;
		private ObjectDeltaObject<?> sourceContext;
		private PrismObjectDefinition<?> targetContext;
		private Collection<Source<?, ?>> sources = new ArrayList<>();
		private OriginType originType;
		private ObjectType originObject;
		private FilterManager<Filter> filterManager;
		private StringPolicyResolver stringPolicyResolver;
		private boolean conditionMaskOld = true;
		private boolean conditionMaskNew = true;
		private XMLGregorianCalendar now;
		private XMLGregorianCalendar defaultReferenceTime;
		private boolean profiling;
		private String contextDescription;
		private QName mappingQName;
		private RefinedObjectClassDefinition refinedObjectClassDefinition;
		private PrismContext prismContext;


		public Builder<V,D> expressionFactory(ExpressionFactory val) {
			expressionFactory = val;
			return this;
		}

		public Builder<V,D> variables(ExpressionVariables val) {
			variables = val;
			return this;
		}

		public Builder<V,D> mappingType(MappingType val) {
			mappingType = val;
			return this;
		}

		public Builder<V,D> objectResolver(ObjectResolver val) {
			objectResolver = val;
			return this;
		}

		public Builder<V,D> securityEnforcer(SecurityEnforcer val) {
			securityEnforcer = val;
			return this;
		}

		public Builder<V,D> defaultSource(Source<?, ?> val) {
			defaultSource = val;
			return this;
		}

		public Builder<V,D> defaultTargetDefinition(D val) {
			defaultTargetDefinition = val;
			return this;
		}

		public Builder<V,D> defaultTargetPath(ItemPath val) {
			defaultTargetPath = val;
			return this;
		}

		public Builder<V,D> originalTargetValues(Collection<V> values) {
			originalTargetValues = values;
			return this;
		}

		public Builder<V,D> sourceContext(ObjectDeltaObject<?> val) {
			sourceContext = val;
			return this;
		}

		public Builder<V,D> targetContext(PrismObjectDefinition<?> val) {
			targetContext = val;
			return this;
		}

		public Builder<V,D> sources(Collection<Source<?, ?>> val) {
			sources = val;
			return this;
		}

		public Builder<V,D> originType(OriginType val) {
			originType = val;
			return this;
		}

		public Builder<V,D> originObject(ObjectType val) {
			originObject = val;
			return this;
		}

		public Builder<V,D> filterManager(FilterManager<Filter> val) {
			filterManager = val;
			return this;
		}

		public Builder<V,D> stringPolicyResolver(StringPolicyResolver val) {
			stringPolicyResolver = val;
			return this;
		}

		public Builder<V,D> conditionMaskOld(boolean val) {
			conditionMaskOld = val;
			return this;
		}

		public Builder<V,D> conditionMaskNew(boolean val) {
			conditionMaskNew = val;
			return this;
		}

		public Builder<V,D> now(XMLGregorianCalendar val) {
			now = val;
			return this;
		}

		public Builder<V,D> defaultReferenceTime(XMLGregorianCalendar val) {
			defaultReferenceTime = val;
			return this;
		}

		public Builder<V,D> profiling(boolean val) {
			profiling = val;
			return this;
		}

		public Builder<V,D> contextDescription(String val) {
			contextDescription = val;
			return this;
		}

		public Builder<V,D> mappingQName(QName val) {
			mappingQName = val;
			return this;
		}

		public Builder<V,D> refinedObjectClassDefinition(RefinedObjectClassDefinition val) {
			refinedObjectClassDefinition = val;
			return this;
		}

		public Builder<V,D> prismContext(PrismContext val) {
			prismContext = val;
			return this;
		}

		public Mapping<V,D> build() {
			return new Mapping<>(this);
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

		public SecurityEnforcer getSecurityEnforcer() {
			return securityEnforcer;
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

		public FilterManager<Filter> getFilterManager() {
			return filterManager;
		}

		public StringPolicyResolver getStringPolicyResolver() {
			return stringPolicyResolver;
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
			return addVariableDefinition(null,(Object)objectRef);
		}

		public Builder<V, D> rootNode(ObjectDeltaObject<?> odo) {
			return addVariableDefinition(null,(Object)odo);
		}

		public Builder<V, D> rootNode(ObjectType objectType) {
			return addVariableDefinition(null,(Object)objectType);
		}

		public Builder<V, D> rootNode(PrismObject<? extends ObjectType> mpObject) {
			return addVariableDefinition(null,(Object)mpObject);
		}

		@Deprecated
		public void setRootNode(ObjectReferenceType objectRef) {
			rootNode(objectRef);
		}

		@Deprecated
		public void setRootNode(ObjectDeltaObject<?> odo) {
			rootNode(odo);
		}

		@Deprecated
		public void setRootNode(ObjectType objectType) {
			rootNode(objectType);
		}

		@Deprecated
		public void setRootNode(PrismObject<? extends ObjectType> mpObject) {
			rootNode(mpObject);
		}

		public PrismContext getPrismContext() {
			return prismContext;
		}

		public Builder<V, D> addVariableDefinition(ExpressionVariableDefinitionType varDef) throws SchemaException {
			if (varDef.getObjectRef() != null) {
				ObjectReferenceType ref = varDef.getObjectRef();
				ref.setType(getPrismContext().getSchemaRegistry().qualifyTypeName(ref.getType()));
				return addVariableDefinition(varDef.getName(), ref);
			} else if (varDef.getValue() != null) {
				return addVariableDefinition(varDef.getName(),varDef.getValue());
			} else {
				LOGGER.warn("Empty definition of variable {} in {}, ignoring it", varDef.getName(), getContextDescription());
				return this;
			}
		}

		public Builder<V, D> addVariableDefinition(QName name, ObjectReferenceType objectRef) {
			return addVariableDefinition(name, (Object)objectRef);
		}

		public Builder<V, D> addVariableDefinition(QName name, ObjectType objectType) {
			return addVariableDefinition(name,(Object)objectType);
		}

		public Builder<V, D> addVariableDefinition(QName name, PrismObject<? extends ObjectType> midpointObject) {
			return addVariableDefinition(name,(Object)midpointObject);
		}

		public Builder<V, D> addVariableDefinition(QName name, String value) {
			return addVariableDefinition(name,(Object)value);
		}

		public Builder<V, D> addVariableDefinition(QName name, int value) {
			return addVariableDefinition(name,(Object)value);
		}

		public Builder<V, D> addVariableDefinition(QName name, Element value) {
			return addVariableDefinition(name,(Object)value);
		}

		public Builder<V, D> addVariableDefinition(QName name, PrismValue value) {
			return addVariableDefinition(name,(Object)value);
		}

		public Builder<V, D> addVariableDefinition(QName name, ObjectDeltaObject<?> value) {
			return addVariableDefinition(name,(Object)value);
		}

		public Builder<V, D> addVariableDefinitions(Map<QName, Object> extraVariables) {
			variables.addVariableDefinitions(extraVariables);
			return this;
		}

		public Builder<V, D> addVariableDefinition(QName name, Object value) {
			variables.addVariableDefinition(name, value);
			return this;
		}

		public boolean hasVariableDefinition(QName varName) {
			return variables.containsKey(varName);
		}

		public boolean isApplicableToChannel(String channel) {
			return Mapping.isApplicableToChannel(mappingType, channel);
		}

		public Builder<V, D> addSource(Source<?,?> source) {
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
		public void setSecurityEnforcer(SecurityEnforcer securityEnforcer) {
			this.securityEnforcer = securityEnforcer;
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
		public void setFilterManager(
				FilterManager<Filter> filterManager) {
			this.filterManager = filterManager;
		}

		@Deprecated
		public void setStringPolicyResolver(
				StringPolicyResolver stringPolicyResolver) {
			this.stringPolicyResolver = stringPolicyResolver;
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
			return Mapping.getStrength(mappingType);
		}
	}
}
