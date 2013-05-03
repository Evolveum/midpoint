/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.ExpressionUtil;
import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingSourceDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingTargetDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValueFilterType;

/**
 * 
 * Mapping is non-recyclable single-use object. Once evaluated it should not be evaluated again. It will retain its original
 * inputs and outputs that can be read again and again. But these should not be changed after evaluation.
 * 
 * @author Radovan Semancik
 *
 */
public class Mapping<V extends PrismValue> implements Dumpable, DebugDumpable {
	
	private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");
	
	private ExpressionFactory expressionFactory;
	private Map<QName,Object> variables = new HashMap<QName,Object>();
	private String contextDescription;
	private String mappingContextDescription = null;
	private MappingType mappingType;
	private ObjectResolver objectResolver = null;
	private Source<?> defaultSource = null;
	private ItemDefinition defaultTargetDefinition = null;
	private ObjectDeltaObject<?> sourceContext = null;
	private PrismObjectDefinition<?> targetContext = null;
	private PrismValueDeltaSetTriple<V> outputTriple = null;
	private ItemDefinition outputDefinition;
	private ItemPath outputPath;
	private Collection<Source<?>> sources;
	private boolean conditionMaskOld = true;
	private boolean conditionMaskNew = true;
	private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionOutputTriple;
	private OriginType originType = null;
	private ObjectType originObject = null;
	private FilterManager<Filter> filterManager;
	private StringPolicyResolver stringPolicyResolver;
	
	// This is single-use only. Once evaluated it is not used any more
	// it is remembered only for tracing purposes.
	private Expression<V> expression;
	
	private static final Trace LOGGER = TraceManager.getTrace(Mapping.class);
	
