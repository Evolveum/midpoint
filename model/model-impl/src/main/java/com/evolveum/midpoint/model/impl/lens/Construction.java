/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

/**
 * Live class that contains "construction" - a definition how to construct a
 * resource object. It in fact reflects the definition of ConstructionType but
 * it also contains "live" objects and can evaluate the construction. It also
 * contains intermediary and side results of the evaluation.
 *
 * @author Radovan Semancik
 * <p>
 * This class is Serializable but it is not in fact serializable. It
 * implements Serializable interface only to be storable in the
 * PrismPropertyValue.
 */
public class Construction<AH extends AssignmentHolderType> extends AbstractConstruction<AH, ConstructionType> {

    private static final Trace LOGGER = TraceManager.getTrace(Construction.class);

    private static final String OP_EVALUATE = Construction.class.getName() + ".evaluate";

    private ObjectType orderOneObject;
    private ResolvedResource resolvedResource;
    private ExpressionProfile expressionProfile;
    private MappingFactory mappingFactory;
    private MappingEvaluator mappingEvaluator;
    private Collection<MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>>> attributeMappings;
    private Collection<MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> associationMappings;
    private RefinedObjectClassDefinition refinedObjectClassDefinition;
    private List<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions;
    private AssignmentPathVariables assignmentPathVariables = null;
    private PrismContainerDefinition<ShadowAssociationType> associationContainerDefinition;
    private PrismObject<SystemConfigurationType> systemConfiguration; // only to provide $configuration variable (MID-2372)
    private LensProjectionContext projectionContext;

    public Construction(ConstructionType constructionType, ObjectType source) {
        super(constructionType, source);
        this.attributeMappings = null;
        // TODO: this is wrong. It should be set up during the evaluation process.
        this.expressionProfile = MiscSchemaUtil.getExpressionProfile();
    }

