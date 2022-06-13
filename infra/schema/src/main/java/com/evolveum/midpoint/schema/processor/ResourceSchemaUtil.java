/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Various utility methods related to resource schema handling, including sophisticated object definition lookup.
 */
public class ResourceSchemaUtil {

    /**
     * Looks up appropriate definition for "bulk operation" i.e. operation that is executed for given kind/intent/objectclass
     * on given resource.
     *
     * Currently, this is the same as {@link #findObjectDefinitionPrecisely(ResourceType, ShadowKindType, String, QName,
     * Collection, boolean)} with no additional aux OCs and unknown values not supported.
     *
     * *BEWARE* This method is really complex. If at all possible, please consider using specific lookup methods
     * present in {@link ResourceSchema}. Maybe we'll remove this method in the future.
     */
    public static @Nullable ResourceObjectDefinition findDefinitionForBulkOperation(
            @NotNull ResourceType resource,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName) throws SchemaException, ConfigurationException {
        return findObjectDefinitionPrecisely(resource, kind, intent, objectClassName, List.of(), false);
    }

    /**
     * Determines object type/class definition in a precise way. The decision is based on kind/intent/objectclass triple,
     * and later enriched by aux object class names.
     *
     * Basic schema is:
     *
     * . No kind, intent, nor object class: No definition (the `null` value) is returned.
     * . No kind (or kind=UNKNOWN, if allowed) -> the decision is based on the object class name:
     * .. if there is a type definition (for given object class name) marked as "default for object class", it is returned;
     * .. otherwise, the object class definition (refined, if there's any; raw otherwise) is returned.
     * . Kind is present
     * .. if intent is specified, then the type definition for given kind/intent is found (and its object class is
     * checked for equality with the specified one, if there's any);
     * .. if no intent is specified, then the some complex heuristics are employed (see
     * {@link ResourceObjectDefinitionResolver#findObjectDefinitionForKindInternal(ResourceSchema, ShadowKindType, QName)}:
     * ... if there is a type with given kind and "default for kind" set, it is returned;
     * ... if there is a type with given kind and intent = "default", it is returned;
     * ... if there is a single type matching given kind and object class
     *
     * *BEWARE* This method is really complex. If at all possible, please consider using specific lookup methods
     * present in {@link ResourceSchema}. Maybe we'll remove this method in the future.
     *
     * @param additionalAuxiliaryObjectClassNames Auxiliary object classes that should be "baked into" the definition,
     *                                            in addition to any classes specified in the resolved object type.
     * @param unknownValuesSupported If we allow the use of "unknown" kind or intent. This is usually the case when
     *                               we obtain the coordinates from an existing shadow. On the contrary, if we are looking up
     *                               according to user-provided data (e.g. query), we don't want to allow unknown values.
     *
     * @see ResourceObjectDefinitionResolver#findObjectDefinition(ResourceSchema, ShadowKindType, String, QName)
     * @see ResourceSchema#findDefinitionForObjectClass(QName)
     * @see ResourceObjectDefinitionResolver#findDefinitionForShadow(ResourceSchema, ShadowType)
     */
    @Contract("   _,  null,  null,  null, _, _ ->  null;" // K+I+OC null -> null
            + "   _,     _,     _, !null, _, _ -> !null;" // OC non-null -> non null (or fail)
            + "   _, !null, !null,     _, _, _ -> !null;" // K+I non-null -> non null (or fail)
            + "   _,  null, !null,     _, _, _ ->  fail") // K null, I non-null -> fail
    public static ResourceObjectDefinition findObjectDefinitionPrecisely(
            @NotNull ResourceType resource,
            @Nullable ShadowKindType kind,
            @Nullable String intent,
            @Nullable QName objectClassName,
            @NotNull Collection<QName> additionalAuxiliaryObjectClassNames,
            boolean unknownValuesSupported) throws SchemaException, ConfigurationException {
        if (!unknownValuesSupported) {
            argCheck(kind != ShadowKindType.UNKNOWN && !SchemaConstants.INTENT_UNKNOWN.equals(intent),
                    "Unknown kind/intent values are not supported here: %s/%s/%s", kind, intent, objectClassName);
        }

        if (kind == null && intent == null && objectClassName == null) {
            // Covering any object on the resource. We do not ever need the schema.
            // This can occur when an operation is explicitly requested with null kind/intent/class
            // (like e.g. live sync of all classes).
            return null;
        }

        return ResourceObjectDefinitionResolver.findObjectDefinitionPrecisely(
                resource,
                ResourceSchemaFactory.getCompleteSchemaRequired(resource),
                kind,
                intent,
                objectClassName,
                additionalAuxiliaryObjectClassNames);
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

    /** TEMPORARY */
    public static boolean isIgnored(ResourceAttributeDefinitionType attrDefBean) throws SchemaException {
        List<PropertyLimitationsType> limitations = attrDefBean.getLimitations();
        if (limitations == null) {
            return false;
        }
        // TODO review as part of MID-7929 resolution
        PropertyLimitationsType limitationsBean = MiscSchemaUtil.getLimitationsLabeled(limitations, LayerType.MODEL);
        if (limitationsBean == null) {
            return false;
        }
        if (limitationsBean.getProcessing() != null) {
            return limitationsBean.getProcessing() == ItemProcessingType.IGNORE;
        }
        return false;
    }
}
