/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.merger.objdef.ResourceObjectTypeDefinitionMergeOperation;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Creates refined class and object type definitions in {@link ResourceSchemaImpl} objects.
 *
 * These definitions are derived from:
 *
 * 1. raw object class definitions ({@link ResourceObjectClassDefinition}) (obtained dynamically or statically),
 * 2. configured {@link SchemaHandlingType} beans in resource definition.
 *
 * This class is instantiated for each parsing operation.
 */
public class RefinedResourceSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(RefinedResourceSchemaParser.class);

    /** Resource whose refined schema is being parsed. */
    @NotNull private final ResourceType resource;

    /** Raw resource schema we start with. */
    @NotNull private final ResourceSchema rawResourceSchema;

    @NotNull private final String contextDescription;

    @NotNull private final ResourceSchemaImpl completeSchema = new ResourceSchemaImpl();

    public RefinedResourceSchemaParser(@NotNull ResourceType resource, @NotNull ResourceSchema rawResourceSchema) {
        this.resource = resource;
        this.rawResourceSchema = rawResourceSchema;
        this.contextDescription = "definition of " + resource;
    }

    /**
     * Creates the refined resource schema.
     *
     * Returns null if the resource has no (raw) schema.
     */
    public @NotNull ResourceSchema parse() throws SchemaException, ConfigurationException {

        if (resource.getSchemaHandling() == null) {
            return rawResourceSchema;
        }

        createEmptyObjectClassDefinitions();
        createEmptyObjectTypeDefinitions();

        // In theory, this could be done alongside creation of empty object type definitions.
        resolveAuxiliaryObjectClassNames();

        // Associations refer to object types, so we have to parse them after empty type definitions are created.
        parseAssociations();

        // We can parse attributes only after we have all the object class info parsed (including auxiliary object classes)
        parseAttributes();

        // Protected objects, delineation, and so on.
        parseOtherFeatures();

        completeSchema.freeze();

        return completeSchema;
    }

    private void createEmptyObjectClassDefinitions() throws ConfigurationException {

        LOGGER.trace("Creating refined object class definitions");

        List<ResourceObjectTypeDefinitionType> definitionBeans = resource.getSchemaHandling().getObjectClass();
        for (ResourceObjectTypeDefinitionType definitionBean : definitionBeans) {
            QName objectClassName =
                    MiscUtil.requireNonNull(
                            getObjectClassName(definitionBean),
                            () -> new ConfigurationException(
                                    "Object class name must not be null in " + contextDescription));

            ResourceObjectClassDefinition rawObjectClassDefinition =
                    MiscUtil.requireNonNull(
                            rawResourceSchema.findObjectClassDefinition(objectClassName),
                            () -> new ConfigurationException(
                                    "Object class " + objectClassName + " referenced by schemaHandling does not exist in "
                                            + resource));

            assertClassNotDefinedYet(objectClassName);
            completeSchema.add(
                    ResourceObjectClassDefinitionImpl.refined(rawObjectClassDefinition, definitionBean));
        }

        LOGGER.trace("Created {} refined object class definitions from beans; creating remaining ones", definitionBeans.size());

        for (ResourceObjectClassDefinition rawObjectClassDefinition : rawResourceSchema.getObjectClassDefinitions()) {
            if (completeSchema.findObjectClassDefinition(rawObjectClassDefinition.getObjectClassName()) == null) {
                completeSchema.add(
                        ResourceObjectClassDefinitionImpl.refined(rawObjectClassDefinition, null));
            }
        }

        LOGGER.trace("Successfully created {} refined object type definitions (in total)",
                completeSchema.getObjectClassDefinitions().size());
    }

    private void assertClassNotDefinedYet(QName objectClassName) throws ConfigurationException {
        var existing = completeSchema.findObjectClassDefinition(objectClassName);
        if (existing != null) {
            throw new ConfigurationException(
                    "Multiple definitions for object class " + objectClassName + " in " + contextDescription);
        }
    }

    private void createEmptyObjectTypeDefinitions() throws SchemaException, ConfigurationException {
        LOGGER.trace("Creating empty object type definitions");
        int created = 0;
        for (ResourceObjectTypeDefinitionType definitionBean : resource.getSchemaHandling().getObjectType()) {
            if (!isAbstract(definitionBean)) {
                ResourceObjectTypeDefinition definition = createEmptyObjectTypeDefinition(definitionBean);
                LOGGER.trace("Created (empty) object type definition: {}", definition);
                assertTypeNotDefinedYet(definition);
                completeSchema.add(definition);
                created++;
            } else {
                LOGGER.trace("Ignoring abstract definition bean: {}", definitionBean);
            }
        }
        checkForMultipleDefaults();
        LOGGER.trace("Successfully created {} empty object type definitions", created);
    }

    private boolean isAbstract(@NotNull ResourceObjectTypeDefinitionType definitionBean) {
        return Boolean.TRUE.equals(definitionBean.isAbstract());
    }

    private void assertTypeNotDefinedYet(ResourceObjectTypeDefinition definition) throws ConfigurationException {
        ResourceObjectTypeIdentification identification = definition.getTypeIdentification();
        var existing = completeSchema.getObjectTypeDefinition(identification);
        if (existing != null) {
            throw new ConfigurationException("Multiple definitions of " + identification + " in " + contextDescription);
        }
    }

    private ResourceObjectTypeDefinition createEmptyObjectTypeDefinition(@NotNull ResourceObjectTypeDefinitionType definitionBean)
            throws SchemaException, ConfigurationException {

        ResourceObjectTypeIdentification identification = ResourceObjectTypeIdentification.of(definitionBean);

        // Object class refinement is not merged here (yet). We assume that the object class name could be woven into
        // the bean at any level. And we hope that although we do the merging in the top-bottom direction, it will cause
        // no harm if we merge the object class refinement (i.e. topmost component) at last.
        ResourceObjectTypeDefinitionType expandedBean = expand(definitionBean);

        QName objectClassName = MiscUtil.configNonNull(
                getObjectClassName(expandedBean),
                () -> "Definition of " + identification + " does not have objectclass, in " + contextDescription);

        ResourceObjectClassDefinition objectClassDefinition =
                MiscUtil.configNonNull(
                        completeSchema.findObjectClassDefinition(objectClassName),
                        () -> "Unknown object class " + objectClassName + " (referenced by " + identification
                                + ") not present in " + contextDescription);

        ResourceObjectTypeDefinitionType objectClassRefinementBean = objectClassDefinition.getDefinitionBean();
        merge(expandedBean, objectClassRefinementBean); // no-op if refinement bean is empty

        return new ResourceObjectTypeDefinitionImpl(
                identification,
                completeSchema.findObjectClassDefinitionRequired(objectClassName),
                CloneUtil.toImmutable(expandedBean),
                resource.getOid());
    }

    /**
     * Expands the definition by resolving its ancestor (if there's any), recursively if needed.
     *
     * TODO should we move the expansion code into a separate class?
     */
    private @NotNull ResourceObjectTypeDefinitionType expand(
            @NotNull ResourceObjectTypeDefinitionType definitionBean) throws ConfigurationException, SchemaException {
        if (definitionBean.getSuper() == null) {
            return definitionBean;
        } else {
            var expanded = expand(definitionBean, Sets.newIdentityHashSet());
            LOGGER.trace("Expanded object type definition into:\n{}", expanded.debugDumpLazily(1));
            return expanded;
        }
    }

    /**
     * Expands the definition by resolving its ancestor (if there's any), recursively if needed.
     *
     * Does not modify existing {@link #resource} object, so it clones the beans that are being expanded.
     *
     * @param seen stores beans that have been seen when resolving the ancestors. Used to detect cycles.
     */
    private @NotNull ResourceObjectTypeDefinitionType expand(
            @NotNull ResourceObjectTypeDefinitionType subBean,
            @NotNull Set<ResourceObjectTypeDefinitionType> seen) throws ConfigurationException, SchemaException {
        SuperObjectTypeReferenceType superRef = subBean.getSuper();
        if (superRef == null) {
            return subBean;
        } else {
            ResourceObjectTypeDefinitionType superBean = find(superRef);
            if (!seen.add(superBean)) {
                throw new ConfigurationException("A cycle in super-type hierarchy, detected at " + superBean
                        + " in " + contextDescription);
            }
            ResourceObjectTypeDefinitionType expandedSuperBean = expand(superBean, seen);
            ResourceObjectTypeDefinitionType expandedSubBean = subBean.clone();
            merge(expandedSubBean, expandedSuperBean);
            return expandedSubBean;
        }
    }

    /**
     * Resolves the reference to a super-type. Must be in the same resource.
     */
    private @NotNull ResourceObjectTypeDefinitionType find(@NotNull SuperObjectTypeReferenceType superRefBean)
            throws ConfigurationException {
        SuperReference superRef = SuperReference.of(superRefBean);
        List<ResourceObjectTypeDefinitionType> matching = resource.getSchemaHandling().getObjectType().stream()
                .filter(superRef::matches)
                .collect(Collectors.toList());
        return MiscUtil.extractSingletonRequired(matching,
                () -> new ConfigurationException("Multiple definitions matching " + superRef + " found in " + contextDescription),
                () -> new ConfigurationException("No definition matching " + superRef + " found in " + contextDescription));
    }

    /**
     * Merges "source" (super-type) into "target" (sub-type).
     */
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
    private void resolveAuxiliaryObjectClassNames() throws SchemaException {
        for (ResourceObjectDefinition objectDef: completeSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .resolveAuxiliaryObjectClassNames();
        }
    }

    /**
     * Creates definitions for associations; includes resolving their targets (given by kind + intent(s)).
     */
    private void parseAssociations() throws SchemaException {
        for (ResourceObjectDefinition objectDef : completeSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .parseAssociations();
        }
    }

    private void parseAttributes() throws SchemaException {
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
         * Definition bean from `schemaHandling` section.
         */
        @NotNull private final ResourceObjectTypeDefinitionType definitionBean;

        ResourceObjectDefinitionParser(@NotNull ResourceObjectDefinition definition) {
            this.definition = (AbstractResourceObjectDefinitionImpl) definition;
            this.definitionBean = this.definition.getDefinitionBean();
        }

        void resolveAuxiliaryObjectClassNames() throws SchemaException {
            for (QName auxObjectClassName : getAuxiliaryObjectClassNames(definitionBean)) {
                LOGGER.trace("Resolving auxiliary object class name: {} for {}", auxObjectClassName, definition);
                definition.addAuxiliaryObjectClassDefinition(
                        MiscUtil.requireNonNull(
                                completeSchema.findObjectClassDefinition(auxObjectClassName),
                                () -> "Auxiliary object class " + auxObjectClassName + " specified in " +
                                        definition + " does not exist"));
            }
        }

        void parseAssociations() throws SchemaException {
            for (ResourceObjectAssociationType associationDefBean : definitionBean.getAssociation()) {
                ResourceAssociationDefinition associationDef = new ResourceAssociationDefinition(associationDefBean);
                associationDef.setAssociationTarget(
                        resolveAssociationTarget(associationDef));
                definition.addAssociationDefinition(associationDef);
            }
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
        private ResourceObjectTypeDefinition resolveAssociationTarget(ResourceAssociationDefinition associationDef)
                throws SchemaException {
            @NotNull ShadowKindType kind = associationDef.getKind();
            @NotNull Collection<String> intents = associationDef.getIntents();
            Collection<ResourceObjectTypeDefinition> matching =
                    completeSchema.getObjectTypeDefinitions().stream()
                            .filter(def -> matches(def, associationDef))
                            .collect(Collectors.toList());
            if (matching.isEmpty()) {
                throw new SchemaException("No object type definition for association " + associationDef + " in " + contextDescription);
            } else if (ResourceSchemaUtil.areDefinitionsCompatible(matching)) {
                return matching.iterator().next();
            } else {
                throw new SchemaException(
                        "Incompatible definitions found for kind " + kind + ", intents: " + intents + ": " + matching);
            }
        }

        /**
         * Returns `true` if the (target) type definition matches requirements of the association definition: kind + intent(s).
         */
        private boolean matches(
                @NotNull ResourceObjectTypeDefinition def, @NotNull ResourceAssociationDefinition associationDef) {
            if (associationDef.getKind() != def.getKind()) {
                return false;
            }
            Collection<String> intents = associationDef.getIntents();
            if (intents.isEmpty()) {
                return def.isDefaultForKind();
            } else {
                return intents.contains(def.getIntent());
            }
        }

        /**
         * Fills-in attribute definitions in `typeDef` by traversing all "raw" attributes defined in the structural
         * object class and all the auxiliary object classes.
         *
         * Initializes identifier names and protected objects patterns.
         */
        private void parseAttributes() throws SchemaException {

            LOGGER.trace("Parsing attributes of {}", definition);

            parseAttributesFromObjectClass(definition.getRawObjectClassDefinition(), false);
            for (ResourceObjectDefinition auxDefinition : definition.getAuxiliaryDefinitions()) {
                parseAttributesFromObjectClass(auxDefinition.getRawObjectClassDefinition(), true);
            }

            assertNoOtherAttributes();

            setupIdentifiers();
        }

        /**
         * There should be no attributes in the `schemaHandling` definition without connector-provided (raw schema)
         * counterparts.
         */
        private void assertNoOtherAttributes() throws SchemaException {
            for (ResourceAttributeDefinitionType attrDefBean : definitionBean.getAttribute()) {
                QName attrName = ItemPathTypeUtil.asSingleName(
                        Objects.requireNonNull(attrDefBean.getRef(), () -> "No attribute name in " + attrDefBean));
                // TODO check that we really look into aux object classes
                if (!definition.containsAttributeDefinition(attrName) && !ResourceSchemaUtil.isIgnored(attrDefBean)) {
                    throw new SchemaException(String.format(
                            "Definition of attribute %s not found in object class %s nor auxiliary object classes for %s "
                                    + "as defined in %s",
                            attrName, definition.getObjectClassName(), definition, contextDescription));
                }
            }
        }

        /**
         * Takes all attributes from resource object class definition, and pairs (enriches)
         * them with `schemaHandling` information.
         */
        private void parseAttributesFromObjectClass(@NotNull ResourceObjectClassDefinition rawClassDef, boolean auxiliary)
                throws SchemaException {
            for (ResourceAttributeDefinition<?> rawAttrDef : rawClassDef.getAttributeDefinitions()) {
                parseAttributeFromObjectClass(rawAttrDef, auxiliary);
            }
        }

        private void parseAttributeFromObjectClass(
                @NotNull ResourceAttributeDefinition<?> rawAttrDef, boolean auxiliary) throws SchemaException {

            ItemName attrName = rawAttrDef.getItemName();

            LOGGER.trace("Parsing attribute {} (auxiliary = {})", attrName, auxiliary);

            // TODO make this context description lazily evaluated
            String attrContextDescription = attrName + ", in " + contextDescription;

            // We MUST NOT skip ignored attribute definitions here. We must include them in the schema as
            // the shadows will still have that attributes and we will need their type definition to work
            // well with them. They may also be mandatory. We cannot pretend that they do not exist.

            if (definition.containsAttributeDefinition(attrName)) {
                if (auxiliary) {
                    return;
                } else {
                    throw new SchemaException("Duplicate definition of attribute " + attrName + " in "
                            + definition.getHumanReadableName() + ", in " + contextDescription);
                }
            }

            ResourceAttributeDefinitionType attrDefBean = findAttributeDefinitionBean(attrName, attrContextDescription);
            ResourceAttributeDefinition<?> attrDef = ResourceAttributeDefinitionImpl.create(rawAttrDef, attrDefBean);
            definition.add(attrDef);

            if (attrDef.isDisplayNameAttribute()) {
                definition.setDisplayNameAttributeName(attrName);
            }
        }

        private ResourceAttributeDefinitionType findAttributeDefinitionBean(QName attrName, String contextDescription)
                throws SchemaException {
            List<ResourceAttributeDefinitionType> matchingDefBeans = new ArrayList<>();
            for (ResourceAttributeDefinitionType attrDefBean : definitionBean.getAttribute()) {
                if (attrDefBean.getRef() == null) {
                    throw new SchemaException("Missing reference to the attribute schema definition in definition "
                            + SchemaDebugUtil.prettyPrint(attrDefBean) + " during processing of " + contextDescription);
                }
                if (QNameUtil.match(
                        ItemPathTypeUtil.asSingleNameOrFail(attrDefBean.getRef()),
                        attrName)) {
                    matchingDefBeans.add(attrDefBean);
                }
            }
            return MiscUtil.extractSingleton(
                    matchingDefBeans,
                    () -> new SchemaException("Duplicate definition of attribute " + attrName + " in "
                            + definition + ", in " + contextDescription));
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
            if (definitionBean.getProtected().isEmpty()) {
                return;
            }
            PrismObjectDefinition<ShadowType> prismObjectDef = definition.computePrismObjectDefinition();
            for (ResourceObjectPatternType protectedPatternBean : definitionBean.getProtected()) {
                ResourceObjectPattern protectedPattern = convertToPattern(protectedPatternBean, prismObjectDef);
                definition.addProtectedObjectPattern(protectedPattern);
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
            List<QName> auxiliaryObjectClassNames = getAuxiliaryObjectClassNames(definitionBean);
            ResourceObjectTypeDelineationType delineationBean = definitionBean.getDelineation();
            if (delineationBean != null) {
                configCheck(definitionBean.getBaseContext() == null,
                        "Legacy base context cannot be set when delineation is configured. In %s", definition);
                configCheck(definitionBean.getSearchHierarchyScope() == null,
                        "Legacy search hierarchy scope cannot be set when delineation is configured. In %s", definition);
                definition.setDelineation(
                        ResourceObjectTypeDelineation.of(
                                delineationBean,
                                objectClassName,
                                auxiliaryObjectClassNames));
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
}
