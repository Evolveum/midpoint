/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author mederly
 * @author Radovan Semancik
 */
public class EvaluatedConstructionImpl<AH extends AssignmentHolderType> implements EvaluatedConstructible<AH>, EvaluatedConstruction {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedConstructionImpl.class);

    @NotNull final private Construction<AH,?> construction;
    @NotNull final private ResourceShadowDiscriminator rsd;
    @NotNull final private Collection<MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>>> attributeMappings = new ArrayList<>();;
    @NotNull final private Collection<MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> associationMappings = new ArrayList<>();
    private LensProjectionContext projectionContext;

    /**
     * @pre construction is already evaluated and not ignored (has resource)
     */
    EvaluatedConstructionImpl(@NotNull final Construction<AH,?> construction, @NotNull final ResourceShadowDiscriminator rsd) {
        this.construction = construction;
        this.rsd = rsd;
    }

    @Override
    public Construction<AH,?> getConstruction() {
        return construction;
    }

    public ResourceShadowDiscriminator getResourceShadowDiscriminator() {
        return rsd;
    }

    @Override
    public PrismObject<ResourceType> getResource() {
        return construction.getResource().asPrismObject();
    }

    @Override
    public ShadowKindType getKind() {
        return rsd.getKind();
    }

    @Override
    public String getIntent() {
        return rsd.getIntent();
    }

    @Override
    public String getTag() {
        return rsd.getTag();
    }

    @Override
    public boolean isDirectlyAssigned() {
        return construction.getAssignmentPath() == null || construction.getAssignmentPath().size() == 1;
    }

    @Override
    public AssignmentPath getAssignmentPath() {
        return construction.getAssignmentPath();
    }

    @Override
    public boolean isWeak() {
        return construction.isWeak();
    }

    public LensProjectionContext getProjectionContext() {
        return projectionContext;
    }

    public Collection<MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>>> getAttributeMappings() {
        return attributeMappings;
    }

    public MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> getAttributeMapping(QName attrName) {
        for (MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> myVc : getAttributeMappings()) {
            if (myVc.getItemName().equals(attrName)) {
                return myVc;
            }
        }
        return null;
    }

    protected void addAttributeMapping(
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping) {
        getAttributeMappings().add(mapping);
    }

    public Collection<MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> getAssociationMappings() {
        return associationMappings;
    }

    protected void addAssociationMapping(
            MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> mapping) {
        getAssociationMappings().add(mapping);
    }

    public NextRecompute evaluate(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        initializeProjectionContext();
        evaluateAttributes(task, result);
        evaluateAssociations(task, result);
        return null;
    }

    protected void initializeProjectionContext() {
        projectionContext = construction.getLensContext().findProjectionContext(rsd);
        // projection context may not exist yet (existence might not be yet decided)
    }


    private void evaluateAttributes(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        for (ResourceAttributeDefinitionType attributeDefinition : construction.getConstructionType().getAttribute()) {
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
            MappingType outboundMappingType = attributeDefinition.getOutbound();
            if (outboundMappingType == null) {
                throw new SchemaException("No outbound section in definition of attribute " + attrName
                        + " in account construction in " + construction.getSource());
            }
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> attributeMapping = evaluateAttribute(
                    attributeDefinition, task, result);
            if (attributeMapping != null) {
                addAttributeMapping(attributeMapping);
            }
        }
    }

    private <T> MappingImpl<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> evaluateAttribute(
            ResourceAttributeDefinitionType attributeDefinition, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        QName attrName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(attributeDefinition.getRef());
        if (attrName == null) {
            throw new SchemaException("Missing 'ref' in attribute construction in account construction in "
                    + construction.getSource());
        }
        if (!attributeDefinition.getInbound().isEmpty()) {
            throw new SchemaException("Cannot process inbound section in definition of attribute " + attrName
                    + " in account construction in " + construction.getSource());
        }
        MappingType outboundMappingType = attributeDefinition.getOutbound();
        if (outboundMappingType == null) {
            throw new SchemaException("No outbound section in definition of attribute " + attrName
                    + " in account construction in " + construction.getSource());
        }
        ResourceAttributeDefinition<T> outputDefinition = construction.findAttributeDefinition(attrName);
        if (outputDefinition == null) {
            throw new SchemaException("Attribute " + attrName + " not found in schema for account type "
                    + getIntent() + ", " + construction.getResolvedResource().resource
                    + " as defined in " + construction.getSource(), attrName);
        }
        MappingImpl.Builder<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> builder = construction.getMappingFactory().createMappingBuilder(
                outboundMappingType,
                "for attribute " + PrettyPrinter.prettyPrint(attrName) + " in " + construction.getSource());

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

    boolean hasValueForAttribute(QName attributeName) {
        for (MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> attributeConstruction : attributeMappings) {
            if (attributeName.equals(attributeConstruction.getItemName())) {
                PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> outputTriple = attributeConstruction
                        .getOutputTriple();
                if (outputTriple != null && !outputTriple.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void evaluateAssociations(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        for (ResourceObjectAssociationType associationDefinitionType : construction.getConstructionType().getAssociation()) {
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
            MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> assocMapping =
                    evaluateAssociation(associationDefinitionType, task, result);
            if (assocMapping != null) {
                associationMappings.add(assocMapping);
            }
        }
    }

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

        MappingImpl.Builder<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> mappingBuilder =
                construction.getMappingFactory().<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>createMappingBuilder()
                        .mappingType(outboundMappingType)
                        .contextDescription("for association " + PrettyPrinter.prettyPrint(assocName) + " in " + construction.getSource())
                        .originType(OriginType.ASSIGNMENTS)
                        .originObject(construction.getSource());

        ItemPath implicitTargetPath = ShadowType.F_ASSOCIATION.append(assocName); // not quite correct
        MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> evaluatedMapping = evaluateMapping(
                mappingBuilder, implicitTargetPath, assocName, outputDefinition, rAssocDef.getAssociationTarget(), task, result);

        LOGGER.trace("Evaluated mapping for association {}: {}", assocName, evaluatedMapping);
        return evaluatedMapping;
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> MappingImpl<V, D> evaluateMapping(
            MappingImpl.Builder<V, D> builder, ItemPath implicitTargetPath, QName mappingQName, D outputDefinition,
            RefinedObjectClassDefinition assocTargetObjectClassDefinition, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        construction.initializeMappingBuilder(builder, implicitTargetPath, mappingQName, outputDefinition, assocTargetObjectClassDefinition, task, result);

        // TODO
        // builder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, TODO);

        MappingImpl<V, D> mapping = builder.build();
            construction.getMappingEvaluator().evaluateMapping(mapping, construction.getLensContext(), projectionContext, task, result);

        return mapping;
    }


    private String getHumanReadableConstructionDescription() {
        return "construction for (" + (construction.getResolvedResource() != null ? construction.getResolvedResource().resource : null)
                + "/" + getKind() + "/" + getIntent() + "/" + getTag() + ") in " + construction.getSource();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, this.getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelShortDumpLn(sb, "discriminator", rsd, indent + 1);
        // We do not want to dump construction here. This can lead to cycles.
        // We usually dump EvaluatedConstruction is a Construction dump anyway, therefore the context should be quite clear.
        DebugUtil.debugDumpWithLabelToString(sb, "projectionContext", projectionContext, indent + 1);
        if (attributeMappings != null && !attributeMappings.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "attribute mappings", indent + 1);
            for (MappingImpl<?, ?> mapping : attributeMappings) {
                sb.append("\n");
                sb.append(mapping.debugDump(indent + 2));
            }
        }
        if (associationMappings != null && !associationMappings.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "association mappings", indent + 1);
            for (MappingImpl<?, ?> mapping : associationMappings) {
                sb.append("\n");
                sb.append(mapping.debugDump(indent + 2));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedConstructionImpl(" +
                "discriminator=" + rsd +
                ", construction=" + construction +
                ", projectionContext='" + projectionContext +
                ')';
    }
}
