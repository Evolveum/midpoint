/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.processor.ResourceObjectDefinitionResolver.findObjectDefinition;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Various utility methods related to resource schema handling, including sophisticated object definition lookup.
 */
public class ResourceSchemaUtil {

    /**
     * Looks up appropriate definition for "bulk operation" i.e. operation that is executed for given kind/intent/objectclass
     * on given resource.
     *
     * Currently, this is the same as {@link #findObjectDefinitionPrecisely(ResourceType, ShadowKindType, String, QName)}.
     *
     * *BEWARE* This method is really complex. If at all possible, please consider using specific lookup methods
     * present in {@link ResourceSchema}. Most probably we'll remove this method in the future.
     */
    public static @Nullable ResourceObjectDefinition findDefinitionForBulkOperation(
            @NotNull ResourceType resource,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) throws SchemaException, ConfigurationException {
        return findObjectDefinitionPrecisely(resource, kind, intent, objectClassName);
    }

    /**
     * Determines object type/class definition in a precise way. The decision is based on kind/intent/objectclass triple,
     * and later enriched by aux object class names.
     *
     * Basic schema is:
     *
     * . No kind, intent, nor object class is present: No definition (the `null` value) is returned.
     * . Kind is not present -> the decision is based on the object class name:
     * .. if there is a type definition (for given object class name) marked as "default for object class", it is returned;
     * .. otherwise, the object class definition (refined, if there's any; raw otherwise) is returned.
     * . Kind is present:
     * .. if intent is specified, then the type definition for given kind/intent is found (and its object class is
     * checked for equality with the specified one, if there's any);
     * .. if no intent is specified, then "default for kind" definition is looked for.
     *
     * !!! *BEWARE* This method is really complex. If at all possible, please consider using specific lookup methods
     * present in {@link ResourceSchema}. Most probably we'll remove this method in the future. !!!
     *
     * @see ResourceObjectDefinitionResolver#findObjectDefinition(ResourceSchema, ShadowKindType, String, QName)
     * @see ResourceSchema#findDefinitionForObjectClass(QName)
     */
    @Contract("   _,  null,  null,  null ->  null;" // K+I+OC null -> null
            + "   _,     _,     _, !null -> !null;" // OC non-null -> non null (or fail)
            + "   _, !null, !null,     _ -> !null;" // K+I non-null -> non null (or fail)
            + "   _,  null, !null,     _ ->  fail") // K null, I non-null -> fail
    public static ResourceObjectDefinition findObjectDefinitionPrecisely(
            @NotNull ResourceType resource,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) throws SchemaException, ConfigurationException {

        if (kind == null && intent == null && objectClassName == null) {
            return null; // Exotic case. We don't need the schema in this situation. See TestOpenDjNegative.test195.
        }

        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        return findObjectDefinitionPrecisely(resourceSchema, kind, intent, objectClassName, resource);
    }

    public static ResourceObjectDefinition findObjectDefinitionPrecisely(
            @NotNull ResourceSchema resourceSchema,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName,
            Object errorCtx) throws SchemaException, ConfigurationException {

        argCheck(kind != ShadowKindType.UNKNOWN && !SchemaConstants.INTENT_UNKNOWN.equals(intent),
                "Unknown kind/intent values are not supported here: %s/%s/%s", kind, intent, objectClassName);

        if (kind == null && intent == null && objectClassName == null) {
            return null;
        }
        ResourceObjectDefinition objectDefinition;
        if (kind != null) {
            objectDefinition =
                    MiscUtil.configNonNull(
                            findObjectDefinition(resourceSchema, kind, intent, null),
                            () -> String.format("No object type definition for %s/%s in %s", kind, intent, errorCtx));
            checkObjectClassCompatibility(kind, intent, objectClassName, objectDefinition);
        } else {
            if (intent != null) {
                throw new UnsupportedOperationException("Determination of object definition with kind being null "
                        + "and intent being non-null is not supported: null/" + intent + "/" + objectClassName);
            }
            objectDefinition =
                    MiscUtil.configNonNull(
                            resourceSchema.findDefinitionForObjectClass(Objects.requireNonNull(objectClassName)),
                            () -> String.format("No object type or class definition for object class %s in %s",
                                    objectClassName, errorCtx));
        }

        return addOwnAuxiliaryObjectClasses(objectDefinition, resourceSchema);
    }

    /**
     * Checks whether object definition (found using kind/intent) matches object class name - if the name is specified.
     *
     * TODO consider changing exception to {@link ConfigurationException}
     */
    static void checkObjectClassCompatibility(
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

    public static @NotNull ResourceObjectDefinition addOwnAuxiliaryObjectClasses(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ResourceSchema resourceSchema) throws SchemaException {
        return addAuxiliaryObjectClasses(
                objectDefinition,
                objectDefinition.getConfiguredAuxiliaryObjectClassNames(),
                resourceSchema);
    }

    /**
     * Adds resolved auxiliary object classes to a given resource object definition.
     * (Creating {@link CompositeObjectDefinition} if needed.)
     */
    static @NotNull ResourceObjectDefinition addAuxiliaryObjectClasses(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Collection<QName> auxiliaryObjectClassNames,
            @NotNull ResourceSchema resourceSchema) throws SchemaException {
        if (auxiliaryObjectClassNames.isEmpty()) {
            return objectDefinition;
        }
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions =
                new ArrayList<>(auxiliaryObjectClassNames.size());
        for (QName auxiliaryObjectClassName: auxiliaryObjectClassNames) {
            auxiliaryObjectClassDefinitions.add(
                    resourceSchema.findDefinitionForObjectClassRequired(auxiliaryObjectClassName));
        }
        return CompositeObjectDefinition.of(objectDefinition, auxiliaryObjectClassDefinitions);
    }

    /**
     * Checks if the definitions are compatible in the sense of {@link ResourceObjectAssociationType#getIntent()} (see XSD).
     *
     * Currently only object class name equality is checked. (Note that we assume these names are fully qualified,
     * so {@link Object#equals(Object)} comparison can be used.
     */
    static boolean areDefinitionsCompatible(Collection<ResourceObjectTypeDefinition> definitions) {
        Set<QName> objectClassNames = definitions.stream()
                .map(ResourceObjectDefinition::getObjectClassName)
                .collect(Collectors.toSet());
        return objectClassNames.size() <= 1;
    }
}