	Mapping(MappingType mappingType, String contextDescription, ExpressionFactory expressionFactory) {
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
			sb.append(" in ");
			sb.append(contextDescription);
			mappingContextDescription  = sb.toString();
		}
		return mappingContextDescription;
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
		for (Entry<QName, Object> entry : extraVariables.entrySet()) {
			variables.put(entry.getKey(), entry.getValue());
		}
	}
	
	private void addVariableDefinition(QName name, Object value) {
		if (variables.containsKey(name)) {
			LOGGER.warn("Duplicate definition of variable {} in {}, ignoring it",name,getMappingContextDescription());
			return;
		}
		variables.put(name, value);
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
		List<String> applicableChannels = mappingType.getChannel();
		if (applicableChannels == null || applicableChannels.isEmpty()) {
			return true;
		}
		return applicableChannels.contains(channelUri);
	}

	public void evaluate(OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		OperationResult result = parentResult.createMinorSubresult(Mapping.class.getName()+".evaluate");
		
		try {
			sources = parseSources(result);
			parseTarget();
	
			if (outputDefinition == null) {
				throw new IllegalArgumentException("No output definition, cannot evaluate "+getMappingContextDescription());
			}
			
			evaluateCondition(result);
			
			boolean conditionOutputOld = computeConditionResult(conditionOutputTriple.getNonPositiveValues());
			boolean conditionResultOld = conditionOutputOld && conditionMaskOld;
			
			boolean conditionOutputNew = computeConditionResult(conditionOutputTriple.getNonNegativeValues());
			boolean conditionResultNew = conditionOutputNew && conditionMaskNew;
			
			if (!conditionResultOld && !conditionResultNew) {
				traceSuccess(conditionResultOld, conditionResultNew);
				return;
			}
			// TODO: input filter
			evaluateExpression(result, conditionResultOld, conditionResultNew);
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
	
	private void traceSuccess(boolean conditionResultOld, boolean conditionResultNew) {
		if (!LOGGER.isTraceEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Mapping trace:\n");
		appendTraceHeader(sb);
		sb.append("\nCondition: ").append(conditionResultOld).append(" -> ").append(conditionResultNew);
		sb.append("\nResult: ");
		if (outputTriple == null) {
			sb.append("null");
		} else {
			sb.append(outputTriple.toHumanReadableString());
		}
		appendTraceFooter(sb);
		LOGGER.trace(sb.toString());
	}
	
	private void traceFailure(Exception e) {
		LOGGER.error("Error evaluating {}: {}", new Object[]{getMappingContextDescription(), e.getMessage(), e});
		if (!LOGGER.isTraceEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Mapping failure:\n");
		appendTraceHeader(sb);
		sb.append("\nERROR: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
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
			if (value == Boolean.TRUE) {
				return true;
			}
			if (value == Boolean.FALSE) {
				hasFalse = true;
			}
		}
		if (hasFalse) {
			return false;
		}
		// No value or all values null. Return default.
		return true;
	}

	private Collection<Source<?>> parseSources(OperationResult result) throws SchemaException, ObjectNotFoundException {
		Collection<Source<?>> sources = new ArrayList<Source<?>>();
		List<MappingSourceDeclarationType> sourceTypes = mappingType.getSource();
		if (sourceTypes == null || sourceTypes.isEmpty()) {
			if (defaultSource != null) {
				defaultSource.recompute();
				sources.add(defaultSource);
			}
			return sources;
		} else {
			for (MappingSourceDeclarationType sourceType: sourceTypes) {
				Source<?> source = parseSource(sourceType, result);
				source.recompute();
				sources.add(source);
			}
		}
		return sources;
	}

	private <X extends PrismValue> Source<X> parseSource(MappingSourceDeclarationType sourceType, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Element pathElement = sourceType.getPath();
		if (pathElement == null) {
			throw new SchemaException("No path in source definition in "+getMappingContextDescription());
		}
		ItemPath path = new XPathHolder(pathElement).toItemPath();
		if (path.isEmpty()) {
			throw new SchemaException("Empty source path in "+getMappingContextDescription());
		}
		QName name = sourceType.getName();
		if (name == null) {
			name = ItemPath.getName(path.last());
		}
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
			} else if (sourceObject instanceof Item<?>) {
				itemOld = (Item<X>) sourceObject;
				itemNew = (Item<X>) sourceObject;
			} else {
				throw new IllegalStateException("Unknown resolve result "+sourceObject);
			}
		}
		Source<X> source = new Source<X>(itemOld, delta, itemNew, name);
		source.setResidualPath(residualPath);
		return source;
	}
	
	private void parseTarget() throws SchemaException {
		MappingTargetDeclarationType targetType = mappingType.getTarget();
		if (targetType == null) {
			outputDefinition = defaultTargetDefinition;
		} else {
			Element pathElement = targetType.getPath();
			if (pathElement == null) {
				throw new SchemaException("No path in target definition in "+getMappingContextDescription());
			}
			ItemPath path = new XPathHolder(pathElement).toItemPath();
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
	
	private void evaluateCondition(OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ExpressionType conditionExpressionType = mappingType.getCondition();
		if (conditionExpressionType == null) {
			// True -> True
			conditionOutputTriple = new PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>>();
			conditionOutputTriple.addToZeroSet(new PrismPropertyValue<Boolean>(Boolean.TRUE));
			return;
		}
		ItemDefinition conditionOutput = new PrismPropertyDefinition(CONDITION_OUTPUT_NAME, null, DOMUtil.XSD_BOOLEAN, expressionFactory.getPrismContext());
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(conditionExpressionType, 
				conditionOutput, "condition in "+getMappingContextDescription(), result);
		ExpressionEvaluationContext params = new ExpressionEvaluationContext(sources, variables, "condition in "+getMappingContextDescription(), result);
		params.setStringPolicyResolver(stringPolicyResolver);
		conditionOutputTriple = expression.evaluate(params);
	}

	
	private void evaluateExpression(OperationResult result, boolean conditionResultOld, boolean conditionResultNew) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ExpressionType expressionType = null;
		if (mappingType != null) {
			expressionType = mappingType.getExpression();
		}
		expression = expressionFactory.makeExpression(expressionType, outputDefinition, 
				"expression in "+getMappingContextDescription(), result);
		ExpressionEvaluationContext params = new ExpressionEvaluationContext(sources, variables, "expression in "+getMappingContextDescription(), result);
		params.setRegress(!conditionResultNew);
		params.setStringPolicyResolver(stringPolicyResolver);
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

	
	public static <X> PrismProperty<X> getPropertyStatic(ExpressionType expressionType, PrismPropertyDefinition outputDefinition, 
			String contextDescription, PrismContext prismContext) throws SchemaException {
		Collection<JAXBElement<?>> expressionEvaluatorElement = expressionType.getExpressionEvaluator();
		return (PrismProperty) LiteralExpressionEvaluatorFactory.parseValueElements(expressionEvaluatorElement, outputDefinition, contextDescription, prismContext);
	}

	/**
	 * Always returns collection, even for single-valued results. 
	 */
	public static <X> Collection<X> getPropertyStaticRealValues(ExpressionType expressionType, PrismPropertyDefinition outputDefinition, 
			String contextDescription, PrismContext prismContext) throws SchemaException {
		PrismProperty<X> output = getPropertyStatic(expressionType, outputDefinition, contextDescription, prismContext);
		return output.getRealValues();
	}
	
	/**
	 * Returns either Object (if result is supposed to be single-value) or Collection<X> (if result is supposed to be multi-value) 
	 */
	public static Object getStaticOutput(ExpressionType expressionType, PrismPropertyDefinition outputDefinition, 
			String contextDescription, ExpressionReturnMultiplicityType preferredMultiplicity, PrismContext prismContext) throws SchemaException {
		PrismProperty<?> output = getPropertyStatic(expressionType, outputDefinition, contextDescription, prismContext);
		ExpressionReturnMultiplicityType multiplicity = preferredMultiplicity;
		if (expressionType.getReturnMultiplicity() != null) {
			multiplicity = expressionType.getReturnMultiplicity();
		} else if (output.size() > 1) {
			multiplicity = ExpressionReturnMultiplicityType.MULTI;
		}
		switch (multiplicity) {
			case MULTI: return output.getRealValues();
			case SINGLE: return output.getRealValue();
			default: throw new IllegalStateException("Unknown return type "+multiplicity);
		}
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
	public String dump() {
		return debugDump();
	}

	@Override
	public String toString() {
		return "M(" + SchemaDebugUtil.prettyPrint(outputDefinition.getName()) + " = " + outputTriple + toStringStrength() + ")";
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
