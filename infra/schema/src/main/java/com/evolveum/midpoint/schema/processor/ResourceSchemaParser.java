/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.CapabilityUtil.getCapability;
import static com.evolveum.midpoint.schema.config.ConfigurationItem.*;
import static com.evolveum.midpoint.schema.config.ConfigurationItemOrigin.inResourceOrAncestor;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.SuperReference;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.config.*;
import com.evolveum.midpoint.schema.merger.objdef.ResourceObjectTypeDefinitionMergeOperation;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReferencesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

/**
 * Creates refined class and object type definitions in {@link ResourceSchemaImpl} objects.
 *
 * These definitions are derived from:
 *
 * 1. native object class definitions ({@link NativeObjectClassDefinition}) (obtained dynamically or statically),
 * 2. configured {@link SchemaHandlingType} beans in resource definition.
 *
 * This class is instantiated for each parsing operation.
 *
 * TODO migrate all uses of {@link SchemaException} in the configuration beans to {@link ConfigurationException} ones
 *  The schema exception should be thrown only if there is a genuine error in the underlying native schema
 */
class ResourceSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSchemaParser.class);

    /** Resource whose refined schema is being parsed. */
    @NotNull private final ResourceType resource;

    /** The `schemaHandling` from {@link #resource}, empty if missing. */
    @NotNull private final SchemaHandlingConfigItem schemaHandling;

    /** The extract of the resource information, to be weaved into type/class definitions. */
    @NotNull private final BasicResourceInformation basicResourceInformation;

    /** Raw resource schema we start with. */
    @NotNull private final NativeResourceSchema nativeSchema;

    /** This is used when parsing "modern" reference types. */
    @Nullable private final ReferencesCapabilityConfigItem referencesCapabilityCI;

    @NotNull private final String contextDescription;

    /** The schema being created. */
    @NotNull private final ResourceSchemaImpl resourceSchema;

    /**
     * Class definition configuration items. We don't want to put these (instead of beans) into
     * {@link ResourceObjectClassDefinition} objects, as they are too fat. However, we need them
     * when parsing specific features of object classes. The value may be `null` if there's no config item.
     *
     * Indexed by class name local part (as the names are in `ri` namespace).
     */
    @NotNull private final Map<String, ResourceObjectClassDefinitionConfigItem> classDefinitionConfigItemMap = new HashMap<>();

    /**
     * As {@link #classDefinitionConfigItemMap} but for object types (regular and associated).
     * Indexed by type identification, e.g., `account/default`.
     */
    @NotNull private final Map<ResourceObjectTypeIdentification, AbstractResourceObjectTypeDefinitionConfigItem<?>>
            typeDefinitionConfigItemMap = new HashMap<>();

    private ResourceSchemaParser(
            @NotNull ResourceType resource,
            @NotNull SchemaHandlingConfigItem schemaHandling,
            @NotNull NativeResourceSchema nativeSchema,
            @Nullable ReferencesCapabilityConfigItem referencesCapabilityCI,
            @NotNull String contextDescription,
            @NotNull ResourceSchemaImpl resourceSchema) {
        this.resource = resource;
        this.schemaHandling = schemaHandling;
        this.basicResourceInformation = BasicResourceInformation.of(resource);
        this.nativeSchema = nativeSchema;
        this.referencesCapabilityCI = referencesCapabilityCI;
        this.contextDescription = contextDescription;
        this.resourceSchema = resourceSchema;
    }

    /** Creates {@link CompleteResourceSchema} for the given resource. This is the main functionality of this parser. */
    static @NotNull CompleteResourceSchema parseComplete(
            @NotNull ResourceType resource, @NotNull NativeResourceSchema nativeSchema)
            throws SchemaException, ConfigurationException {
        var schemaHandlingBean = resource.getSchemaHandling();
        var schemaHandling =
                schemaHandlingBean != null ?
                        configItem(
                                schemaHandlingBean,
                                ConfigurationItemOrigin.inResourceOrAncestor(resource, ResourceType.F_SCHEMA_HANDLING),
                                SchemaHandlingConfigItem.class) :
                        emptySchemaHandlingConfigItem();
        var associationCapabilityBean = CapabilityUtil.getCapability(resource, ReferencesCapabilityType.class);
        var associationsCapabilityCI = configItemNullable(
                associationCapabilityBean,
                inResourceOrAncestor(
                        resource,
                        ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CONFIGURED, CapabilityCollectionType.F_REFERENCES)),
                ReferencesCapabilityConfigItem.class);
        var completeResourceSchema = new CompleteResourceSchemaImpl(
                nativeSchema,
                BasicResourceInformation.of(resource),
                ResourceTypeUtil.isCaseIgnoreAttributeNames(resource));
        var parser = new ResourceSchemaParser(
                resource,
                schemaHandling,
                nativeSchema,
                associationsCapabilityCI,
                "definition of " + resource,
                completeResourceSchema);
        parser.parse();
        return completeResourceSchema;
    }

    /** Creates a resource schema based solely on the native one. For tests and some very special occasions. */
    static BareResourceSchema parseBare(@NotNull NativeResourceSchema nativeSchema) throws SchemaException {
        var bareSchema = new BareResourceSchemaImpl(nativeSchema);
        var parser = new ResourceSchemaParser(
                new ResourceType(),
                emptySchemaHandlingConfigItem(),
                nativeSchema,
                null,
                "bare schema",
                bareSchema);
        try {
            parser.parse();
        } catch (ConfigurationException e) {
            throw SystemException.unexpected(e);
        }
        return bareSchema;
    }

    private static @NotNull SchemaHandlingConfigItem emptySchemaHandlingConfigItem() {
        return configItem(
                new SchemaHandlingType(),
                ConfigurationItemOrigin.generated(), // hopefully no errors will be reported against this config item
                SchemaHandlingConfigItem.class);
    }

    /** Creates the parsed resource schema. */
    private void parse() throws SchemaException, ConfigurationException {

        schemaHandling.checkSyntaxOfAttributeNames();

        createEmptyObjectClassDefinitions();
        createEmptyObjectTypeDefinitions();
        setEffectiveObjectClassDefinitionsFromTypes();

        forAllObjects(o -> o.resolveAuxiliaryObjectClassNames());

        // These require object classes and object types (even if they are empty at this moment).
        parseNativeReferenceTypes();

        // We can parse attributes only after we have all the object class info parsed (including auxiliary object classes).
        // Simulated references and associations are not parsed here.
        forAllObjects(o -> o.parseNativeAttributes());

        // Protected objects and delineation. They refer to attributes.
        forAllObjects(o -> o.parseOtherFeatures());

        // Simulated reference types refer to object types, attributes, and delineation, so they must come after them.
        parseModernSimulatedReferenceTypes();

        // Now we can check the remaining attributes.
        forAllObjects(o -> o.parseModernSimulatedReferenceAttributes());
        forAllObjects(o -> o.checkNoDanglingAttributeDefinitions());

        // Finally, here come the associations.
        forAllObjects(o -> o.parseModernAssociations());
        forAllObjects(o -> o.parseLegacySimulatedAssociations());
        checkDanglingAssociationTypeReferences();

        resourceSchema.freeze();
    }

    private void createEmptyObjectClassDefinitions() throws ConfigurationException, SchemaException {

        LOGGER.trace("Creating object class definitions, part one: refined ones");

        var classDefinitionCIs = schemaHandling.getObjectClasses();
        for (var classDefinitionCI : classDefinitionCIs) {
            var objectClassName = classDefinitionCI.getObjectClassName();
            var nativeObjectClassDefinition =
                    classDefinitionCI.configNonNull(
                            nativeSchema.findObjectClassDefinition(objectClassName),
                            "Object class %s referenced in %s does not exist on the resource".formatted(objectClassName, DESC));
            assertClassNotDefinedYet(objectClassName);
            resourceSchema.add(
                    ResourceObjectClassDefinitionImpl.create(
                            basicResourceInformation, nativeObjectClassDefinition, classDefinitionCI.value()));
            classDefinitionConfigItemMap.put(objectClassName.getLocalPart(), classDefinitionCI);
        }

        LOGGER.trace("Created {} refined object class definitions from beans; now remaining ones", classDefinitionCIs.size());

        for (var nativeObjectClassDefinition : nativeSchema.getObjectClassDefinitions()) {
            if (resourceSchema.findObjectClassDefinition(nativeObjectClassDefinition.getQName()) == null) {
                resourceSchema.add(
                        ResourceObjectClassDefinitionImpl.create(
                                basicResourceInformation, nativeObjectClassDefinition, null));
                classDefinitionConfigItemMap.put(nativeObjectClassDefinition.getName(), null);
            }
        }

        LOGGER.trace("Successfully created {} object class definitions (in total)",
                resourceSchema.getObjectClassDefinitions().size());
    }

    private void assertClassNotDefinedYet(QName objectClassName) throws ConfigurationException {
        MiscUtil.configCheck(
                resourceSchema.findObjectClassDefinition(objectClassName) == null,
                "Multiple definitions for object class %s in %s", objectClassName, resource);
    }

    private void createEmptyObjectTypeDefinitions() throws SchemaException, ConfigurationException {
        LOGGER.trace("Creating empty object type definitions");
        int created = 0;
        for (var typeDefinitionCI : schemaHandling.getAllObjectTypes()) {
            if (!typeDefinitionCI.isAbstract()) {
                ResourceObjectTypeDefinition definition = createEmptyObjectTypeDefinition(typeDefinitionCI);
                LOGGER.trace("Created (empty) object type definition: {}", definition);
                assertTypeNotDefinedYet(definition);
                resourceSchema.add(definition);
                created++;
            } else {
                LOGGER.trace("Ignoring abstract definition bean: {}", typeDefinitionCI);
            }
        }
        checkForMultipleDefaultsForKind();
        LOGGER.trace("Successfully created {} empty object type definitions", created);
    }

    private void setEffectiveObjectClassDefinitionsFromTypes() {
        for (var objectClassDefinition : resourceSchema.getObjectClassDefinitions()) {
            var defaultTypeDef = ResourceObjectDefinitionResolver.findDefaultObjectTypeDefinitionForObjectClass(
                    resourceSchema, objectClassDefinition.getObjectClassName());
            if (defaultTypeDef != null) {
                ((ResourceObjectClassDefinitionImpl) objectClassDefinition).setEffectiveDefinition(defaultTypeDef);
            }
        }
    }

    private void assertTypeNotDefinedYet(ResourceObjectTypeDefinition definition) throws ConfigurationException {
        ResourceObjectTypeIdentification identification = definition.getTypeIdentification();
        var existing = resourceSchema.getObjectTypeDefinition(identification);
        if (existing != null) {
            throw new ConfigurationException("Multiple definitions of " + identification + " in " + contextDescription);
        }
    }

    private <B extends ResourceObjectTypeDefinitionType> ResourceObjectTypeDefinition createEmptyObjectTypeDefinition(
            @NotNull AbstractResourceObjectTypeDefinitionConfigItem<B> definitionCI)
            throws SchemaException, ConfigurationException {

        ResourceObjectTypeIdentification identification = definitionCI.getTypeIdentification();

        // Object class refinement is not merged here (yet). We assume that the object class name could be woven into
        // the bean at any level. And we hope that although we do the merging in the top-bottom direction, it will cause
        // no harm if we merge the object class refinement (i.e. topmost component) at last.
        ObjectTypeExpansion expansion = new ObjectTypeExpansion();
        B expandedBean = expansion.expand(definitionCI.value());

        // We assume that the path was not changed. Quite a hack, though.
        AbstractResourceObjectTypeDefinitionConfigItem<?> expandedCI;
        if (definitionCI instanceof AssociatedResourceObjectTypeDefinitionConfigItem) {
            //noinspection RedundantTypeArguments : The type arguments aren't redundant: they are needed for some Java compilers
            expandedCI = ConfigurationItem.<AssociatedResourceObjectTypeDefinitionType, AssociatedResourceObjectTypeDefinitionConfigItem>configItem(
                    (AssociatedResourceObjectTypeDefinitionType) expandedBean,
                    definitionCI.origin(),
                    AssociatedResourceObjectTypeDefinitionConfigItem.class);
        } else {
            //noinspection RedundantTypeArguments : see above
            expandedCI = ConfigurationItem.<ResourceObjectTypeDefinitionType, ResourceObjectTypeDefinitionConfigItem>configItem(
                    expandedBean,
                    definitionCI.origin(),
                    ResourceObjectTypeDefinitionConfigItem.class);
        }

        QName objectClassName = expandedCI.getObjectClassName();
        ResourceObjectClassDefinition objectClassDefinition = getObjectClassDefinitionRequired(objectClassName, expandedCI);

        ResourceObjectTypeDefinitionType objectClassRefinementBean = objectClassDefinition.getDefinitionBean();
        merge(expandedBean, objectClassRefinementBean); // no-op if refinement bean is empty

        typeDefinitionConfigItemMap.put(expandedCI.getTypeIdentification(), expandedCI);

        return new ResourceObjectTypeDefinitionImpl(
                basicResourceInformation,
                identification,
                expansion.getAncestorsIds(),
                objectClassDefinition,
                CloneUtil.toImmutable(expandedBean));
    }

    private @NotNull ResourceObjectClassDefinition getObjectClassDefinitionRequired(
            @NotNull QName objectClassName, @NotNull ConfigurationItem<?> contextCI) throws ConfigurationException {
        return contextCI.configNonNull(
                resourceSchema.findObjectClassDefinition(objectClassName),
                "No definition found for object class '%s' referenced in %s", objectClassName, DESC);
    }

    /** Merges "source" (super-type) into "target" (sub-type). */
    private void merge(
            @NotNull ResourceObjectTypeDefinitionType target,
            @NotNull ResourceObjectTypeDefinitionType source) throws SchemaException, ConfigurationException {
        new ResourceObjectTypeDefinitionMergeOperation(target, source, null)
                .execute();
    }

    /**
     * Checks that there is at most single default for any kind.
     *
     * @throws ConfigurationException If there's a problem. Note that during run time, we throw {@link IllegalStateException}
     * in these cases (as we assume this check was already done).
     */
    private void checkForMultipleDefaultsForKind() throws ConfigurationException {
        for (ShadowKindType kind : ShadowKindType.values()) {
            var defaults = resourceSchema.getObjectTypeDefinitions().stream()
                    .filter(def -> def.matchesKind(kind) && def.isDefaultForKind())
                    .toList();
            configCheck(defaults.size() <= 1, "More than one default %s definition in %s: %s",
                    kind, contextDescription, defaults);
        }
    }

    /**
     * Creates {@link AbstractShadowReferenceTypeDefinition} objects in {@link ResourceSchemaImpl#referenceTypeDefinitionMap}
     * for "modern" simulated references.
     *
     * Does *not* include legacy simulated associations! They are "anonymous" as they do not have a type name.
     */
    private void parseModernSimulatedReferenceTypes() throws ConfigurationException {
        if (referencesCapabilityCI != null && referencesCapabilityCI.isEnabled()) {
            for (var simulatedRefTypeDefCI : referencesCapabilityCI.getReferenceTypes()) {
                resourceSchema.addReferenceTypeDefinition(
                        SimulatedShadowReferenceTypeDefinition.Modern.parse(
                                simulatedRefTypeDefCI,
                                resourceSchema));
            }
        }
    }

    /**
     * Creates {@link AbstractShadowReferenceTypeDefinition} objects in {@link ResourceSchemaImpl#referenceTypeDefinitionMap}
     * for natively provided references.
     */
    private void parseNativeReferenceTypes() throws ConfigurationException {
        for (var nativeReferenceTypeDefinition : nativeSchema.getReferenceTypeDefinitions()) {
            resourceSchema.addReferenceTypeDefinition(
                    NativelyProvidedShadowReferenceTypeDefinition.create(
                            nativeReferenceTypeDefinition,
                            resourceSchema));
        }
    }

    private void forAllObjects(SpecificFeatureParser parser) throws ConfigurationException {
        for (var objectDef : resourceSchema.getResourceObjectDefinitions()) {
            parser.execute(new ResourceObjectDefinitionParser(objectDef));
        }
    }

    /**
     * Association types are parsed for each individual object type definition. So, if there's an invalid reference to
     * either object type or the reference attribute, we should report it. (Otherwise, the association type would be
     * silently skipped.)
     */
    private void checkDanglingAssociationTypeReferences() throws ConfigurationException {
        for (var associationTypeCI : schemaHandling.getAssociationTypes()) {
            checkParticipantObjectTypes(associationTypeCI.getSubject());
            for (var objectCI : associationTypeCI.getObjects()) {
                checkParticipantObjectTypes(objectCI);
            }
        }
    }

    private void checkParticipantObjectTypes(ShadowAssociationTypeParticipantDefinitionConfigItem<?> participant)
            throws ConfigurationException {
        var refAttrName = participant instanceof ShadowAssociationTypeSubjectDefinitionConfigItem ?
                participant.getReferenceAttributeNameRequired() : participant.getReferenceAttributeNameOptional();
        for (ResourceObjectTypeIdentification typeIdentification : participant.getTypeIdentifiers()) {
            var typeDef = participant.configNonNull(
                    resourceSchema.getObjectTypeDefinition(typeIdentification),
                    "Association type participant references object type '%s' that does not exist; in %s",
                    typeIdentification, DESC);
            if (refAttrName != null) {
                participant.configNonNull(
                        typeDef.findReferenceAttributeDefinition(refAttrName),
                        "Association type references attribute '%s' that does not exist in object type '%s'; in %s",
                        refAttrName, typeIdentification, DESC);
            }
        }
    }

    /**
     * Creates and updates {@link ResourceObjectTypeDefinition} from
     *
     * - "raw" {@link ResourceObjectClassDefinition},
     * - refinements defined in {@link ResourceObjectTypeDefinitionType} (`schemaHandling`)
     *
     * Note: this class is instantiated multiple times during parsing of a schema. It should not be
     * a problem, as it is quite lightweight.
     */
    private class ResourceObjectDefinitionParser {

        /**
         * Specific object definition being updated.
         */
        @NotNull private final AbstractResourceObjectDefinitionImpl definition;

        /**
         * Definition CI from `schemaHandling` section; taken from {@link #definition}.
         *
         * It is merged from all the super-resources and super-types. Its CI origin and CI parent are
         * set artificially (for now).
         */
        @NotNull private final AbstractResourceObjectDefinitionConfigItem<?> definitionCI;

        /** Currently, we mark the source attribute for simulated activation as ignored (if requested so). */
        @NotNull private final Collection<QName> attributesToIgnore;

        ResourceObjectDefinitionParser(@NotNull ResourceObjectDefinition definition) {
            this.definition = (AbstractResourceObjectDefinitionImpl) definition;
            if (definition instanceof ResourceObjectTypeDefinition typeDefinition) {
                this.definitionCI = stateNonNull(
                        typeDefinitionConfigItemMap.get(typeDefinition.getTypeIdentification()),
                        "No cached configuration item for %s", typeDefinition);
            } else if (definition instanceof ResourceObjectClassDefinition) {
                //noinspection RedundantTypeArguments : actually needed by the Java compiler
                this.definitionCI = Objects.requireNonNullElseGet(
                        classDefinitionConfigItemMap.get(definition.getObjectClassName().getLocalPart()),
                        () -> ConfigurationItem.<ResourceObjectTypeDefinitionType, ResourceObjectClassDefinitionConfigItem>configItem(
                                new ResourceObjectTypeDefinitionType(),
                                ConfigurationItemOrigin.generated(),
                                ResourceObjectClassDefinitionConfigItem.class));
            } else {
                throw new IllegalStateException("Neither object type or object class? " + definition);
            }
            attributesToIgnore = computeAttributesToIgnore();
        }

        /**
         * See ResourceSchemaAdjuster in 4.8 and below. The actual code is adapted from
         * {@code 133f999746b4ab3d55c9684fcb1b4f07855c222f} (4.8.4)
         */
        private @NotNull Collection<QName> computeAttributesToIgnore() {
            var activationCapability = getCapability(resource, definition.getTypeDefinition(), ActivationCapabilityType.class);
            if (activationCapability == null) {
                return Set.of();
            }
            var statusCapability = CapabilityUtil.getEnabledActivationStatus(activationCapability);
            if (statusCapability == null) {
                return Set.of();
            }
            if (Boolean.FALSE.equals(statusCapability.isIgnoreAttribute())) {
                return Set.of();
            }
            var attributeName = statusCapability.getAttribute();
            if (attributeName == null) {
                return Set.of();
            }
            var nativeAttrDef = definition.getNativeObjectClassDefinition().findSimpleAttributeDefinition(attributeName);
            if (nativeAttrDef == null) {
                // Simulated activation attribute points to something that is not in the schema
                // technically, this is an error. But it looks to be quite common in connectors.
                // The enable/disable is using operational attributes that are not exposed in the
                // schema, but they work if passed to the connector.
                // Therefore we don't want to break anything. We could log an warning here, but the
                // warning would be quite frequent. Maybe a better place to warn user would be import
                // of the object.
                LOGGER.debug("Simulated activation attribute {} for objectclass {} in {} does not exist in "
                        + "the resource schema. This may work well, but it is not clean. Connector exposing "
                        + "such schema should be fixed.", attributeName, definition.getObjectClassName(), resource);
                return Set.of();
            }
            return Set.of(nativeAttrDef.getItemName());
        }

        /**
         * Fills in list of auxiliary object class definitions (in object class/type definitions)
         * with definitions resolved from their qualified names.
         */
        void resolveAuxiliaryObjectClassNames() throws ConfigurationException {
            for (QName auxObjectClassName : definitionCI.getAuxiliaryObjectClassNames()) {
                definition.addAuxiliaryObjectClassDefinition(
                        getObjectClassDefinitionRequired(auxObjectClassName, definitionCI));
            }
        }

        /**
         * Attaches association definitions to their respective reference-attribute definitions.
         * Assumes that both native and modern simulated reference attributes are already parsed.
         */
        void parseModernAssociations() throws ConfigurationException {

            LOGGER.trace("Parsing native and modern-simulated associations of {}", definition);
            for (var refAttrDef : definition.getReferenceAttributeDefinitions()) {
                LOGGER.trace("Parsing associations for reference attribute {}", refAttrDef);
                for (var assocTypeCI : getRelevantAssociationTypes(refAttrDef.getItemName())) {
                    parseModernAssociation(refAttrDef, assocTypeCI);
                }
            }
        }

        /** Returns association type definition beans for associations based on the specified reference attribute. */
        private @NotNull Collection<ShadowAssociationTypeDefinitionConfigItem> getRelevantAssociationTypes(ItemName refAttrName)
                throws ConfigurationException {
            var subjectTypeId = definition.getTypeIdentification();
            return subjectTypeId != null ? schemaHandling.getAssociationTypesFor(subjectTypeId, refAttrName) : List.of();
        }

        /**
         * Combines reference attribute definition with the association type definition (if present).
         */
        private void parseModernAssociation(
                @NotNull ShadowReferenceAttributeDefinition refAttrDef,
                @NotNull ShadowAssociationTypeDefinitionConfigItem assocTypeCI)
                throws ConfigurationException {

            var subjectSideCI = assocTypeCI.getSubject().getAssociation();
            if (subjectSideCI == null) {
                return; // The association type detached from subjects makes no sense (yet)
            }
            definition.add(
                    ShadowAssociationDefinitionImpl.modern(
                            assocTypeCI.getSubject().getAssociationNameRequired(),
                            refAttrDef,
                            subjectSideCI,
                            assocTypeCI,
                            resourceSchema));
        }

        /**
         * Creates both reference attributes and association definitions.
         */
        void parseLegacySimulatedAssociations() throws ConfigurationException {
            LOGGER.trace("Parsing legacy simulated associations of {}", definition);
            for (var legacyAssocDefCI : definitionCI.getLegacyAssociations()) {
                legacyAssocDefCI.configCheck(
                        !definition.containsAttributeDefinition(legacyAssocDefCI.getItemName()),
                        "Legacy association '%s' is already defined as an attribute in %s",
                            legacyAssocDefCI.getItemName(), DESC);
                // The item is not present as an attribute, so it must be legacy one. We will parse it, and add both
                // the association definition and the (simulated) reference attribute definition.
                var legacyAssocDef = new LegacyAssociationParser(legacyAssocDefCI.asLegacy(), definition).parse();
                definition.add(legacyAssocDef);
                definition.add(legacyAssocDef.getReferenceAttributeDefinition());
            }
        }

        /**
         * Fills-in attribute definitions in {@link #definition} by traversing all native attributes defined in the structural
         * object class and all the auxiliary object classes. Initializes identifier names as well.
         *
         * What remains are simulated reference attributes defined in the `references` capability.
         * They cannot be parsed here, as they refer to existing attributes.
         */
        private void parseNativeAttributes() throws ConfigurationException {

            LOGGER.trace("Parsing native attributes of {}", definition);

            parseAttributesFromNativeObjectClass(definition.getNativeObjectClassDefinition(), false);
            for (ResourceObjectDefinition auxDefinition : definition.getAuxiliaryDefinitions()) {
                parseAttributesFromNativeObjectClass(auxDefinition.getNativeObjectClassDefinition(), true);
            }

            setupIdentifiers();
        }

        /**
         * There should be no attribute definitions in `schemaHandling` without connector-provided (raw schema)
         * or modern simulated references counterparts.
         */
        private void checkNoDanglingAttributeDefinitions() throws ConfigurationException {
            for (ResourceAttributeDefinitionConfigItem attributeDefCI : definitionCI.getAttributes()) {
                QName attrName = attributeDefCI.getAttributeName();
                // TODO check that we really look into aux object classes
                if (!definition.containsAttributeDefinition(attrName)
                        && !attributeDefCI.isIgnored()) {
                    throw attributeDefCI.configException(
                            "Definition of attribute '%s' not found in object class '%s' "
                                    + "nor auxiliary object classes for '%s' as defined in %s",
                            attrName, definition.getObjectClassName(), definition, DESC);
                }
            }
        }

        /**
         * Takes all attributes from resource object class definition, and pairs (enriches)
         * them with `schemaHandling` information.
         */
        private void parseAttributesFromNativeObjectClass(@NotNull NativeObjectClassDefinition nativeClassDef, boolean auxiliary)
                throws ConfigurationException {
            for (var attrDef : nativeClassDef.getAttributeDefinitions()) {
                parseNativeAttribute(attrDef, auxiliary);
            }
        }

        private void parseNativeAttribute(@NotNull NativeShadowAttributeDefinition nativeAttrDef, boolean fromAuxClass)
                throws ConfigurationException {

            ItemName attrName = nativeAttrDef.getItemName();

            LOGGER.trace("Parsing attribute {} (auxiliary = {})", attrName, fromAuxClass);

            // We MUST NOT skip ignored attribute definitions here. We must include them in the schema as
            // the shadows will still have that attributes and we will need their type definition to work
            // well with them. They may also be mandatory. We cannot pretend that they do not exist.

            if (definition.containsAttributeDefinition(attrName)) {
                if (fromAuxClass) {
                    return;
                } else {
                    throw definitionCI.configException("Duplicate definition of attribute '%s' in %s", attrName, DESC);
                }
            }

            ResourceAttributeDefinitionType attrDefBean = value(definitionCI.getAttributeDefinitionIfPresent(attrName));
            ItemDefinition<?> attrDef;
            if (nativeAttrDef.isSimple()) {
                boolean ignored = attributesToIgnore.contains(attrName);
                var simpleAttrDef = ShadowSimpleAttributeDefinitionImpl.create(nativeAttrDef.asSimple(), attrDefBean, ignored);
                if (simpleAttrDef.isDisplayNameAttribute()) {
                    definition.setDisplayNameAttributeName(attrName);
                }
                attrDef = simpleAttrDef;
            } else if (nativeAttrDef.isReference()) {
                var nativeRefAttrDef = nativeAttrDef.asReference();
                var refTypeDef = resourceSchema.getReferenceTypeDefinitionRequired(
                        nativeRefAttrDef.getReferenceTypeName(),
                        lazy(() -> "when parsing '%s' in %s".formatted(attrName, definition)));
                attrDef = ShadowReferenceAttributeDefinitionImpl.fromNative(nativeRefAttrDef, refTypeDef, attrDefBean);
            } else {
                throw new UnsupportedOperationException("Unknown kind of attribute: " + nativeAttrDef);
            }
            definition.add(attrDef);
        }

        /**
         * Copy all primary identifiers from the raw definition.
         *
         * For secondary ones, use configured information (if present). Otherwise, use raw definition as well.
         */
        private void setupIdentifiers() {
            var nativeDefinition = definition.getNativeObjectClassDefinition();

            for (ShadowSimpleAttributeDefinition<?> attrDef : definition.getSimpleAttributeDefinitions()) {
                ItemName attrName = attrDef.getItemName();

                if (nativeDefinition.isPrimaryIdentifier(attrName)) {
                    definition.getPrimaryIdentifiersNames().add(attrName);
                }
                if (attrDef.isSecondaryIdentifierOverride() == null) {
                    if (nativeDefinition.isSecondaryIdentifier(attrName)) {
                        definition.getSecondaryIdentifiersNames().add(attrName);
                    }
                } else {
                    if (attrDef.isSecondaryIdentifierOverride()) {
                        definition.getSecondaryIdentifiersNames().add(attrName);
                    }
                }
            }
        }

        /** Fills-in attribute definitions in {@link #definition} for simulated reference attributes. */
        private void parseModernSimulatedReferenceAttributes() throws ConfigurationException {
            LOGGER.trace("Parsing simulated reference attributes of {}", definition);
            for (var refTypeDef : resourceSchema.getReferenceTypes()) {
                if (refTypeDef instanceof SimulatedShadowReferenceTypeDefinition simulatedRefTypeDef
                        && simulatedRefTypeDef.isRelevantForSubject(definition)) {
                    var refName = simulatedRefTypeDef.getLocalSubjectItemName();
                    LOGGER.trace("Parsing simulated reference {}", simulatedRefTypeDef);
                    definition.add(
                            ShadowReferenceAttributeDefinitionImpl.fromSimulated(
                                    simulatedRefTypeDef,
                                    value(definitionCI.getAttributeDefinitionIfPresent(refName))));
                }
            }
        }

        /**
         * Parses protected objects, delineation, and so on.
         */
        private void parseOtherFeatures() throws ConfigurationException {
            parseMarkingRules();
            parseDelineation();
        }

        /**
         * Converts protected objects patterns from "bean" to "compiled" form.
         */
        private void parseMarkingRules() throws ConfigurationException {
            definition.setShadowMarkingRules(
                    ShadowMarkingRules.parse(definitionCI, definition));
        }

        private void parseDelineation() throws ConfigurationException {
            QName objectClassName = definition.getObjectClassName();
            var definitionBean = definitionCI.value(); // TODO move this processing right into the CI
            List<QName> auxiliaryObjectClassNames = definitionCI.getAuxiliaryObjectClassNames();
            ResourceObjectTypeDelineationType delineationBean = definitionBean.getDelineation();
            if (delineationBean != null) {
                definitionCI.configCheck(definitionBean.getBaseContext() == null,
                        "Legacy base context cannot be set when delineation is configured; in %s", DESC);
                definitionCI.configCheck(definitionBean.getSearchHierarchyScope() == null,
                        "Legacy search hierarchy scope cannot be set when delineation is configured; in %s", DESC);
                definition.setDelineation(
                        ResourceObjectTypeDelineation.of(
                                delineationBean,
                                objectClassName,
                                auxiliaryObjectClassNames,
                                definition));
            } else {
                definition.setDelineation(
                        ResourceObjectTypeDelineation.of(
                                definitionBean.getBaseContext(),
                                definitionBean.getSearchHierarchyScope(),
                                objectClassName,
                                auxiliaryObjectClassNames));
            }
        }
    }

    /** Resolves super-type references for given object type definition bean. */
    private class ObjectTypeExpansion {

        /** Object type identifiers that have been seen when resolving the ancestors. Used also to detect cycles. */
        private final Set<ResourceObjectTypeIdentification> ancestorsIds = new HashSet<>();

        /**
         * Expands the definition by resolving its ancestor (if there's any), recursively if needed.
         *
         * Does not modify existing {@link #resource} object, so it clones the beans that are being expanded.
         */
        private <T extends ResourceObjectTypeDefinitionType> @NotNull T expand(@NotNull T definitionBean)
                throws ConfigurationException, SchemaException {
            var superRef = definitionBean.getSuper();
            if (superRef == null) {
                return definitionBean;
            } else {
                ResourceObjectTypeDefinitionType superBean = find(superRef);
                if (!ancestorsIds.add(ResourceObjectTypeIdentification.of(superBean))) {
                    throw new ConfigurationException(
                            "A cycle in super-type hierarchy, detected at %s (contains: %s) in %s".formatted(
                                    superBean, ancestorsIds, contextDescription));
                }
                ResourceObjectTypeDefinitionType expandedSuperBean = expand(superBean);
                //noinspection unchecked
                T expandedSubBean = (T) definitionBean.clone();
                merge(expandedSubBean, expandedSuperBean);
                return expandedSubBean;
            }
        }

        /** Resolves the reference to a super-type. Must be in the same resource. TODO migrate to CIs. */
        private @NotNull ResourceObjectTypeDefinitionType find(@NotNull ResourceObjectTypeIdentificationType superRefBean)
                throws ConfigurationException {
            SuperReference superRef = SuperReference.of(superRefBean);
            List<ResourceObjectTypeDefinitionType> matching = schemaHandling.getAllObjectTypes().stream()
                    .map(ci -> ci.value())
                    .filter(superRef::matches)
                    .collect(Collectors.toList());
            return MiscUtil.extractSingletonRequired(matching,
                    () -> new ConfigurationException("Multiple definitions matching " + superRef + " found in " + contextDescription),
                    () -> new ConfigurationException("No definition matching " + superRef + " found in " + contextDescription));
        }

        private @NotNull Set<ResourceObjectTypeIdentification> getAncestorsIds() {
            return ancestorsIds;
        }
    }

    /** Parses given legacy ("pre-4.9") association type. */
    private class LegacyAssociationParser {

        @NotNull private final ResourceObjectAssociationConfigItem.Legacy associationDefCI;

        /** This one is being built. */
        @NotNull private final ResourceObjectDefinition subjectDefinition;

        LegacyAssociationParser(
                @NotNull ResourceObjectAssociationConfigItem.Legacy associationDefCI,
                @NotNull ResourceObjectDefinition subjectDefinition) {
            this.associationDefCI = associationDefCI;
            this.subjectDefinition = subjectDefinition;
        }

        @NotNull ShadowAssociationDefinitionImpl parse() throws ConfigurationException {
            return ShadowAssociationDefinitionImpl.parseLegacy(
                    associationDefCI, subjectDefinition, getObjectTypeDefinitions(), resourceSchema);
        }

        /**
         * Returns object type definition matching given kind and one of the intents, specified in the association definition.
         *
         * (If no intents are provided, default type for given kind is returned.
         * We are not very eager here - by default we mean just the flag "default for kind" being set.)
         *
         * The matching types must share at least the object class name. This is checked by this method.
         * However, in practice they must share much more, as described in the description for
         * {@link ResourceObjectAssociationType#getIntent()} (see XSD).
         */
        private @NotNull Collection<ResourceObjectTypeDefinition> getObjectTypeDefinitions()
                throws ConfigurationException {

            var kind = associationDefCI.getKind();
            var intents = associationDefCI.getIntents();

            Predicate<ResourceObjectTypeDefinition> predicate =
                    objectDef -> {
                        if (objectDef.getKind() != kind) {
                            return false;
                        }
                        if (intents.isEmpty()) {
                            return objectDef.isDefaultForKind();
                        } else {
                            return intents.contains(objectDef.getIntent());
                        }
                    };

            var predicateDescription = lazy(() -> "kind " + kind + ", intents " + intents);

            Collection<ResourceObjectTypeDefinition> matching =
                    resourceSchema.getObjectTypeDefinitions().stream()
                            .filter(predicate)
                            .toList();
            if (matching.isEmpty()) {
                throw associationDefCI.configException(
                        "No matching object type definition found for %s in %s", predicateDescription, DESC);
            } else if (ResourceSchemaUtil.areDefinitionsCompatible(matching)) {
                return matching;
            } else {
                throw associationDefCI.configException(
                        "Incompatible definitions found for %s in %s: %s", predicateDescription, DESC, matching);
            }
        }
    }

    private interface SpecificFeatureParser {
        void execute(ResourceObjectDefinitionParser definitionParser) throws ConfigurationException;
    }
}
