/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.SuperReference;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * Creates type definitions in {@link ResourceSchemaImpl} objects.
 *
 * These definitions are derived from:
 *
 * 1. object class definitions ({@link ResourceObjectClassDefinition}) (obtained dynamically or statically),
 * 2. configured {@link SchemaHandlingType} beans in resource definition.
 *
 * This class is instantiated for each parsing operation.
 */
public class RefinedResourceSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(RefinedResourceSchemaParser.class);

    @NotNull private final ResourceType resource;

    @NotNull private final String contextDescription;

    /**
     * Complete (i.e. raw+refined) schema being built. Created in {@link #parse()}.
     */
    private ResourceSchemaImpl completeSchema;

    public RefinedResourceSchemaParser(@NotNull ResourceType resource) {
        this.resource = resource;
        this.contextDescription = "definition of " + resource;
    }

    /**
     * Creates the refined resource schema.
     *
     * Returns null if the resource has no (raw) schema.
     */
    public @Nullable ResourceSchema parse() throws SchemaException, ConfigurationException {
        ResourceSchemaImpl rawSchema = (ResourceSchemaImpl) ResourceSchemaFactory.getRawSchema(resource);
        if (rawSchema != null) {
            return parseWithGivenSchema(rawSchema);
        } else {
            return null;
        }
    }

    public @NotNull ResourceSchema parseWithGivenSchema(@NotNull ResourceSchema rawSchema)
            throws SchemaException, ConfigurationException {

        if (resource.getSchemaHandling() == null) {
            return rawSchema;
        }

        // We are safe here. There is currently this one implementation only.
        completeSchema = (ResourceSchemaImpl) rawSchema.clone();

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

    private void createEmptyObjectTypeDefinitions() throws SchemaException, ConfigurationException {
        LOGGER.trace("Creating empty object type definitions");
        int created = 0;
        for (ResourceObjectTypeDefinitionType definitionBean : resource.getSchemaHandling().getObjectType()) {
            if (!isAbstract(definitionBean)) {
                ResourceObjectTypeDefinition definition = createEmptyObjectTypeDefinition(definitionBean);
                LOGGER.trace("Created (empty) object type definition: {}", definition);
                completeSchema.add(definition);
                created++;
            } else {
                LOGGER.trace("Ignoring abstract definition bean: {}", definitionBean);
            }
        }
        checkForMultipleDefaults();
        checkTypeUniqueness(); // We should perhaps check this when adding definitions to the schema.
        LOGGER.trace("Successfully created {} empty object type definitions", created);
    }

    private boolean isAbstract(@NotNull ResourceObjectTypeDefinitionType definitionBean) {
        return Boolean.TRUE.equals(definitionBean.isAbstract());
    }

    private ResourceObjectTypeDefinition createEmptyObjectTypeDefinition(@NotNull ResourceObjectTypeDefinitionType definitionBean)
            throws SchemaException, ConfigurationException {

        ResourceObjectTypeIdentification identification = ResourceObjectTypeIdentification.of(definitionBean);

        ResourceObjectTypeDefinitionType expandedBean = expand(definitionBean);

        QName objectClassName = MiscUtil.requireNonNull(
                expandedBean.getObjectClass(),
                () -> "Definition of " + identification + " does not have objectclass, in " + contextDescription);

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
        new ResourceObjectTypeDefinitionMergeOperation(target, source)
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
     * There should be only a single definition for each kind/intent.
     */
    private void checkTypeUniqueness() throws ConfigurationException {
        Set<ResourceObjectTypeIdentification> identifications = new HashSet<>();
        for (ResourceObjectTypeDefinition definition : completeSchema.getObjectTypeDefinitions()) {
            ResourceObjectTypeIdentification identification = definition.getIdentification();
            if (!identifications.add(identification)) {
                throw new ConfigurationException("Multiple definitions of " + identification + " in " + contextDescription);
            }
        }
    }

    /**
     * Fills in list of auxiliary object class definitions (in object type definitions)
     * with definitions resolved from their qualified names.
     */
    private void resolveAuxiliaryObjectClassNames() throws SchemaException {
        for (ResourceObjectTypeDefinition typeDef: completeSchema.getObjectTypeDefinitions()) {
            new ResourceObjectTypeDefinitionParser(typeDef)
                    .resolveAuxiliaryObjectClassNames();
        }
    }

    /**
     * Creates definitions for associations; includes resolving their targets (given by kind + intent(s)).
     */
    private void parseAssociations() throws SchemaException {
        for (ResourceObjectTypeDefinition typeDef : completeSchema.getObjectTypeDefinitions()) {
            new ResourceObjectTypeDefinitionParser(typeDef)
                    .parseAssociations();
        }
    }

    private void parseAttributes() throws SchemaException {
        for (ResourceObjectTypeDefinition typeDef : completeSchema.getObjectTypeDefinitions()) {
            new ResourceObjectTypeDefinitionParser(typeDef)
                    .parseAttributes();
        }
    }

    private void parseOtherFeatures() throws SchemaException, ConfigurationException {
        for (ResourceObjectTypeDefinition typeDef : completeSchema.getObjectTypeDefinitions()) {
            new ResourceObjectTypeDefinitionParser(typeDef)
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
    private class ResourceObjectTypeDefinitionParser {

        /**
         * Specific object type definition being updated. (Empty on the beginning.)
         */
        @NotNull private final ResourceObjectTypeDefinitionImpl definition;

        /**
         * Definition bean from `schemaHandling` section.
         */
        @NotNull private final ResourceObjectTypeDefinitionType definitionBean;

        ResourceObjectTypeDefinitionParser(@NotNull ResourceObjectTypeDefinition definition) {
            this.definition = (ResourceObjectTypeDefinitionImpl) definition;
            this.definitionBean = this.definition.getDefinitionBean();
        }

        void resolveAuxiliaryObjectClassNames() throws SchemaException {
            for (QName auxObjectClassName : definitionBean.getAuxiliaryObjectClass()) {
                LOGGER.trace("Resolving auxiliary object class name: {} for {}", auxObjectClassName, definition);
                definition.addAuxiliaryObjectClassDefinition(
                        MiscUtil.requireNonNull(
                                completeSchema.findDefinitionForObjectClass(auxObjectClassName),
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
         * We are not very eager here - by default we mean just the flag "default for kind" being set.
         * This is in contrast with e.g. {@link ResourceSchema#findObjectDefinitionForKindInternal(ShadowKindType, QName)}
         * that makes crazy attempts to find a suitable definition. But here we won't go into such levels.)
         *
         * The matching types must share at least the object class name. This is checked by this method.
         * However, in practice they must share much more, as described in the description for
         * {@link ResourceObjectAssociationType#getIntent()} (see XSD).
         */
        private ResourceObjectTypeDefinition resolveAssociationTarget(
                ResourceAssociationDefinition associationDef)
                throws SchemaException {
            @NotNull ShadowKindType kind = associationDef.getKind();
            @NotNull Collection<String> intents = associationDef.getIntents();
            Collection<ResourceObjectTypeDefinition> matching =
                    completeSchema.getObjectTypeDefinitions().stream()
                            .filter(def -> def.matchesKind(kind) && matchesAnyIntent(def, intents))
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

        // Just a helper for previous method
        private boolean matchesAnyIntent(@NotNull ResourceObjectTypeDefinition def, @NotNull Collection<String> intents) {
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

            parseAttributesFromObjectClass(definition.getObjectClassDefinition(), false);
            for (ResourceObjectDefinition auxDefinition : definition.getAuxiliaryDefinitions()) {
                parseAttributesFromObjectClass(auxDefinition.getObjectClassDefinition(), true);
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
                    throw new SchemaException("Definition of attribute " + attrName + " not found in object class " +
                            definition.getObjectClassName() + " nor auxiliary object classes for " + definition +
                            " as defined in " + contextDescription);
                }
            }
        }

        /**
         * Takes all attributes from resource object class definition, and pairs (enriches)
         * them with `schemaHandling` information.
         */
        private void parseAttributesFromObjectClass(@NotNull ResourceObjectClassDefinition ocDef, boolean auxiliary)
                throws SchemaException {
            for (ResourceAttributeDefinition<?> attrDef : ocDef.getAttributeDefinitions()) {
                parseAttributeFromObjectClass(attrDef, auxiliary);
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
                            + definition.getKind() + "/" + definition.getIntent() + ", in " + contextDescription));
        }

        /**
         * Copy all primary identifiers from the raw definition.
         *
         * For secondary ones, use configured information (if present). Otherwise, use raw definition as well.
         */
        private void setupIdentifiers() {
            ResourceObjectClassDefinition rawDefinition = definition.getObjectClassDefinition();

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
        private void parseProtected() throws SchemaException {
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
                ResourceObjectPatternType patternBean, PrismObjectDefinition<ShadowType> prismObjectDef) throws SchemaException {
            ResourceObjectPattern resourceObjectPattern = new ResourceObjectPattern(definition);
            SearchFilterType filterBean =
                    MiscUtil.requireNonNull(
                            patternBean.getFilter(),
                            () -> new SchemaException("No filter in resource object pattern"));
            ObjectFilter filter = PrismContext.get().getQueryConverter().parseFilter(filterBean, prismObjectDef);
            resourceObjectPattern.setFilter(filter);
            return resourceObjectPattern;
        }

        private void parseDelineation() throws ConfigurationException {
            ResourceObjectTypeDelineationType delineationBean = definitionBean.getObjectsSetDelineation();
            if (delineationBean != null) {
                configCheck(definitionBean.getBaseContext() == null,
                        "Base context cannot be set when delineation is configured. In %s", definition);
                configCheck(definitionBean.getSearchHierarchyScope() == null,
                        "Search hierarchy scope cannot be set when delineation is configured. In %s", definition);
                definition.setDelineation(
                        ResourceObjectTypeDelineation.of(delineationBean));
            } else {
                definition.setDelineation(
                        ResourceObjectTypeDelineation.of(
                                definitionBean.getBaseContext(),
                                definitionBean.getSearchHierarchyScope()));
            }
        }
    }
}
