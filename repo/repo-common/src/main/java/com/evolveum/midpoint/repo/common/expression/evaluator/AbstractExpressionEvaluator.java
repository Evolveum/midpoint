/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.Function;

/**
 * Evaluates an expression defined by {@link #expressionEvaluatorBean}.
 *
 * @param <E> evaluator bean (configuration) type
 *
 * @author Radovan Semancik
 */
public abstract class AbstractExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>, E>
        implements ExpressionEvaluator<V> {

    /**
     * Qualified name of the evaluator.
     */
    @NotNull private final QName elementName;

    /**
     * Bean (i.e. configuration) for the evaluator.
     * In some cases it can be null - e.g. for implicit asIs evaluator.
     */
    protected final E expressionEvaluatorBean;

    /**
     * Definition of the output item.
     * Needed for some of the evaluators (the question is if it's really needed).
     */
    protected final D outputDefinition;

    protected final Protector protector;

    @NotNull protected final PrismContext prismContext = PrismContext.get();

    public AbstractExpressionEvaluator(
            @NotNull QName elementName, E expressionEvaluatorBean, D outputDefinition, Protector protector) {
        this.elementName = elementName;
        this.expressionEvaluatorBean = expressionEvaluatorBean;
        this.outputDefinition = outputDefinition;
        this.protector = protector;
    }

    @Override
    public @NotNull QName getElementName() {
        return elementName;
    }

    /**
     * Check expression profile. Throws security exception if the execution is not allowed by the profile.
     * <p>
     * This implementation works only for simple evaluators that do not have any profile settings.
     * Complex evaluators should override this method.
     *
     * @throws SecurityViolationException expression execution is not allowed by the profile.
     */
    protected void checkEvaluatorProfile(ExpressionEvaluationContext context) throws SecurityViolationException {
        ExpressionUtil.checkEvaluatorProfileSimple(this, context);
    }

    public @NotNull PrismContext getPrismContext() {
        return prismContext;
    }

    public D getOutputDefinition() {
        return outputDefinition;
    }

    public Protector getProtector() {
        return protector;
    }

    /**
     * Converts intermediate expression result triple to the final output triple
     * according to expected Java type and additional convertor.
     *
     * TODO why it is used only for some evaluators?
     */
    public PrismValueDeltaSetTriple<V> finishOutputTriple(PrismValueDeltaSetTriple<V> resultTriple,
            Function<Object, Object> additionalConvertor, ItemPath residualPath) {

        if (resultTriple == null) {
            return null;
        }

        final Class<?> resultTripleValueClass = resultTriple.getRealValueClass();
        if (resultTripleValueClass == null) {
            // triple is empty. type does not matter.
            return resultTriple;
        }
        Class<?> expectedJavaType = getClassForType(outputDefinition.getTypeName());
        if (resultTripleValueClass == expectedJavaType) {
            return resultTriple;
        }

        resultTriple.accept(visitable -> {
            if (visitable instanceof PrismPropertyValue<?>) {
                //noinspection unchecked
                PrismPropertyValue<Object> pval = (PrismPropertyValue<Object>) visitable;
                Object realVal = pval.getValue();
                if (realVal != null) {
                    if (Structured.class.isAssignableFrom(resultTripleValueClass)) {
                        if (residualPath != null && !residualPath.isEmpty()) {
                            realVal = ((Structured) realVal).resolve(residualPath);
                        }
                    }
                    if (expectedJavaType != null) {
                        pval.setValue(
                                ExpressionUtil.convertValue(expectedJavaType, additionalConvertor, realVal, protector));
                    }
                }
            }
        });
        return resultTriple;
    }

    // TODO this should be a standard method
    private Class<?> getClassForType(@NotNull QName typeName) {
        Class<?> aClass = XsdTypeMapper.toJavaType(typeName);
        if (aClass != null) {
            return aClass;
        } else {
            return prismContext.getSchemaRegistry().getCompileTimeClass(typeName);
        }
    }

    public TypedValue<?> findInSourcesAndVariables(ExpressionEvaluationContext context, String variableName) {
        for (Source<?, ?> source : context.getSources()) {
            if (variableName.equals(source.getName().getLocalPart())) {
                return new TypedValue<>(source, source.getDefinition());
            }
        }

        if (context.getVariables() != null) {
            return context.getVariables().get(variableName);
        } else {
            return null;
        }
    }

    /**
     * Adds Internal origin for given prismValue. Assumes that value has no metadata.
     * (Currently does not fill-in actorRef nor channel.)
     */
    protected void addInternalOrigin(PrismValue value, ExpressionEvaluationContext context) throws SchemaException {
        if (value != null && !value.hasValueMetadata() && context.getValueMetadataComputer() != null) {
            ValueMetadataType metadata = new ValueMetadataType()
                    .beginProvenance()
                        .beginAcquisition()
                            .originRef(SystemObjectsType.ORIGIN_INTERNAL.value(), ServiceType.COMPLEX_TYPE)
                            .timestamp(XmlTypeConverter.createXMLGregorianCalendar())
                        .<ProvenanceMetadataType>end()
                    .end();
            //noinspection unchecked
            value.getValueMetadataAsContainer().add(metadata.asPrismContainerValue());
        }
    }

    /**
     * Applies value metadata to the triple to-be-outputted.
     *
     * To be used for simple mappings that basically copy data from given source (input IDI or some other data source)
     * to the output.
     */
    public void applyValueMetadata(PrismValueDeltaSetTriple<V> triple, ExpressionEvaluationContext context,
            OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (triple != null) {
            for (V value : triple.getPlusSet()) {
                applyValueMetadata(value, context, result);
            }
            for (V value : triple.getZeroSet()) {
                applyValueMetadata(value, context, result);
            }
            for (V value : triple.getMinusSet()) {
                // For values in minus set we must delete any existing metadata. The reason is that a mapping
                // computes own metadata for its outputs - so metadata from the input values in minus set are no longer
                // relevant on the output.
                if (value != null) {
                    value.getValueMetadata().clear();
                }
            }
        }
    }

    private void applyValueMetadata(PrismValue value, ExpressionEvaluationContext context, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (value != null) {
            if (context.getValueMetadataComputer() != null) {
                value.setValueMetadata(
                        context.getValueMetadataComputer()
                                .compute(Collections.singletonList(value), result));
            } else {
                // This is to clear pre-existing metadata for asIs mappings that are not configured e.g. for provenance
                // metadata propagation.
                value.getValueMetadata().clear();
            }
        }
    }
}
