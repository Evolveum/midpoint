/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Provides logic for automatically suggesting {@link ShadowAssociationTypeDefinitionType} definitions
 * based on the structure of native reference attributes in the resource schema.
 * <p>
 * This class inspects subject and object object types defined in the resource,
 * evaluates their reference attributes, and generates association suggestions accordingly.
 * </p>
 *
 * The resulting {@link AssociationsSuggestionType} object can be used to configure
 * associations between object types in MidPoint.
 */
public class SmartAssociationImpl {

    private static final Trace LOGGER = TraceManager.getTrace(SmartAssociationImpl.class);

    private List<ResourceObjectTypeDefinition> findObjectTypesForObjectClass(ResourceSchema resourceSchema, QName objectClassName) {
        return resourceSchema.getObjectTypeDefinitions().stream()
                .filter(ot -> ot.getObjectClassName().equals(objectClassName))
                .toList();
    }

    private boolean isReferenceComplex(ShadowReferenceAttributeDefinition ref) {
        var isComplex = ref.getNativeDefinition().isComplexAttribute();
        var isTargetingEmbeddedClass = ref.isTargetingSingleEmbeddedObjectClass();
        var isTargetingAuxiliaryClass = ref.getGeneralizedObjectSideObjectDefinition().getObjectClassDefinition().isAuxiliary();
        return isComplex || isTargetingEmbeddedClass || isTargetingAuxiliaryClass;
    }

    /**
     * Suggests potential associations between the provided subject and object types on the given resource.
     * Associations are derived by analyzing reference attributes in native object class definitions.
     *
     * @param resource The resource in which to evaluate possible associations.
     * @param isInbound if true suggest inbound mapping otherwise suggest outbound mapping
     * @return Suggested associations as {@link AssociationsSuggestionType}.
     */
    public AssociationsSuggestionType suggestSmartAssociation(
            @NotNull ResourceType resource,
            boolean isInbound
    ) throws SchemaException, ConfigurationException {

        var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
        NativeResourceSchema nativeSchema = resourceSchema.getNativeSchema();

        AssociationsSuggestionType associationsSuggestionType = new AssociationsSuggestionType();

        for (var subjectObjectType : resourceSchema.getObjectTypeDefinitions()) {
            for (var refAttribute : subjectObjectType.getReferenceAttributeDefinitions()) {
                if (!refAttribute.canRead()) {
                    continue;
                }
                var isObjectToSubject = refAttribute.getParticipantRole().equals(ShadowReferenceParticipantRole.OBJECT);
                if (isObjectToSubject) {
                    // object-to-subject not supported
                    continue;
                }
                if (isReferenceComplex(refAttribute)) {
                    // complex associations not supported yet
                    continue;
                }
                var targetObjectClass = refAttribute.getTargetObjectClassName();
                var objectObjectTypes = findObjectTypesForObjectClass(resourceSchema, targetObjectClass);
                for (var objectObjectType : objectObjectTypes) {
                    var associationDef = resolveRefBasedAssociation(nativeSchema, subjectObjectType, refAttribute, objectObjectType, isInbound);
                    var suggestion = new AssociationSuggestionType().definition(associationDef);
                    associationsSuggestionType.getAssociation().add(suggestion);
                }
            }
        }

        return associationsSuggestionType;
    }

    /**
     * Resolves candidate associations for a given reference attribute by identifying matching participants
     * and constructing a {@link ShadowAssociationTypeDefinitionType} for each match.
     *
     * @param nativeSchema The native resource schema.
     * @param isInbound inbound/outbound flag
     * @return A list of derived association definitions.
     */
    private ShadowAssociationTypeDefinitionType resolveRefBasedAssociation(
            @NotNull NativeResourceSchema nativeSchema,
            @NotNull ResourceObjectTypeDefinition subjectObjectType,
            @NotNull ShadowReferenceAttributeDefinition attributeReference,
            @NotNull ResourceObjectTypeDefinition objectObjectType,
            boolean isInbound) {

        ShadowAssociationTypeDefinitionType association = new ShadowAssociationTypeDefinitionType();

        String assocName = constructAssociationName(subjectObjectType, objectObjectType, attributeReference);
        QName assocQName = new QName(assocName);
        association.setName(assocQName);
        association.setDisplayName(assocName); // TODO: generate better display name

        var subject = buildSubjectParticipantDefinitionType(subjectObjectType, objectObjectType, attributeReference, assocName, isInbound);
        var object = buildObjectParticipantDefinitionType(objectObjectType);

        String descriptionNote = createAssociationDescription(attributeReference, subjectObjectType, objectObjectType);

        association.setSubject(subject);
        association.getObject().add(object);
        association.setDescription(descriptionNote);
        return association;
    }

