/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.DefinitionResolver;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author Radovan Semancik
 */
public class PathExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> implements ExpressionEvaluator<V> {

    private final QName elementName;
    private final ItemPath path;
    private final ObjectResolver objectResolver;
    private final PrismContext prismContext;
    private final D outputDefinition;
    private final Protector protector;

    PathExpressionEvaluator(QName elementName, ItemPath path, ObjectResolver objectResolver,
            D outputDefinition, Protector protector, PrismContext prismContext) {
        this.elementName = elementName;
        this.path = path;
        this.objectResolver = objectResolver;
        this.outputDefinition = outputDefinition;
        this.prismContext = prismContext;
        this.protector = protector;
    }

    @Override
    public QName getElementName() {
        return elementName;
    }

    @Override
    public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException {
        ExpressionUtil.checkEvaluatorProfileSimple(this, context);

        ItemDeltaItem<?,?> resolveContext = null;

        ItemPath resolvePath = path;
        if (context.getSources() != null && context.getSources().size() == 1) {
            Source<?,?> source = context.getSources().iterator().next();
            if (path.isEmpty()) {
                PrismValueDeltaSetTriple<V> outputTriple = (PrismValueDeltaSetTriple<V>) source.toDeltaSetTriple(prismContext);
                return outputTriple.clone();
            }
            resolveContext = source;
            //FIXME quite a hack, but should work for now.
            if (QNameUtil.match(path.firstName(), source.getName())) {
                resolvePath = path.rest();
            }
        }

        Map<String, TypedValue> variablesAndSources = ExpressionUtil.compileVariablesAndSources(context);

        Object first = path.first();
        if (ItemPath.isVariable(first)) {
            String variableName = ItemPath.toVariableName(first).getLocalPart();
            TypedValue variableValueAndDefinition = variablesAndSources.get(variableName);
            if (variableValueAndDefinition == null) {
                throw new ExpressionEvaluationException("No variable with name "+variableName+" in "+ context.getContextDescription());
            }
            Object variableValue = variableValueAndDefinition.getValue();

            if (variableValue == null) {
                return null;
            }
            if (variableValue instanceof Item || variableValue instanceof ItemDeltaItem<?,?>) {
                resolveContext = ExpressionUtil.toItemDeltaItem(variableValue, objectResolver,
                        "path expression in "+ context.getContextDescription(), result);
            } else if (variableValue instanceof PrismPropertyValue<?>){
                PrismValueDeltaSetTriple<V> outputTriple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
                outputTriple.addToZeroSet((V) variableValue);
                return ExpressionUtil.toOutputTriple(outputTriple, outputDefinition, context.getAdditionalConvertor(), null, protector, prismContext);
            } else {
                throw new ExpressionEvaluationException("Unexpected variable value "+variableValue+" ("+variableValue.getClass()+")");
            }

            resolvePath = path.rest();
        }

        if (resolveContext == null) {
            return null;
        }

        while (!resolvePath.isEmpty()) {

            if (resolveContext.isContainer()) {
                DefinitionResolver defResolver = (parentDef, path) -> {
                    if (parentDef != null && parentDef.isDynamic()) {
                        // This is the case of dynamic schema extensions, such as assignment extension.
                        // Those may not have a definition. In that case just assume strings.
                        // In fact, this is a HACK. All such schemas should have a definition.
                        // Otherwise there may be problems with parameter types for caching compiles scripts and so on.
                        return prismContext.definitionFactory().createPropertyDefinition(path.firstName(), PrimitiveType.STRING.getQname());
                    } else {
                        return null;
                    }
                };

                try {
                    resolveContext = resolveContext.findIdi(resolvePath.firstAsPath(), defResolver);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(e.getMessage()+"; resolving path "+resolvePath.firstAsPath()+" on "+resolveContext, e);
                }

                if (resolveContext == null) {
                    throw new ExpressionEvaluationException("Cannot find item using path "+path+" in "+ context.getContextDescription());
                }
                resolvePath = resolvePath.rest();

            } else if (resolveContext.isStructuredProperty()) {
                // The output path does not really matter. The delta will be converted to triple anyway
                // But the path cannot be null, oherwise the code will die
                resolveContext = resolveContext.resolveStructuredProperty(resolvePath, (PrismPropertyDefinition) outputDefinition,
                        ItemPath.EMPTY_PATH, prismContext);
                break;

            } else if (resolveContext.isNull()){
                break;

            } else {
                throw new ExpressionEvaluationException("Cannot resolve path "+resolvePath+" on "+resolveContext+" in "+ context.getContextDescription());
            }

        }

        PrismValueDeltaSetTriple<V> outputTriple = ItemDeltaUtil.toDeltaSetTriple((Item<V,D>)resolveContext.getItemOld(),
                (ItemDelta<V,D>)resolveContext.getDelta(), prismContext);

        if (outputTriple == null) {
            return null;
        }

        return ExpressionUtil.toOutputTriple(outputTriple, outputDefinition, context.getAdditionalConvertor(), null, protector, prismContext);
    }

    @Override
    public String shortDebugDump() {
        return "path: "+path;
    }
}
