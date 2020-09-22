/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
public class EvaluatedAssignedResourceObjectConstructionImpl<AH extends AssignmentHolderType> extends EvaluatedResourceObjectConstructionImpl<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedResourceObjectConstructionImpl.class);

    /**
     * Precondition: {@link ResourceObjectConstruction} is already evaluated and not ignored (has resource).
     */
    EvaluatedAssignedResourceObjectConstructionImpl(
            @NotNull ResourceObjectConstruction<AH, ?> construction,
            @NotNull ResourceShadowDiscriminator rsd) {
        super(construction, rsd);
    }

    public NextRecompute evaluate(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        initializeProjectionContext();
        evaluateAttributes(task, result);
        evaluateAssociations(task, result);
        return null;
    }

    private void evaluateAttributes(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        for (ResourceAttributeDefinitionType attributeDefinition : construction.getConstructionBean().getAttribute()) {
            QName attrName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(attributeDefinition.getRef());
            if (attrName == null) {
                throw new SchemaException(
                        "No attribute name (ref) in attribute definition in account construction in "
                                + construction.getSource());
            }
            if (!attributeDefinition.getInbound().isEmpty()) {
                throw new SchemaException("Cannot process inbound section in definition of attribute "
                        + attrName + " in account construction in " + construction.getSource());
            }
            MappingType outboundMappingBean = attributeDefinition.getOutbound();
            if (outboundMappingBean == null) {
                throw new SchemaException("No outbound section in definition of attribute " + attrName
                        + " in account construction in " + construction.getSource());
            }
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> attributeMapping =
                    evaluateAttribute(attrName, outboundMappingBean, task, result);
            if (attributeMapping != null) {
                addAttributeMapping(attributeMapping);
            }
        }
    }

    /**
     * @return null if mapping is not applicable
     */
    private <T> MappingImpl<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> evaluateAttribute(
            QName attrName, MappingType mappingBean, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        ResourceAttributeDefinition<T> outputDefinition = construction.findAttributeDefinition(attrName);
        if (outputDefinition == null) {
            throw new SchemaException("Attribute " + attrName + " not found in schema for account type "
                    + getIntent() + ", " + construction.getResolvedResource().resource
                    + " as defined in " + construction.getSource(), attrName);
        }
        String shortDesc = "for attribute " + PrettyPrinter.prettyPrint(attrName) + " in " + construction.getSource();
        MappingBuilder<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> builder =
                construction.getMappingFactory().createMappingBuilder(mappingBean, shortDesc);

        MappingImpl<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> evaluatedMapping;

        //noinspection CaughtExceptionImmediatelyRethrown
        try {

            evaluatedMapping = evaluateMapping(builder, ShadowType.F_ATTRIBUTES.append(attrName),
                    attrName, outputDefinition, null, task, result);

        } catch (SchemaException e) {
            throw new SchemaException(getAttributeEvaluationErrorMessage(attrName, e), e);
        } catch (ExpressionEvaluationException e) {
            // No need to specially handle this here. It was already handled in the expression-processing
            // code and it has proper description.
            throw e;
        } catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException(getAttributeEvaluationErrorMessage(attrName, e), e);
        } catch (SecurityViolationException e) {
            throw new SecurityViolationException(getAttributeEvaluationErrorMessage(attrName, e), e);
        } catch (ConfigurationException e) {
            throw new ConfigurationException(getAttributeEvaluationErrorMessage(attrName, e), e);
        } catch (CommunicationException e) {
            throw new CommunicationException(getAttributeEvaluationErrorMessage(attrName, e), e);
        }

        LOGGER.trace("Evaluated mapping for attribute {}: {}", attrName, evaluatedMapping);
        return evaluatedMapping;
    }

    private String getAttributeEvaluationErrorMessage(QName attrName, Exception e) {
        return "Error evaluating mapping for attribute " + PrettyPrinter.prettyPrint(attrName) + " in " + getHumanReadableConstructionDescription() + ": " + e.getMessage();
    }

    private void evaluateAssociations(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        for (ResourceObjectAssociationType associationDefinitionType : construction.getConstructionBean().getAssociation()) {
            QName assocName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(associationDefinitionType.getRef());
            if (assocName == null) {
                throw new SchemaException(
                        "No association name (ref) in association definition in construction in " + construction.getSource());
            }
            MappingType outboundMappingType = associationDefinitionType.getOutbound();
            if (outboundMappingType == null) {
                throw new SchemaException("No outbound section in definition of association " + assocName
                        + " in construction in " + construction.getSource());
            }
            CollectionUtils.addIgnoreNull(associationMappings, evaluateAssociation(associationDefinitionType, task, result));
        }
    }

    /**
     * @return null if mapping is not applicable
     */
    private MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> evaluateAssociation(
            ResourceObjectAssociationType associationDefinitionType, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        QName assocName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(associationDefinitionType.getRef());
        if (assocName == null) {
            throw new SchemaException("Missing 'ref' in association in construction in " + construction.getSource());
        }

        RefinedAssociationDefinition rAssocDef = construction.getRefinedObjectClassDefinition().findAssociationDefinition(assocName);
        if (rAssocDef == null) {
            throw new SchemaException("No association " + assocName + " in object class "
                    + construction.getRefinedObjectClassDefinition().getHumanReadableName() + " in construction in " + construction.getSource());
        }
        // Make sure that assocName is complete with the namespace and all.
        assocName = rAssocDef.getName();

        MappingType outboundMappingType = associationDefinitionType.getOutbound();
        if (outboundMappingType == null) {
            throw new SchemaException("No outbound section in definition of association " + assocName
                    + " in construction in " + construction.getSource());
        }
        PrismContainerDefinition<ShadowAssociationType> outputDefinition = construction.getAssociationContainerDefinition();

        MappingBuilder<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> mappingBuilder =
                construction.getMappingFactory().<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>createMappingBuilder()
                        .mappingBean(outboundMappingType)
                        .contextDescription("for association " + PrettyPrinter.prettyPrint(assocName) + " in " + construction.getSource())
                        .originType(OriginType.ASSIGNMENTS)
                        .originObject(construction.getSource());

        ItemPath implicitTargetPath = ShadowType.F_ASSOCIATION.append(assocName); // not quite correct
        MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> evaluatedMapping = evaluateMapping(
                mappingBuilder, implicitTargetPath, assocName, outputDefinition, rAssocDef.getAssociationTarget(), task, result);

        LOGGER.trace("Evaluated mapping for association {}: {}", assocName, evaluatedMapping);
        return evaluatedMapping;
    }

    /**
     * @return null if mapping is not applicable
     */
    private <V extends PrismValue, D extends ItemDefinition<?>> MappingImpl<V, D> evaluateMapping(
            MappingBuilder<V, D> builder, ItemPath implicitTargetPath, QName mappingQName, D outputDefinition,
            RefinedObjectClassDefinition assocTargetObjectClassDefinition, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        builder = construction.initializeMappingBuilder(builder, implicitTargetPath, mappingQName, outputDefinition, assocTargetObjectClassDefinition, task, result);
        if (builder == null) {
            return null;
        }

        // TODO
        // builder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, TODO);

        MappingImpl<V, D> mapping = builder.build();

        construction.getMappingEvaluator().evaluateMapping(mapping, construction.getLensContext(), projectionContext, task, result);

        return mapping;
    }

}
