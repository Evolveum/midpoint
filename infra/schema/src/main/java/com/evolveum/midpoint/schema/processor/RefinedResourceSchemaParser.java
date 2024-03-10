/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.*;
import static com.evolveum.midpoint.schema.config.ConfigurationItemOrigin.inResourceOrAncestor;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.SuperReference;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.config.*;
import com.evolveum.midpoint.schema.merger.objdef.ResourceObjectTypeDefinitionMergeOperation;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AssociationsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Creates refined class and object type definitions in {@link CompleteResourceSchemaImpl} objects.
 *
 * These definitions are derived from:
 *
 * 1. raw object class definitions ({@link ResourceObjectClassDefinition}) (obtained dynamically or statically),
 * 2. configured {@link SchemaHandlingType} beans in resource definition.
 *
 * This class is instantiated for each parsing operation.
 *
 * TODO migrate all uses of {@link SchemaException} to {@link ConfigurationException} ones
 */
class RefinedResourceSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(RefinedResourceSchemaParser.class);

    /** Resource whose refined schema is being parsed. */
    @NotNull private final ResourceType resource;

    /** The `schemaHandling` from {@link #resource}, empty if missing. */
    @NotNull private final SchemaHandlingConfigItem schemaHandling;

    /** The extract of the resource information, to be weaved into type/class definitions. */
    @NotNull private final BasicResourceInformation basicResourceInformation;

    /** Raw resource schema we start with. */
    @NotNull private final ResourceSchema rawResourceSchema;

    /** This is used when parsing "new" association types. */
    @Nullable private final AssociationsCapabilityConfigItem associationsCapabilityCI;

    @NotNull private final String contextDescription;

    /** The schema being created. */
    @NotNull private final CompleteResourceSchemaImpl completeSchema;

    RefinedResourceSchemaParser(@NotNull ResourceType resource, @NotNull ResourceSchema rawResourceSchema) {
        this.resource = resource;
        var schemaHandlingBean = resource.getSchemaHandling();
        this.schemaHandling =
                schemaHandlingBean != null ?
                        configItem(
                                schemaHandlingBean,
                                ConfigurationItemOrigin.inResourceOrAncestor(resource, ResourceType.F_SCHEMA_HANDLING),
                                SchemaHandlingConfigItem.class) :
                        configItem(
                                new SchemaHandlingType(),
                                ConfigurationItemOrigin.generated(), // hopefully no errors will be reported here
                                SchemaHandlingConfigItem.class);
        this.basicResourceInformation = BasicResourceInformation.of(resource);
        this.rawResourceSchema = rawResourceSchema;
        var associationCapabilityBean = CapabilityUtil.getCapability(resource, null, AssociationsCapabilityType.class);
        this.associationsCapabilityCI = configItemNullable(
                associationCapabilityBean,
                inResourceOrAncestor(
                        resource,
                        ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CONFIGURED, CapabilityCollectionType.F_ASSOCIATIONS)),
                AssociationsCapabilityConfigItem.class);
        this.contextDescription = "definition of " + resource;
        this.completeSchema = new CompleteResourceSchemaImpl(
                basicResourceInformation,
                ResourceTypeUtil.isCaseIgnoreAttributeNames(resource));
    }

    /** Creates the complete resource schema. */
    @NotNull CompleteResourceSchema parse() throws SchemaException, ConfigurationException {

        schemaHandling.checkAttributeNames();

        createEmptyObjectClassDefinitions();
        createEmptyObjectTypeDefinitions();

        // In theory, this could be done alongside creation of empty object type definitions.
        resolveAuxiliaryObjectClassNames();

        // We can parse attributes only after we have all the object class info parsed (including auxiliary object classes)
        parseAttributes();

        // Protected objects and delineation. They refer to attributes.
        parseOtherFeatures();

        // Associations refer to object types, attributes, and delineation, so they must come after them.
        parseAssociationTypesDefinitions();
        parseLegacyAssociations();
        parseNativeAssociations();

        completeSchema.freeze();

        return completeSchema;
    }

    private void createEmptyObjectClassDefinitions() throws ConfigurationException, SchemaException {

        LOGGER.trace("Creating refined object class definitions");

        var classDefinitionCIs = schemaHandling.getObjectClasses();
        for (var classDefinitionCI : classDefinitionCIs) {
            var objectClassName = classDefinitionCI.getObjectClassName();
            var rawObjectClassDefinition =
                    classDefinitionCI.configNonNull(
                            rawResourceSchema.findObjectClassDefinition(objectClassName),
                            "Object class %s referenced in %s does not exist on the resource".formatted(objectClassName, DESC));
            assertClassNotDefinedYet(objectClassName);
            completeSchema.add(
                    ResourceObjectClassDefinitionImpl.refined(
                            basicResourceInformation, rawObjectClassDefinition, classDefinitionCI.value()));
        }

        LOGGER.trace("Created {} refined object class definitions from beans; creating remaining ones", classDefinitionCIs.size());

        for (ResourceObjectClassDefinition rawObjectClassDefinition : rawResourceSchema.getObjectClassDefinitions()) {
            if (completeSchema.findObjectClassDefinition(rawObjectClassDefinition.getObjectClassName()) == null) {
                completeSchema.add(
                        ResourceObjectClassDefinitionImpl.refined(
                                basicResourceInformation, rawObjectClassDefinition, null));
            }
        }

        LOGGER.trace("Successfully created {} refined object type definitions (in total)",
                completeSchema.getObjectClassDefinitions().size());
    }

    private void assertClassNotDefinedYet(QName objectClassName) throws ConfigurationException {
        MiscUtil.configCheck(
                completeSchema.findObjectClassDefinition(objectClassName) == null,
                "Multiple definitions for object class %s in %s", objectClassName, resource);
    }

    private void createEmptyObjectTypeDefinitions() throws SchemaException, ConfigurationException {
        LOGGER.trace("Creating empty object type definitions");
        int created = 0;
        for (var typeDefinitionCI : schemaHandling.getObjectTypes()) {
            if (!typeDefinitionCI.isAbstract()) {
                ResourceObjectTypeDefinition definition = createEmptyObjectTypeDefinition(typeDefinitionCI);
                LOGGER.trace("Created (empty) object type definition: {}", definition);
                assertTypeNotDefinedYet(definition);
                completeSchema.add(definition);
                created++;
            } else {
                LOGGER.trace("Ignoring abstract definition bean: {}", typeDefinitionCI);
            }
        }
        checkForMultipleDefaults();
        LOGGER.trace("Successfully created {} empty object type definitions", created);
    }


    private void assertTypeNotDefinedYet(ResourceObjectTypeDefinition definition) throws ConfigurationException {
        ResourceObjectTypeIdentification identification = definition.getTypeIdentification();
        var existing = completeSchema.getObjectTypeDefinition(identification);
        if (existing != null) {
            throw new ConfigurationException("Multiple definitions of " + identification + " in " + contextDescription);
        }
    }

    private ResourceObjectTypeDefinition createEmptyObjectTypeDefinition(
            @NotNull ResourceObjectTypeDefinitionConfigItem definitionCI)
            throws SchemaException, ConfigurationException {

        ResourceObjectTypeIdentification identification = definitionCI.getTypeIdentification();

        // Object class refinement is not merged here (yet). We assume that the object class name could be woven into
        // the bean at any level. And we hope that although we do the merging in the top-bottom direction, it will cause
        // no harm if we merge the object class refinement (i.e. topmost component) at last.
        ObjectTypeExpansion expansion = new ObjectTypeExpansion();
        ResourceObjectTypeDefinitionType expandedBean = expansion.expand(definitionCI.value());

        // We assume that the path was not changed. Quite a hack, though.
        var expandedCI = configItem(expandedBean, definitionCI.origin(), ResourceObjectTypeDefinitionConfigItem.class);

        QName objectClassName = expandedCI.getObjectClassName();
        ResourceObjectClassDefinition objectClassDefinition = getObjectClassDefinitionRequired(objectClassName, expandedCI);

        ResourceObjectTypeDefinitionType objectClassRefinementBean = objectClassDefinition.getDefinitionBean();
        merge(expandedBean, objectClassRefinementBean); // no-op if refinement bean is empty

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
                completeSchema.findObjectClassDefinition(objectClassName),
                "No definition found for object class '%s' referenced in %s", objectClassName, DESC);
    }

    private @NotNull ResourceObjectTypeDefinition getObjectTypeDefinitionRequired(
            @NotNull ResourceObjectTypeIdentification identification, @NotNull ConfigurationItem<?> contextCI)
            throws ConfigurationException {
        return contextCI.configNonNull(
                completeSchema.getObjectTypeDefinition(identification),
                "No definition found for object type '%s' referenced in %s", identification, DESC);
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
    private void checkForMultipleDefaults() throws ConfigurationException {
        for (ShadowKindType kind : ShadowKindType.values()) {
            var defaults = completeSchema.getObjectTypeDefinitions().stream()
                    .filter(def -> def.matchesKind(kind) && def.isDefaultForKind())
                    .collect(Collectors.toList());
            configCheck(defaults.size() <= 1, "More than one default %s definition in %s: %s",
                    kind, contextDescription, defaults);
        }
    }

    /**
     * Fills in list of auxiliary object class definitions (in object type definitions)
     * with definitions resolved from their qualified names.
     */
    private void resolveAuxiliaryObjectClassNames() throws ConfigurationException {
        for (ResourceObjectDefinition objectDef: completeSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .resolveAuxiliaryObjectClassNames();
        }
    }

    /**
     * Creates definitions for associations; includes resolving their targets (given by kind + intent(s)).
     */
    private void parseLegacyAssociations() throws ConfigurationException {
        for (ResourceObjectTypeDefinition objectTypeDef : completeSchema.getObjectTypeDefinitions()) {
            new ResourceObjectDefinitionParser(objectTypeDef)
                    .parseLegacyAssociations();
        }
    }

    /** TEMPORARY CODE!!! TO BE REMOVED. */
    private void parseNativeAssociations() {
        for (ResourceObjectDefinition objectDef : completeSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .parseNativeAssociations();
        }
    }

    /** These are associations from the {@link SchemaHandlingType#getAssociationType()} section. */
    private void parseAssociationTypesDefinitions() throws ConfigurationException {
        for (var assocTypeDefCI : schemaHandling.getAssociationTypes()) {
            LOGGER.trace("Parsing association type {}", assocTypeDefCI);
            new AssociationTypeParser(assocTypeDefCI)
                    .parse();
        }
    }

    private void parseAttributes() throws ConfigurationException {
        for (ResourceObjectDefinition objectDef : completeSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .parseAttributes();
        }
    }

    private void parseOtherFeatures() throws SchemaException, ConfigurationException {
        for (ResourceObjectDefinition objectDef : completeSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .parseOtherFeatures();
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
        @NotNull private final AbstractResourceObjectDefinitionConfigItem definitionCI;

        ResourceObjectDefinitionParser(@NotNull ResourceObjectDefinition definition) {
            this.definition = (AbstractResourceObjectDefinitionImpl) definition;
            var definitionBean = definition.getDefinitionBean();
            if (definition instanceof ResourceObjectTypeDefinition) {
                this.definitionCI = configItem(
                        definitionBean,
                        inResourceOrAncestor(
                                resource,
                                ResourceType.F_SCHEMA_HANDLING.append(SchemaHandlingType.F_OBJECT_TYPE)), // ignoring ID for now
                        schemaHandling,
                        ResourceObjectTypeDefinitionConfigItem.class);
            } else if (definition instanceof ResourceObjectClassDefinition) {
                this.definitionCI = configItem(
                        definitionBean,
                        inResourceOrAncestor(
                                resource,
                                ResourceType.F_SCHEMA_HANDLING.append(SchemaHandlingType.F_OBJECT_CLASS)), // ignoring ID for now
                        schemaHandling,
                        ResourceObjectClassDefinitionConfigItem.class);
            } else {
                throw new IllegalStateException("Neither object type or object class? " + definition);
            }
        }

        void resolveAuxiliaryObjectClassNames() throws ConfigurationException {
            for (QName auxObjectClassName : definitionCI.getAuxiliaryObjectClassNames()) {
                definition.addAuxiliaryObjectClassDefinition(
                        getObjectClassDefinitionRequired(auxObjectClassName, definitionCI));
            }
        }

        /** Those associations are attached right onto the object type definition. */
        void parseLegacyAssociations() throws ConfigurationException {
            for (var associationDefCI : definitionCI.getAssociations()) {
                // TODO check if not already there!
                definition.addAssociationDefinition(
                        new LegacyAssociationParser(associationDefCI, definition)
                                .parse());
            }
        }

        /**
         * Fills-in attribute definitions in `typeDef` by traversing all "raw" attributes defined in the structural
         * object class and all the auxiliary object classes.
         *
         * Initializes identifier names and protected objects patterns.
         */
        private void parseAttributes() throws ConfigurationException {

            LOGGER.trace("Parsing attributes of {}", definition);

            parseAttributesFromRawObjectClass(definition.getRawObjectClassDefinition(), false);
            for (ResourceObjectDefinition auxDefinition : definition.getAuxiliaryDefinitions()) {
                parseAttributesFromRawObjectClass(auxDefinition.getRawObjectClassDefinition(), true);
            }

            assertNoOtherAttributes();

            setupIdentifiers();
        }

        /**
         * There should be no attributes in the `schemaHandling` definition without connector-provided (raw schema)
         * counterparts.
         */
        private void assertNoOtherAttributes() throws ConfigurationException {
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
        private void parseAttributesFromRawObjectClass(@NotNull ResourceObjectClassDefinition rawClassDef, boolean auxiliary)
                throws ConfigurationException {
            assert rawClassDef.isRaw();
            for (ResourceAttributeDefinition<?> attrDef : rawClassDef.getAttributeDefinitions()) {
                if (attrDef instanceof RawResourceAttributeDefinition<?> rawAttrDef) {
                    parseRawAttribute(rawAttrDef, auxiliary);
                } else {
                    throw new IllegalStateException(
                            "Non-raw attribute in raw object class? %s in %s; as defined in %s".formatted(
                                    attrDef, rawClassDef, contextDescription));
                }
            }
        }

        private void parseRawAttribute(@NotNull RawResourceAttributeDefinition<?> rawAttrDef, boolean fromAuxClass)
                throws ConfigurationException {

            ItemName attrName = rawAttrDef.getItemName();

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
            ResourceAttributeDefinition<?> attrDef;
            try {
                attrDef = ResourceAttributeDefinitionImpl.create(rawAttrDef, attrDefBean);
            } catch (SchemaException e) { // TODO throw the configuration exception right in the 'create' method
                throw definitionCI.configException("Error while parsing attribute '%s' in %s: %s", attrName, DESC, e.getMessage());
            }
            definition.add(attrDef);

            if (attrDef.isDisplayNameAttribute()) {
                definition.setDisplayNameAttributeName(attrName);
            }
        }

        /**
         * Copy all primary identifiers from the raw definition.
         *
         * For secondary ones, use configured information (if present). Otherwise, use raw definition as well.
         */
        private void setupIdentifiers() {
            ResourceObjectClassDefinition rawDefinition = definition.getRawObjectClassDefinition();

            for (ResourceAttributeDefinition<?> attrDef : definition.getAttributeDefinitions()) {
                ItemName attrName = attrDef.getItemName();

                if (rawDefinition.isPrimaryIdentifier(attrName)) {
                    definition.getPrimaryIdentifiersNames().add(attrName);
                }
                if (attrDef.isSecondaryIdentifierOverride() == null) {
                    if (rawDefinition.isSecondaryIdentifier(attrName)) {
                        definition.getSecondaryIdentifiersNames().add(attrName);
                    }
                } else {
                    if (attrDef.isSecondaryIdentifierOverride()) {
                        definition.getSecondaryIdentifiersNames().add(attrName);
                    }
                }
            }
        }

        /** TEMPORARY CODE!!! TO BE REMOVED. */
        void parseNativeAssociations() {
            var rawObjectClassDef = definition.getRawObjectClassDefinition();
            for (var origAssocDef : rawObjectClassDef.getAssociationDefinitions()) {
                stateCheck(origAssocDef.isRaw(), "Non-raw definition %s in %s", origAssocDef, rawObjectClassDef);
                // TODO connect with the refinements
                definition.addAssociationDefinition(
                        ShadowAssociationDefinition.fromRaw(
                                origAssocDef.getRawDefinitionRequired(), null));
            }
        }

        /**
         * Parses protected objects, delineation, and so on.
         */
        private void parseOtherFeatures() throws SchemaException, ConfigurationException {
            parseProtected();
            parseDelineation();
        }

        /**
         * Converts protected objects patterns from "bean" to "compiled" form.
         */
        private void parseProtected() throws SchemaException, ConfigurationException {
            List<ResourceObjectPatternType> protectedPatternBeans = definitionCI.value().getProtected();
            if (protectedPatternBeans.isEmpty()) {
                return;
            }
            var prismObjectDef = definition.toPrismObjectDefinition();
            for (ResourceObjectPatternType protectedPatternBean : protectedPatternBeans) {
                definition.addProtectedObjectPattern(
                        convertToPattern(protectedPatternBean, prismObjectDef));
            }
        }

        private ResourceObjectPattern convertToPattern(
                ResourceObjectPatternType patternBean, PrismObjectDefinition<ShadowType> prismObjectDef)
                throws SchemaException, ConfigurationException {
            SearchFilterType filterBean =
                    MiscUtil.configNonNull(
                            patternBean.getFilter(),
                            () -> "No filter in resource object pattern");
            ObjectFilter filter =
                    MiscUtil.configNonNull(
                            PrismContext.get().getQueryConverter().parseFilter(filterBean, prismObjectDef),
                            () -> "No filter in resource object pattern");
            return new ResourceObjectPattern(definition, filter);
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
        private @NotNull ResourceObjectTypeDefinitionType expand(
                @NotNull ResourceObjectTypeDefinitionType definitionBean) throws ConfigurationException, SchemaException {
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
                ResourceObjectTypeDefinitionType expandedSubBean = definitionBean.clone();
                merge(expandedSubBean, expandedSuperBean);
                return expandedSubBean;
            }
        }

        /** Resolves the reference to a super-type. Must be in the same resource. TODO migrate to CIs. */
        private @NotNull ResourceObjectTypeDefinitionType find(@NotNull ResourceObjectTypeIdentificationType superRefBean)
                throws ConfigurationException {
            SuperReference superRef = SuperReference.of(superRefBean);
            List<ResourceObjectTypeDefinitionType> matching = schemaHandling.getObjectTypes().stream()
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

    /** Parses given "4.9+ format" association type. */
    private class AssociationTypeParser {

        @NotNull private final ShadowAssociationTypeDefinitionConfigItem associationTypeDefinitionCI;

        /** Currently, there must be a simulated association class, as we have no native ones. */
        @NotNull private final SimulatedAssociationClassConfigItem simulationCI;

        @NotNull private final Collection<ResourceObjectTypeDefinition> subjectTypeDefinitions;

        @NotNull private final Collection<ResourceObjectTypeDefinition> objectTypeDefinitions;

        AssociationTypeParser(@NotNull ShadowAssociationTypeDefinitionConfigItem associationTypeDefinitionCI)
                throws ConfigurationException {
            this.associationTypeDefinitionCI = associationTypeDefinitionCI;
            // This is a temporary check.
            MiscUtil.configNonNull(associationsCapabilityCI,
                    "No simulated associations for %s (native associations are not implemented yet)", resource);
            var associationClassName = associationTypeDefinitionCI.getAssociationClassName();
            this.simulationCI =
                    associationTypeDefinitionCI.configNonNull(
                            associationsCapabilityCI.getAssociationClass(associationClassName),
                            "Simulated association class '%s' not found; referenced from %s", associationClassName, DESC);
            this.subjectTypeDefinitions = determineTypeDefinitions(
                    associationTypeDefinitionCI.getSubjectTypeIdentifiers(),
                    simulationCI.getSubject());
            this.objectTypeDefinitions = determineTypeDefinitions(
                    associationTypeDefinitionCI.getObjectTypeIdentifiers(),
                    simulationCI.getObject());
        }

        void parse() throws ConfigurationException {

            var simulationDefinition = ShadowAssociationClassSimulationDefinition.parseModern(
                    associationTypeDefinitionCI, simulationCI, completeSchema, subjectTypeDefinitions, objectTypeDefinitions);

            var typeDefinition = ShadowAssociationTypeDefinition.parseAssociationType(
                    associationTypeDefinitionCI, simulationDefinition, subjectTypeDefinitions, objectTypeDefinitions);

            // Attaching to subject types
            for (ResourceObjectTypeDefinition subjectTypeDef : subjectTypeDefinitions) {
                // TODO connect to the raw definition, if one is present
                var associationDef = ShadowAssociationDefinition.parseAssociationType(
                        simulationDefinition.getLocalSubjectItemName(), typeDefinition, null, associationTypeDefinitionCI);
                configCheck(!subjectTypeDef.containsAssociationDefinition(associationDef.getItemName()),
                        "Association %s already exists in %s in %s",
                        associationDef.getItemName(), subjectTypeDef, associationTypeDefinitionCI);
                ((AbstractResourceObjectDefinitionImpl) subjectTypeDef)
                        .addAssociationDefinition(associationDef);
            }
        }

        private @NotNull Set<ResourceObjectTypeDefinition> determineTypeDefinitions(
                @NotNull Collection<? extends ResourceObjectTypeIdentification> typeIdentifiers,
                @NotNull SimulatedAssociationClassParticipantConfigItem simulatedParticipant)
                throws ConfigurationException {

            Set<ResourceObjectTypeDefinition> definitions = new HashSet<>();

            // Explicit type enumeration takes precedence over everything else.
            if (!typeIdentifiers.isEmpty()) {
                for (ResourceObjectTypeIdentification typeIdentifier : typeIdentifiers) {
                    definitions.add(
                            getObjectTypeDefinitionRequired(typeIdentifier, associationTypeDefinitionCI));
                }
                return definitions;
            }

            // Second, we try to determine the types from the supported object classes - we simply take all matching types.
            for (QName participantObjectClassName : simulatedParticipant.getObjectClassNames()) {
                // Just to check the sanity
                getObjectClassDefinitionRequired(participantObjectClassName, simulatedParticipant);
                var matchingTypeDefinitions = completeSchema
                        .getObjectTypeDefinitions(def -> QNameUtil.match(def.getObjectClassName(), participantObjectClassName));
                configCheck(!matchingTypeDefinitions.isEmpty(),
                        "No object type definition(s) found for object class '%s' in %s",
                        participantObjectClassName, simulatedParticipant);
                definitions.addAll(matchingTypeDefinitions);
            }

            if (!definitions.isEmpty()) {
                return definitions;
            } else {
                // May happen if the simulated participant has no delineations.
                throw new ConfigurationException("No object type/class definitions found for " + simulatedParticipant);
            }
        }
    }

    /** Parses given legacy ("pre-4.9") association type. */
    private class LegacyAssociationParser {

        @NotNull private final ResourceObjectAssociationConfigItem associationDefCI;

        /** This one is being built. */
        @NotNull private final ResourceObjectTypeDefinition subjectTypeDefinition;

        LegacyAssociationParser(
                @NotNull ResourceObjectAssociationConfigItem associationDefCI,
                @NotNull AbstractResourceObjectDefinitionImpl subjectDefinition) throws ConfigurationException {
            this.associationDefCI = associationDefCI;
            if (!(subjectDefinition instanceof ResourceObjectTypeDefinition _subjectTypeDefinition)) {
                throw new ConfigurationException("Associations cannot be defined on object classes: " + subjectDefinition);
            }
            this.subjectTypeDefinition = _subjectTypeDefinition;
        }

        @NotNull ShadowAssociationDefinition parse() throws ConfigurationException {

            var objectTypeDefinitions = getObjectTypeDefinitions();

            var simulationDefinition = ShadowAssociationClassSimulationDefinition.parseLegacy(
                    associationDefCI, completeSchema, subjectTypeDefinition, objectTypeDefinitions);

            var associationTypeDefinition = ShadowAssociationTypeDefinition.parseLegacy(
                    associationDefCI, simulationDefinition, subjectTypeDefinition, objectTypeDefinitions);

            // TODO check if raw definition is not present
            return ShadowAssociationDefinition.parseLegacy(associationTypeDefinition, associationDefCI);
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
                        if (((Collection<String>) intents).isEmpty()) {
                            return objectDef.isDefaultForKind();
                        } else {
                            return ((Collection<String>) intents).contains(objectDef.getIntent());
                        }
                    };

            var predicateDescription = lazy(() -> "kind " + kind + ", intents " + intents);

            Collection<ResourceObjectTypeDefinition> matching =
                    completeSchema.getObjectTypeDefinitions().stream()
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
}
