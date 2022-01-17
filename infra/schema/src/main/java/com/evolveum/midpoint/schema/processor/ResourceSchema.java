/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * A schema covering the whole resource.
 *
 * It contains both "raw" object class definition and "refined" object type definitions.
 *
 * - Raw definitions are represented by {@link ResourceObjectClassDefinition} objects
 * and are obtained directly from the connector.
 * - Refined definitions (represented by {@link ResourceObjectTypeDefinition}) are derived from the raw ones
 * by merging them with information in `schemaHandling` part of the resource definition.
 *
 * This interface contains a lot of methods that try to find object type/class definition matching
 * criteria. Similar methods (more comprehensive) are in {@link ResourceObjectDefinitionResolver} class.
 *
 * NOTE: Some of the names will probably change soon.
 *
 * NOTE: There can be schemas that contain no refined definitions. Either the resource definition
 * contains no `schemaHandling`, or we work at lower layers (e.g. when fetching and parsing the schema
 * in ConnId connector).
 *
 * @author semancik
 */
public interface ResourceSchema extends PrismSchema, Cloneable, LayeredDefinition {

    /** Returns definitions for all the object classes. */
    default @NotNull Collection<ResourceObjectClassDefinition> getObjectClassDefinitions() {
        return getDefinitions(ResourceObjectClassDefinition.class);
    }

    /** Returns definitions for all the object types. */
    default @NotNull Collection<ResourceObjectTypeDefinition> getObjectTypeDefinitions() {
        return getDefinitions(ResourceObjectTypeDefinition.class);
    }

    /** Returns definitions for all types with given kind. (If null, returns all types.) */
    default @NotNull List<? extends ResourceObjectTypeDefinition> getObjectTypeDefinitions(@Nullable ShadowKindType kind) {
        return getObjectTypeDefinitions().stream()
                .filter(def -> def.matchesKind(kind))
                .collect(Collectors.toList());
    }

    /**
     * Returns object definition (type or class) matching given kind and intent, and object class.
     *
     * The object class is used to:
     *
     * 1. verify that object type that matches given kind and intent is compatible with (currently: equal to) the object class;
     * 2. provide a complementary means to select a type when intent is not specified.
     *
     * There is a special treatment for:
     *
     * - intent being null: see {@link #findObjectDefinitionForKind(ShadowKindType, QName)};
     * - (a hack) for ACCOUNT/default: see {@link #findDefaultAccountObjectClass()} [this may be removed later]
     *
     * The "unknown" values for kind/intent are not supported. The client must known if these
     * are even applicable, or (if they are) how they should be interpreted.
     *
     * @throws IllegalStateException if there are more matching definitions for known kind/intent
     * (we should have checked this when parsing)
     * @throws IllegalArgumentException if "unknown" values are present; or if only the kind is specified, and
     * there's more than one applicable definition for the kind (TODO or should be that {@link ConfigurationException}?)
     */
    default @Nullable ResourceObjectDefinition findObjectDefinition(
            @NotNull ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) {

        argCheck(kind != ShadowKindType.UNKNOWN,
                "Unknown kind is not supported here: %s/%s in %s", kind, intent, this);
        argCheck(!SchemaConstants.INTENT_UNKNOWN.equals(intent),
                "Unknown intent is not supported here: %s/%s in %s", kind, intent, this);

        var found = findObjectDefinitionInternal(kind, intent, objectClassName);
        if (found != null) {
            return found;
        }

        // BRUTAL HACK to allow finding ACCOUNT/default definitions, see e.g. TestAssignmentErrors
        if (kind == ShadowKindType.ACCOUNT && SchemaConstants.INTENT_DEFAULT.equals(intent)) {
            return findDefaultAccountObjectClass();
        } else {
            return null;
        }
    }

    /**
     * The whole logic of finding the definition for kind/intent (+OC), except for ACCOUNT/default hack.
     */
    private @Nullable ResourceObjectDefinition findObjectDefinitionInternal(
            @NotNull ShadowKindType kind, @Nullable String intent, @Nullable QName objectClassName) {
        if (intent == null) {
            return findObjectDefinitionForKind(kind, objectClassName);
        } else {
            var matching = MiscUtil.extractSingleton(
                    getObjectTypeDefinitions().stream()
                            .filter(def -> def.matches(kind, intent))
                            .collect(Collectors.toList()),
                    () -> new IllegalStateException(
                            "More than one non-default definition for " + kind + "/" + intent + " in " + this));
            if (matching != null) {
                ResourceObjectDefinitionResolver.checkObjectClassCompatibility(kind, null, objectClassName, matching);
                return matching;
            } else {
                return null;
            }
        }
    }

    /**
     * Like {@link #findObjectDefinition(ShadowKindType, String, QName)} but without object class name.
     */
    default @Nullable ResourceObjectDefinition findObjectDefinition(
            @NotNull ShadowKindType kind, @Nullable String intent) {
        return findObjectDefinition(kind, intent, null);
    }

