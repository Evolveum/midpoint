/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Methods for determining object definition for given kind/intent/object class. They build upon basic methods
 * for definition lookup in {@link ResourceSchema}.
 *
 * There are two basic methods:
 *
 * - {@link #getDefinitionForShadow(ResourceSchema, ShadowType)} does an approximate lookup. Providing some information
 * is more important than absolute precision.
 *
 * - {@link #getObjectDefinitionPrecisely(ResourceType, ShadowKindType, String, QName, Collection, boolean)} is the ultimate
 * method for determining the definition as precisely as possible.
 *
 * The particular cases of definition lookup are handled by {@link ResourceSchema} itself. Other differences between
 * these two classes are:
 *
 * - Methods in this class try to handle kind/intent values of "unknown".
 * - Methods in this class add auxiliary object classes to definitions found.
 *
 * NOTE: This is a preliminary version of the algorithms. These methods will most probably evolve in the future.
 */
public class ResourceObjectDefinitionResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectDefinitionResolver.class);

    /**
     * Returns appropriate {@link ResourceObjectDefinition} for given shadow. We are not too strict here.
     * Unknown kind/intent values are ignored (treated like null). Intent without kind is ignored.
     *
     * Takes auxiliary object classes defined in the shadow into account.
     *
     * Note: we could be even more relaxed (in the future):
     *
     * 1. Currently the consistency between kind, intent, and OC is checked. We could avoid this.
     * 2. The {@link ResourceSchema#findObjectDefinition(ShadowKindType, String, QName)} method used throws an exception
     * when it cannot decide among various definitions for given kind (when intent and OC is null). We could be more
     * permissive and return any of them.
     */
    public static @Nullable ResourceObjectDefinition getDefinitionForShadow(
            @NotNull ResourceSchema resourceSchema, @NotNull ShadowType shadow) {

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
                LOGGER.warn("Partially-classified {}. Looking for default type of {}, if present", shadow, kind);
            }
            structuralDefinition = resourceSchema.findObjectDefinition(kind, intent, objectClassName);
        } else if (objectClassName != null) {
            // TODO or findObjectClassDefinition?
            structuralDefinition = resourceSchema.findDefinitionForObjectClass(objectClassName);
        } else {
            structuralDefinition = null;
        }

        if (structuralDefinition != null) {
            return addAuxiliaryObjectClasses(structuralDefinition, shadow.getAuxiliaryObjectClass(), resourceSchema);
        } else {
            return null;
        }
    }

    /**
     * A version of {@link #getDefinitionForShadow(ResourceSchema, ShadowType)}.
     */
    public static @Nullable ResourceObjectDefinition getDefinitionForShadow(
            @NotNull ResourceSchema resourceSchema, @NotNull PrismObject<ShadowType> shadow) {
        return getDefinitionForShadow(resourceSchema, shadow.asObjectable());
    }

    /**
     * Looks up appropriate definition for "bulk operation" i.e. operation that is executed for given kind/intent/objectclass
     * on given resource.
     *
     * Currently, this is the same as {@link #getObjectDefinitionPrecisely(ResourceType, ShadowKindType, String, QName,
     * Collection, boolean)} with no additional aux OCs and unknown values not supported.
     */
    public static @Nullable ResourceObjectDefinition getForBulkOperation(
        @NotNull ResourceType resource,
        @Nullable ShadowKindType kind,
        @Nullable String intent,
        @Nullable QName objectClassName) throws SchemaException, ConfigurationException {
        return getObjectDefinitionPrecisely(resource, kind, intent, objectClassName, List.of(), false);
    }

    /**
     * Determines object type/class definition in a precise way. The decision is based on kind/intent/objectclass triple,
     * and later enriched by aux object class names.
     *
     * Basic schema is:
     *
     *  - no kind or kind=UNKNOWN -> decision is based on the OC
     *  - kind present -> decision is based on kind/intent (optionally OC), then checked against OC
     *
     * (For details please see the code.)
     *
     * @param additionalAuxiliaryObjectClassNames Auxiliary object classes that should be "baked into" the definition,
     *                                            in addition to any classes specified in the resolved object type.
     * @param unknownValuesSupported If we allow the use of "unknown" kind or intent. This is usually the case when
     *                               we obtain the coordinates from an existing shadow. On the contrary, if we are looking up
     *                               according to user-provided data (e.g. query), we don't want to allow unknown values.
     *
     * @see ResourceSchema#findObjectDefinition(ShadowKindType, String, QName)
     * @see ResourceSchema#findDefinitionForObjectClass(QName)
     * @see #getDefinitionForShadow(ResourceSchema, ShadowType)
     */
    @Contract("   _,  null,  null,  null, _, _ ->  null;" // K+I+OC null -> null
            + "   _,     _,     _, !null, _, _ -> !null;" // OC non-null -> non null (or fail)
            + "   _, !null, !null,     _, _, _ -> !null;" // K+I non-null -> non null (or fail)
            + "   _,  null, !null,     _, _, _ ->  fail") // K null, I non-null -> fail
    public static ResourceObjectDefinition getObjectDefinitionPrecisely(
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

        if (kind == ShadowKindType.UNKNOWN && objectClassName == null) {
            throw new IllegalStateException("Not possible to determine object definition with kind being \"unknown\""
                    + " and no object class name present");
        }

        disallowIntentWithoutKind(kind, intent, objectClassName);

        boolean specificKindPresent = kind != null && kind != ShadowKindType.UNKNOWN;
        boolean specificIntentPresent = intent != null && !SchemaConstants.INTENT_UNKNOWN.equals(intent);
        String specificIntent = specificIntentPresent ? intent : null;

        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);

        ResourceObjectDefinition objectDefinition;
        if (specificKindPresent) {
            objectDefinition = resourceSchema.findObjectDefinition(kind, specificIntent, objectClassName);
            if (objectDefinition == null) {
                throw new ConfigurationException("No object type definition for " + kind + "/" + specificIntent
                        + (objectClassName != null ? " (object class " + objectClassName.getLocalPart() + ")" : "")
                        + " in " + resource
                        + (SchemaConstants.INTENT_UNKNOWN.equals(intent) ? " (looking for unknown intent)" : ""));
            }
            checkObjectClassCompatibility(kind, intent, objectClassName, objectDefinition);
        } else {
            // Kind is null or unknown, so the object class name must be specified
            // FIXME Sometimes we want to look for the raw OC definition, e.g. when searching for all items in OC. (MID-7470.)
            //  But maybe that's out of scope of this method, and should be resolved in ProvisioningContextFactory.
            // TODO or findObjectClassDefinition here?
            objectDefinition = resourceSchema.findDefinitionForObjectClass(objectClassName);
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
}
