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
package com.evolveum.midpoint.repo.common.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class Expression<V extends PrismValue,D extends ItemDefinition> {

	final private ExpressionType expressionType;
	final private D outputDefinition;
	final private PrismContext prismContext;
	final private ObjectResolver objectResolver;
	final private SecurityEnforcer securityEnforcer;
	private List<ExpressionEvaluator<V,D>> evaluators = new ArrayList<>(1);

	private static final Trace LOGGER = TraceManager.getTrace(Expression.class);

	public Expression(ExpressionType expressionType, D outputDefinition, ObjectResolver objectResolver, SecurityEnforcer securityEnforcer, PrismContext prismContext) {
		//Validate.notNull(outputDefinition, "null outputDefinition");
		Validate.notNull(objectResolver, "null objectResolver");
		Validate.notNull(prismContext, "null prismContext");
		this.expressionType = expressionType;
		this.outputDefinition = outputDefinition;
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
		this.securityEnforcer = securityEnforcer;
	}

	public void parse(ExpressionFactory factory, String contextDescription, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		if (expressionType == null) {
			evaluators.add(createDefaultEvaluator(factory, contextDescription, task, result));
			return;
		}
		if (expressionType.getExpressionEvaluator() == null /* && expressionType.getSequence() == null */) {
			throw new SchemaException("No evaluator was specified in "+contextDescription);
		}
		if (expressionType.getExpressionEvaluator() != null) {
			ExpressionEvaluator evaluator = createEvaluator(expressionType.getExpressionEvaluator(), factory,
					contextDescription, task, result);
			evaluators.add(evaluator);
		}
		if (evaluators.isEmpty()) {
			evaluators.add(createDefaultEvaluator(factory, contextDescription, task, result));
		}
	}

	private ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, ExpressionFactory factory,
													 String contextDescription, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		if (evaluatorElements.isEmpty()) {
			throw new SchemaException("Empty evaluator list in "+contextDescription);
		}
		JAXBElement<?> fistEvaluatorElement = evaluatorElements.iterator().next();
		ExpressionEvaluatorFactory evaluatorFactory = factory.getEvaluatorFactory(fistEvaluatorElement.getName());
		if (evaluatorFactory == null) {
			throw new SchemaException("Unknown expression evaluator element "+fistEvaluatorElement.getName()+" in "+contextDescription);
		}
		return evaluatorFactory.createEvaluator(evaluatorElements, outputDefinition, contextDescription, task, result);
	}

	private ExpressionEvaluator<V,D> createDefaultEvaluator(ExpressionFactory factory, String contextDescription,
															Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		ExpressionEvaluatorFactory evaluatorFactory = factory.getDefaultEvaluatorFactory();
		if (evaluatorFactory == null) {
			throw new SystemException("Internal error: No default expression evaluator factory");
		}
		return evaluatorFactory.createEvaluator(null, outputDefinition, contextDescription, task, result);
	}

	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {

		ExpressionVariables processedVariables = null;

		try {

			processedVariables = processInnerVariables(context.getVariables(), context.getContextDescription(),
					context.getTask(), context.getResult());

			ExpressionEvaluationContext contextWithProcessedVariables = context.shallowClone();
			contextWithProcessedVariables.setVariables(processedVariables);
			PrismValueDeltaSetTriple<V> outputTriple;

			ObjectReferenceType runAsRef = null;
			if (expressionType != null) {
				runAsRef = expressionType.getRunAsRef();
			}

			if (runAsRef == null) {

				outputTriple = evaluateExpressionEvaluators(contextWithProcessedVariables);

			} else {

				UserType userType = objectResolver.resolve(runAsRef, UserType.class, null,
						"runAs in "+context.getContextDescription(), context.getTask(), context.getResult());

				LOGGER.trace("Running {} as {} ({})", context.getContextDescription(), userType, runAsRef);

				try {

					outputTriple = securityEnforcer.runAs(() -> {
						try {
							return evaluateExpressionEvaluators(contextWithProcessedVariables);
						} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException e) {
							throw new TunnelException(e);
						}
					}, userType.asPrismObject());

				} catch (TunnelException te) {
					Throwable e = te.getCause();
					if (e instanceof RuntimeException) {
						throw (RuntimeException)e;
					}
					if (e instanceof Error) {
						throw (Error)e;
					}
					if (e instanceof SchemaException) {
						throw (SchemaException)e;
					}
					if (e instanceof ExpressionEvaluationException) {
						throw (ExpressionEvaluationException)e;
					}
					if (e instanceof ObjectNotFoundException) {
						throw (ObjectNotFoundException)e;
					}
					throw te;
				}

			}

			traceSuccess(context, processedVariables, outputTriple);
			return outputTriple;

		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | RuntimeException | Error e) {
			traceFailure(context, processedVariables, e);
			throw e;
		}
	}


	private PrismValueDeltaSetTriple<V> evaluateExpressionEvaluators(ExpressionEvaluationContext contextWithProcessedVariables)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		for (ExpressionEvaluator<?,?> evaluator: evaluators) {
			PrismValueDeltaSetTriple<V> outputTriple = (PrismValueDeltaSetTriple<V>) evaluator.evaluate(contextWithProcessedVariables);
			if (outputTriple != null) {
				boolean allowEmptyRealValues = false;
				if (expressionType != null) {
					allowEmptyRealValues = BooleanUtils.isTrue(expressionType.isAllowEmptyValues());
				}
				outputTriple.removeEmptyValues(allowEmptyRealValues);
				if (InternalsConfig.consistencyChecks) {
					try {
						outputTriple.checkConsistence();
					} catch (IllegalStateException e) {
						throw new IllegalStateException(e.getMessage() + "; in expression " + this +", evaluator " + evaluator, e);
					}
				}
				return outputTriple;
			}
		}

		return null;

	}

	private void traceSuccess(ExpressionEvaluationContext context, ExpressionVariables processedVariables, PrismValueDeltaSetTriple<V> outputTriple) {
		if (!isTrace()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Expression trace:\n");
		appendTraceHeader(sb, context, processedVariables);
		sb.append("\nResult: ");
		if (outputTriple == null) {
			sb.append("null");
		} else {
			sb.append(outputTriple.toHumanReadableString());
		}
		appendTraceFooter(sb);
		trace(sb.toString());
	}

	private void traceFailure(ExpressionEvaluationContext context, ExpressionVariables processedVariables, Throwable e) {
		LOGGER.error("Error evaluating expression in {}: {}", new Object[]{context.getContextDescription(), e.getMessage(), e});
		if (!isTrace()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Expression failure:\n");
		appendTraceHeader(sb, context, processedVariables);
		sb.append("\nERROR: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
		appendTraceFooter(sb);
		trace(sb.toString());
	}

	private boolean isTrace() {
		return LOGGER.isTraceEnabled() || (expressionType != null && expressionType.isTrace() == Boolean.TRUE);
	}

	private void trace(String msg) {
		if (expressionType != null && expressionType.isTrace() == Boolean.TRUE) {
			LOGGER.info(msg);
		} else {
			LOGGER.trace(msg);
		}
	}

	private void appendTraceHeader(StringBuilder sb, ExpressionEvaluationContext context, ExpressionVariables processedVariables) {
		sb.append("---[ EXPRESSION in ");
		sb.append(context.getContextDescription());
		sb.append("]---------------------------");
		sb.append("\nSources:");
		Collection<Source<?,?>> sources = context.getSources();
		if (sources == null) {
			sb.append(" null");
		} else {
			for (Source<?,?> source: sources) {
				sb.append("\n");
				sb.append(source.debugDump(1));
			}
		}
		sb.append("\nVariables:");
		if (processedVariables == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			sb.append(processedVariables.debugDump(1));
		}
		sb.append("\nOutput definition: ").append(MiscUtil.toString(outputDefinition));
		sb.append("\nEvaluators: ");
		sb.append(shortDebugDump());
	}

	private void appendTraceFooter(StringBuilder sb) {
		sb.append("\n------------------------------------------------------");
	}

	private ExpressionVariables processInnerVariables(ExpressionVariables variables, String contextDescription,
													  Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (expressionType == null) {
			// shortcut
			return variables;
		}
		ExpressionVariables newVariables = new ExpressionVariables();

		// We need to add actor variable before we switch user identity (runAs)
		ExpressionUtil.addActorVariable(newVariables, securityEnforcer);
		boolean actorDefined = newVariables.get(ExpressionConstants.VAR_ACTOR) != null;

		for (Entry<QName,Object> entry: variables.entrySet()) {
			if (ExpressionConstants.VAR_ACTOR.equals(entry.getKey()) && actorDefined) {
				continue;			// avoid pointless warning about redefined value of actor
			}
			newVariables.addVariableDefinition(entry.getKey(), entry.getValue());
		}

		for (ExpressionVariableDefinitionType variableDefType: expressionType.getVariable()) {
			QName varName = variableDefType.getName();
			if (varName == null) {
				throw new SchemaException("No variable name in expression in "+contextDescription);
			}
			if (variableDefType.getObjectRef() != null) {
                ObjectReferenceType ref = variableDefType.getObjectRef();
                ref.setType(prismContext.getSchemaRegistry().qualifyTypeName(ref.getType()));
				ObjectType varObject = objectResolver.resolve(ref, ObjectType.class, null, "variable "+varName+" in "+contextDescription, task, result);
				newVariables.addVariableDefinition(varName, varObject);
			} else if (variableDefType.getValue() != null) {
				// Only string is supported now
				Object valueObject = variableDefType.getValue();
				if (valueObject instanceof String) {
					newVariables.addVariableDefinition(varName, valueObject);
				} else if (valueObject instanceof Element) {
					newVariables.addVariableDefinition(varName, ((Element)valueObject).getTextContent());
				} else if (valueObject instanceof RawType) {
					newVariables.addVariableDefinition(varName, ((RawType) valueObject).getParsedValue(null, varName));
				} else {
					throw new SchemaException("Unexpected type "+valueObject.getClass()+" in variable definition "+varName+" in "+contextDescription);
				}
			} else if (variableDefType.getPath() != null) {
				ItemPath itemPath = variableDefType.getPath().getItemPath();
				Object resolvedValue = ExpressionUtil.resolvePath(itemPath, variables, null, objectResolver, contextDescription, task, result);
				newVariables.addVariableDefinition(varName, resolvedValue);
			} else {
				throw new SchemaException("No value for variable "+varName+" in "+contextDescription);
			}
		}

		return newVariables;
	}

	@Override
	public String toString() {
		return "Expression(expressionType=" + expressionType + ", outputDefinition=" + outputDefinition
				+ ": " + shortDebugDump() + ")";
	}

	public String shortDebugDump() {
		if (evaluators == null) {
			return "null evaluators";
		}
		if (evaluators.isEmpty()) {
			return "[]";
		}
		if (evaluators.size() == 1) {
			return evaluators.iterator().next().shortDebugDump();
		}
		StringBuilder sb = new StringBuilder("[");
		for (ExpressionEvaluator<V,D> evaluator: evaluators) {
			sb.append(evaluator.shortDebugDump());
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

}
