/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Contains "construction bean" (ConstructionType) - a definition how to construct a resource object.
 * Besides this definition it also contains auxiliary objects that are needed to evaluate the construction.
 *
 * An instance of this class may produce many "evaluated" constructions, e.g. in case that
 * multiaccounts (tags) are used. Evaluated constructions are represented by evaluatedConstructionTriple.
 *
 * @author Radovan Semancik
 */
public class ResourceObjectConstruction<AH extends AssignmentHolderType, EC extends EvaluatedResourceObjectConstructionImpl<AH>>
        extends AbstractConstruction<AH, ConstructionType, EC> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectConstruction.class);

    /**
     * Information on the resource and its resolution status.
     * Can be provided by the builder or by this class.
     */
    private ResolvedConstructionResource resolvedResource;

    // Useful definitions.

    /**
     * The rOCD for the resource object.
     */
    private RefinedObjectClassDefinition refinedObjectClassDefinition;

    /**
     * Auxiliary OCDs mentioned in the construction bean OR all auxiliary OCDs from rOCD.
     */
    private final List<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>();

    /**
     * Definition for associations.
     */
    private PrismContainerDefinition<ShadowAssociationType> associationContainerDefinition;

    /**
     * Delta set triple for evaluated constructions. These correspond to tags triple:
     * - if tags are not used then there is a single zero-set evaluated construction;
     * - if tags are used then the evaluated constructions are modeled after tag triple (plus/minus/zero).
     */
    private DeltaSetTriple<EC> evaluatedConstructionTriple;

    ResourceObjectConstruction(ResourceObjectConstructionBuilder<AH, EC, ?> builder) {
        super(builder);
        this.resolvedResource = builder.resolvedResource;
    }

    public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
        return refinedObjectClassDefinition;
    }

    public void setRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
        this.refinedObjectClassDefinition = refinedObjectClassDefinition;
    }

    public List<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() {
        return auxiliaryObjectClassDefinitions;
    }

    public void addAuxiliaryObjectClassDefinition(RefinedObjectClassDefinition auxiliaryObjectClassDefinition) {
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

    public DeltaSetTriple<EC> getEvaluatedConstructionTriple() {
        return evaluatedConstructionTriple;
    }

    public ResourceType getResource() {
        if (resolvedResource != null) {
            return resolvedResource.resource;
        } else {
            throw new IllegalStateException("Couldn't access resolved resource reference as construction "
                    + "was not evaluated/initialized yet; in " + source);
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

    public NextRecompute evaluate(Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        // Subresult is needed here. If something fails here, this needs to be recorded as a subresult of
        // AssignmentProcessor.processAssignments. Otherwise partial error won't be propagated properly.
        OperationResult result = parentResult.createMinorSubresult(getClass().getName() + ".evaluate"); // different for each subclass
        try {
            LOGGER.trace("Evaluating construction '{}' in {}", this, this.getSource());

            resolveResource(task, result);
            if (hasResource()) {
                initializeDefinitions();
                createEvaluatedConstructions(task, result);
                NextRecompute nextRecompute = evaluateConstructions(task, result);
                result.recordSuccess();
                return nextRecompute;
            } else {
                // If we are here (and not encountered an exception) it means that the resourceRef integrity was relaxed or lax.
                if (resolvedResource.warning) {
                    result.recordWarning("The resource could not be found");
                } else {
                    result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "The resource could not be found");
                }
                return null;
            }
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }
    }

    private boolean hasResource() {
        return getResource() != null;
    }

    protected void resolveResource(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (resolvedResource != null) {
            throw new IllegalStateException("Resolving the resource twice? In: " + source);
        } else {
            ConstructionResourceResolver resourceResolver = new ConstructionResourceResolver(this, task, result);
            resolvedResource = resourceResolver.resolveResource();
        }
    }

    protected void initializeDefinitions() throws SchemaException {
        assert resolvedResource.resource != null;
        assert constructionBean != null;

        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resolvedResource.resource,
                LayerType.MODEL, beans.prismContext);
        if (refinedSchema == null) {
            // Refined schema may be null in some error-related border cases
            throw new SchemaException("No (refined) schema for " + resolvedResource.resource);
        }

        ShadowKindType kind = defaultIfNull(constructionBean.getKind(), ShadowKindType.ACCOUNT);
        String intent = constructionBean.getIntent();

        refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(kind, intent);
        if (refinedObjectClassDefinition == null) {
            if (intent != null) {
                throw new SchemaException(
                        "No " + kind + " type '" + intent + "' found in "
                                + resolvedResource.resource + " as specified in construction in " + getSource());
            } else {
                throw new SchemaException("No default " + kind + " type found in " + resolvedResource.resource
                        + " as specified in construction in " + getSource());
            }
        }

        auxiliaryObjectClassDefinitions.clear();
        for (QName auxiliaryObjectClassName : constructionBean.getAuxiliaryObjectClass()) {
            RefinedObjectClassDefinition auxOcDef = refinedSchema.getRefinedDefinition(auxiliaryObjectClassName);
            if (auxOcDef == null) {
                throw new SchemaException(
                        "No auxiliary object class " + auxiliaryObjectClassName + " found in "
                                + resolvedResource.resource + " as specified in construction in " + source);
            }
            auxiliaryObjectClassDefinitions.add(auxOcDef);
        }
    }

    protected void createEvaluatedConstructions(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        evaluatedConstructionTriple = beans.prismContext.deltaFactory().createDeltaSetTriple();

        PrismValueDeltaSetTriple<PrismPropertyValue<String>> tagTriple = evaluateTagTriple(task, result);
        if (tagTriple == null) {
            // Single-account case (not multi-account). We just create a simple EvaluatedConstruction
            EC evaluatedConstruction = createEvaluatedConstruction((String)null);
            evaluatedConstructionTriple.addToZeroSet(evaluatedConstruction);

        } else {

            tagTriple.transform(evaluatedConstructionTriple, tag -> createEvaluatedConstruction(tag.getRealValue()));
        }
    }


    private PrismValueDeltaSetTriple<PrismPropertyValue<String>> evaluateTagTriple(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ResourceObjectMultiplicityType multiplicity = refinedObjectClassDefinition.getMultiplicity();
        if (!RefinedDefinitionUtil.isMultiaccount(multiplicity)) {
            return null;
        }
        ShadowTagSpecificationType tagSpec = multiplicity.getTag();
        if (tagSpec == null) {
            // TODO: do something better
            return null;
        }
        MappingType outboundMappingSpec = tagSpec.getOutbound();
        if (outboundMappingSpec == null) {
            // TODO: do something better
            return null;
        }

        MappingBuilder<PrismPropertyValue<String>, PrismPropertyDefinition<String>> builder = beans.mappingFactory.createMappingBuilder(
                outboundMappingSpec,
                "for outbound tag mapping in " + getSource());

        MutablePrismPropertyDefinition<String> outputDefinition = beans.mappingFactory.getExpressionFactory().getPrismContext().definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, PrimitiveType.STRING.getQname());
        outputDefinition.setMaxOccurs(-1);

        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> evaluatedMapping = evaluateMapping(builder, ShadowType.F_TAG,
                ShadowType.F_TAG, outputDefinition, null, task, result);

        if (evaluatedMapping != null) {
            return evaluatedMapping.getOutputTriple();
        } else {
            return null;
        }
    }

    private EC createEvaluatedConstruction(String tag) {
        ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(getResourceOid(), refinedObjectClassDefinition.getKind(), refinedObjectClassDefinition.getIntent(), tag, false);
        return createEvaluatedConstruction(rsd);
    }

    protected EC createEvaluatedConstruction(ResourceShadowDiscriminator rsd) {
        //noinspection unchecked
        return (EC) new EvaluatedResourceObjectConstructionImpl<>(this, rsd);
    }

    protected NextRecompute evaluateConstructions(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        NextRecompute nextRecompute = null;

        // This code may seem primitive and old-fashioned.
        // But equivalent functional code (using foreach()) is just insane due to nextRecompute and exception handling.
        for (EvaluatedResourceObjectConstructionImpl<AH> evaluatedConstruction : evaluatedConstructionTriple.getZeroSet()) {
            NextRecompute constructionNextRecompute = evaluatedConstruction.evaluate(task, result);
            nextRecompute = NextRecompute.update(constructionNextRecompute, nextRecompute);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH> evaluatedConstruction : evaluatedConstructionTriple.getPlusSet()) {
            NextRecompute constructionNextRecompute = evaluatedConstruction.evaluate(task, result);
            nextRecompute = NextRecompute.update(constructionNextRecompute, nextRecompute);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH> evaluatedConstruction : evaluatedConstructionTriple.getMinusSet()) {
            NextRecompute constructionNextRecompute = evaluatedConstruction.evaluate(task, result);
            nextRecompute = NextRecompute.update(constructionNextRecompute, nextRecompute);
        }

        return nextRecompute;
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

    public PrismContainerDefinition<ShadowAssociationType> getAssociationContainerDefinition() {
        if (associationContainerDefinition == null) {
            PrismObjectDefinition<ShadowType> shadowDefinition = beans.prismContext.getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(ShadowType.class);
            associationContainerDefinition = shadowDefinition
                    .findContainerDefinition(ShadowType.F_ASSOCIATION);
        }
        return associationContainerDefinition;
    }

    /**
     * @return null if mapping is not applicable
     */
    <V extends PrismValue, D extends ItemDefinition<?>> MappingBuilder<V, D> initializeMappingBuilder(
            MappingBuilder<V, D> builder, ItemPath implicitTargetPath, QName mappingQName, D outputDefinition,
            RefinedObjectClassDefinition assocTargetObjectClassDefinition, Task task, OperationResult result) throws SchemaException {

        if (!builder.isApplicableToChannel(lensContext.getChannel())) {
            return null;
        }

        builder = builder.mappingQName(mappingQName)
                .mappingKind(MappingKindType.CONSTRUCTION)
                .implicitTargetPath(implicitTargetPath)
                .sourceContext(getFocusOdoAbsolute())
                .defaultTargetDefinition(outputDefinition)
                .defaultTargetPath(implicitTargetPath)
                .originType(getOriginType())
                .originObject(getSource())
                .refinedObjectClassDefinition(getRefinedObjectClassDefinition())
                .rootNode(getFocusOdoAbsolute())
                .addVariableDefinition(ExpressionConstants.VAR_USER, getFocusOdoAbsolute())
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, getFocusOdoAbsolute())
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, getSource(), ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_CONTAINING_OBJECT, getSource(), ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_THIS_OBJECT,
                        assignmentPath != null ? assignmentPath.getConstructionThisObject() : null, ObjectType.class);

        if (assocTargetObjectClassDefinition != null) {
            builder = builder.addVariableDefinition(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION,
                    assocTargetObjectClassDefinition, RefinedObjectClassDefinition.class);
        }
        builder = builder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, getResource(), ResourceType.class);
        builder = LensUtil.addAssignmentPathVariables(builder, getAssignmentPathVariables(), beans.prismContext);
        builder = builder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, lensContext.getSystemConfiguration(), SystemConfigurationType.class);
        // TODO: other variables ?

        // Set condition masks. There are used as a brakes to avoid evaluating
        // to nonsense values in case user is not present
        // (e.g. in old values in ADD situations and new values in DELETE
        // situations).
        if (getFocusOdoAbsolute().getOldObject() == null) {
            builder = builder.conditionMaskOld(false);
        }
        if (getFocusOdoAbsolute().getNewObject() == null) {
            builder = builder.conditionMaskNew(false);
        }

        return builder;
    }

    /**
     * @return null if mapping is not applicable
     */
    private <V extends PrismValue, D extends ItemDefinition<?>> MappingImpl<V, D> evaluateMapping(
            MappingBuilder<V, D> builder, ItemPath implicitTargetPath, QName mappingQName, D outputDefinition,
            RefinedObjectClassDefinition assocTargetObjectClassDefinition, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        builder = initializeMappingBuilder(builder, implicitTargetPath, mappingQName, outputDefinition, assocTargetObjectClassDefinition, task, result);
        if (builder == null) {
            return null;
        }

        MappingImpl<V, D> mapping = builder.build();
        beans.mappingEvaluator.evaluateMapping(mapping, getLensContext(), null, task, result);

        return mapping;
    }

    protected void loadFullShadow(LensProjectionContext projectionContext, String desc, Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        beans.contextLoader.loadFullShadow(getLensContext(), projectionContext, desc, task, result);
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
        ResourceObjectConstruction<?,?> other = (ResourceObjectConstruction<?,?>) obj;
        if (associationContainerDefinition == null) {
            if (other.associationContainerDefinition != null) {
                return false;
            }
        } else if (!associationContainerDefinition.equals(other.associationContainerDefinition)) {
            return false;
        }
        if (auxiliaryObjectClassDefinitions == null) {
            if (other.auxiliaryObjectClassDefinitions != null) {
                return false;
            }
        } else if (!auxiliaryObjectClassDefinitions.equals(other.auxiliaryObjectClassDefinitions)) {
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


    /**
     * Should this construction be ignored e.g. because the resource couldn't be resolved?
     * The construction was already evaluated.
     */
    public boolean isIgnored() {
        return getResource() == null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, this.getClass().getSimpleName(), indent);
        if (refinedObjectClassDefinition == null) {
            sb.append(" (no object class definition)");
            if (constructionBean != null && constructionBean.getResourceRef() != null) { // should be always the case
                sb.append("\n");
                DebugUtil.debugDumpLabel(sb, "resourceRef / kind / intent", indent + 1);
                sb.append(" ");
                sb.append(ObjectTypeUtil.toShortString(constructionBean.getResourceRef()));
                sb.append(" / ");
                sb.append(constructionBean.getKind());
                sb.append(" / ");
                sb.append(constructionBean.getIntent());
            }
        } else {
            sb.append(refinedObjectClassDefinition.getShadowDiscriminator());
        }
        if (constructionBean != null && constructionBean.getStrength() == ConstructionStrengthType.WEAK) {
            sb.append(" weak");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "valid", isValid(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "wasValid", getWasValid(), indent + 1);
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
        if (constructionBean != null && constructionBean.getDescription() != null) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "description", indent + 1);
            sb.append(" ").append(constructionBean.getDescription());
        }

        if (assignmentPath != null) {
            sb.append("\n");
            sb.append(assignmentPath.debugDump(indent + 1));
        }
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "evaluated constructions", indent + 1);
        if (evaluatedConstructionTriple == null) {
            sb.append(" (null)");
        } else if (evaluatedConstructionTriple.isEmpty()) {
            sb.append(" (empty)");
        } else {
            sb.append("\n");
            sb.append(evaluatedConstructionTriple.debugDump(indent + 2));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Construction(");
        if (refinedObjectClassDefinition == null) {
            sb.append(constructionBean);
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

    public MappingFactory getMappingFactory() {
        return beans.mappingFactory;
    }

    public MappingEvaluator getMappingEvaluator() {
        return beans.mappingEvaluator;
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }

    public ResolvedConstructionResource getResolvedResource() {
        return resolvedResource;
    }

}
