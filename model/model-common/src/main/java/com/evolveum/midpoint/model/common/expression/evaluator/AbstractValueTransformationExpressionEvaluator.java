/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.*;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PlusMinusZeroType;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractValueTransformationExpressionEvaluator<V extends PrismValue, D extends ItemDefinition, E extends TransformExpressionEvaluatorType>
                        extends AbstractExpressionEvaluator<V, D, E> {

    private static final String OP_EVALUATE_EXPRESSION = AbstractValueTransformationExpressionEvaluator.class.getName() + ".evaluateExpression";
    private static final String OP_PROCESS_VALUES_COMBINATION = AbstractValueTransformationExpressionEvaluator.class.getName() + ".processValuesCombination";

    protected final SecurityContextManager securityContextManager;
    protected final LocalizationService localizationService;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractValueTransformationExpressionEvaluator.class);

    protected AbstractValueTransformationExpressionEvaluator(QName elementName, E expressionEvaluatorType, D outputDefinition, Protector protector, PrismContext prismContext,
            SecurityContextManager securityContextManager, LocalizationService localizationService) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector, prismContext);
        this.securityContextManager = securityContextManager;
        this.localizationService = localizationService;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java.util.Collection, java.util.Map, boolean, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        checkEvaluatorProfile(context);

        PrismValueDeltaSetTriple<V> outputTriple;

        if (getExpressionEvaluatorType().getRelativityMode() == TransformExpressionRelativityModeType.ABSOLUTE) {

            outputTriple = evaluateAbsoluteExpression(context.getSources(), context.getVariables(), context,
                    context.getContextDescription(), context.getTask(), result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Evaluated absolute expression {}, output triple:\n{}", context.getContextDescription(), outputTriple==null?null:outputTriple.debugDump(1));
            }

        } else if (getExpressionEvaluatorType().getRelativityMode() == null || getExpressionEvaluatorType().getRelativityMode() == TransformExpressionRelativityModeType.RELATIVE) {

            if (context.getSources() == null || context.getSources().isEmpty()) {
                // Special case. No sources, so there will be no input variables and no combinations. Everything goes to zero set.
                outputTriple = evaluateAbsoluteExpression(null, context.getVariables(), context,
                        context.getContextDescription(), context.getTask(), result);
                LOGGER.trace("Evaluated relative sourceless expression {}, output triple:\n{}", context.getContextDescription(), outputTriple==null?null:outputTriple.debugDump(1));
            } else {
                List<SourceTriple<?,?>> sourceTriples = processSources(context.getSources(), context);
                outputTriple = evaluateRelativeExpression(sourceTriples, context.getVariables(), context.isSkipEvaluationMinus(), context.isSkipEvaluationPlus(),
                        context, context.getContextDescription(), context.getTask(), result);
                LOGGER.trace("Evaluated relative expression {}, output triple:\n{}", context.getContextDescription(), outputTriple==null?null:outputTriple.debugDump(1));
            }

        } else {
            throw new IllegalArgumentException("Unknown relativity mode "+getExpressionEvaluatorType().getRelativityMode());
        }


        return outputTriple;
    }

    protected boolean isIncludeNullInputs() {
        Boolean includeNullInputs = getExpressionEvaluatorType().isIncludeNullInputs();
        return includeNullInputs == null || includeNullInputs;
    }

    protected boolean isRelative() {
        return getExpressionEvaluatorType().getRelativityMode() != TransformExpressionRelativityModeType.ABSOLUTE;
    }

    private List<SourceTriple<?,?>> processSources(Collection<Source<?,?>> sources,
            ExpressionEvaluationContext params) {
        List<SourceTriple<?,?>> sourceTriples =
            new ArrayList<>(sources == null ? 0 : sources.size());
        if (sources == null) {
            return sourceTriples;
        }
        for (Source<?,?> source: sources) {
            SourceTriple<?,?> sourceTriple = new SourceTriple<>(source, prismContext);
            ItemDelta<?,?> delta = source.getDelta();
            if (delta != null) {
                sourceTriple.merge((DeltaSetTriple) delta.toDeltaSetTriple((Item) source.getItemOld()));
            } else {
                if (source.getItemOld() != null) {
                    sourceTriple.addAllToZeroSet((Collection)source.getItemOld().getValues());
                }
            }
            if (isIncludeNullInputs()) {
                // Make sure that we properly handle the "null" states, i.e. the states when we enter
                // "empty" value and exit "empty" value for a property
                // We need this to properly handle "negative" expressions, i.e. expressions that return non-null
                // value for null input. We need to make sure such expressions receive the null input when needed
                Item<?,?> itemOld = source.getItemOld();
                Item<?,?> itemNew = source.getItemNew();
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
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processed source {} triple\n{}", source.getName().getLocalPart(), sourceTriple.debugDump(1));
            }
        }
        return sourceTriples;
    }

    private PrismValueDeltaSetTriple<V> evaluateAbsoluteExpression(Collection<Source<?,?>> sources,
            ExpressionVariables variables, ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        PrismValueDeltaSetTriple<V> outputTriple;

        if (hasDeltas(sources) || hasDeltas(variables)) {

            Collection<V> outputSetOld = null;
            if (!params.isSkipEvaluationMinus()) {
                outputSetOld = evaluateExpression(sources, variables, contextDescription, false, params, task, result);
            }
            Collection<V> outputSetNew = null;
            if (!params.isSkipEvaluationPlus()) {
                outputSetNew = evaluateExpression(sources, variables, contextDescription, true, params, task, result);
            }

            outputTriple = DeltaSetTripleUtil.diffPrismValueDeltaSetTriple(outputSetOld, outputSetNew, prismContext);

        } else {
            // No need to execute twice. There is no change.
            Collection<V> outputSetNew = evaluateExpression(sources, variables, contextDescription, true, params, task, result);
            outputTriple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
            outputTriple.addAllToZeroSet(outputSetNew);
        }

        return outputTriple;
    }

    private boolean hasDeltas(Collection<Source<?,?>> sources) {
        if (sources == null) {
            return false;
        }
        for (Source<?,?> source: sources) {
            if (source.getDelta() != null && !source.getDelta().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDeltas(ExpressionVariables variables) {
        for (Entry<String, TypedValue> entry: variables.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            Object value = entry.getValue().getValue();
            if (value instanceof ObjectDeltaObject<?>) {
                if (((ObjectDeltaObject<?>)value).getObjectDelta() != null && !((ObjectDeltaObject<?>)value).getObjectDelta().isEmpty()) {
                    return true;
                }
            } else if (value instanceof ItemDeltaItem<?,?>) {
                if (((ItemDeltaItem<?,?>)value).getDelta() != null && !((ItemDeltaItem<?,?>)value).getDelta().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Collection<V> evaluateExpression(Collection<Source<?,?>> sources,
            ExpressionVariables variables, String contextDescription, boolean useNew, ExpressionEvaluationContext params,
            Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE_EXPRESSION)
                .setMinor()
                .addContext("context", contextDescription)
                .addContext("evaluator", getClass().getName())
                .build();
        try {
            ExpressionVariables scriptVariables = new ExpressionVariables();
            if (useNew) {
                scriptVariables.addVariableDefinitionsNew(variables);
            } else {
                scriptVariables.addVariableDefinitionsOld(variables);
            }

            if (sources != null) {
                // Add sources to variables
                for (Source<?,?> source: sources) {
                    LOGGER.trace("source: {}", source);
                    String name = source.getName().getLocalPart();
                    if (name == null) {
                        if (sources.size() == 1) {
                            name = ExpressionConstants.VAR_INPUT;
                        } else {
                            throw new ExpressionSyntaxException("No name definition for source in "+contextDescription);
                        }
                    }

                    Object value;
                    if (useNew) {
                        value = getRealContent(source.getItemNew(), source.getResidualPath());
                    } else {
                        value = getRealContent(source.getItemOld(), source.getResidualPath());
                    }
                    scriptVariables.addVariableDefinition(name, value, source.getDefinition());
                }
            }

            List<V> scriptResults = transformSingleValue(scriptVariables, null, useNew, params,
                    (useNew ? "(new) " : "(old) " ) + contextDescription, task, result);

            if (scriptResults == null || scriptResults.isEmpty()) {
                return null;
            }

            Collection<V> outputSet = new ArrayList<>(scriptResults.size());
            for (V pval: scriptResults) {
                if (pval instanceof PrismPropertyValue<?>) {
                    if (((PrismPropertyValue<?>) pval).getValue() == null) {
                        continue;
                    }
                    Object realValue = ((PrismPropertyValue<?>)pval).getValue();
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
                }
                outputSet.add(pval);
            }

            return outputSet;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    protected abstract List<V> transformSingleValue(ExpressionVariables variables, PlusMinusZero valueDestination,
            boolean useNew, ExpressionEvaluationContext context, String contextDescription, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException;

    private Object getRealContent(Item<?,?> item, ItemPath residualPath) {
        if (residualPath == null || residualPath.isEmpty()) {
            return item;
        }
        if (item == null) {
            return null;
        }
        return item.find(residualPath);
    }

    private Object getRealContent(PrismValue pval, ItemPath residualPath) {
        if (residualPath == null || residualPath.isEmpty()) {
            return pval;
        }
        if (pval == null) {
            return null;
        }
        return pval.find(residualPath);
    }

    private PrismValueDeltaSetTriple<V> evaluateRelativeExpression(final List<SourceTriple<?,?>> sourceTriples,
            final ExpressionVariables variables, final boolean skipEvaluationMinus, final boolean skipEvaluationPlus,
            final ExpressionEvaluationContext evaluationContext, final String contextDescription,
            final Task task, final OperationResult parentResult)
                    throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        List<Collection<? extends PrismValue>> valueCollections = new ArrayList<>(sourceTriples.size());
        for (SourceTriple<?,?> sourceTriple: sourceTriples) {
            Collection<? extends PrismValue> values = sourceTriple.union();
            if (values.isEmpty()) {
                // No values for this source. Add null instead. It will make sure that the expression will
                // be evaluate at least once.
                values.add(null);
            }
            valueCollections.add(values);
        }

        final PrismValueDeltaSetTriple<V> outputTriple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();

        Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> conditionExpression;
        if (getExpressionEvaluatorType().getCondition() != null) {
            conditionExpression = ExpressionUtil.createCondition(getExpressionEvaluatorType().getCondition(), evaluationContext.getExpressionProfile(),
                    evaluationContext.getExpressionFactory(), "condition in " + contextDescription, task, parentResult);
        } else {
            conditionExpression = null;
        }

        Processor<Collection<? extends PrismValue>> processor = pvalues -> {
            // This lambda will be called for all combination of all values in the sources
            // The pvalues parameter is a list of values, each values comes from a difference source

            OperationResult result = parentResult.subresult(OP_PROCESS_VALUES_COMBINATION)
                    .setMinor()
                    .build();
            ValueTransformationTraceType trace;
            if (result.isTraced()) {
                result.addParam("context", contextDescription);
                trace = new ValueTransformationTraceType(prismContext);
                dumpValueCombination(pvalues, sourceTriples, trace);
                result.getTraces().add(trace);
            } else {
                trace = null;
            }
            try {
                if (!isIncludeNullInputs() && MiscUtil.isAllNull(pvalues)) {
                    // The case that all the sources are null. There is no point executing the expression.
                    if (trace != null) {
                        trace.setComment("All sources are null and includeNullInputs is true");
                    }
                    return;
                }
                ExpressionVariables scriptVariables = new ExpressionVariables();
                Iterator<SourceTriple<PrismValue,?>> sourceTriplesIterator = (Iterator)sourceTriples.iterator();
                boolean hasMinus = false;
                boolean hasZero = false;
                boolean hasPlus = false;
                for (PrismValue pval: pvalues) {
                    // This may look strange, but it actually makes sense.
                    // The carthesian method below will select values from valueCollections, which is collection of lists
                    // The pvalues parameter to this lambda is one particular selection from valueCollections.
                    // The carthesian method will select first value in pvalues from the first source, second values from second source and so on.
                    // Therefore this strange construction will match the value with the source that it came from
                    // TODO: maybe refactor for readibility
                    SourceTriple<PrismValue,?> sourceTriple = sourceTriplesIterator.next();
                    String name = sourceTriple.getName().getLocalPart();
                    ItemDefinition definition = sourceTriple.getSource().getDefinition();
                    if (definition == null) {
                        LOGGER.error("Source '{}' without a definition; came from a source triple: {}", name, sourceTriple);
                        throw new IllegalArgumentException("Source '"+name+"' without a definition");
                    }
                    scriptVariables.put(name, getRealContent(pval, sourceTriple.getResidualPath()), definition);
                    // Note: a value may be both in plus and minus sets, e.g. in case that the value is replaced
                    // with the same value. We pretend that this is the same as ADD case.
                    // TODO: maybe we will need better handling in the future. Maybe we would need
                    // to execute the script twice?
                    if (sourceTriple.presentInPlusSet(pval)) {
                        hasPlus = true;
                    } else if (sourceTriple.presentInZeroSet(pval)) {
                        hasZero = true;
                    } else if (sourceTriple.presentInMinusSet(pval)) {
                        hasMinus = true;
                    }
    //                if (LOGGER.isTraceEnabled()) {
    //                    LOGGER.trace("source {}, pval {}\n   hasPlus={}, hasZero={}, hasMinus={}, skipEvaluationPlus={}, skipEvaluationMinus={}",
    //                            sourceTriple.getName().getLocalPart(), pval, hasPlus, hasZero, hasMinus, skipEvaluationPlus, skipEvaluationMinus);
    //                }
                    if (evaluationContext != null && evaluationContext.getVariableProducer() != null) {
    //                    LOGGER.trace("Variable producer for {}", pval);
                        evaluationContext.getVariableProducer().produce(pval, variables);
                    }
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Processing value combination {} in {}\n   hasPlus={}, hasZero={}, hasMinus={}, skipEvaluationPlus={}, skipEvaluationMinus={}",
                            dumpValueCombination(pvalues, sourceTriples), contextDescription, hasPlus, hasZero, hasMinus, skipEvaluationPlus, skipEvaluationMinus);
                }

                if (!hasPlus && !hasMinus && !hasZero && !MiscUtil.isAllNull(pvalues)) {
                    throw new IllegalStateException("Internal error! The impossible has happened! pvalues="+pvalues+"; source triples: "+sourceTriples+"; in "+contextDescription);
                }
                if (hasPlus && hasMinus) {
                    // The combination of values that are both in plus and minus. Evaluating this combination
                    // does not make sense. Just skip it.
                    // Note: There will NOT be a single value that is in both plus and minus (e.g. "replace with itself" case).
                    // That case is handled by the elseif branches above. This case strictly applies to
                    // combination of different values from the plus and minus sets.
                    LOGGER.trace("The combination of values that are both in plus and minus. Evaluating this combination does not make sense. Just skip it.");
                    if (trace != null) {
                        trace.setComment("The combination of values that are both in plus and minus. Evaluating this combination does not make sense. Just skip it.");
                    }
                    return;
                }

                if (hasPlus && skipEvaluationPlus) {
                    if (trace != null) {
                        trace.setComment("The results will end up in the plus set and skipEvaluationPlus is true, therefore we can skip them.");
                    }
                    return;
                } else if (hasMinus && skipEvaluationMinus) {
                    if (trace != null) {
                        trace.setComment("The results will end up in the minus set and skipEvaluationMinus is true, therefore we can skip them.");
                    }
                    return;
                }

                PlusMinusZero valueDestination;
                boolean useNew = false;
                if (hasPlus) {
                    // Pluses and zeroes: Result goes to plus set, use NEW values for variables
                    scriptVariables.addVariableDefinitionsNew(variables);
                    valueDestination = PlusMinusZero.PLUS;
                    useNew = true;
                } else if (hasMinus) {
                    // Minuses and zeroes: Result goes to minus set, use OLD values for variables
                    scriptVariables.addVariableDefinitionsOld(variables);
                    valueDestination = PlusMinusZero.MINUS;
                } else {
                    // All zeros: Result goes to zero set, use NEW values for variables
                    scriptVariables.addVariableDefinitionsNew(variables);
                    valueDestination = PlusMinusZero.ZERO;
                    useNew = true;
                }

                List<V> scriptResults;
                try {
                    boolean conditionResult;
                    if (conditionExpression != null) {
                        ExpressionEvaluationContext ctx = new ExpressionEvaluationContext(null, scriptVariables,
                                "condition in " + contextDescription, task);
                        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> triple = conditionExpression.evaluate(ctx, result);
                        conditionResult = ExpressionUtil.computeConditionResult(triple.getNonNegativeValues());
                    } else {
                        conditionResult = true;
                    }
                    if (conditionResult) {
                        scriptResults = transformSingleValue(scriptVariables, valueDestination, useNew, evaluationContext,
                                contextDescription, task, result);
                    } else {
                        LOGGER.trace("Skipping value transformation because condition evaluated to false in {}", contextDescription);
                        scriptResults = Collections.emptyList();
                    }
                } catch (ExpressionEvaluationException e) {
                    ExpressionEvaluationException exex = new ExpressionEvaluationException(
                            e.getMessage() + "("+scriptVariables.dumpSingleLine()+") in "+contextDescription,
                            e,
                            ExceptionUtil.getUserFriendlyMessage(e));
                    if (localizationService != null) {
                        localizationService.translate(exex);
                    }
                    throw new TunnelException(exex);

                } catch (ObjectNotFoundException e) {
                    throw new TunnelException(new ObjectNotFoundException(e.getMessage()+
                            "("+scriptVariables.dumpSingleLine()+") in "+contextDescription,e));
                } catch (SchemaException e) {
                    throw new TunnelException(new SchemaException(e.getMessage()+
                            "("+scriptVariables.dumpSingleLine()+") in "+contextDescription,e));
                } catch (RuntimeException e) {
                    throw new TunnelException(new RuntimeException(e.getMessage()+
                            "("+scriptVariables.dumpSingleLine()+") in "+contextDescription,e));
                } catch (CommunicationException e) {
                    throw new TunnelException(new CommunicationException(e.getMessage()+
                            "("+scriptVariables.dumpSingleLine()+") in "+contextDescription,e));
                } catch (ConfigurationException e) {
                    throw new TunnelException(new ConfigurationException(e.getMessage()+
                            "("+scriptVariables.dumpSingleLine()+") in "+contextDescription,e));
                } catch (SecurityViolationException e) {
                    throw new TunnelException(new SecurityViolationException(e.getMessage()+
                            "("+scriptVariables.dumpSingleLine()+") in "+contextDescription,e));
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Processed value combination {} in {}\n  valueDestination: {}\n  scriptResults:{}",
                            dumpValueCombination(pvalues, sourceTriples), contextDescription, valueDestination, scriptResults);
                }

                if (trace != null) {
                    trace.setDestination(PlusMinusZeroType.fromValue(valueDestination));
                    trace.getOutput().addAll(TraceUtil.toAnyValueTypeList(scriptResults, prismContext));
                }

                outputTriple.addAllToSet(valueDestination, scriptResults);
            } catch (Throwable t) {
                result.recordFatalError(t.getMessage(), t);
                throw t;
            } finally {
                result.computeStatusIfUnknown();
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
            } else if (originalException instanceof CommunicationException) {
                throw (CommunicationException)originalException;
            } else if (originalException instanceof ConfigurationException) {
                throw (ConfigurationException)originalException;
            } else if (originalException instanceof SecurityViolationException) {
                throw (SecurityViolationException)originalException;
            } else if (originalException instanceof RuntimeException) {
                throw (RuntimeException)originalException;
            } else {
                throw new IllegalStateException("Unexpected exception: "+e+": "+e.getMessage(),e);
            }
        }

        cleanupTriple(outputTriple);

        return outputTriple;
    }

    private String dumpValueCombination(Collection<? extends PrismValue> pvalues, List<SourceTriple<?, ?>> sourceTriples) {
        StringBuilder sb = new StringBuilder();
        Iterator<SourceTriple<?,?>> sourceTriplesIterator = sourceTriples.iterator();
        for (PrismValue pval: pvalues) {
            SourceTriple<?,?> sourceTriple = sourceTriplesIterator.next();
            sb.append(sourceTriple.getName().getLocalPart()).append('=');
            sb.append(pval==null?null:(Object)pval.getRealValueOrRawType(prismContext));
            if (sourceTriplesIterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private void dumpValueCombination(Collection<? extends PrismValue> pvalues, List<SourceTriple<?, ?>> sourceTriples,
            ValueTransformationTraceType trace) {
        Iterator<SourceTriple<?,?>> sourceTriplesIterator = sourceTriples.iterator();
        for (PrismValue pval: pvalues) {
            SourceTriple<?,?> sourceTriple = sourceTriplesIterator.next();
            trace.getInput().add(TraceUtil.toNamedValueType(pval, sourceTriple.getName(), prismContext));
        }
    }

    private void cleanupTriple(PrismValueDeltaSetTriple<V> triple) {
        if (triple == null) {
            return;
        }
        Collection<V> minusSet = triple.getMinusSet();
        Collection<V> plusSet = triple.getPlusSet();
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

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
     */
    @Override
    public abstract String shortDebugDump();

}
