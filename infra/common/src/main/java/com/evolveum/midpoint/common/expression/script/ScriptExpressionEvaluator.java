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
package com.evolveum.midpoint.common.expression.script;

import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.common.expression.ExpressionUtil;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionRelativityModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionReturnTypeType;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Radovan Semancik
 */
public class ScriptExpressionEvaluator<V extends PrismValue> implements ExpressionEvaluator<V> {

	private ScriptExpressionEvaluatorType scriptType;
	private ScriptExpression scriptExpression;
	
	private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionEvaluator.class);

    ScriptExpressionEvaluator(ScriptExpressionEvaluatorType scriptType, ScriptExpression scriptExpression) {
    	this.scriptType = scriptType;
        this.scriptExpression = scriptExpression;
    }

    /* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java.util.Collection, java.util.Map, boolean, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
		
        PrismValueDeltaSetTriple<V> outputTriple = new PrismValueDeltaSetTriple<V>();
        	    
        if (scriptType.getRelativityMode() == ScriptExpressionRelativityModeType.ABSOLUTE) {
        	
        	outputTriple = evaluateAbsoluteExpression(context.getSources(), context.getVariables(), context.getContextDescription(), context.getResult());
        
        } else if (scriptType.getRelativityMode() == null || scriptType.getRelativityMode() == ScriptExpressionRelativityModeType.RELATIVE) {
        	
        	if (context.getSources() == null || context.getSources().isEmpty()) {
        		// Special case. No sources, so there will be no input variables and no combinations. Everything goes to zero set.
        		outputTriple = evaluateAbsoluteExpression(null, context.getVariables(), context.getContextDescription(), context.getResult());
        	} else {
        		List<SourceTriple<? extends PrismValue>> sourceTriples = processSources(context.getSources(), scriptType.isIncludeNullInputs());
        		outputTriple = evaluateRelativeExpression(sourceTriples, context.getVariables(), context.isSkipEvaluationMinus(), context.isSkipEvaluationPlus(), 
        				scriptType.isIncludeNullInputs(), context.getContextDescription(), context.getResult());
        	}
        	
        } else {
        	throw new IllegalArgumentException("Unknown relativity mode "+scriptType.getRelativityMode());
        }
	        
        
        return outputTriple;        
    }
	
	private List<SourceTriple<? extends PrismValue>> processSources(Collection<Source<? extends PrismValue>> sources, Boolean includeNulls) {
		List<SourceTriple<? extends PrismValue>> sourceTriples = 
			new ArrayList<SourceTriple<? extends PrismValue>>(sources == null ? 0 : sources.size());
		if (sources == null) {
			return sourceTriples;
		}
		for (Source<? extends PrismValue> source: sources) {
			SourceTriple<? extends PrismValue> sourceTriple = new SourceTriple<PrismValue>((Source<PrismValue>) source);
			ItemDelta<? extends PrismValue> delta = source.getDelta();
			if (delta != null) {
				sourceTriple.merge((DeltaSetTriple) delta.toDeltaSetTriple((Item) source.getItemOld()));
			} else {
				if (source.getItemOld() != null) {
					sourceTriple.addAllToZeroSet((Collection)source.getItemOld().getValues());
				}
			}
			if (includeNulls == null || includeNulls) {
				// Make sure that we properly handle the "null" states, i.e. the states when we enter
				// "empty" value and exit "empty" value for a property
				// We need this to properly handle "negative" expressions, i.e. expressions that return non-null
				// value for null input. We need to make sure such expressions receive the null input when needed
				Item<? extends PrismValue> itemOld = source.getItemOld();
				Item<? extends PrismValue> itemNew = source.getItemNew();
				if (itemOld == null || itemOld.isEmpty()) {
					if (!(itemNew == null || itemNew.isEmpty())) { 
						// change empty -> non-empty: we are removing "null" value
						sourceTriple.addToMinusSet(null);
					} else if (sourceTriple.hasMinusSet()) {
						// special case: change empty -> empty, but there is still a delete delta
						// so it seems something was deleted. This is strange case, but we prefer the delta over
						// the absolute states (which may be out of date).
						// Similar case than that of non-empty -> empty (see below)
						sourceTriple.addToPlusSet(null);
					}
				} else {
					if (itemNew == null || itemNew.isEmpty()) {
						// change non-empty -> empty: we are adding "null" value
						sourceTriple.addToPlusSet(null);
					}
				}
			}
			sourceTriples.add(sourceTriple);
			LOGGER.trace("Processes source tripple\n{}",sourceTriple.dump());
		}
		return sourceTriples;
	}

	private PrismValueDeltaSetTriple<V> evaluateAbsoluteExpression(Collection<Source<? extends PrismValue>> sources,
			Map<QName, Object> variables, String contextDescription, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		Collection<V> outputSetOld = evaluateScriptExpression(sources, variables, contextDescription, false, result);
		Collection<V> outputSetNew = evaluateScriptExpression(sources, variables, contextDescription, true, result);
		
		PrismValueDeltaSetTriple<V> outputTriple = PrismValueDeltaSetTriple.diffPrismValueDeltaSetTriple(outputSetOld, outputSetNew);
		return outputTriple;
	}
	
	private Collection<V> evaluateScriptExpression(Collection<Source<? extends PrismValue>> sources,
			Map<QName, Object> variables, String contextDescription, boolean useNew, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		ScriptVariables scriptVariables = new ScriptVariables();
		if (useNew) {
			scriptVariables.addVariableDefinitionsNew(variables);
		} else {
			scriptVariables.addVariableDefinitionsOld(variables);
		}
		
		if (sources != null) {
			// Add sources to variables
			for (Source<? extends PrismValue> source: sources) {
				LOGGER.trace("source: {}", source);
				QName name = source.getName();
				if (name == null) {
					if (sources.size() == 1) {
						name = ExpressionConstants.VAR_INPUT;
					} else {
						throw new ExpressionSyntaxException("No name definition for source in "+contextDescription);
					}
				}
				
				Object value = null;
				if (useNew) {
					value = getRealContent(source.getItemNew(), source.getResidualPath());
				} else {
					value = getRealContent(source.getItemOld(), source.getResidualPath());
				}
				scriptVariables.addVariableDefinition(name, value);
			}
		}
		
		List<PrismPropertyValue<Object>> scriptResults = scriptExpression.evaluate(scriptVariables, null, useNew,
				(useNew ? "(new) " : "(old) " ) + contextDescription, result);
		
		if (scriptResults == null || scriptResults.isEmpty()) {
			return null;
		}
		
		Collection<V> outputSet = new ArrayList<V>(scriptResults.size());
		for (PrismPropertyValue<Object> pval: scriptResults) {
			if (pval == null || pval.getValue() == null) {
				continue;
			}
			Object realValue = pval.getValue();
			if (realValue instanceof String) {
				if (((String)realValue).isEmpty()) {
					continue;
				}
			}
			if (realValue instanceof PolyString) {
				if (((PolyString)realValue).isEmpty()) {
					continue;
				}
			}
			outputSet.add((V) pval);
		}
		
		return outputSet;
	}
	
	private Object getRealContent(Item<? extends PrismValue> item, ItemPath residualPath) {
		if (residualPath == null || residualPath.isEmpty()) {
			return item;
		}
		return item.find(residualPath);
	}
	
	private Object getRealContent(PrismValue pval, ItemPath residualPath) {
		if (residualPath == null || residualPath.isEmpty()) {
			return pval;
		}
		return pval.find(residualPath);
	}

	private PrismValueDeltaSetTriple<V> evaluateRelativeExpression(final List<SourceTriple<? extends PrismValue>> sourceTriples,
			final Map<QName, Object> variables, final boolean skipEvaluationMinus, final boolean skipEvaluationPlus, 
			final Boolean includeNulls, final String contextDescription, final OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		List<Collection<? extends PrismValue>> valueCollections = new ArrayList<Collection<? extends PrismValue>>(sourceTriples.size());
		for (SourceTriple<? extends PrismValue> sourceTriple: sourceTriples) {
			Collection<? extends PrismValue> values = sourceTriple.union();
			if (values.isEmpty()) {
				// No values for this source. Add null instead. It will make sure that the expression will
				// be evaluate at least once.
				values.add(null);
			}
			valueCollections.add(values);
		}
		
		final PrismValueDeltaSetTriple<V> outputTriple = new PrismValueDeltaSetTriple<V>();
		
		Processor<Collection<? extends PrismValue>> processor = new Processor<Collection<? extends PrismValue>>() {
			@Override
			public void process(Collection<? extends PrismValue> pvalues) {
				if (includeNulls != null && !includeNulls && MiscUtil.isAllNull(pvalues)) {
					// The case that all the sources are null. There is no point executing the expression.
					return;
				}
				Map<QName, Object> sourceVariables = new HashMap<QName, Object>();
				Iterator<SourceTriple<? extends PrismValue>> sourceTriplesIterator = sourceTriples.iterator();
				boolean hasMinus = false;
				boolean hasZero = false;
				boolean hasPlus = false;
				for (PrismValue pval: pvalues) {
					SourceTriple<PrismValue> sourceTriple = (SourceTriple<PrismValue>) sourceTriplesIterator.next();
					QName name = sourceTriple.getName();
					sourceVariables.put(name, getRealContent(pval, sourceTriple.getResidualPath()));
					if (sourceTriple.presentInMinusSet(pval)) {
						hasMinus = true;
					}
					if (sourceTriple.presentInZeroSet(pval)) {
						hasZero = true;
					}
					if (sourceTriple.presentInPlusSet(pval)) {
						hasPlus = true;
					}
				}
				if (!hasPlus && !hasMinus && !hasZero && !MiscUtil.isAllNull(pvalues)) {
					throw new IllegalStateException("Internal error! The impossible has happened! pvalues="+pvalues+"; source triples: "+sourceTriples+"; in "+contextDescription);
				}
				if (hasPlus && hasMinus) {
					// Both plus and minus. Ignore this combination. It should not appear in output
					return;
				}
				
				if (hasPlus && skipEvaluationPlus) {
					// The results will end up in the plus set, therefore we can skip it
					return;
				} else if (hasMinus && skipEvaluationMinus) {
					// The results will end up in the minus set, therefore we can skip it
					return;
				}
				
				ScriptVariables scriptVariables = new ScriptVariables();
				scriptVariables.addVariableDefinitions(sourceVariables);
				boolean useNew = false;
				if (hasPlus) {
					// Pluses and zeroes: Result goes to plus set, use NEW values for variables
					scriptVariables.addVariableDefinitionsNew(variables);
					useNew = true;
				} else if (hasMinus) {
					// Minuses and zeroes: Result goes to minus set, use OLD values for variables
					scriptVariables.addVariableDefinitionsOld(variables);
				} else {
					// All zeros: Result goes to zero set, use NEW values for variables
					scriptVariables.addVariableDefinitionsNew(variables);
					useNew = true;
				}
				
				List<V> scriptResults;
				try {
					scriptResults = (List<V>) scriptExpression.evaluate(scriptVariables, ScriptExpressionReturnTypeType.SCALAR, 
							useNew, contextDescription, result);
				} catch (ExpressionEvaluationException e) {
					throw new TunnelException(new ExpressionEvaluationException(e.getMessage()+
							"("+dumpSourceValues(sourceVariables)+") in "+contextDescription,e));
				} catch (ObjectNotFoundException e) {
					throw new TunnelException(new ObjectNotFoundException(e.getMessage()+
							"("+dumpSourceValues(sourceVariables)+") in "+contextDescription,e));
				} catch (SchemaException e) {
					throw new TunnelException(new SchemaException(e.getMessage()+
							"("+dumpSourceValues(sourceVariables)+") in "+contextDescription,e));
				} catch (RuntimeException e) {
					throw new TunnelException(new RuntimeException(e.getMessage()+
							"("+dumpSourceValues(sourceVariables)+") in "+contextDescription,e));
				}
				
				if (hasPlus) {
					// Pluses and zeroes: Result goes to plus set
					outputTriple.addAllToPlusSet(scriptResults);
				} else if (hasMinus) {
					// Minuses and zeroes: Result goes to minus set
					outputTriple.addAllToMinusSet(scriptResults);
				} else {
					// All zeros: Result goes to zero set
					outputTriple.addAllToZeroSet(scriptResults);
				}
			}			
		};
		try {
			MiscUtil.carthesian((Collection)valueCollections, (Processor)processor);
		} catch (TunnelException e) {
			Throwable originalException = e.getCause();
			if (originalException instanceof ExpressionEvaluationException) {
				throw (ExpressionEvaluationException)originalException;
			} else if (originalException instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException)originalException;
			} else if (originalException instanceof SchemaException) {
				throw (SchemaException)originalException;
			} else if (originalException instanceof RuntimeException) {
				throw (RuntimeException)originalException;
			} else {
				throw new IllegalStateException("Unexpected exception: "+e+": "+e.getMessage(),e);
			}
		}
		
		cleanupTriple(outputTriple);
		
		return outputTriple;
	}
	
	private void cleanupTriple(PrismValueDeltaSetTriple<V> triple) {
		if (triple == null) {
			return;
		}
		Collection<V> minusSet = triple.getMinusSet();
		if (minusSet == null) {
			return;
		}
		Collection<V> plusSet = triple.getPlusSet();
		if (plusSet == null) {
			return;
		}
		Iterator<V> plusIter = plusSet.iterator();
		while (plusIter.hasNext()) {
			V plusVal = plusIter.next();
			if (minusSet.contains(plusVal)) {
				plusIter.remove();
				minusSet.remove(plusVal);
				triple.addToZeroSet(plusVal);
			}
		}
	}

	private String dumpSourceValues(Map<QName, Object> variables) {
		StringBuilder sb = new StringBuilder();
		for (Entry<QName, Object> entry: variables.entrySet()) {
			sb.append(PrettyPrinter.prettyPrint(entry.getKey()));
			sb.append("=");
			sb.append(PrettyPrinter.prettyPrint(entry.getValue()));
			sb.append("; ");
		}
		return sb.toString();
	}
	
	public ItemPath parsePath(String path) {
		if (path == null) {
			return null;
		}
		Element codeElement = scriptType.getCode();
		XPathHolder xPathHolder = new XPathHolder(path, codeElement);
		if (xPathHolder == null) {
			return null;
		}
		return xPathHolder.toItemPath();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "script: "+scriptExpression.toString();
	}
	
}
