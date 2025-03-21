/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.EvaluatedResourceObjectConstruction;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.forModelContext;

/**
 * Contains "construction bean" (ConstructionType) - a definition how to construct a resource object.
 * Besides this definition it also contains auxiliary objects that are needed to evaluate the construction.
 *
 * An instance of this class produces one or more "evaluated" constructions: more of them in case that
 * multiaccounts (tags) are used. Evaluated constructions are represented by evaluatedConstructionTriple.
 *
 * @author Radovan Semancik
 */
public abstract class ResourceObjectConstruction<
        AH extends AssignmentHolderType, EC extends EvaluatedResourceObjectConstructionImpl<AH, ?>>
        extends AbstractConstruction<AH, ConstructionType, EC> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectConstruction.class);

    /**
     * Information on the resource and its resolution status.
     * Can be provided by the builder or by this class.
     */
    private ResolvedConstructionResource resolvedResource;

    // Useful definitions.

    /**
     * The object type or object class definition for the resource object.
     *
     * [EP:M:Tag] DONE we rely on the fact that this definition is related to {@link #getResource()}
     */
    private ResourceObjectDefinition resourceObjectDefinition;

    /**
     * Auxiliary OCDs mentioned in the construction bean OR all auxiliary OCDs from rOCD.
     */
    private final List<ResourceObjectDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>();

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

    public DeltaSetTriple<EC> getEvaluatedConstructionTriple() {
        return evaluatedConstructionTriple;
    }

    //region Construction evaluation
    /**
     * Evaluates this construction. Note that evaluation is delegated to {@link EvaluatedResourceObjectConstruction} objects,
     * which are created here (based on tag mapping evaluation).
     */
    public NextRecompute evaluate(Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        // Subresult is needed here. If something fails here, this needs to be recorded as a subresult of
        // AssignmentProcessor.processAssignments. Otherwise partial error won't be propagated properly.
        OperationResult result = parentResult.createMinorSubresult(getClass().getName() + ".evaluate"); // different for each subclass
        try {
            LOGGER.trace("Evaluating construction '{}' in {}", this, this.getSource());

            resolveResource(task, result);
            if (hasResource()) {
                initializeDefinitions();
                createEvaluatedConstructions(task, result);
                return evaluateConstructions(task, result);
            } else {
                // If we are here (and not encountered an exception) it means that the resourceRef integrity was relaxed or lax.
                if (resolvedResource.warning) {
                    result.recordWarning("The resource could not be found");
                } else {
                    result.recordNotApplicable("The resource could not be found");
                }
                return null;
            }
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    private void createEvaluatedConstructions(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        evaluatedConstructionTriple = PrismContext.get().deltaFactory().createDeltaSetTriple();

        PrismValueDeltaSetTriple<PrismPropertyValue<String>> tagTriple = evaluateTagTriple(task, result);
        if (tagTriple == null) {
            // Single-account case (not multi-account). We just create a single EvaluatedConstruction
            EC evaluatedConstruction = createEvaluatedConstruction((String)null);
            evaluatedConstructionTriple.addToZeroSet(evaluatedConstruction);
        } else {
            tagTriple.transform(evaluatedConstructionTriple, tag -> createEvaluatedConstruction(tag.getRealValue()));
        }
    }

    private PrismValueDeltaSetTriple<PrismPropertyValue<String>> evaluateTagTriple(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceObjectMultiplicityType multiplicity = resourceObjectDefinition.getObjectMultiplicity();
        if (!RefinedDefinitionUtil.isMultiaccount(multiplicity)) {
            return null;
        }
        ShadowTagSpecificationType tagSpec = multiplicity.getTag();
        if (tagSpec == null) {
            // TODO: do something better
            return null;
        }
        MappingType tagMappingBean = tagSpec.getOutbound();
        if (tagMappingBean == null) {
            // TODO: do something better
            return null;
        }

        // [EP:M:Tag] DONE, obviously the tag corresponds to the resource
        ConfigurationItemOrigin origin = ConfigurationItemOrigin.inResourceOrAncestor(getResource());

        MappingBuilder<PrismPropertyValue<String>, PrismPropertyDefinition<String>> builder =
                getMappingFactory().createMappingBuilder( // [EP:M:Tag] DONE
                        tagMappingBean, origin,
                        "for outbound tag mapping in " + getSource());

        builder = initializeMappingBuilder(
                builder, ShadowType.F_TAG, ShadowType.F_TAG, createTagDefinition(), null, task);
        if (builder == null) {
            return null;
        }
        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> mapping = builder.build();

        getMappingEvaluator().evaluateMapping(
                mapping,
                forModelContext(getLensContext()),
                task,
                result);

        return mapping.getOutputTriple();
    }

    private @NotNull PrismPropertyDefinition<String> createTagDefinition() {
        return PrismContext.get().definitionFactory().newPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, PrimitiveType.STRING.getQname(), 0, -1);
    }

    private EC createEvaluatedConstruction(String tag) {
        ResourceObjectDefinition objectDefinition = getResourceObjectDefinitionRequired();
        ConstructionTargetKey key;
        ResourceObjectTypeIdentification typeIdentification = objectDefinition.getTypeIdentification();
        if (typeIdentification != null) {
            // this is the usual case
            key = new ConstructionTargetKey(
                    getResourceOid(),
                    typeIdentification.getKind(),
                    typeIdentification.getIntent(),
                    tag);
        } else {
            // let's check if we can go with the default account definition (see TestAssignmentErrors.test100-101)
            if (objectDefinition.getObjectClassDefinition().isDefaultAccountDefinition()) {
                key = new ConstructionTargetKey(
                        getResourceOid(),
                        ShadowKindType.ACCOUNT,
                        SchemaConstants.INTENT_DEFAULT,
                        tag);
            } else {
                throw new IllegalStateException(
                        "Construction " + this + " cannot be evaluated because of missing type definition for " + objectDefinition);
            }
        }
        return createEvaluatedConstruction(key);
    }

    /**
     * @param targetKey Projection into which this construction belong. Must be classified!
     */
    protected abstract EC createEvaluatedConstruction(@NotNull ConstructionTargetKey targetKey);

    private NextRecompute evaluateConstructions(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        NextRecompute nextRecompute = null;

        // This code may seem primitive and old-fashioned.
        // But equivalent functional code (using foreach()) is just insane due to nextRecompute and exception handling.
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction : evaluatedConstructionTriple.getZeroSet()) {
            NextRecompute constructionNextRecompute = evaluatedConstruction.evaluate(task, result);
            nextRecompute = NextRecompute.update(constructionNextRecompute, nextRecompute);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction : evaluatedConstructionTriple.getPlusSet()) {
            NextRecompute constructionNextRecompute = evaluatedConstruction.evaluate(task, result);
            nextRecompute = NextRecompute.update(constructionNextRecompute, nextRecompute);
        }
        for (EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction : evaluatedConstructionTriple.getMinusSet()) {
            NextRecompute constructionNextRecompute = evaluatedConstruction.evaluate(task, result);
            nextRecompute = NextRecompute.update(constructionNextRecompute, nextRecompute);
        }

        return nextRecompute;
    }

    /**
     * @return null if mapping is not applicable
     */
    <V extends PrismValue, D extends ItemDefinition<?>> MappingBuilder<V, D> initializeMappingBuilder(
            MappingBuilder<V, D> builder,
            ItemPath implicitTargetPath,
            QName targetItemName,
            D outputDefinition,
            ShadowAssociationDefinition associationDefinition,
            Task task) throws SchemaException {

        if (!builder.isApplicableToChannel(lensContext.getChannel())) {
            LOGGER.trace("Skipping outbound mapping for {} because the channel does not match", implicitTargetPath);
            return null;
        }
        if (!builder.isApplicableToExecutionMode(task.getExecutionMode())) {
            LOGGER.trace("Skipping outbound mapping for {} because the execution mode does not match", implicitTargetPath);
            return null;
        }

        ObjectDeltaObject<AH> focusOdoAbsolute = getFocusOdoAbsolute();

        builder = builder.targetItemName(targetItemName)
                .mappingKind(MappingKindType.CONSTRUCTION)
                .implicitTargetPath(implicitTargetPath)
                .defaultSourceContextIdi(focusOdoAbsolute)
                .defaultTargetDefinition(outputDefinition)
                .defaultTargetPath(implicitTargetPath)
                .originType(originType)
                .originObject(source)
                .addRootVariableDefinition(focusOdoAbsolute)
                .addVariableDefinition(ExpressionConstants.VAR_USER, focusOdoAbsolute)
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdoAbsolute)
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, source, ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_CONTAINING_OBJECT, source, ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_THIS_OBJECT,
                        assignmentPath != null ? assignmentPath.getConstructionThisObject() : null, ObjectType.class)
                .ignoreValueMetadata();

        if (associationDefinition != null) {
            builder = builder.addVariableDefinition(
                    ExpressionConstants.VAR_ASSOCIATION_DEFINITION, associationDefinition, ShadowReferenceAttributeDefinition.class);
        }
        builder = builder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, getResource(), ResourceType.class);
        builder = LensUtil.addAssignmentPathVariables(builder, getAssignmentPathVariables());
        builder = builder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, lensContext.getSystemConfiguration(), SystemConfigurationType.class);
        // TODO: other variables ?

        // Set condition masks. There are used as a brakes to avoid evaluating
        // to nonsense values in case user is not present
        // (e.g. in old values in ADD situations and new values in DELETE
        // situations).
        if (focusOdoAbsolute.getOldObject() == null) {
            builder = builder.conditionMaskOld(false);
        }
        if (focusOdoAbsolute.getNewObject() == null) {
            builder = builder.conditionMaskNew(false);
        }

        builder.now(now);

        return builder;
    }
    //endregion

    //region Trivia
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ResourceObjectConstruction<?, ?> that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(resolvedResource, that.resolvedResource);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, this.getClass().getSimpleName(), indent);
        if (resourceObjectDefinition == null) {
            sb.append(" (no object type/class definition - yet)");
            if (constructionBean != null && constructionBean.getResourceRef() != null) { // should be always the case
                sb.append("\n");
                DebugUtil.debugDumpLabel(sb, "resourceRef / kind / intent (in bean)", indent + 1);
                sb.append(" ");
                sb.append(ObjectTypeUtil.toShortString(constructionBean.getResourceRef()));
                sb.append(" / ");
                sb.append(constructionBean.getKind());
                sb.append(" / ");
                sb.append(constructionBean.getIntent());
            }
        } else {
            sb.append(resourceObjectDefinition);
        }
        if (constructionBean != null && constructionBean.getStrength() == ConstructionStrengthType.WEAK) {
            sb.append(" weak");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "valid", isValid(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "wasValid", getWasValid(), indent + 1);
        DebugUtil.debugDumpLabel(sb, "auxiliary object classes", indent + 1);
        if (auxiliaryObjectClassDefinitions.isEmpty()) {
            sb.append(" (empty)");
        } else {
            for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent + 2);
                sb.append(auxiliaryObjectClassDefinition.getTypeName());
            }
        }
        debugDumpConstructionDescription(sb, indent);
        debugDumpAssignmentPath(sb, indent);
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
        if (resourceObjectDefinition == null) {
            sb.append(constructionBean);
        } else {
            sb.append(resourceObjectDefinition);
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
        return ModelBeans.get().mappingFactory;
    }

    MappingEvaluator getMappingEvaluator() {
        return ModelBeans.get().mappingEvaluator;
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }
    //endregion

    //region Resource management

    ResolvedConstructionResource getResolvedResource() {
        return resolvedResource;
    }

    void setResolvedResource(ResolvedConstructionResource resolvedResource) {
        this.resolvedResource = resolvedResource;
    }

    protected abstract void resolveResource(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException;

    /**
     * Should this construction be ignored e.g. because the resource couldn't be resolved?
     * The construction was already evaluated.
     */
    public boolean isIgnored() {
        return !hasResource();
    }

    private boolean hasResource() {
        return getResource() != null;
    }

    public ResourceType getResource() {
        if (resolvedResource != null) {
            return resolvedResource.resource;
        } else {
            throw new IllegalStateException("Couldn't access resolved resource reference as construction "
                    + "was not evaluated/initialized yet; in " + source);
        }
    }

    public @NotNull String getResourceOid() {
        ResourceType resource = getResource();
        if (resource != null) {
            return MiscUtil.stateNonNull(resource.getOid(), () -> "No resource OID");
        } else {
            throw new IllegalStateException("Couldn't obtain resource OID because the resource does not exist in " + getSource());
        }
    }
    //endregion

    //region Definitions management

    protected abstract void initializeDefinitions() throws SchemaException, ConfigurationException;

    public ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    @NotNull ResourceObjectDefinition getResourceObjectDefinitionRequired() {
        return Objects.requireNonNull(resourceObjectDefinition, () -> "no resource object definition in " + this);
    }

    void setResourceObjectDefinition(ResourceObjectDefinition resourceObjectDefinition) {
        this.resourceObjectDefinition = resourceObjectDefinition;
    }

    public List<ResourceObjectDefinition> getAuxiliaryObjectClassDefinitions() {
        return auxiliaryObjectClassDefinitions;
    }

    void addAuxiliaryObjectClassDefinition(ResourceObjectDefinition auxiliaryObjectClassDefinition) {
        auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDefinition);
    }

    public ShadowAttributeDefinition<?, ?, ?, ?> findAttributeDefinition(QName attributeName) {
        if (resourceObjectDefinition == null) {
            throw new IllegalStateException("Construction " + this + " was not evaluated:\n" + this.debugDump());
        }
        var inMain = resourceObjectDefinition.findAttributeDefinition(attributeName);
        if (inMain != null) {
            return inMain;
        }
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            var auxAttrDef = auxiliaryObjectClassDefinition.findAttributeDefinition(attributeName);
            if (auxAttrDef != null) {
                return auxAttrDef;
            }
        }
        return null;
    }

    @NotNull ShadowAssociationDefinition findAssociationDefinitionRequired(QName associationName, Object errorCtx)
            throws ConfigurationException {
        if (resourceObjectDefinition == null) {
            throw new IllegalStateException("Construction " + this + " was not evaluated:\n" + this.debugDump());
        }
        var inMain = resourceObjectDefinition.findAssociationDefinition(associationName);
        if (inMain != null) {
            return inMain;
        }
        throw new ConfigurationException(
                "Association '%s' not found in schema for resource object %s; as defined in %s".formatted(
                        associationName, resourceObjectDefinition, errorCtx));
    }
    //endregion

    //region Other
    protected void loadFullShadow(LensProjectionContext projectionContext, String desc, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ModelBeans.get().contextLoader.loadFullShadow(projectionContext, desc, task, result);
    }
    //endregion
}
