/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

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
    public @Nullable ResourceSchema parse() throws SchemaException {
        ResourceSchemaImpl rawSchema = (ResourceSchemaImpl) ResourceSchemaFactory.getRawSchema(resource);
        if (rawSchema == null) {
            return null;
        }

        if (resource.getSchemaHandling() == null) {
            return rawSchema;
        }

        completeSchema = rawSchema.clone();

        createEmptyObjectTypeDefinitions();

        // In theory, this could be done alongside creation of empty object type definitions.
        resolveAuxiliaryObjectClassNames();

        // Associations refer to object types, so we have to parse them after empty type definitions are created.
        parseAssociations();

        // We can parse attributes only after we have all the object class info parsed (including auxiliary object classes)
        parseAttributes();

        completeSchema.freeze();

        return completeSchema;
    }

    private void createEmptyObjectTypeDefinitions() throws SchemaException {
        for (ResourceObjectTypeDefinitionType definitionBean : resource.getSchemaHandling().getObjectType()) {
            completeSchema.add(createEmptyObjectTypeDefinition(definitionBean));
        }
        checkForMultipleDefaults();
    }

    private ResourceObjectTypeDefinition createEmptyObjectTypeDefinition(@NotNull ResourceObjectTypeDefinitionType definitionBean)
            throws SchemaException {

        ShadowKindType kind = Objects.requireNonNullElse(definitionBean.getKind(), ShadowKindType.ACCOUNT);
        String intent = Objects.requireNonNullElse(definitionBean.getIntent(), SchemaConstants.INTENT_DEFAULT);
        QName objectClassName = MiscUtil.requireNonNull(
                definitionBean.getObjectClass(),
                () -> "Definition of " + kind + "/" + intent + " does not have objectclass, in " + contextDescription);

        return new ResourceObjectTypeDefinitionImpl(
                kind,
                intent,
                completeSchema.findObjectClassDefinitionRequired(objectClassName),
                CloneUtil.toImmutable(definitionBean),
                resource.getOid());
    }

    /**
     * Checks that there is at most single default for any kind.
     *
     * @throws SchemaException If there's a problem. Note that during run time, we throw {@link IllegalStateException}
     * in these cases (as we assume this check was already done).
     */
    private void checkForMultipleDefaults() throws SchemaException {
        for (ShadowKindType kind : ShadowKindType.values()) {
            var defaults = completeSchema.getObjectTypeDefinitions().stream()
                    .filter(def -> def.matchesKind(kind) && def.isDefaultForKind())
                    .collect(Collectors.toList());
            schemaCheck(defaults.size() <= 1, "More than one default %s definition in %s: %s",
                    kind, contextDescription, defaults);
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

    /**
     * Creates and updates {@link ResourceObjectTypeDefinition} from
     *
     * - "raw" {@link ResourceObjectClassDefinition},
     * - refinements defined in {@link ResourceObjectTypeDefinitionType} (`schemaHandling`)
     *
     * Note: this class is instantiated multiple times during parsing of a schema. It should not be
     * a problem, as it is quite lightweight.
     */
    class ResourceObjectTypeDefinitionParser {

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
                        MiscUtil.requireNonNull(
                                completeSchema.findObjectTypeDefinitionForAnyMatchingIntent(
                                        associationDef.getKind(),
                                        associationDef.getIntents()),
                                () -> "No object type definition for association " + associationDef + " in " + contextDescription));
                definition.addAssociationDefinition(associationDef);
            }
        }

        /**
         * Fills-in attribute definitions in `typeDef` by traversing all "raw" attributes defined in the structural
         * object class and all the auxiliary object classes.
         *
         * Initializes identifier names and protected objects patterns.
         */
        private void parseAttributes() throws SchemaException {

            parseAttributesFromObjectClass(definition.getObjectClassDefinition(), false);
            for (ResourceObjectDefinition auxDefinition : definition.getAuxiliaryDefinitions()) {
                parseAttributesFromObjectClass(auxDefinition.getObjectClassDefinition(), true);
            }

            assertNoOtherAttributes();

            setupIdentifiers();
            parseProtected();
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
    }
}