    /**
     * As {@link #findObjectDefinition(ShadowKindType, String)} but throws {@link NullPointerException} if a definition
     * cannot be found (in a normal way). All other exceptions from the above method apply.
     */
    default @NotNull ResourceObjectDefinition findObjectDefinitionRequired(
            @NotNull ShadowKindType kind, @Nullable String intent) {
        return Objects.requireNonNull(
                findObjectDefinition(kind, intent),
                () -> "No object type definition for " + kind + "/" + intent + " in " + this);
    }

    /**
     * Returns object _type_ definition for given kind and intent.
     *
     * Not used in standard cases. Consider {@link #findObjectDefinitionRequired(ShadowKindType, String)} instead.
     */
    @VisibleForTesting
    default @NotNull ResourceObjectTypeDefinition findObjectTypeDefinitionRequired(
            @NotNull ShadowKindType kind, @Nullable String intent) {
        ResourceObjectDefinition definition = findObjectDefinition(kind, intent);
        stateCheck(definition != null,
                "No definition for %s/%s could be found", kind, intent);
        stateCheck(definition instanceof ResourceObjectTypeDefinition,
                "No type definition for %s/%s could be found; only %s", kind, intent, definition);
        return (ResourceObjectTypeDefinition) definition;
    }

    /**
     * Determines object definition (type or class level) when only kind is specified (i.e. intent is null).
     * Optional filtering using object class name is available.
     *
     * See the code for the algorithm description.
     *
     * @throws IllegalArgumentException If the default definition for given kind cannot be determined.
     * This is not a {@link ConfigurationException} because the configuration itself may be legal.
     * (Or should we throw that one?)
     */
    private @Nullable ResourceObjectDefinition findObjectDefinitionForKind(
            @NotNull ShadowKindType kind, @Nullable QName objectClassName) {

        // #1: Is there a type definition that is declared as default for its kind?
        ResourceObjectTypeDefinition markedAsDefault = findDefaultObjectTypeDefinition(kind, objectClassName);
        if (markedAsDefault != null) {
            return markedAsDefault;
        }

        // #2: Is there a type definition with the intent="default"? We are intentionally not using
        // the object class name in the call, to avoid exceptions if there is a default but with non-matching OC name.
        ResourceObjectDefinition intentDefault = findObjectDefinitionInternal(
                kind, SchemaConstants.INTENT_DEFAULT, null);
        if (intentDefault != null) {
            return intentDefault;
        }

        // #3: Is there a single definition with given kind + OC (if specified)?
        List<? extends ResourceObjectTypeDefinition> matchingKindAndObjectClass =
                getObjectTypeDefinitions(kind).stream()
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
            return findObjectClassDefinition(objectClassName);
        }

        // #5: If the kind is "account", and connector designated an object class as "default for account"
        // (currently __ACCOUNT__ for ConnId), then let's use that.
        if (kind == ShadowKindType.ACCOUNT) {
            return findDefaultAccountObjectClass();
        }

