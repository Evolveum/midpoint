/*
 * Copyright (c) 2010-2013 Evolveum
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.InternalsConfig;
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
import com.evolveum.midpoint.model.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSourceDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingTargetDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingTimeDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueFilterType;

/**
 * 
 * Mapping is non-recyclable single-use object. Once evaluated it should not be evaluated again. It will retain its original
 * inputs and outputs that can be read again and again. But these should not be changed after evaluation.
 * 
 * @author Radovan Semancik
 *
 */
public class Mapping<V extends PrismValue> implements DebugDumpable {
	
	private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");
	
	private ExpressionFactory expressionFactory;
	private ExpressionVariables variables = new ExpressionVariables();
	private String contextDescription;
	private String mappingContextDescription = null;
	private MappingType mappingType;
	private ObjectResolver objectResolver = null;
	private Source<?> defaultSource = null;
	private ItemDefinition defaultTargetDefinition = null;
	private ItemPath defaultTargetPath = null;
	private ObjectDeltaObject<?> sourceContext = null;
	private PrismObjectDefinition<?> targetContext = null;
	private PrismValueDeltaSetTriple<V> outputTriple = null;
	private ItemDefinition outputDefinition;
	private ItemPath outputPath;
	private Collection<Source<?>> sources = new ArrayList<Source<?>>();
	private boolean conditionMaskOld = true;
	private boolean conditionMaskNew = true;
	private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionOutputTriple;
	private OriginType originType = null;
	private ObjectType originObject = null;
	private FilterManager<Filter> filterManager;
	private StringPolicyResolver stringPolicyResolver;
	private XMLGregorianCalendar now;
	private XMLGregorianCalendar defaultReferenceTime = null;
	private Boolean timeConstraintValid = null;
	private XMLGregorianCalendar nextRecomputeTime = null;
	private boolean profiling = false;
	private Long evaluationStartTime = null;
	private Long evaluationEndTime = null;
	// This is sometimes used to identify the element that mapping produces
	// if it is different from itemName. E.g. this happens with associations.
	private QName mappingQName;
	private RefinedObjectClassDefinition refinedObjectClassDefinition;
	
	// This is single-use only. Once evaluated it is not used any more
	// it is remembered only for tracing purposes.
	private Expression<V> expression;
	
	private static final Trace LOGGER = TraceManager.getTrace(Mapping.class);
	