    /**
     * Builds a human-readable description of the generated association for trace/debug purposes.
     *
     * @return A string describing how the association was derived.
     */
    private @NotNull String createAssociationDescription(
            @NotNull ShadowReferenceAttributeDefinition attributeReference,
            @NotNull ResourceObjectTypeDefinition subjectObjectType,
            @NotNull ResourceObjectTypeDefinition objectObjectType) {
        return "This association is derived from reference attribute '" + attributeReference.getItemName().getLocalPart() + "' on "
                + "first participant (class=" + subjectObjectType.getObjectClassName()
                + ", kind=" + subjectObjectType.getKind()
                + ", intent=" + subjectObjectType.getIntent() + ") "
                + "which refers to second participant (class=" + objectObjectType.getObjectClassName()
                + ", kind=" + objectObjectType.getKind()
                + ", intent=" + objectObjectType.getIntent() + ").";
    }

    @SuppressWarnings("unchecked")
    private <T> ExpressionType makeExpressionType(QName elementName, T container) {
        ExpressionType expression = new ExpressionType();
        var clazz = (Class<T>) container.getClass();
        JAXBElement<T> element = new JAXBElement<>(elementName, clazz, container);
        expression.expressionEvaluator(element);
        return expression;
    }

    private MappingType makeOutboundMapping(String associationName) {
        var fromLinkEvaluator = new AssociationFromLinkExpressionEvaluatorType();
        var fromLinkExpression = makeExpressionType(SchemaConstantsGenerated.C_ASSOCIATION_FROM_LINK, fromLinkEvaluator);

        var constructionEvaluator = new AssociationConstructionExpressionEvaluatorType()
                .objectRef(new AttributeOutboundMappingsDefinitionType()
                        .mapping(new MappingType()
                                .name("associationFromLink")
                                .expression(fromLinkExpression)
                        )
                );
        var constructionExpression = makeExpressionType(SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION, constructionEvaluator);
        var outboundMappingName = associationName + "-outbound";
        return new MappingType()
                .name(outboundMappingName)
                .strength(MappingStrengthType.STRONG)
                .expression(constructionExpression);
    }

    private InboundMappingType makeInboundMapping(String associationName) {
        var mapping = new InboundMappingType()
                .name("shadowOwner-into-targetRef")
                .target(new VariableBindingDefinitionType().path(new ItemPathType(ItemPath.fromString("targetRef"))))
                .expression(makeExpressionType(SchemaConstantsGenerated.C_SHADOW_OWNER_REFERENCE_SEARCH, new ShadowOwnerReferenceSearchExpressionEvaluatorType()));
        var synchronizationEvaluator = new AssociationSynchronizationExpressionEvaluatorType()
                .synchronization(new ItemSynchronizationReactionsType()
                        .reaction(new ItemSynchronizationReactionType()
                                .name("unmatched-add")
                                .situation(ItemSynchronizationSituationType.UNMATCHED)
                                .actions(new ItemSynchronizationActionsType()
                                        .addFocusValue(new AddFocusValueItemSynchronizationActionType())
                                )
                        )
                        .reaction(new ItemSynchronizationReactionType()
                                .name("matched-synchronize")
                                .situation(ItemSynchronizationSituationType.MATCHED)
                                .actions(new ItemSynchronizationActionsType()
                                        .synchronize(new SynchronizeItemSynchronizationActionType())
                                )
                        )
                        .reaction(new ItemSynchronizationReactionType()
                                .name("indirectly-matched-ignore")
                                .situation(ItemSynchronizationSituationType.MATCHED_INDIRECTLY)
                        )
                )
                .objectRef(new AttributeInboundMappingsDefinitionType()
                        .correlator(new ItemCorrelatorDefinitionType())
                        .mapping(mapping)
                );
        var synchronizationExpression = makeExpressionType(SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION, synchronizationEvaluator);

        var inboundMappingName = associationName + "-inbound";
        return new InboundMappingType()
                .name(inboundMappingName)
                .expression(synchronizationExpression);
    }

