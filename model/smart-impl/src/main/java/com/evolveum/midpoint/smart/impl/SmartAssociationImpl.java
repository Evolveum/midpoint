/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Describes one participant (subject or object) in an association.
     *
     * @param participantRole                       The role of the participant (subject or object).
     * @param participantObjectClassDefinition      Native object class definition for the participant.
     * @param participantObjectTypeIdentification   Identification of the participant's object type (kind + intent).
     */
    private record ParticipantDescriptor(
            ShadowReferenceParticipantRole participantRole,
            NativeObjectClassDefinition participantObjectClassDefinition,
            ResourceObjectTypeIdentification participantObjectTypeIdentification) {

        public boolean isSubject() {
            return participantRole == ShadowReferenceParticipantRole.SUBJECT;
        }
    }

    /**
     * Suggests potential associations between the provided subject and object types on the given resource.
     * Associations are derived by analyzing reference attributes in native object class definitions.
     *
     * @param resource The resource in which to evaluate possible associations.
     * @param subjectTypeIdentifications Identifiers of object types considered as "subjects".
     * @param objectTypeIdentifications Identifiers of object types considered as "objects".
     * @param combinedAssociation If true, associationSuggestion objects will be combined into a single association
     * @return Suggested associations as {@link AssociationsSuggestionType}.
     */
    public AssociationsSuggestionType suggestSmartAssociation(
            @NotNull ResourceType resource,
            @NotNull Collection<ResourceObjectTypeIdentification> subjectTypeIdentifications,
            @NotNull Collection<ResourceObjectTypeIdentification> objectTypeIdentifications,
            boolean combinedAssociation) throws SchemaException,
            ConfigurationException {

        var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);

        if (refinedSchema == null) {
            throw new ConfigurationException("Resource schema is not available for resource: " + resource.getName());
        }

        NativeResourceSchema nativeSchema = refinedSchema.getNativeSchema();

        Map<QName, List<ParticipantDescriptor>> objectClassParticipantsMap = new HashMap<>();

        registerParticipantDescriptorValues(subjectTypeIdentifications, ShadowReferenceParticipantRole.SUBJECT,
                resourceSchema, nativeSchema, objectClassParticipantsMap);
        registerParticipantDescriptorValues(objectTypeIdentifications, ShadowReferenceParticipantRole.OBJECT,
                resourceSchema, nativeSchema, objectClassParticipantsMap);

        AssociationsSuggestionType associationsSuggestionType = new AssociationsSuggestionType();

        objectClassParticipantsMap.values().stream()
                .flatMap(List::stream)
                .filter(ParticipantDescriptor::isSubject)
                .forEach(pair -> {
                    var firstParticipantRole = pair.participantRole();
                    var firstParticipantDef = pair.participantObjectClassDefinition();
                    var firstParticipantObjectTypeIdentification = pair.participantObjectTypeIdentification();

                    debugParticipantInfo(firstParticipantDef, firstParticipantObjectTypeIdentification);

                   var firstParticipantRefAttributeDefinitions = firstParticipantDef.getReferenceAttributeDefinitions();

                    firstParticipantRefAttributeDefinitions.forEach(firstParticipantRefAttr -> {
                        var associations = resolveRefBasedAssociations(
                                nativeSchema,
                                objectClassParticipantsMap,
                                firstParticipantRefAttr,
                                firstParticipantDef,
                                firstParticipantRole,
                                firstParticipantObjectTypeIdentification,
                                combinedAssociation);
                        debugAssociationResult(associations, firstParticipantObjectTypeIdentification, firstParticipantRefAttr);
                        associations.stream()
                                .map(def -> new AssociationSuggestionType().definition(def))
                                .forEach(associationsSuggestionType.getAssociation()::add);
                    });

                });

        return associationsSuggestionType;
    }

    /**
     * Resolves candidate associations for a given reference attribute by identifying matching participants
     * and constructing a {@link ShadowAssociationTypeDefinitionType} for each match.
     *
     * @param nativeSchema The native resource schema.
     * @param classDefRoleMap Participant role map indexed by object class name.
     * @param firstParticipantRefAttr The reference attribute to evaluate.
     * @param firstParticipantDef The source participant's object class definition.
     * @param firstParticipantRole The role (subject or object) of the source participant.
     * @param firstParticipantObjectTypeIdentification The source participant's identification (kind, intent).
     * @param combinedAssociation If true, associations will be combined into a single association
     * @return A list of derived association definitions.
     */
    private @NotNull List<ShadowAssociationTypeDefinitionType> resolveRefBasedAssociations(
            @NotNull NativeResourceSchema nativeSchema,
            @NotNull Map<QName, List<ParticipantDescriptor>> classDefRoleMap,
            @NotNull NativeShadowReferenceAttributeDefinition firstParticipantRefAttr,
            @NotNull NativeObjectClassDefinition firstParticipantDef,
            @NotNull ShadowReferenceParticipantRole firstParticipantRole,
            @NotNull ResourceObjectTypeIdentification firstParticipantObjectTypeIdentification,
            boolean combinedAssociation) {

        List<ShadowAssociationTypeDefinitionType> result = new ArrayList<>();

        ItemName refAttrItemName = firstParticipantRefAttr.getItemName();
        QName referencedAttributeObjectClassName = firstParticipantRefAttr.getReferencedObjectClassName();


        // TODO: In some cases, the reference attribute may not set the referenced object class name.
        //  Check DummyAdAssociationsScenario -> AccountGroup -> .withObjectClassNames(Account.OBJECT_CLASS_NAME.local(), Group.OBJECT_CLASS_NAME.local()) (member)
        if (referencedAttributeObjectClassName == null) {
            LOGGER.warn("   Attribute does not have a properly set referenced object class name. Reference attribute: {}",
                    refAttrItemName);
            return result;
        }

        List<ParticipantDescriptor> linkedObjects = findMatchingParticipantDefinitions(
                referencedAttributeObjectClassName,
                firstParticipantObjectTypeIdentification,
                classDefRoleMap,
                ShadowReferenceParticipantRole.OBJECT);

        if (linkedObjects == null) {
            LOGGER.warn("   No class definition found for referenced object class name: {}",
                    referencedAttributeObjectClassName);
            return result;
        }

        //TODO: combine associations if requested (means association can have multiple objects/subjects - not separated)
        for (var pair : linkedObjects) {
            NativeObjectClassDefinition secondParticipantDef = pair.participantObjectClassDefinition();
            ResourceObjectTypeIdentification secondParticipantTypeIdentification = pair.participantObjectTypeIdentification();

            ShadowAssociationTypeDefinitionType association = new ShadowAssociationTypeDefinitionType();

            String assocName = constructAssociationName(firstParticipantObjectTypeIdentification, secondParticipantTypeIdentification,
                    refAttrItemName);
            QName assocQName = new QName(nativeSchema.getNamespace(), assocName);
            association.setName(assocQName);

            ShadowAssociationTypeSubjectDefinitionType subject;
            ShadowAssociationTypeObjectDefinitionType object;

            // Currently we want to support subjectToObject associations only, but should we use refAttributeDef.getReferenceParticipantRole()?
            if (firstParticipantRole == ShadowReferenceParticipantRole.SUBJECT) {
                subject = buildSubjectParticipantDefinitionType(firstParticipantObjectTypeIdentification, firstParticipantDef);
                object = buildObjectParticipantDefinitionType(secondParticipantTypeIdentification, secondParticipantDef);
            } else {
                //NOTE this is never used in the current implementation, but it is here for completeness.
                subject = buildSubjectParticipantDefinitionType(secondParticipantTypeIdentification, secondParticipantDef);
                object = buildObjectParticipantDefinitionType(firstParticipantObjectTypeIdentification, firstParticipantDef);
            }

            String descriptionNote = createAssociationDescription(refAttrItemName,
                    firstParticipantDef, firstParticipantObjectTypeIdentification,
                    secondParticipantDef, secondParticipantTypeIdentification);

            association.setSubject(subject);
            association.getObject().add(object);
            association.setDescription(descriptionNote);
            result.add(association);
        }
        return result;
    }

    /**
     * Builds a human-readable description of the generated association for trace/debug purposes.
     *
     * @param refAttrItemName                            Reference attribute that defines the association.
     * @param firstParticipantDef                        Object class of the subject.
     * @param firstParticipantObjectTypeIdentification   Subject identification (kind, intent).
     * @param secondParticipantDef                       Object class of the object.
     * @param secondParticipantTypeIdentification        Object identification (kind, intent).
     * @return A string describing how the association was derived.
     */
    private static @NotNull String createAssociationDescription(
            @NotNull ItemName refAttrItemName, @NotNull NativeObjectClassDefinition firstParticipantDef,
            @NotNull ResourceObjectTypeIdentification firstParticipantObjectTypeIdentification,
            @NotNull NativeObjectClassDefinition secondParticipantDef,
            @NotNull ResourceObjectTypeIdentification secondParticipantTypeIdentification) {
        return "This association is derived from reference attribute '" + refAttrItemName.getLocalPart() + "' on "
                + "first participant (class=" + firstParticipantDef.getName()
                + ", kind=" + firstParticipantObjectTypeIdentification.getKind()
                + ", intent=" + firstParticipantObjectTypeIdentification.getIntent() + ") "
                + "which refers to second participant (class=" + secondParticipantDef.getName()
                + ", kind=" + secondParticipantTypeIdentification.getKind()
                + ", intent=" + secondParticipantTypeIdentification.getIntent() + ").";
    }

    /**
     * Registers all given type identifications into the class-role map as {@link ParticipantDescriptor}s.
     *
     * @param typeIdentifications  Object types to register.
     * @param role                 Whether the type is considered a subject or object.
     * @param resourceSchema       Complete resource schema.
     * @param nativeSchema         Native schema holding low-level object class definitions.
     * @param classDefRoleMap      Mutable output map where descriptors are stored.
     */
    private void registerParticipantDescriptorValues(
            @NotNull Collection<ResourceObjectTypeIdentification> typeIdentifications,
            @NotNull ShadowReferenceParticipantRole role,
            @NotNull ResourceSchema resourceSchema,
            @NotNull NativeResourceSchema nativeSchema,
            @NotNull Map<QName, List<ParticipantDescriptor>> classDefRoleMap) {

        for (var typeId : typeIdentifications) {
            var def = resourceSchema.getObjectTypeDefinitionRequired(typeId);
            var className = def.getObjectClassName();
            var classDef = nativeSchema.findObjectClassDefinition(className);
            if (classDef != null) {
                classDefRoleMap
                        .computeIfAbsent(className, __ -> new ArrayList<>())
                        .add(new ParticipantDescriptor(role, classDef, typeId));
            }
        }
    }

    /**
     * Finds all participant descriptors for a given object class name,
     * excluding the one with the provided identification (to avoid self-reference).
     *
     * @param objectClassName              Name of the object class to search.
     * @param excludedObjectIdentification Identification to exclude from results.
     * @param classDefRoleMap              Map of all known participant descriptors.
     * @return A list of matching participants excluding the given one.
     */
    private List<ParticipantDescriptor> findMatchingParticipantDefinitions(
            @NotNull QName objectClassName,
            @NotNull ResourceObjectTypeIdentification excludedObjectIdentification,
            @NotNull Map<QName, List<ParticipantDescriptor>> classDefRoleMap,
            @Nullable ShadowReferenceParticipantRole participantRole) {

        List<ParticipantDescriptor> entries = classDefRoleMap.get(objectClassName);

        if (entries == null) {
            return List.of();
        }

        return entries.stream()
                .filter(pair -> !pair.participantObjectTypeIdentification().equals(excludedObjectIdentification))
                .filter(pair -> participantRole == null || pair.participantRole() == participantRole)
                .collect(Collectors.toList());
    }

    /**
     * Builds a subject-side association participant definition.
     *
     * @param subjectType   Type identification for the subject.
     * @param subjectClassDef Native object class definition for the subject.
     * @return A fully initialized {@link ShadowAssociationTypeSubjectDefinitionType}.
     */
    private static @NotNull ShadowAssociationTypeSubjectDefinitionType buildSubjectParticipantDefinitionType(
            @NotNull ResourceObjectTypeIdentification subjectType,
            @NotNull NativeObjectClassDefinition subjectClassDef) {

        return new ShadowAssociationTypeSubjectDefinitionType()
                .ref(subjectClassDef.getQName())
                .objectType(new ResourceObjectTypeIdentificationType()
                        .kind(subjectType.getKind())
                        .intent(subjectType.getIntent()));
    }

    /**
     * Builds an object-side association participant definition.
     *
     * @param objectType     Type identification for the object.
     * @param objectClassDef Native object class definition for the object.
     * @return A fully initialized {@link ShadowAssociationTypeObjectDefinitionType}.
     */
    private static @NotNull ShadowAssociationTypeObjectDefinitionType buildObjectParticipantDefinitionType(
            @NotNull ResourceObjectTypeIdentification objectType,
            @NotNull NativeObjectClassDefinition objectClassDef) {

        return new ShadowAssociationTypeObjectDefinitionType()
                .ref(objectClassDef.getQName())
                .objectType(new ResourceObjectTypeIdentificationType()
                        .kind(objectType.getKind())
                        .intent(objectType.getIntent()));
    }

    //TODO: Design better strategy for generating association names. Also think for combined/separated associations.
    /**
     * Constructs a machine-readable and unique name for an association based on subject/object kinds,
     * intents, and the reference attribute.
     *
     * @param firstParticipantObjectTypeIdentification  Identification of the source participant.
     * @param secondParticipantTypeIdentification       Identification of the target participant.
     * @param refAttr                                   Reference attribute the association is based on.
     * @return The generated association name.
     */
    private static @NotNull String constructAssociationName(
            @NotNull ResourceObjectTypeIdentification firstParticipantObjectTypeIdentification,
            @NotNull ResourceObjectTypeIdentification secondParticipantTypeIdentification,
            @NotNull ItemName refAttr) {
        String firstParticipantSubjectKind = StringUtils.capitalize(firstParticipantObjectTypeIdentification.getKind().value());
        String firstParticipantSubjectIntent = StringUtils.capitalize(firstParticipantObjectTypeIdentification.getIntent()
                .replaceAll("[^a-zA-Z0-9]", ""));

        String secondParticipantSubjectKind = StringUtils.capitalize(secondParticipantTypeIdentification.getKind().value());
        String secondParticipantSubjectIntent = StringUtils.capitalize(secondParticipantTypeIdentification.getIntent()
                .replaceAll("[^a-zA-Z0-9]", ""));

        String referenceName = StringUtils.capitalize(refAttr.getLocalPart());

        return StringUtils.uncapitalize(firstParticipantSubjectKind + firstParticipantSubjectIntent + "To"
                + secondParticipantSubjectKind+secondParticipantSubjectIntent + referenceName);
    }

    /**
     * Logs the result of resolved associations for a given participant and reference attribute.
     *
     * @param associations         The list of associations found (possibly empty).
     * @param objectIdentification Identification of the participant.
     * @param refAttr              Reference attribute being evaluated.
     */
    private static void debugAssociationResult(@NotNull List<ShadowAssociationTypeDefinitionType> associations,
            ResourceObjectTypeIdentification objectIdentification,
            NativeShadowReferenceAttributeDefinition refAttr) {
        if (!associations.isEmpty()) {
            for (ShadowAssociationTypeDefinitionType shadowAssociationTypeDefinitionType : associations) {
                debugShadowAssociation(shadowAssociationTypeDefinitionType);
            }
        } else {
            LOGGER.debug("   No associations found for subject type: {}, reference attribute: {}\n",
                    objectIdentification, refAttr.getItemName());
        }
    }

    /**
     * Logs the details of an individual association definition.
     *
     * @param assoc The association to log.
     */
    private static void debugShadowAssociation(@NotNull ShadowAssociationTypeDefinitionType assoc) {
        String assocName = assoc.getName().getLocalPart();
        LOGGER.debug(" Association: {}", assocName);

        var subject = assoc.getSubject().getObjectType().get(0);
        var objects = assoc.getObject();
        var description = assoc.getDescription();

        LOGGER.debug("   Subject:   {} / {}", subject.getKind(), subject.getIntent());

        if (objects.size() == 1) {
            var obj = objects.get(0).getObjectType().get(0);
            LOGGER.debug("   Object:    {} / {}", obj.getKind(), obj.getIntent());
        } else {
            LOGGER.debug("   Objects:");
            for (var objPart : objects) {
                var obj = objPart.getObjectType().get(0);
                LOGGER.debug("     • {} / {}", obj.getKind(), obj.getIntent());
            }
        }

        LOGGER.debug("   Description: {}", description);
    }

    /**
     * Logs a basic summary of a participant (class name, kind, and intent).
     *
     * @param def    Object class definition of the participant.
     * @param typeId Type identification (kind, intent) of the participant.
     */
    private static void debugParticipantInfo(
            @NotNull NativeObjectClassDefinition def,
            @NotNull ResourceObjectTypeIdentification typeId) {
        LOGGER.debug("-".repeat(100));
        LOGGER.debug("PROCESSING PARTICIPANT:");
        LOGGER.debug("  Class     : {}", def.getName());
        LOGGER.debug("  Kind      : {}", typeId.getKind());
        LOGGER.debug("  Intent    : {}", typeId.getIntent());
    }
}