	Mapping(MappingType mappingType, String contextDescription, ExpressionFactory expressionFactory) {
		Validate.notNull(mappingType);
		this.contextDescription = contextDescription;
		this.mappingType = mappingType;
		this.expressionFactory = expressionFactory;
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
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

	public void setOriginType(OriginType sourceType) {
		this.originType = sourceType;
	}

	public ObjectType getOriginObject() {
		return originObject;
	}

	public void setOriginObject(ObjectType originObject) {
		this.originObject = originObject;
	}

	public void addSource(Source<?> source) {
		sources.add(source);
	}
	
	public Source<?> getDefaultSource() {
		return defaultSource;
	}

	public void setDefaultSource(Source<?> defaultSource) {
		this.defaultSource = defaultSource;
	}

	public ItemDefinition getDefaultTargetDefinition() {
		return defaultTargetDefinition;
	}

	public void setDefaultTargetDefinition(ItemDefinition defaultTargetDefinition) {
		this.defaultTargetDefinition = defaultTargetDefinition;
	}

	public ItemPath getDefaultTargetPath() {
		return defaultTargetPath;
	}

	public void setDefaultTargetPath(ItemPath defaultTargetPath) {
		this.defaultTargetPath = defaultTargetPath;
	}

	public ObjectDeltaObject<?> getSourceContext() {
		return sourceContext;
	}

	public void setSourceContext(ObjectDeltaObject<?> sourceContext) {
		this.sourceContext = sourceContext;
	}

	public PrismObjectDefinition<?> getTargetContext() {
		return targetContext;
	}

	public void setTargetContext(PrismObjectDefinition<?> targetContext) {
		this.targetContext = targetContext;
	}

	public String getContextDescription() {
		return contextDescription;
	}

	public void setContextDescription(String contextDescription) {
		this.contextDescription = contextDescription;
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

	public void setRootNode(ObjectReferenceType objectRef) {
		addVariableDefinition(null,(Object)objectRef);
	}
	
	public void setRootNode(ObjectDeltaObject<?> odo) {
		addVariableDefinition(null,(Object)odo);
	}
	
	public void setRootNode(ObjectType objectType) {
		addVariableDefinition(null,(Object)objectType);
	}
	
	public void setRootNode(PrismObject<? extends ObjectType> mpObject) {
		addVariableDefinition(null,(Object)mpObject);
	}

	public void addVariableDefinition(ExpressionVariableDefinitionType varDef) {
		if (varDef.getObjectRef() != null) {
			addVariableDefinition(varDef.getName(),varDef.getObjectRef());
		} else if (varDef.getValue() != null) {
			addVariableDefinition(varDef.getName(),varDef.getValue());
		} else {
			LOGGER.warn("Empty definition of variable {} in {}, ignoring it",varDef.getName(),getMappingContextDescription());
		}
		
	}
	
	public void addVariableDefinition(QName name, ObjectReferenceType objectRef) {
		addVariableDefinition(name,(Object)objectRef);
	}
	
	public void addVariableDefinition(QName name, ObjectType objectType) {
		addVariableDefinition(name,(Object)objectType);
	}
	
	public void addVariableDefinition(QName name, PrismObject<? extends ObjectType> midpointObject) {
		addVariableDefinition(name,(Object)midpointObject);
	}

	public void addVariableDefinition(QName name, String value) {
		addVariableDefinition(name,(Object)value);
	}

	public void addVariableDefinition(QName name, int value) {
		addVariableDefinition(name,(Object)value);
	}

	public void addVariableDefinition(QName name, Element value) {
		addVariableDefinition(name,(Object)value);
	}
	
	public void addVariableDefinition(QName name, PrismValue value) {
		addVariableDefinition(name,(Object)value);
	}
	
	public void addVariableDefinition(QName name, ObjectDeltaObject<?> value) {
		addVariableDefinition(name,(Object)value);
	}

	public void addVariableDefinitions(Map<QName, Object> extraVariables) {
		variables.addVariableDefinitions(extraVariables);
	}
	
	public void addVariableDefinition(QName name, Object value) {
		variables.addVariableDefinition(name, value);
	}
	
	public boolean hasVariableDefinition(QName varName) {
		return variables.containsKey(varName);
	}
	
	public MappingStrengthType getStrength() {
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
	
	public boolean isConditionMaskOld() {
		return conditionMaskOld;
	}

	public void setConditionMaskOld(boolean conditionMaskOld) {
		this.conditionMaskOld = conditionMaskOld;
	}

	public boolean isConditionMaskNew() {
		return conditionMaskNew;
	}

	public void setConditionMaskNew(boolean conditionMaskNew) {
		this.conditionMaskNew = conditionMaskNew;
	}
	
	private PrismContext getPrismContext() {
		return outputDefinition.getPrismContext();
	}

	public FilterManager<Filter> getFilterManager() {
		return filterManager;
	}

	public void setFilterManager(FilterManager<Filter> filterManager) {
		this.filterManager = filterManager;
	}
	
	public StringPolicyResolver getStringPolicyResolver() {
		return stringPolicyResolver;
	}

	public void setStringPolicyResolver(StringPolicyResolver stringPolicyResolver) {
		this.stringPolicyResolver = stringPolicyResolver;
	}
	
	public boolean isApplicableToChannel(String channelUri) {
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

	public void setNow(XMLGregorianCalendar now) {
		this.now = now;
	}

	public XMLGregorianCalendar getDefaultReferenceTime() {
		return defaultReferenceTime;
	}

	public void setDefaultReferenceTime(XMLGregorianCalendar defaultReferenceTime) {
		this.defaultReferenceTime = defaultReferenceTime;
	}

	public XMLGregorianCalendar getNextRecomputeTime() {
		return nextRecomputeTime;
	}

	public void setNextRecomputeTime(XMLGregorianCalendar nextRecomputeTime) {
		this.nextRecomputeTime = nextRecomputeTime;
	}

	public boolean isProfiling() {
		return profiling;
	}

	public void setProfiling(boolean profiling) {
		this.profiling = profiling;
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

	public QName getMappingQName() {
		return mappingQName;
	}

	public void setMappingQName(QName mappingQName) {
		this.mappingQName = mappingQName;
	}

	public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
		return refinedObjectClassDefinition;
	}

	public void setRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
		this.refinedObjectClassDefinition = refinedObjectClassDefinition;
	}

	public void evaluate(Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		OperationResult result = parentResult.createMinorSubresult(Mapping.class.getName()+".evaluate");
		
		traceEvaluationStart();
		
		try {
			evaluateTimeConstraintValid(result);
			
			if (!timeConstraintValid) {
				outputTriple = null;
				result.recordNotApplicableIfUnknown();
				traceDeferred();
				return;
			}
			
			parseSources(result);
			parseTarget();
	
			if (outputDefinition == null) {
				throw new IllegalArgumentException("No output definition, cannot evaluate "+getMappingContextDescription());
			}
			
			evaluateCondition(task, result);
			
			boolean conditionOutputOld = computeConditionResult(conditionOutputTriple.getNonPositiveValues());
			boolean conditionResultOld = conditionOutputOld && conditionMaskOld;
			
			boolean conditionOutputNew = computeConditionResult(conditionOutputTriple.getNonNegativeValues());
			boolean conditionResultNew = conditionOutputNew && conditionMaskNew;
			
			if (!conditionResultOld && !conditionResultNew) {
				result.recordSuccess();
				traceSuccess(conditionResultOld, conditionResultNew);
				return;
			}
			// TODO: input filter
			evaluateExpression(task, result, conditionResultOld, conditionResultNew);
			fixDefinition();
			recomputeValues();
			setOrigin();
			// TODO: output filter
			
			result.recordSuccess();
			traceSuccess(conditionResultOld, conditionResultNew);
			
		} catch (ExpressionEvaluationException e) {
			result.recordFatalError(e);
			traceFailure(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			traceFailure(e);
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError(e);
			traceFailure(e);
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			traceFailure(e);
			throw e;
		}
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
		if (!LOGGER.isTraceEnabled()) {
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
		LOGGER.trace(sb.toString());
	}
	
	private void traceDeferred() {
		traceEvaluationEnd();
		if (!LOGGER.isTraceEnabled()) {
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
		LOGGER.trace(sb.toString());
	}
	
	private void traceFailure(Exception e) {
		LOGGER.error("Error evaluating {}: {}", new Object[]{getMappingContextDescription(), e.getMessage(), e});
		traceEvaluationEnd();
		if (!LOGGER.isTraceEnabled()) {
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
		LOGGER.trace(sb.toString());
	}

	private void appendTraceHeader(StringBuilder sb) {
		sb.append("---[ MAPPING ");
		if (mappingType.getName() != null) {
			sb.append("'").append(mappingType.getName()).append("' ");
		}
		sb.append(" in ");
		sb.append(contextDescription);
		sb.append("]---------------------------");
		for (Source<?> source: sources) {
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
		if (booleanPropertyValues == null || booleanPropertyValues.isEmpty()) {
			// No value means false
			return false;
		}
		boolean hasFalse = false;
		for (PrismPropertyValue<Boolean> pval: booleanPropertyValues) {
			Boolean value = pval.getValue();
			if (Boolean.TRUE.equals(value)) {
				return true;
			}
			if (Boolean.FALSE.equals(value)) {
				hasFalse = true;
			}
		}
		if (hasFalse) {
			return false;
		}
		// No value or all values null. Return default.
		return true;
	}

	public Boolean evaluateTimeConstraintValid(OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (timeConstraintValid == null) {
			parseTimeConstraints(result);
		}
		return timeConstraintValid;
	}

	private void parseTimeConstraints(OperationResult result) throws SchemaException, ObjectNotFoundException {
		MappingTimeDeclarationType timeFromType = mappingType.getTimeFrom();
		MappingTimeDeclarationType timeToType = mappingType.getTimeTo();
		if (timeFromType == null && timeToType == null) {
			timeConstraintValid = true;
			return;
		}
		
		XMLGregorianCalendar timeFrom = parseTime(timeFromType, result);
		if (timeFrom == null && timeFromType != null) {
			// Time is specified but there is no value for it.
			// This means that event that should start validity haven't happened yet
			// therefore the mapping is not yet valid. 
			timeConstraintValid = false;
			return;
		}
		XMLGregorianCalendar timeTo = parseTime(timeToType, result);
		
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
	
	private XMLGregorianCalendar parseTime(MappingTimeDeclarationType timeType, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (timeType == null) {
			return null;
		}
		XMLGregorianCalendar time = null;
		MappingSourceDeclarationType referenceTimeType = timeType.getReferenceTime();
		if (referenceTimeType == null) {
			if (time == null) {
				throw new SchemaException("No reference time specified (and there is also no default) in time specification in "+getMappingContextDescription());
			} else {
				time = (XMLGregorianCalendar) defaultReferenceTime.clone();
			}
		} else {
			time = parseTimeSource(referenceTimeType, result);
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

	private XMLGregorianCalendar parseTimeSource(MappingSourceDeclarationType sourceType, OperationResult result) throws SchemaException, ObjectNotFoundException {
		ItemPathType itemPathType = sourceType.getPath();
		if (itemPathType == null) {
			throw new SchemaException("No path in source definition in "+getMappingContextDescription());
		}
		ItemPath path = itemPathType.getItemPath();
		if (path.isEmpty()) {
			throw new SchemaException("Empty source path in "+getMappingContextDescription());
		}
		
		Object sourceObject = ExpressionUtil.resolvePath(path, variables, sourceContext, objectResolver, "reference time definition in "+getMappingContextDescription(), result);
		if (sourceObject == null) {
			return null;
		}
		PrismProperty<XMLGregorianCalendar> timeProperty;
		if (sourceObject instanceof ItemDeltaItem<?>) {
			timeProperty = (PrismProperty<XMLGregorianCalendar>) ((ItemDeltaItem<?>)sourceObject).getItemNew();
		} else if (sourceObject instanceof Item<?>) {
			timeProperty = (PrismProperty<XMLGregorianCalendar>) sourceObject;
		} else {
			throw new IllegalStateException("Unknown resolve result "+sourceObject);
		}
		if (timeProperty == null) {
			return null;
		}
		return timeProperty.getRealValue();
	}

	private Collection<Source<?>> parseSources(OperationResult result) throws SchemaException, ObjectNotFoundException {
		List<MappingSourceDeclarationType> sourceTypes = mappingType.getSource();
		if (defaultSource != null) {
			defaultSource.recompute();
			sources.add(defaultSource);
			defaultSource.recompute();
		}
		if (sourceTypes != null) {
			for (MappingSourceDeclarationType sourceType: sourceTypes) {
				Source<?> source = parseSource(sourceType, result);
				source.recompute();
				
				// Override existing sources (e.g. default source)
				Iterator<Source<?>> iterator = sources.iterator();
				while (iterator.hasNext()) {
					Source<?> next = iterator.next();
					if (next.getName().equals(source.getName())) {
						iterator.remove();
					}
				}
				
				sources.add(source);
			}
		}
		return sources;
	}

	private <X extends PrismValue> Source<X> parseSource(MappingSourceDeclarationType sourceType, OperationResult result) throws SchemaException, ObjectNotFoundException {
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
		Object sourceObject = ExpressionUtil.resolvePath(path, variables, sourceContext, objectResolver, "source definition in "+getMappingContextDescription(), result);
		Item<X> itemOld = null;
		ItemDelta<X> delta = null;
		Item<X> itemNew = null;
		ItemPath residualPath = null;
		if (sourceObject != null) {
			if (sourceObject instanceof ItemDeltaItem<?>) {
				itemOld = ((ItemDeltaItem<X>)sourceObject).getItemOld();
				delta = ((ItemDeltaItem<X>)sourceObject).getDelta();
				itemNew = ((ItemDeltaItem<X>)sourceObject).getItemNew();
				residualPath = ((ItemDeltaItem<X>)sourceObject).getResidualPath();
				resolvePath = ((ItemDeltaItem<X>)sourceObject).getResolvePath();
			} else if (sourceObject instanceof Item<?>) {
				itemOld = (Item<X>) sourceObject;
				itemNew = (Item<X>) sourceObject;
			} else {
				throw new IllegalStateException("Unknown resolve result "+sourceObject);
			}
		}
		Source<X> source = new Source<X>(itemOld, delta, itemNew, name);
		source.setResidualPath(residualPath);
		source.setResolvePath(resolvePath);
		return source;
	}
	
	private void parseTarget() throws SchemaException {
		MappingTargetDeclarationType targetType = mappingType.getTarget();
		if (targetType == null) {
			outputDefinition = defaultTargetDefinition;
			outputPath = defaultTargetPath;
		} else {
			ItemPathType itemPathType = targetType.getPath();
			if (itemPathType == null) {
				throw new SchemaException("No path in target definition in "+getMappingContextDescription());
			}
			ItemPath path = itemPathType.getItemPath();
			outputDefinition = ExpressionUtil.resolveDefinitionPath(path, variables, targetContext, "target definition in "+getMappingContextDescription());
			if (outputDefinition == null) {
				throw new SchemaException("No target item that would conform to the path "+path+" in "+getMappingContextDescription());
			}

			// Make the path relative if needed
			if (!path.isEmpty() && (path.first() instanceof NameItemPathSegment) && 
					((NameItemPathSegment)path.first()).isVariable()) {
				outputPath = path.rest();
			} else {
				outputPath = path;
			}
		}
		if (stringPolicyResolver != null) {
			stringPolicyResolver.setOutputDefinition(outputDefinition);
			stringPolicyResolver.setOutputPath(outputPath);
		}
	}
	
	public ItemDefinition getOutputDefinition() throws SchemaException {
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
		ItemDefinition conditionOutput = new PrismPropertyDefinition(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN, expressionFactory.getPrismContext());
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(conditionExpressionType, 
				conditionOutput, "condition in "+getMappingContextDescription(), result);
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
				"expression in "+getMappingContextDescription(), result);
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
			return;
		}
		
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

	public Item<V> getOutput() throws SchemaException {
		if (outputTriple == null) {
			return null;
		}
		Item<V> output = outputDefinition.instantiate();
		output.addAll(PrismValue.cloneCollection(outputTriple.getNonNegativeValues()));
		return output;
	}
	
	public ItemDelta<V> createEmptyDelta(ItemPath path) {
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
	public Mapping<V> clone() {
		Mapping<V> clone = new Mapping<V>(mappingType, contextDescription, expressionFactory);
		clone.conditionMaskNew = this.conditionMaskNew;
		clone.conditionMaskOld = this.conditionMaskOld;
		if (this.conditionOutputTriple != null) {
			clone.conditionOutputTriple = this.conditionOutputTriple.clone();
		}
		clone.defaultSource = this.defaultSource;
		clone.defaultTargetDefinition = this.defaultTargetDefinition;
		clone.expressionFactory = this.expressionFactory;
		clone.mappingType = this.mappingType;
		clone.objectResolver = this.objectResolver;
		clone.originObject = this.originObject;
		clone.originType = this.originType;
		clone.outputDefinition = this.outputDefinition;
		clone.outputPath = this.outputPath;
		if (this.outputTriple != null) {
			clone.outputTriple = this.outputTriple.clone();
		}
		clone.contextDescription = this.contextDescription;
		clone.sourceContext = this.sourceContext;
		clone.sources = this.sources;
		clone.targetContext = this.targetContext;
		clone.variables = this.variables;
		
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
		return "M(" + getMappingDisplayName() + " = " + outputTriple + toStringStrength() + ")";
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
	
}