    /**
     * Association ref is a custom attribute without namespace that identifies the reference
     * - on GUI it can either distinguish (if different) or merge (if same) objects with the same subject coming from different association types
     * - in the association suggestion is this ref constructed in a way it distinguishes between these objects (suffixing with an object intent)
     */
    private ItemPathType buildAssociationRef(ResourceObjectTypeDefinition objectObjectType, ShadowReferenceAttributeDefinition attributeReference) {
        var refName = attributeReference.getItemName().withoutNamespace() + "-" + objectObjectType.getIntent();
        return new ItemPathType(ItemPath.fromString(refName));
    }

    /**
     * Builds a subject-side association participant definition.
     *
     * @param subjectObjectType association subject
     * @param attributeReference referenced attribute
     * @param associationName Root association name
     * @param isInbound inbound/outbound flag
     * @return A fully initialized {@link ShadowAssociationTypeSubjectDefinitionType}.
     */
    private @NotNull ShadowAssociationTypeSubjectDefinitionType buildSubjectParticipantDefinitionType(
            @NotNull ResourceObjectTypeDefinition subjectObjectType,
            @NotNull ResourceObjectTypeDefinition objectObjectType,
            @NotNull ShadowReferenceAttributeDefinition attributeReference,
            String associationName,
            boolean isInbound
    ) {

        var sourceRef = attributeReference.getItemName().toBean();
        var ref = buildAssociationRef(objectObjectType, attributeReference);
        ShadowAssociationDefinitionType assocDef = new ShadowAssociationDefinitionType()
                .ref(ref)
                .sourceAttributeRef(sourceRef);

        if (isInbound) {
            assocDef.inbound(makeInboundMapping(associationName));
        } else {
            assocDef.outbound(makeOutboundMapping(associationName));
        }

        return new ShadowAssociationTypeSubjectDefinitionType()
                .ref(subjectObjectType.getObjectClassName())
                .association(assocDef)
                .objectType(new ResourceObjectTypeIdentificationType()
                        .kind(subjectObjectType.getKind())
                        .intent(subjectObjectType.getIntent()));
    }

    /**
     * Builds an object-side association participant definition.
     *
     * @param objectObjectType association object.
     * @return A fully initialized {@link ShadowAssociationTypeObjectDefinitionType}.
     */
    private @NotNull ShadowAssociationTypeObjectDefinitionType buildObjectParticipantDefinitionType(
            @NotNull ResourceObjectTypeDefinition objectObjectType) {

        return new ShadowAssociationTypeObjectDefinitionType()
                .ref(objectObjectType.getObjectClassName())
                .objectType(new ResourceObjectTypeIdentificationType()
                        .kind(objectObjectType.getKind())
                        .intent(objectObjectType.getIntent()));
    }

    //TODO: Design better strategy for generating association names. Also think for combined/separated associations.

    /**
     * Constructs a machine-readable and unique name for an association based on subject/object kinds,
     * intents, and the reference attribute.
     *
     * @param objectObjectType association object.
     * @param subjectObjectType association subject.
     * @param refAttr Reference attribute the association is based on.
     * @return The generated association name.
     */
    private @NotNull String constructAssociationName(
            @NotNull ResourceObjectTypeDefinition subjectObjectType,
            @NotNull ResourceObjectTypeDefinition objectObjectType,
            @NotNull ShadowReferenceAttributeDefinition refAttr) {
        String firstParticipantSubjectKind = StringUtils.capitalize(subjectObjectType.getKind().value());
        String secondParticipantSubjectKind = StringUtils.capitalize(objectObjectType.getKind().value());

        String firstParticipantSubjectIntent = StringUtils.capitalize(subjectObjectType.getIntent()
                .replaceAll("[^a-zA-Z0-9]", ""));

        String secondParticipantSubjectIntent = StringUtils.capitalize(objectObjectType.getIntent()
                .replaceAll("[^a-zA-Z0-9]", ""));

        return firstParticipantSubjectKind + firstParticipantSubjectIntent + "-"
                + secondParticipantSubjectKind+secondParticipantSubjectIntent;
    }
}
