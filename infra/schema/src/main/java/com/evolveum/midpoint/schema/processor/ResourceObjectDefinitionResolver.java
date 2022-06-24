/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.google.common.base.Strings;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
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
     * See the code for the algorithm description. The behavior is described also in
     * {@link ResourceSchemaUtil#findObjectDefinitionPrecisely(ResourceType, ShadowKindType, String, QName, Collection, boolean)} javadoc.
     *
     * @throws IllegalArgumentException If the default definition for given kind cannot be determined.
     * This is not a {@link ConfigurationException} because the configuration itself may be legal.
     * (Or should we throw that one?)
     */
    private static @Nullable ResourceObjectDefinition findObjectDefinitionForKindInternal(
            @NotNull ResourceSchema schema,
            @NotNull ShadowKindType kind,
            @Nullable QName objectClassName) {

        // #1: Is there a type definition that is declared as default for its kind?
        ResourceObjectTypeDefinition markedAsDefault = findDefaultObjectTypeDefinitionInternal(schema, kind, objectClassName);
        if (markedAsDefault != null) {
            return markedAsDefault;
        }

        // #2: Is there a type definition with the intent="default"? We are intentionally not using
        // the object class name in the call, to avoid exceptions if there is a default but with non-matching OC name.
        ResourceObjectDefinition intentDefault = findObjectDefinitionForKindAndIntentInternal(
                schema, kind, SchemaConstants.INTENT_DEFAULT, null);
        if (intentDefault != null) {
            return intentDefault;
            // OC compatibility is checked in upper layers e.g. ResourceObjectDefinitionResolver#findObjectDefinitionPrecisely.
        }

        // #3: Is there a single definition with given kind + OC (if specified)?
        // TODO is this correct?!
        List<? extends ResourceObjectTypeDefinition> matchingKindAndObjectClass =
                schema.getObjectTypeDefinitions(kind).stream()
                        .filter(def -> def.matchesObjectClassName(objectClassName))
                        .collect(Collectors.toList());

        if (matchingKindAndObjectClass.size() == 1) {
            return matchingKindAndObjectClass.get(0);
        } else if (matchingKindAndObjectClass.size() > 1 && objectClassName == null) {
            // Out of luck. We have nothing more to try. There are certainly some definitions,
            // but we just have no chance to decide among them.
            throw new IllegalArgumentException("Couldn't determine the default definition for kind " + kind
                    + ", because there are " + matchingKindAndObjectClass.size() + " candidates");
        }

        // #4: Fallback: if an object class is known, let us look up its raw definition.
        if (objectClassName != null) {
            return schema.findObjectClassDefinition(objectClassName);
        }

        // #5: Sorry.
        return null;
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
     * Returns appropriate {@link ResourceObjectDefinition} for given shadow. We are not too strict here.
     * Unknown kind/intent values are ignored (treated like null). Intent without kind is ignored.
     *
     * Takes auxiliary object classes defined in the shadow into account.
     *
     * Note: we could be even more relaxed (in the future):
     *
     * 1. Currently the consistency between kind, intent, and OC is checked. We could avoid this.
     * 2. The {@link #findObjectDefinition(ResourceSchema, ShadowKindType, String, QName)} method used throws an exception
     * when it cannot decide among various definitions for given kind (when intent and OC is null). We could be more
     * permissive and return any of them.
     */
    static @Nullable ResourceObjectDefinition findDefinitionForShadow(
            @NotNull ResourceSchema schema, @NotNull ShadowType shadow) {

        QName objectClassName = shadow.getObjectClass();
        ShadowKindType kind;
        String intent;

        // Ignoring "UNKNOWN" values
        if (ShadowUtil.isNotKnown(shadow.getKind())) {
            kind = null;
            intent = null;
        } else {
            kind = shadow.getKind();
            if (ShadowUtil.isNotKnown(shadow.getIntent())) {
                intent = null;
            } else {
                intent = shadow.getIntent();
            }
        }

        ResourceObjectDefinition structuralDefinition;

        if (kind != null) {
            if (intent == null) {
                // TODO should this be really a warning or a lower-level message suffices?
                // FIXME no partial classification should be allowed!
                LOGGER.warn("Partially-classified {}. Looking for default type of {}, if present", shadow, kind);
            }
            structuralDefinition = findObjectDefinition(schema, kind, intent, objectClassName);
        } else if (objectClassName != null) {
            // TODO or findObjectClassDefinition?
            structuralDefinition = schema.findDefinitionForObjectClass(objectClassName);
        } else {
            structuralDefinition = null;
        }

        if (structuralDefinition != null) {
            return addAuxiliaryObjectClasses(structuralDefinition, shadow.getAuxiliaryObjectClass(), schema);
        } else {
            return null;
        }
    }

    /**
     * See {@link ResourceSchemaUtil#findObjectDefinitionPrecisely(ResourceType, ShadowKindType, String, QName, Collection, boolean)}
     * for the description.
     */
    static ResourceObjectDefinition findObjectDefinitionPrecisely(
            @NotNull ResourceType resource,
            @NotNull ResourceSchema resourceSchema,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName,
            @NotNull Collection<QName> additionalAuxiliaryObjectClassNames) throws ConfigurationException {

        if (kind == ShadowKindType.UNKNOWN && objectClassName == null) {
            throw new IllegalStateException("Not possible to determine object definition with kind being \"unknown\""
                    + " and no object class name present");
        }

        disallowIntentWithoutKind(kind, intent, objectClassName);

        boolean specificKindPresent = kind != null && kind != ShadowKindType.UNKNOWN;
        boolean specificIntentPresent = intent != null && !SchemaConstants.INTENT_UNKNOWN.equals(intent);
        String specificIntent = specificIntentPresent ? intent : null;

        ResourceObjectDefinition objectDefinition;
        if (specificKindPresent) {
            objectDefinition = findObjectDefinition(resourceSchema, kind, specificIntent, objectClassName);
            if (objectDefinition == null) {
                throw new ConfigurationException("No object type definition for " + kind + "/" + specificIntent
                        + (objectClassName != null ? " (object class " + objectClassName.getLocalPart() + ")" : "")
                        + " in " + resource
                        + (SchemaConstants.INTENT_UNKNOWN.equals(intent) ? " (looking for unknown intent)" : ""));
            }
            checkObjectClassCompatibility(kind, intent, objectClassName, objectDefinition);
        } else {
            // Kind is null or unknown, so the object class name must be specified
            objectDefinition = resourceSchema.findDefinitionForObjectClass(Objects.requireNonNull(objectClassName));
            if (objectDefinition == null) {
                throw new ConfigurationException("No object type or class definition for object class: " + objectClassName
                        + " in " + resource);
            }
        }

        Collection<QName> allAuxiliaryClassNames = MiscUtil.union(
                additionalAuxiliaryObjectClassNames,
                objectDefinition.getConfiguredAuxiliaryObjectClassNames());

        return addAuxiliaryObjectClasses(objectDefinition, allAuxiliaryClassNames, resourceSchema);
    }

    /**
     * Adds resolved auxiliary object classes to a given resource object definition.
     * (Creating {@link CompositeObjectDefinition} if needed.)
     */
    private static @NotNull ResourceObjectDefinition addAuxiliaryObjectClasses(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<QName> auxiliaryObjectClassNames,
            @NotNull ResourceSchema resourceSchema) {
        if (auxiliaryObjectClassNames.isEmpty()) {
            return objectDefinition;
        }
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions =
                new ArrayList<>(auxiliaryObjectClassNames.size());
        for (QName auxiliaryObjectClassName: auxiliaryObjectClassNames) {
            auxiliaryObjectClassDefinitions.add(
                    resourceSchema.findDefinitionForObjectClassRequired(auxiliaryObjectClassName));
        }
        return new CompositeObjectDefinitionImpl(objectDefinition, auxiliaryObjectClassDefinitions);
    }

    /**
     * Check that we do not have intent without kind.
     *
     * This is a safety check, to not return definition in poorly defined (ambiguous) cases.
     * We may remove this check in the future, if needed.
     */
    private static void disallowIntentWithoutKind(ShadowKindType kind, String intent, QName objectClassName) {
        if (kind == null) {
            if (intent != null) {
                throw new UnsupportedOperationException("Determination of object definition with kind being null "
                        + "and intent being non-null is not supported: null/" + intent + "/" + objectClassName);
            }
        } else if (kind == ShadowKindType.UNKNOWN) {
            if (intent != null && !SchemaConstants.INTENT_UNKNOWN.equals(intent)) {
                throw new UnsupportedOperationException("Determination of object definition with kind being \"unknown\""
                        + " and intent being known is not supported: " + kind + "/" + intent + "/" + objectClassName);
            }
        }
    }

    /**
     * Checks whether object definition (found using kind/intent) matches object class name - if the name is specified.
     *
     * TODO consider changing exception to {@link ConfigurationException}
     */
    private static void checkObjectClassCompatibility(
            @NotNull ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName,
            @Nullable ResourceObjectDefinition objectDefinition) {
        if (objectDefinition != null
                && objectClassName != null
                && !QNameUtil.match(objectClassName, objectDefinition.getObjectClassName())) {
            throw new IllegalStateException(Strings.lenientFormat(
                    "Specified kind/intent (%s/%s) point to object class %s which differs from the requested one: %s",
                    kind, intent, objectDefinition.getObjectClassName(), objectClassName));
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

    static @Nullable ResourceObjectTypeDefinition findDefaultOrAnyObjectTypeDefinition(@NotNull ResourceSchema schema, @NotNull ShadowKindType kind) {
        ResourceObjectTypeDefinition defaultDefinition = findDefaultObjectTypeDefinitionInternal(schema, kind, null);
        if (defaultDefinition != null) {
            return defaultDefinition;
        } else {
            return schema.getObjectTypeDefinitions().stream()
                    .filter(def -> def.matchesKind(kind))
                    .findFirst()
                    .orElse(null);
        }
    }
}
