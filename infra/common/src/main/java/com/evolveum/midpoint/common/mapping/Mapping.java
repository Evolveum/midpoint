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
import com.evolveum.midpoint.common.expression.ExpressionEvaluationParameters;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.ExpressionUtil;
import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
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
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.SourceType;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
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
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingSourceDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingTargetDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValueFilterType;

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
	private MappingType mappingType;
	private ObjectResolver objectResolver = null;
	private Source<?> defaultSource = null;
	private ItemDefinition defaultTargetDefinition = null;
	private ObjectDeltaObject<?> sourceContext = null;
	private PrismObjectDefinition<?> targetContext = null;
	private PrismValueDeltaSetTriple<V> outputTriple = null;
	private ItemDefinition outputDefinition;
	private PropertyPath outputPath;
	private Collection<Source<?>> sources;
	private boolean conditionMaskOld = true;
	private boolean conditionMaskNew = true;
	private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionOutputTriple;
	private SourceType originType = null;
	private ObjectType originObject = null;
	private FilterManager<Filter> filterManager;
	private StringPolicyResolver stringPolicyResolver;
	
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
	
	public SourceType getOriginType() {
		return originType;
	}

	public void setOriginType(SourceType sourceType) {
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
			LOGGER.warn("Empty definition of variable {} in expression {}, ignoring it",varDef.getName(),contextDescription);
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
			LOGGER.warn("Duplicate definition of variable {} in expression {}, ignoring it",name,contextDescription);
			return;
		}
		variables.put(name, value);
	}
	
	public boolean hasVariableDefinition(QName varName) {
		return variables.containsKey(varName);
	}
	
	public boolean isInitial() {
		if (mappingType == null) {
			return false;
		}
		Boolean value = mappingType.isInitial();
		if (value == null) {
			value = false;
		}
		return value;
	}

	public boolean isAuthoritative() {
		if (mappingType == null) {
			return false;
		}
		Boolean value = mappingType.isAuthoritative();
		if (value == null) {
			value = false;
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

	public void evaluate(OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {		
		sources = parseSources(result);
		parseTarget();

		if (outputDefinition == null) {
			throw new IllegalArgumentException("No output definition, cannot evaluate construction "+contextDescription);
		}
		
		evaluateCondition(result);
		
		boolean conditionOutputOld = computeBoolean(conditionOutputTriple.getNonPositiveValues());
		boolean conditionResultOld = conditionOutputOld && conditionMaskOld;
		
		boolean conditionOutputNew = computeBoolean(conditionOutputTriple.getNonNegativeValues());
		boolean conditionResultNew = conditionOutputNew && conditionMaskNew;
		
		if (!conditionResultOld && !conditionResultNew) {
			// TODO: tracing
			return;
		}
		// TODO: input filter
		evaluateExpression(result, conditionResultOld, conditionResultNew);
		fixDefinition();
		recomputeValues();
		setOrigin();
		// TODO: output filter
	}
	
	private boolean computeBoolean(Collection<PrismPropertyValue<Boolean>> booleanPropertyValues) {
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
			throw new SchemaException("No path in source definition in "+contextDescription);
		}
		PropertyPath path = new XPathHolder(pathElement).toPropertyPath();
		if (path.isEmpty()) {
			throw new SchemaException("Empty source path in "+contextDescription);
		}
		QName name = sourceType.getName();
		if (name == null) {
			name = path.last().getName();
		}
		Object sourceObject = ExpressionUtil.resolvePath(path, variables, sourceContext, objectResolver, "source definition in "+contextDescription, result);
		Item<X> itemOld = null;
		ItemDelta<X> delta = null;
		Item<X> itemNew = null;
		if (sourceObject != null) {
			if (sourceObject instanceof ItemDeltaItem<?>) {
				itemOld = ((ItemDeltaItem<X>)sourceObject).getItemOld();
				delta = ((ItemDeltaItem<X>)sourceObject).getDelta();
				itemNew = ((ItemDeltaItem<X>)sourceObject).getItemNew();
			} else if (sourceObject instanceof Item<?>) {
				itemOld = (Item<X>) sourceObject;
				itemNew = (Item<X>) sourceObject;
			} else {
				throw new IllegalStateException("Unknown resolve result "+sourceObject);
			}
		}
		Source<X> source = new Source<X>(itemOld, delta, itemNew, name);
		return source;
	}
	
	private void parseTarget() throws SchemaException {
		MappingTargetDeclarationType targetType = mappingType.getTarget();
		if (targetType == null) {
			outputDefinition = defaultTargetDefinition;
		} else {
			Element pathElement = targetType.getPath();
			if (pathElement == null) {
				throw new SchemaException("No path in target definition in "+contextDescription);
			}
			PropertyPath path = new XPathHolder(pathElement).toPropertyPath();
			outputDefinition = ExpressionUtil.resolveDefinitionPath(path, variables, targetContext, "target definition in "+contextDescription);
			if (outputDefinition == null) {
				throw new SchemaException("No target item that would comform to the path "+path+" in "+getContextDescription());
			}

			// Make the path relative if needed
			if (!path.isEmpty() && path.first().isVariable()) {
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
	
	public PropertyPath getOutputPath() throws SchemaException {
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
		Expression<PrismValue> expression = expressionFactory.makeExpression(conditionExpressionType, conditionOutput, "condition in "+contextDescription);
		ExpressionEvaluationParameters params = new ExpressionEvaluationParameters(sources, variables, "condition in "+contextDescription, result);
		params.setStringPolicyResolver(stringPolicyResolver);
		conditionOutputTriple = expression.evaluate(params);
	}

	
	private void evaluateExpression(OperationResult result, boolean conditionResultOld, boolean conditionResultNew) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ExpressionType expressionType = null;
		if (mappingType != null) {
			expressionType = mappingType.getExpression();
		}
		Expression<PrismValue> expression = expressionFactory.makeExpression(expressionType, outputDefinition, "expression in "+contextDescription);
		ExpressionEvaluationParameters params = new ExpressionEvaluationParameters(sources, variables, "expression in "+contextDescription, result);
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
	
	public ItemDelta<V> createEmptyDelta(PropertyPath path) {
		return outputDefinition.createEmptyDelta(path);
	}
	
	public static List<Object> getStaticValueList(ExpressionType valueConstruction) throws SchemaException {
		JAXBElement<?> expressionEvaluatorElement = valueConstruction.getExpressionEvaluator();
		if (expressionEvaluatorElement == null) {
			return null;
		}
		if (!expressionEvaluatorElement.getName().equals(SchemaConstants.C_VALUE)) {
			throw new IllegalArgumentException("Expected static value constructor but found "+expressionEvaluatorElement.getName()+" in value construction");
		}
		Element element = (Element)expressionEvaluatorElement.getValue();
		return XmlTypeConverter.convertValueElementAsList(element);
	}
	
	private <T> PrismPropertyValue<T> filterValue(PrismPropertyValue<T> propertyValue, List<ValueFilterType> filters) {
        PrismPropertyValue<T> filteredValue = propertyValue.clone();
        filteredValue.setOriginType(SourceType.INBOUND);

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
		return "M(" + SchemaDebugUtil.prettyPrint(outputDefinition.getName()) + " = " + outputTriple + ")";
	}
	
}