        // #6: Sorry.
        return null;
    }

    /**
     * Returns the default object class definition for accounts, if defined.
     */
    private @Nullable ResourceObjectClassDefinition findDefaultAccountObjectClass() {
        List<ResourceObjectClassDefinition> defaultDefinitions = getObjectClassDefinitions().stream()
                .filter(ResourceObjectClassDefinition::isDefaultAccountDefinition)
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                defaultDefinitions,
                () -> new IllegalStateException("More than one definition marked as 'default account definition': "
                        + defaultDefinitions + " in " + this));
    }

    /**
     * Returns object type definition matching given kind and one of the intents.
     * (Or, if no intents are provided, default type for given kind is returned.
     * We are not very eager here - by default we mean just the flag "default" being set.
     * This is in contrast with e.g. {@link ResourceSchema#findObjectDefinitionForKind(ShadowKindType, QName)}.
     * But here we won't go into such levels. This is quite a specialized method.)
     *
     * The matching types must be co share at least the object class name. This is checked by this method.
     * However, in practice they must share much more, as described in the description for
     * {@link ResourceObjectAssociationType#getIntent()} (see XSD).
     *
     * To be used in special circumstances.
     *
     */
    default ResourceObjectTypeDefinition findObjectTypeDefinitionForAnyMatchingIntent(
            @NotNull ShadowKindType kind, @NotNull Collection<String> intents)
            throws SchemaException {
        Collection<ResourceObjectTypeDefinition> matching = getObjectTypeDefinitions().stream()
                .filter(def -> def.matchesKind(kind) && matchesAnyIntent(def, intents))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            return null;
        } else if (ResourceSchemaUtil.areDefinitionsCompatible(matching)) {
            return matching.iterator().next();
        } else {
            throw new SchemaException("Incompatible definitions found for kind " + kind + ", intents: " + intents + ": " + matching);
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
     * Returns the default definition for given kind. The `default` flag must be explicitly set.
     * Object class must match, if it's specified.
     */
    default @Nullable ResourceObjectTypeDefinition findDefaultObjectTypeDefinition(
            @NotNull ShadowKindType kind, @Nullable QName objectClassName) {
        return MiscUtil.extractSingleton(
                getObjectTypeDefinitions().stream()
                        .filter(def ->
                                def.matchesKind(kind)
                                        && def.matchesObjectClassName(objectClassName)
                                        && def.isDefaultForKind())
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("Multiple default definitions for " + kind + " in " + this));
    }

    /**
     * Returns the definition for given kind. If default one is present, it is returned.
     * Otherwise, any definition is returned.
     *
     * This is similar to pre-4.5 behavior observed when looking for "refined definitions".
     * (Although not exactly the same: now we require type definition, whereas in 4.4 and before
     * we could return a definition even if no schemaHandling was present.)
     *
     * TODO Determine if this method is really needed.
     */
    default @Nullable ResourceObjectTypeDefinition findDefaultOrAnyObjectTypeDefinition(@NotNull ShadowKindType kind) {
        ResourceObjectTypeDefinition defaultDefinition = findDefaultObjectTypeDefinition(kind, null);
        if (defaultDefinition != null) {
            return defaultDefinition;
        } else {
            return getObjectTypeDefinitions().stream()
                    .filter(def -> def.matchesKind(kind))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Returns a definition for a given object class:
     *
     * - if there's a "default for class" type defined, it is returned (this is a kind of pre-4.5 behavior)
     * - otherwise, the object class definition is returned (if there's any)
     */
    default @Nullable ResourceObjectDefinition findDefinitionForObjectClass(@NotNull QName name) {
        ResourceObjectTypeDefinition defaultTypeDef = findDefaultObjectTypeDefinitionForObjectClass(name);
        if (defaultTypeDef != null) {
            return defaultTypeDef;
        } else {
            return findObjectClassDefinition(name);
        }
    }

    /**
     * As {@link #findDefinitionForObjectClass(QName)} but throws an exception if there's no suitable definition.
     *
     * Currently it's {@link NullPointerException}. TODO reconsider what kind of exception should we throw
     */
    default @NotNull ResourceObjectDefinition findDefinitionForObjectClassRequired(@NotNull QName name) {
        return java.util.Objects.requireNonNull(
                findDefinitionForObjectClass(name),
                () -> "No definition for object class " + name + " in " + this);
    }

    /**
     * Returns default object type definition for given object class name (if there's any).
     *
     * We intentionally do not return object type definition that is not marked as default-for-object-class,
     * even if it's the only one defined. (We assume that a user may wish to _not_ provide a default type definition
     * in some situations.)
     */
    private @Nullable ResourceObjectTypeDefinition findDefaultObjectTypeDefinitionForObjectClass(@NotNull QName name) {
        return MiscUtil.extractSingleton(
                getObjectTypeDefinitions().stream()
                        .filter(def ->
                                QNameUtil.match(def.getObjectClassName(), name) && def.isDefaultForObjectClass())
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("More than one default type definition of object class " + name + " in " + this));
    }

    /**
     * Returns object class definition for a given object class name.
     */
    default @Nullable ResourceObjectClassDefinition findObjectClassDefinition(@NotNull QName name) {
        return MiscUtil.extractSingleton(
                getObjectClassDefinitions().stream()
                        .filter(def -> QNameUtil.match(def.getTypeName(), name))
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("More than one definition of object class " + name + " in " + this));
    }

    /**
     * The same as {@link #findObjectClassDefinition(QName)} but throws an exception if there's no such definition.
     */
    default @NotNull ResourceObjectClassDefinition findObjectClassDefinitionRequired(@NotNull QName name)
            throws SchemaException {
        return MiscUtil.requireNonNull(
                findObjectClassDefinition(name),
                () -> "Object class " + name + " not found in " + this);
    }

    /**
     * Returns names of all object classes mentioned in the "raw" resource definition.
     */
    default @NotNull Collection<QName> getObjectClassNames() {
        return getObjectClassDefinitions().stream()
                .map(Definition::getTypeName)
                .collect(Collectors.toSet());
    }

    /** Returns an interface to mutate this schema. */
    MutableResourceSchema toMutable();

    /** Returns a representation of the schema for given layer. */
    ResourceSchema forLayer(LayerType layer);

    @Override
    default @NotNull String getNamespace() {
        return MidPointConstants.NS_RI;
    }

    /** TODO description */
    void validate(PrismObject<ResourceType> resource) throws SchemaException;

    /** TODO description */
    default @NotNull Collection<String> getIntentsForKind(ShadowKindType kind) {
        return getObjectTypeDefinitions(kind).stream()
                .map(ResourceObjectTypeDefinition::getIntent)
                .collect(Collectors.toSet());
    }

    ResourceSchema clone();

    /**
     * Returns true if the schema contains no "refined" (type) definitions.
     */
    default boolean isRaw() {
        return getObjectTypeDefinitions().isEmpty();
    }
}