    void setOrderOneObject(ObjectType orderOneObject) {
        this.orderOneObject = orderOneObject;
    }

    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }

    public void setMappingFactory(MappingFactory mappingFactory) {
        this.mappingFactory = mappingFactory;
    }

    public MappingEvaluator getMappingEvaluator() {
        return mappingEvaluator;
    }

    public void setMappingEvaluator(MappingEvaluator mappingEvaluator) {
        this.mappingEvaluator = mappingEvaluator;
    }

    public PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return systemConfiguration;
    }

    public void setSystemConfiguration(PrismObject<SystemConfigurationType> systemConfiguration) {
        this.systemConfiguration = systemConfiguration;
    }

    RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
        return refinedObjectClassDefinition;
    }

    public void setRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
        this.refinedObjectClassDefinition = refinedObjectClassDefinition;
    }

    public List<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() {
        return auxiliaryObjectClassDefinitions;
    }

    public void addAuxiliaryObjectClassDefinition(
            RefinedObjectClassDefinition auxiliaryObjectClassDefinition) {
        if (auxiliaryObjectClassDefinitions == null) {
            auxiliaryObjectClassDefinitions = new ArrayList<>();
        }
        auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDefinition);
    }

    public ShadowKindType getKind() {
        if (refinedObjectClassDefinition == null) {
            throw new IllegalStateException("Kind can only be fetched from evaluated Construction");
        }
        return refinedObjectClassDefinition.getKind();
    }

    public String getIntent() {
        if (refinedObjectClassDefinition == null) {
            throw new IllegalStateException("Intent can only be fetched from evaluated Construction");
        }
        return refinedObjectClassDefinition.getIntent();
    }

    public Collection<MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>>> getAttributeMappings() {
        if (attributeMappings == null) {
            attributeMappings = new ArrayList<>();
        }
        return attributeMappings;
    }

    MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> getAttributeMapping(
            QName attrName) {
        for (MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> myVc : getAttributeMappings()) {
            if (myVc.getItemName().equals(attrName)) {
                return myVc;
            }
        }
        return null;
    }

    public void addAttributeMapping(
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping) {
        getAttributeMappings().add(mapping);
    }

    public Collection<MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> getAssociationMappings() {
        if (associationMappings == null) {
            associationMappings = new ArrayList<>();
        }
        return associationMappings;
    }

    public void addAssociationMapping(
            MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> mapping) {
        getAssociationMappings().add(mapping);
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    private ResourceType resolveTarget(String sourceDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionVariables variables = ModelImplUtils
                .getDefaultExpressionVariables(getFocusOdo().getNewObject().asObjectable(), null, null, null, mappingEvaluator.getPrismContext());
        if (assignmentPathVariables == null) {
            assignmentPathVariables = LensUtil.computeAssignmentPathVariables(getAssignmentPath());
        }
        ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, variables, getPrismContext());
        LOGGER.debug("Expression variables for filter evaluation: {}", variables);

        ObjectFilter origFilter = getPrismContext().getQueryConverter().parseFilter(getConstructionType().getResourceRef().getFilter(),
                ResourceType.class);
        LOGGER.debug("Orig filter {}", origFilter);
        ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(origFilter, variables,
                expressionProfile, getMappingFactory().getExpressionFactory(), getPrismContext(),
                " evaluating resource filter expression ", task, result);
        LOGGER.debug("evaluatedFilter filter {}", evaluatedFilter);

        if (evaluatedFilter == null) {
            throw new SchemaException(
                    "The OID is null and filter could not be evaluated in assignment targetRef in " + getSource());
        }

        final Collection<PrismObject<ResourceType>> results = new ArrayList<>();
        ResultHandler<ResourceType> handler = (object, parentResult) -> {
            LOGGER.info("Found object {}", object);
            return results.add(object);
        };
        getObjectResolver().searchIterative(ResourceType.class, getPrismContext().queryFactory().createQuery(evaluatedFilter),
                null, handler, task, result);

        if (org.apache.commons.collections.CollectionUtils.isEmpty(results)) {
            throw new ObjectNotFoundException("Got no target from repository, filter:" + evaluatedFilter
                    + ", class:" + ResourceType.class + " in " + sourceDescription);
        }

        if (results.size() > 1) {
            throw new IllegalArgumentException("Got more than one target from repository, filter:"
                    + evaluatedFilter + ", class:" + ResourceType.class + " in " + sourceDescription);
        }

        PrismObject<ResourceType> target = results.iterator().next();
        return target.asObjectable();
    }

    public ResourceType getResource() {
        if (resolvedResource != null) {
            return resolvedResource.resource;
        } else {
            throw new IllegalStateException("Couldn't access resolved resource reference as construction was not evaluated yet; in "
                    + getSource());
        }
    }

    public String getResourceOid() {
        ResourceType resource = getResource();
        if (resource != null) {
            return resource.getOid();
        } else {
            throw new IllegalStateException("Couldn't obtain resource OID because the resource does not exist in " + getSource());
        }
    }

    private ResourceType resolveResource(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (resolvedResource != null) {
            throw new IllegalStateException("Resolving the resource twice? In: " + getSource());
        } else {
            ObjectReferenceType resourceRef = getConstructionType().getResourceRef();
            if (resourceRef != null) {
                @NotNull ResourceType resource;
                //noinspection unchecked
                PrismObject<ResourceType> resourceFromRef = resourceRef.asReferenceValue().getObject();
                if (resourceFromRef != null) {
                    resource = resourceFromRef.asObjectable();
                } else {
                    ReferentialIntegrityType refIntegrity = getReferentialIntegrity(resourceRef);
                    try {
                        if (resourceRef.getOid() == null) {
                            resource = resolveTarget(" resolving resource ", task, result);
                        } else {
                            resource = LensUtil.getResourceReadOnly(getLensContext(), resourceRef.getOid(), getObjectResolver(),
                                    task, result);
                        }
                    } catch (ObjectNotFoundException e) {
                        if (refIntegrity == ReferentialIntegrityType.STRICT) {
                            throw new ObjectNotFoundException("Resource reference seems to be invalid in account construction in "
                                    + getSource() + ": " + e.getMessage(), e);
                        } else if (refIntegrity == ReferentialIntegrityType.RELAXED) {
                            LOGGER.warn("Resource reference couldn't be resolved in {}: {}", getSource(), e.getMessage(), e);
                            resolvedResource = new ResolvedResource(true);
                            return null;
                        } else if (refIntegrity == ReferentialIntegrityType.LAX) {
                            LOGGER.debug("Resource reference couldn't be resolved in {}: {}", getSource(), e.getMessage(), e);
                            resolvedResource = new ResolvedResource(false);
                            return null;
                        } else {
                            throw new IllegalStateException("Unsupported referential integrity: "
                                    + resourceRef.getReferentialIntegrity());
                        }
                    } catch (SecurityViolationException | CommunicationException | ConfigurationException e) {
                        throw new SystemException("Couldn't fetch the resource in account construction in "
                                + getSource() + ": " + e.getMessage(), e);
                    } catch (ExpressionEvaluationException e) {
                        throw new SystemException(
                                "Couldn't evaluate filter expression for the resource in account construction in "
                                        + getSource() + ": " + e.getMessage(),
                                e);
                    }
                }
                getConstructionType().getResourceRef().setOid(resource.getOid());
                resolvedResource = new ResolvedResource(resource);
                return resource;
            } else {
                throw new IllegalStateException("No resourceRef in account construction in " + getSource());
            }
        }
    }

    private ReferentialIntegrityType getReferentialIntegrity(ObjectReferenceType resourceRef) {
        ReferentialIntegrityType value = resourceRef.getReferentialIntegrity();
        if (value == null || value == ReferentialIntegrityType.DEFAULT) {
            return ReferentialIntegrityType.STRICT;
        } else {
            return value;
        }
    }

    public void evaluate(Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        // Subresult is needed here. If something fails here, this needs to be recorded as a subresult of
        // AssignmentProcessor.processAssignments. Otherwise partial error won't be propagated properly.
        OperationResult result = parentResult.createMinorSubresult(OP_EVALUATE);
        try {
            assignmentPathVariables = LensUtil.computeAssignmentPathVariables(getAssignmentPath());
            ResourceType resource = resolveResource(task, result);
            if (resource != null) {
                evaluateKindIntentObjectClass(resource);
                evaluateAttributes(task, result);
                evaluateAssociations(task, result);
                result.recordSuccess();
            } else {
                // If we are here (and not encountered an exception) it means that the resourceRef integrity was relaxed or lax.
                if (resolvedResource.warning) {
                    result.recordWarning("The resource could not be found");
                } else {
                    result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "The resource could not be found");
                }
            }
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }
    }

    private void evaluateKindIntentObjectClass(ResourceType resource) throws SchemaException {
        String resourceOid;
        if (getConstructionType().getResourceRef() != null) {
            resourceOid = getConstructionType().getResourceRef().getOid();
            if (resourceOid != null && !resource.getOid().equals(resourceOid)) {
                throw new IllegalStateException(
                        "The specified resource and the resource in construction does not match");
            }
        } else {
            resourceOid = null;
        }

        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource,
                LayerType.MODEL, getPrismContext());
        if (refinedSchema == null) {
            // Refined schema may be null in some error-related border cases
            throw new SchemaException("No (refined) schema for " + resource);
        }

        ShadowKindType kind = getConstructionType().getKind();
        if (kind == null) {
            kind = ShadowKindType.ACCOUNT;
        }
        refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(kind, getConstructionType().getIntent());

        if (refinedObjectClassDefinition == null) {
            if (getConstructionType().getIntent() != null) {
                throw new SchemaException(
                        "No " + kind + " type '" + getConstructionType().getIntent() + "' found in "
                                + resource + " as specified in construction in " + getSource());
            } else {
                throw new SchemaException("No default " + kind + " type found in " + resource
                        + " as specified in construction in " + getSource());
            }
        }

        auxiliaryObjectClassDefinitions = new ArrayList<>(getConstructionType().getAuxiliaryObjectClass().size());
        for (QName auxiliaryObjectClassName : getConstructionType().getAuxiliaryObjectClass()) {
            RefinedObjectClassDefinition auxOcDef = refinedSchema
                    .getRefinedDefinition(auxiliaryObjectClassName);
            if (auxOcDef == null) {
                throw new SchemaException(
                        "No auxiliary object class " + auxiliaryObjectClassName + " found in "
                                + resource + " as specified in construction in " + getSource());
            }
            auxiliaryObjectClassDefinitions.add(auxOcDef);
        }

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, kind, getConstructionType().getIntent(), null, false);
        projectionContext = getLensContext().findProjectionContext(rat);
        // projection context may not exist yet (existence might not be yet decided)
    }

    private void evaluateAttributes(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        attributeMappings = new ArrayList<>();
        for (ResourceAttributeDefinitionType attributeDefinition : getConstructionType().getAttribute()) {
            QName attrName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(attributeDefinition.getRef());
            if (attrName == null) {
                throw new SchemaException(
                        "No attribute name (ref) in attribute definition in account construction in "
                                + getSource());
            }
            if (!attributeDefinition.getInbound().isEmpty()) {
                throw new SchemaException("Cannot process inbound section in definition of attribute "
                        + attrName + " in account construction in " + getSource());
            }
            MappingType outboundMappingType = attributeDefinition.getOutbound();
            if (outboundMappingType == null) {
                throw new SchemaException("No outbound section in definition of attribute " + attrName
                        + " in account construction in " + getSource());
            }
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> attributeMapping = evaluateAttribute(
                    attributeDefinition, task, result);
            if (attributeMapping != null) {
                attributeMappings.add(attributeMapping);
            }
        }
    }

    private <T> MappingImpl<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> evaluateAttribute(
            ResourceAttributeDefinitionType attributeDefinition, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        QName attrName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(attributeDefinition.getRef());
        if (attrName == null) {
            throw new SchemaException("Missing 'ref' in attribute construction in account construction in "
                    + getSource());
        }
        if (!attributeDefinition.getInbound().isEmpty()) {
            throw new SchemaException("Cannot process inbound section in definition of attribute " + attrName
                    + " in account construction in " + getSource());
        }
        MappingType outboundMappingType = attributeDefinition.getOutbound();
        if (outboundMappingType == null) {
            throw new SchemaException("No outbound section in definition of attribute " + attrName
                    + " in account construction in " + getSource());
        }
        ResourceAttributeDefinition<T> outputDefinition = findAttributeDefinition(attrName);
        if (outputDefinition == null) {
            throw new SchemaException("Attribute " + attrName + " not found in schema for account type "
                    + getIntent() + ", " + resolvedResource.resource
                    + " as defined in " + getSource(), attrName);
        }
        MappingImpl.Builder<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> builder = mappingFactory.createMappingBuilder(
                outboundMappingType,
                "for attribute " + PrettyPrinter.prettyPrint(attrName) + " in " + getSource());

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

    private String getHumanReadableConstructionDescription() {
        return "construction for (" + (resolvedResource != null ? resolvedResource.resource : null)
                + "/" + getKind() + "/" + getIntent() + ") in " + getSource();
    }

    public <T> RefinedAttributeDefinition<T> findAttributeDefinition(QName attributeName) {
        if (refinedObjectClassDefinition == null) {
            throw new IllegalStateException(
                    "Construction " + this + " was not evaluated:\n" + this.debugDump());
        }
        RefinedAttributeDefinition<T> attrDef = refinedObjectClassDefinition
                .findAttributeDefinition(attributeName);
        if (attrDef != null) {
            return attrDef;
        }
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            attrDef = auxiliaryObjectClassDefinition.findAttributeDefinition(attributeName);
            if (attrDef != null) {
                return attrDef;
            }
        }
        return null;
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
        associationMappings = new ArrayList<>();
        for (ResourceObjectAssociationType associationDefinitionType : getConstructionType().getAssociation()) {
            QName assocName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(associationDefinitionType.getRef());
            if (assocName == null) {
                throw new SchemaException(
                        "No association name (ref) in association definition in construction in " + getSource());
            }
            MappingType outboundMappingType = associationDefinitionType.getOutbound();
            if (outboundMappingType == null) {
                throw new SchemaException("No outbound section in definition of association " + assocName
                        + " in construction in " + getSource());
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
            throw new SchemaException("Missing 'ref' in association in construction in " + getSource());
        }

        RefinedAssociationDefinition rAssocDef = refinedObjectClassDefinition.findAssociationDefinition(assocName);
        if (rAssocDef == null) {
            throw new SchemaException("No association " + assocName + " in object class "
                    + refinedObjectClassDefinition.getHumanReadableName() + " in construction in " + getSource());
        }
        // Make sure that assocName is complete with the namespace and all.
        assocName = rAssocDef.getName();

        MappingType outboundMappingType = associationDefinitionType.getOutbound();
        if (outboundMappingType == null) {
            throw new SchemaException("No outbound section in definition of association " + assocName
                    + " in construction in " + getSource());
        }
        PrismContainerDefinition<ShadowAssociationType> outputDefinition = getAssociationContainerDefinition();

        MappingImpl.Builder<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> mappingBuilder =
                mappingFactory.<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>createMappingBuilder()
                        .mappingType(outboundMappingType)
                        .contextDescription("for association " + PrettyPrinter.prettyPrint(assocName) + " in " + getSource())
                        .originType(OriginType.ASSIGNMENTS)
                        .originObject(getSource());

        ItemPath implicitTargetPath = ShadowType.F_ASSOCIATION.append(assocName); // not quite correct
        MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> evaluatedMapping = evaluateMapping(
                mappingBuilder, implicitTargetPath, assocName, outputDefinition, rAssocDef.getAssociationTarget(), task, result);

        LOGGER.trace("Evaluated mapping for association {}: {}", assocName, evaluatedMapping);
        return evaluatedMapping;
    }

    @SuppressWarnings("ConstantConditions")
    private <V extends PrismValue, D extends ItemDefinition<?>> MappingImpl<V, D> evaluateMapping(
            MappingImpl.Builder<V, D> builder, ItemPath implicitTargetPath, QName mappingQName, D outputDefinition,
            RefinedObjectClassDefinition assocTargetObjectClassDefinition, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        if (!builder.isApplicableToChannel(getChannel())) {
            return null;
        }

        builder = builder.mappingQName(mappingQName)
                .mappingKind(MappingKindType.CONSTRUCTION)
                .implicitTargetPath(implicitTargetPath)
                .sourceContext(getFocusOdo())
                .defaultTargetDefinition(outputDefinition)
                .originType(getOriginType())
                .originObject(getSource())
                .refinedObjectClassDefinition(refinedObjectClassDefinition)
                .rootNode(getFocusOdo())
                .addVariableDefinition(ExpressionConstants.VAR_USER, getFocusOdo())
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, getFocusOdo())
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, getSource(), ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_CONTAINING_OBJECT, getSource(), ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_ORDER_ONE_OBJECT, orderOneObject, ObjectType.class);

        if (assocTargetObjectClassDefinition != null) {
            builder = builder.addVariableDefinition(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION,
                    assocTargetObjectClassDefinition, RefinedObjectClassDefinition.class);
        }
        builder = builder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, getResource(), ResourceType.class);
        builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables, getPrismContext());
        if (getSystemConfiguration() != null) {
            builder = builder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, getSystemConfiguration(), SystemConfigurationType.class);
        }
        // TODO: other variables ?

        // Set condition masks. There are used as a brakes to avoid evaluating
        // to nonsense values in case user is not present
        // (e.g. in old values in ADD situations and new values in DELETE
        // situations).
        if (getFocusOdo().getOldObject() == null) {
            builder = builder.conditionMaskOld(false);
        }
        if (getFocusOdo().getNewObject() == null) {
            builder = builder.conditionMaskNew(false);
        }

        MappingImpl<V, D> mapping = builder.build();
        mappingEvaluator.evaluateMapping(mapping, getLensContext(), projectionContext, task, result);

        return mapping;
    }

    private PrismContainerDefinition<ShadowAssociationType> getAssociationContainerDefinition() {
        if (associationContainerDefinition == null) {
            PrismObjectDefinition<ShadowType> shadowDefinition = getPrismContext().getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(ShadowType.class);
            associationContainerDefinition = shadowDefinition
                    .findContainerDefinition(ShadowType.F_ASSOCIATION);
        }
        return associationContainerDefinition;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Construction<?> other = (Construction<?>) obj;
        if (assignmentPathVariables == null) {
            if (other.assignmentPathVariables != null) {
                return false;
            }
        } else if (!assignmentPathVariables.equals(other.assignmentPathVariables)) {
            return false;
        }
        if (associationContainerDefinition == null) {
            if (other.associationContainerDefinition != null) {
                return false;
            }
        } else if (!associationContainerDefinition.equals(other.associationContainerDefinition)) {
            return false;
        }
        if (associationMappings == null) {
            if (other.associationMappings != null) {
                return false;
            }
        } else if (!associationMappings.equals(other.associationMappings)) {
            return false;
        }
        if (attributeMappings == null) {
            if (other.attributeMappings != null) {
                return false;
            }
        } else if (!attributeMappings.equals(other.attributeMappings)) {
            return false;
        }
        if (auxiliaryObjectClassDefinitions == null) {
            if (other.auxiliaryObjectClassDefinitions != null) {
                return false;
            }
        } else if (!auxiliaryObjectClassDefinitions.equals(other.auxiliaryObjectClassDefinitions)) {
            return false;
        }
        if (mappingEvaluator == null) {
            if (other.mappingEvaluator != null) {
                return false;
            }
        } else if (!mappingEvaluator.equals(other.mappingEvaluator)) {
            return false;
        }
        if (mappingFactory == null) {
            if (other.mappingFactory != null) {
                return false;
            }
        } else if (!mappingFactory.equals(other.mappingFactory)) {
            return false;
        }
        if (orderOneObject == null) {
            if (other.orderOneObject != null) {
                return false;
            }
        } else if (!orderOneObject.equals(other.orderOneObject)) {
            return false;
        }
        if (refinedObjectClassDefinition == null) {
            if (other.refinedObjectClassDefinition != null) {
                return false;
            }
        } else if (!refinedObjectClassDefinition.equals(other.refinedObjectClassDefinition)) {
            return false;
        }
        if (resolvedResource == null) {
            if (other.resolvedResource != null) {
                return false;
            }
        } else if (!resolvedResource.equals(other.resolvedResource)) {
            return false;
        }
        return true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, "Construction", indent);
        if (refinedObjectClassDefinition == null) {
            sb.append(" (no object class definition)");
            if (getConstructionType() != null && getConstructionType().getResourceRef() != null) { // should
                // be
                // always
                // the
                // case
                sb.append("\n");
                DebugUtil.debugDumpLabel(sb, "resourceRef / kind / intent", indent + 1);
                sb.append(" ");
                sb.append(ObjectTypeUtil.toShortString(getConstructionType().getResourceRef()));
                sb.append(" / ");
                sb.append(getConstructionType().getKind());
                sb.append(" / ");
                sb.append(getConstructionType().getIntent());
            }
        } else {
            sb.append(refinedObjectClassDefinition.getShadowDiscriminator());
        }
        if (getConstructionType() != null && getConstructionType().getStrength() == ConstructionStrengthType.WEAK) {
            sb.append(" weak");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "isValid", isValid(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "wasValid", getWasValid(), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "relativityMode", getRelativityMode(), indent + 1);
        DebugUtil.debugDumpLabel(sb, "auxiliary object classes", indent + 1);
        if (auxiliaryObjectClassDefinitions == null) {
            sb.append(" (null)");
        } else if (auxiliaryObjectClassDefinitions.isEmpty()) {
            sb.append(" (empty)");
        } else {
            for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent + 2);
                sb.append(auxiliaryObjectClassDefinition.getTypeName());
            }
        }
        if (getConstructionType() != null && getConstructionType().getDescription() != null) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "description", indent + 1);
            sb.append(" ").append(getConstructionType().getDescription());
        }
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
        if (getAssignmentPath() != null) {
            sb.append("\n");
            sb.append(getAssignmentPath().debugDump(indent + 1));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Construction(");
        if (refinedObjectClassDefinition == null) {
            sb.append(getConstructionType());
        } else {
            sb.append(refinedObjectClassDefinition.getShadowDiscriminator());
        }
        sb.append(" in ").append(getSource());
        if (isValid()) {
            if (!getWasValid()) {
                sb.append(", invalid->valid");
            }
        } else {
            if (getWasValid()) {
                sb.append(", valid->invalid");
            } else {
                sb.append(", invalid");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Should this construction be ignored e.g. because the resource couldn't be resolved?
     * The construction was already evaluated.
     */
    public boolean isIgnored() {
        return getResource() == null;
    }

    public static class ResolvedResource {
        @Nullable public final ResourceType resource;
        public boolean warning;

        private ResolvedResource(@NotNull ResourceType resource) {
            this.resource = resource;
            this.warning = false;
        }

        private ResolvedResource(boolean warning) {
            this.resource = null;
            this.warning = warning;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof ResolvedResource)) { return false; }
            ResolvedResource that = (ResolvedResource) o;
            return warning == that.warning &&
                    Objects.equals(resource, that.resource);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, warning);
        }
    }
}
