/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.ValueSetDefinition;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;

/**
 * TODO better name, clean up the code; maybe move operation result management code here
 */
class MappingParser<D extends ItemDefinition<?>, MBT extends AbstractMappingType> implements Serializable {

    private final AbstractMappingImpl<?, D, MBT> m;

    /**
     * Definition of the output item (i.e. target).
     */
    private D outputDefinition;

    /**
     * Path of the output item (i.e. target) in the targetContext.
     */
    private ItemPath outputPath;

    MappingParser(AbstractMappingImpl<?, D, MBT> mapping) {
        this.m = mapping;
    }

    void parseSourcesAndTarget(OperationResult result) throws SchemaException, CommunicationException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        parseSources(result);
        parseTarget();
        assertOutputDefinition();
    }

    private void assertOutputDefinition() {
        if (outputPath != null && outputDefinition == null) {
            throw new IllegalArgumentException("No output definition, cannot evaluate " + m.getMappingContextDescription());
        }
    }

    private void parseTarget() throws SchemaException {
        VariableBindingDefinitionType targetSpecification = m.mappingBean.getTarget();
        if (targetSpecification == null) {
            outputDefinition = m.defaultTargetDefinition;
            outputPath = m.defaultTargetPath;
        } else {
            ItemPathType itemPathType = targetSpecification.getPath();
            if (itemPathType == null) {
                outputDefinition = m.defaultTargetDefinition;
                outputPath = m.defaultTargetPath;
            } else {
                ItemPath path = itemPathType.getItemPath();
                outputDefinition = ExpressionUtil.resolveDefinitionPath(
                        path, m.variables, m.targetContext,
                        "target definition in " + m.getMappingContextDescription());
                if (outputDefinition == null) {
                    throw new SchemaException("No target item that would conform to the path "
                            + path + " in " + m.getMappingContextDescription());
                }
                outputPath = path.stripVariableSegment();
            }
        }
        if (m.valuePolicySupplier != null) {
            m.valuePolicySupplier.setOutputDefinition(outputDefinition);
            m.valuePolicySupplier.setOutputPath(outputPath);
        }
    }

    private void parseSources(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        if (m.defaultSource != null) {
            m.sources.add(m.defaultSource);
            m.defaultSource.recompute();
        }
        // FIXME remove this ugly hack
        if (m.mappingBean instanceof MappingType) {
            for (VariableBindingDefinitionType sourceDefinition : m.mappingBean.getSource()) {
                Source<?, ?> source = parseSource(sourceDefinition, result);
                source.recompute();

                // Override existing sources (e.g. default source)
                m.sources.removeIf(existing -> existing.getName().equals(source.getName()));
                m.sources.add(source);
            }
        }
    }

    @NotNull ItemPath getSourcePath(VariableBindingDefinitionType sourceType) throws SchemaException {
        ItemPathType itemPathType = sourceType.getPath();
        if (itemPathType == null) {
            throw new SchemaException("No path in source definition in " + m.getMappingContextDescription());
        }
        ItemPath path = itemPathType.getItemPath();
        if (path.isEmpty()) {
            throw new SchemaException("Empty source path in " + m.getMappingContextDescription());
        }
        return path;
    }

    private <IV extends PrismValue, ID extends ItemDefinition<?>> Source<IV, ID> parseSource(
            VariableBindingDefinitionType sourceDefinition, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ItemPath path = getSourcePath(sourceDefinition);
        @NotNull QName sourceQName = sourceDefinition.getName() != null ? sourceDefinition.getName() : ItemPath.toName(path.last());
        String variableName = sourceQName.getLocalPart();

        TypedValue<?> typedSourceObject = ExpressionUtil.resolvePathGetTypedValue(path, m.variables, true,
                m.getTypedSourceContext(), ModelCommonBeans.get().objectResolver, PrismContext.get(),
                "source definition in " + m.getMappingContextDescription(), m.getTask(), result);

        Object sourceObject = typedSourceObject != null ? typedSourceObject.getValue() : null;
        Item<IV, ID> itemOld = null;
        ItemDelta<IV, ID> delta = null;
        Item<IV, ID> itemNew = null;
        ItemPath resolvePath = path;
        ItemPath residualPath = null;
        Collection<? extends ItemDelta<?, ?>> subItemDeltas = null;
        if (sourceObject != null) {
            if (sourceObject instanceof ItemDeltaItem<?, ?>) {
                //noinspection unchecked
                itemOld = ((ItemDeltaItem<IV, ID>) sourceObject).getItemOld();
                //noinspection unchecked
                delta = ((ItemDeltaItem<IV, ID>) sourceObject).getDelta();
                //noinspection unchecked
                itemNew = ((ItemDeltaItem<IV, ID>) sourceObject).getItemNew();
                //noinspection unchecked
                residualPath = ((ItemDeltaItem<IV, ID>) sourceObject).getResidualPath();
                //noinspection unchecked
                resolvePath = ((ItemDeltaItem<IV, ID>) sourceObject).getResolvePath();
                //noinspection unchecked
                subItemDeltas = ((ItemDeltaItem<IV, ID>) sourceObject).getSubItemDeltas();
            } else if (sourceObject instanceof Item<?, ?>) {
                //noinspection unchecked
                itemOld = (Item<IV, ID>) sourceObject;
                //noinspection unchecked
                itemNew = (Item<IV, ID>) sourceObject;
            } else {
                throw new IllegalStateException("Unknown resolve result " + sourceObject);
            }
        }

        ID sourceItemDefinition = typedSourceObject != null ? typedSourceObject.getDefinition() : null;

        // apply domain
        ValueSetDefinitionType domainSetType = sourceDefinition.getSet();
        if (domainSetType != null) {
            ValueSetDefinition<IV, ID> setDef = new ValueSetDefinition<>(
                    domainSetType, sourceItemDefinition, m.valueMetadataDefinition,
                    m.expressionProfile, variableName, null,
                    "domain of " + variableName, "domain of " + variableName + " in " + m.getMappingContextDescription(),
                    m.getTask(), result);
            setDef.init(ModelCommonBeans.get().expressionFactory);
            setDef.setAdditionalVariables(m.variables);
            try {

                if (itemOld != null) {
                    //noinspection unchecked
                    itemOld = itemOld.clone();
                    itemOld.filterValues(setDef::containsTunnel);
                    itemOld.filterYields(setDef::containsYieldTunnel);
                }

                if (itemNew != null) {
                    //noinspection unchecked
                    itemNew = itemNew.clone();
                    itemNew.filterValues(setDef::containsTunnel);
                    itemNew.filterYields(setDef::containsYieldTunnel);
                }

                if (delta != null) {
                    delta = delta.clone();
                    delta.filterValues(setDef::containsTunnel);
                    delta.filterYields(setDef::containsYieldTunnel);
                }

            } catch (TunnelException te) {
                unwrapTunnelException(te);
            }
        }

        Source<IV, ID> source = new Source<>(itemOld, delta, itemNew, sourceQName, sourceItemDefinition);
        source.setResidualPath(residualPath);
        source.setResolvePath(resolvePath);
        source.setSubItemDeltas(subItemDeltas);
        return source;
    }

    private void unwrapTunnelException(TunnelException te)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Throwable cause = te.getCause();
        if (cause instanceof SchemaException) {
            throw (SchemaException) cause;
        } else if (cause instanceof ExpressionEvaluationException) {
            throw (ExpressionEvaluationException) cause;
        } else if (cause instanceof ObjectNotFoundException) {
            throw (ObjectNotFoundException) cause;
        } else if (cause instanceof CommunicationException) {
            throw (CommunicationException) cause;
        } else if (cause instanceof ConfigurationException) {
            throw (ConfigurationException) cause;
        } else if (cause instanceof SecurityViolationException) {
            throw (SecurityViolationException) cause;
        } else {
            throw te;
        }
    }

    D getOutputDefinition() {
        if (outputDefinition == null) {
            try {
                parseTarget();
            } catch (SchemaException e) {
                throw new SystemException(e); // we assume that targets are (usually) already parsed
            }
        }
        return outputDefinition;
    }

    ItemPath getOutputPath() {
        if (outputDefinition == null) {
            try {
                parseTarget();
            } catch (SchemaException e) {
                throw new SystemException(e); // we assume that targets are (usually) already parsed
            }
        }
        return outputPath;
    }
}
