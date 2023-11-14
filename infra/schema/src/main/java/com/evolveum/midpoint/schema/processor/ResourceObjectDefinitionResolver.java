/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.processor.ResourceSchemaUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Internal methods for the lookup of object type/class definitions. They are here to offload {@link ResourceSchema} from
 * tons of technical code.
 *
 * This code should be probably cleaned up in the future. The time available for the current wave of refactoring/cleanup
 * does not allow it.
 */
class ResourceObjectDefinitionResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectDefinitionResolver.class);

    /**
     * Returns object definition (type or class) matching given kind and intent, and object class.
     *
     * The object class parameter is used to:
     *
     * 1. verify that object type that matches given kind and intent is compatible with (currently: equal to) the object class;
     * 2. provide a complementary means to select a type when intent is not specified.
     *
     * There is a special treatment for:
     *
     * - intent being null: see {@link #findObjectDefinitionForKindInternal(ResourceSchema, ShadowKindType, QName)};
     * - (a hack) for ACCOUNT/default: see {@link #findDefaultAccountObjectClassInternal(ResourceSchema)} [this may be removed later]
     *
     * The "unknown" values for kind/intent are not supported. The client must know if these
     * are even applicable, or (if they are) how they should be interpreted.
     *
     * @throws IllegalStateException if there are more matching definitions for known kind/intent
     * (we should have checked this when parsing)
     * @throws IllegalArgumentException if "unknown" values are present; or if only the kind is specified, and
     * there's more than one applicable definition for the kind (TODO or should be that {@link ConfigurationException}?)
     */
    static @Nullable ResourceObjectDefinition findObjectDefinition(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) {

        argCheck(kind != ShadowKindType.UNKNOWN,
                "Unknown kind is not supported here: %s/%s in %s", kind, intent, schema);
        argCheck(!SchemaConstants.INTENT_UNKNOWN.equals(intent),
                "Unknown intent is not supported here: %s/%s in %s", kind, intent, schema);

        var found = findObjectDefinitionInternal(schema, kind, intent, objectClassName);
        if (found != null) {
            return found;
        }

        // BRUTAL HACK to allow finding ACCOUNT/default or ACCOUNT/null definitions, see e.g. TestAssignmentErrors
        if (kind == ShadowKindType.ACCOUNT
                && (SchemaConstants.INTENT_DEFAULT.equals(intent) || intent == null)) {
            return findDefaultAccountObjectClassInternal(schema);
        } else {
            return null;
        }
    }

    /**
     * The whole logic of finding the definition for kind/intent (+OC), except for ACCOUNT/default hack.
     */
    private static @Nullable ResourceObjectDefinition findObjectDefinitionInternal(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) {
        if (intent == null) {
            return findObjectDefinitionForKindInternal(schema, kind, objectClassName);
        } else {
            return findObjectDefinitionForKindAndIntentInternal(schema, kind, intent, objectClassName);
        }
    }

    /**
     * Most direct lookup - by kind and intent (must be at most one), then checking object class, if it's present.
     */
    private static @Nullable ResourceObjectTypeDefinition findObjectDefinitionForKindAndIntentInternal(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable QName objectClassName) {
        var matching = MiscUtil.extractSingleton(
                schema.getObjectTypeDefinitions().stream()
                        .filter(def -> def.matches(kind, intent))
                        .collect(Collectors.toList()),
                () -> new IllegalStateException(
                        "More than one non-default definition for " + kind + "/" + intent + " in " + schema));
        if (matching != null) {
            checkObjectClassCompatibility(kind, null, objectClassName, matching);
            return matching;
        } else {
            return null;
        }
    }

    /**
     * Determines object definition (type or class level) when only kind is specified (i.e. intent is null).
     * Optional filtering using object class name is available.
     *
     * @throws IllegalArgumentException If the default definition for given kind cannot be determined.
     * This is not a {@link ConfigurationException} because the configuration itself may be legal.
     * (Or should we throw that one?)
     */
    private static @Nullable ResourceObjectDefinition findObjectDefinitionForKindInternal(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @Nullable QName objectClassName) {

        ResourceObjectTypeDefinition defaultDefinition = findDefaultObjectTypeDefinitionInternal(schema, kind, objectClassName);
        if (defaultDefinition != null) {
            return defaultDefinition;
        } else if (objectClassName != null) {
            return schema.findObjectClassDefinition(objectClassName);
        } else {
            return null;
        }
    }

    /**
     * Returns the default object class definition for accounts, if defined.
     */
    private @Nullable static ResourceObjectClassDefinition findDefaultAccountObjectClassInternal(
            @NotNull ResourceSchema schema) {
        List<ResourceObjectClassDefinition> defaultDefinitions = schema.getObjectClassDefinitions().stream()
                .filter(ResourceObjectClassDefinition::isDefaultAccountDefinition)
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                defaultDefinitions,
                () -> new IllegalStateException("More than one definition marked as 'default account definition': "
                        + defaultDefinitions + " in " + schema));
    }

    /**
     * Returns the default definition for given kind. The `defaultForKind` flag must be explicitly set.
     * Object class must match, if it's specified.
     */
    private static @Nullable ResourceObjectTypeDefinition findDefaultObjectTypeDefinitionInternal(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @Nullable QName objectClassName) {
        return MiscUtil.extractSingleton(
                schema.getObjectTypeDefinitions().stream()
                        .filter(def ->
                                def.matchesKind(kind)
                                        && def.matchesObjectClassName(objectClassName)
                                        && def.isDefaultForKind())
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("Multiple default definitions for " + kind + " in " + schema));
    }

    /**
     * Returns default object type definition for given object class name (if there's any).
     *
     * We intentionally do not return an object type definition that is not marked as default-for-object-class,
     * even if it's the only one defined. (We assume that a user may wish to _not_ provide a default type definition
     * in some situations.)
     */
    @Nullable
    static ResourceObjectTypeDefinition findDefaultObjectTypeDefinitionForObjectClass(
            @NotNull ResourceSchema schema, @NotNull QName name) {
        return MiscUtil.extractSingleton(
                schema.getObjectTypeDefinitions().stream()
                        .filter(def ->
                                QNameUtil.match(def.getObjectClassName(), name) && def.isDefaultForObjectClass())
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("More than one default type definition of object class " + name + " in " + schema));
    }

    /**
     * See {@link ResourceSchema#findDefinitionForShadow(ShadowType)} for the description.
     */
    static @Nullable ResourceObjectDefinition findDefinitionForShadow(
            @NotNull ResourceSchema schema, @NotNull ShadowType shadow, @NotNull Collection<QName> additionalAuxObjectClassNames)
            throws SchemaException {
        ResourceObjectDefinition structuralDefinition = findStructuralDefinitionForShadow(schema, shadow);
        if (structuralDefinition != null) {
            return addAuxiliaryObjectClasses(
                    structuralDefinition,
                    MiscUtil.union(
                            structuralDefinition.getConfiguredAuxiliaryObjectClassNames(),
                            shadow.getAuxiliaryObjectClass(),
                            additionalAuxObjectClassNames),
                    schema);
        } else {
            return null;
        }
    }

    private static @Nullable ResourceObjectDefinition findStructuralDefinitionForShadow(
            @NotNull ResourceSchema schema, @NotNull ShadowType shadow) {
        QName shadowObjectClassName = shadow.getObjectClass();
        ResourceObjectTypeIdentification shadowTypeId = ResourceObjectTypeIdentification.createIfKnown(shadow);
        if (shadowTypeId != null) {
            ResourceObjectTypeDefinition typeDef = schema.getObjectTypeDefinition(shadowTypeId);
            if (typeDef != null) {
                if (!QNameUtil.match(typeDef.getObjectClassName(), shadowObjectClassName)) {
                    LOGGER.warn("Shadow {} with object class name ({}) not compatible with the classification ({})",
                            shadow, shadowObjectClassName, typeDef);
                }
                return typeDef;
            }
        }
        if (shadowObjectClassName != null) {
            return schema.findDefinitionForObjectClass(shadowObjectClassName);
        } else {
            LOGGER.warn("Shadow {} without object class", shadow);
            return null;
        }
    }

    static ResourceObjectDefinition findForConstruction(
            @NotNull ResourceSchema schema,
            @NotNull ConstructionType construction) {
        ShadowKindType kind = Objects.requireNonNullElse(construction.getKind(), ShadowKindType.ACCOUNT);
        String intent = construction.getIntent();
        if (ShadowUtil.isKnown(intent)) {
            return schema.findObjectDefinition(kind, intent);
        } else {
            return schema.findDefaultDefinitionForKind(kind);
        }
    }
}
