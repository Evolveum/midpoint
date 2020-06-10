/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.expression.evaluator;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;

import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.expression.TypedValue;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.function.Function;

/**
 * Utilities that are used by expression evaluators only.
 * (In contrast with ExpressionUtil that are used system-wide.)
 */
public class ExpressionEvaluatorUtil {

    /**
     * Converts intermediate expression result triple to the final output triple.
     */
    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> toOutputTriple(
            PrismValueDeltaSetTriple<V> resultTriple, ItemDefinition outputDefinition,
            Function<Object, Object> additionalConvertor,
            ItemPath residualPath, Protector protector, PrismContext prismContext) {

        PrismValueDeltaSetTriple<V> clonedTriple = resultTriple.clone();

        final Class<?> resultTripleValueClass = resultTriple.getRealValueClass();
        if (resultTripleValueClass == null) {
            // triple is empty. type does not matter.
            return clonedTriple;
        }
        Class<?> expectedJavaType = getClassForType(outputDefinition.getTypeName(), prismContext);
        if (resultTripleValueClass == expectedJavaType) {
            return clonedTriple;
        }

        clonedTriple.accept((Visitor) visitable -> {
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
                        Object convertedVal = ExpressionUtil.convertValue(expectedJavaType, additionalConvertor, realVal, protector, prismContext);
                        pval.setValue(convertedVal);
                    }
                }
            }
        });
        return clonedTriple;
    }

    // TODO this should be a standard method
    private static Class<?> getClassForType(@NotNull QName typeName, PrismContext prismContext) {
        Class<?> aClass = XsdTypeMapper.toJavaType(typeName);
        if (aClass != null) {
            return aClass;
        } else {
            return prismContext.getSchemaRegistry().getCompileTimeClass(typeName);
        }
    }

    public static TypedValue<?> findInSourcesAndVariables(ExpressionEvaluationContext context, String variableName) {
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
}
