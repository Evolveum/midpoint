/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.UNKNOWN;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Information needed to carry out synchronization-related activities (categorization, correlation,
 * and execution of synchronization reactions). This class exists to unify the "legacy" way of specifying
 * this information (in `synchronization` section of resource definition) and "modern" one - right in `schemaHandling` part.
 */
public class SynchronizationPolicy {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationPolicy.class);

    @NotNull private final ShadowKindType kind;

    /**
     * Name of the type of focus objects referenced by this policy. (I.e. we correlate and synchronize to an object
     * of this type.)
     */
    @NotNull private final QName focusTypeName;

    /**
     * Archetype bound to this policy. It is used when correlating and when creating new objects.
     */
    @Nullable private final String archetypeOid;

    /** TODO */
    @NotNull private final QName objectClassName;

    /**
     * Correlation definition.
     *
     * The legacy way of specifying correlation filter + confirmation expression is already reflected here.
     */
    @NotNull private final CorrelationDefinitionType correlationDefinitionBean;

    /**
     * This definition is usually {@link ResourceObjectTypeDefinition}, but in some exceptional cases
     * it may be {@link ResourceObjectClassDefinition} instead. For example, if there's `synchronization` section
     * with no `schemaHandling`.
     */
    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

    /**
     * If `false`, no correlation and no synchronization reaction(s) execution is done.
     *
     * TODO better name
     */
    private final boolean synchronizationEnabled;

    /** TODO */
    private final boolean opportunistic;

    /** legacy name */
    @Nullable private final String name;

    /** TODO */
    @NotNull private final ResourceObjectTypeDelineation delineation;

    /**
     * Reactions, already ordered.
     */
    @NotNull private final List<SynchronizationReactionDefinition> reactions;

    private final boolean hasLegacyConfiguration;

    SynchronizationPolicy(
            @NotNull ShadowKindType kind,
            @Nullable QName focusTypeName,
            @Nullable ObjectReferenceType archetypeRef,
            @NotNull QName objectClassName,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            boolean synchronizationEnabled,
            boolean opportunistic,
            @Nullable String name,
            @NotNull ResourceObjectTypeDelineation delineation,
            @NotNull Collection<SynchronizationReactionDefinition> reactions,
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            boolean hasLegacyConfiguration) {
        this.kind = kind;
        this.focusTypeName = Objects.requireNonNullElse(focusTypeName, UserType.COMPLEX_TYPE);
        this.archetypeOid = getArchetypeOid(archetypeRef);
        this.objectClassName = objectClassName;
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.synchronizationEnabled = synchronizationEnabled;
        this.opportunistic = opportunistic;
        this.name = name;
        this.delineation = delineation;
        this.reactions = new ArrayList<>(reactions);
        this.reactions.sort(Comparator.naturalOrder());
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.hasLegacyConfiguration = hasLegacyConfiguration;
    }

    public static String getArchetypeOid(@Nullable ObjectReferenceType archetypeRef) {
        if (archetypeRef == null) {
            return null;
        }
        String oid = archetypeRef.getOid();
        if (oid != null) {
            return oid;
        }
        throw new UnsupportedOperationException("Dynamic references are not supported for archetypeRef");
    }

    public @NotNull ShadowKindType getKind() {
        return kind;
    }

    /**
     * The returned intent is null only if:
     *
     * 1. standalone synchronization bean is used,
     * 2. the intent is not specified in the bean,
     * 3. no default object type can be found.
     */
    public @Nullable String getIntent() {
        if (resourceObjectDefinition instanceof ResourceObjectTypeDefinition) {
            return ((ResourceObjectTypeDefinition) resourceObjectDefinition).getIntent();
        } else {
            return null;
        }
    }

    public boolean isSynchronizationEnabled() {
        return synchronizationEnabled;
    }

    public boolean isOpportunistic() {
        return opportunistic;
    }

    public @Nullable String getName() {
        return name;
    }

    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return resourceObjectDefinition;
    }

    /**
     * Checks if the synchronization policy matches given "parameters" (object class, kind, intent).
     */
    public boolean isApplicableTo(QName objectClass, ShadowKindType kind, String intent, boolean strictIntent) {
        if (!isObjectClassNameMatching(objectClass)) {
            return false;
        }

        // kind
        LOGGER.trace("Comparing kinds, policy kind: {}, current kind: {}", getKind(), kind);
        if (kind != null && kind != UNKNOWN && !getKind().equals(kind)) {
            LOGGER.trace("Kinds don't match for {}", this);
            return false;
        }

        // intent
        LOGGER.trace("Comparing intents, policy intent: {}, current intent: {} (strict={})", getIntent(), intent, strictIntent);
        if (!strictIntent) {
            if (intent != null
                    && !SchemaConstants.INTENT_UNKNOWN.equals(intent)
                    && !MiscSchemaUtil.equalsIntent(intent, getIntent())) {
                LOGGER.trace("Intents don't match for {}", this);
                return false;
            }
        } else {
            if (!MiscSchemaUtil.equalsIntent(intent, getIntent())) {
                LOGGER.trace("Intents don't match for {}", this);
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isObjectClassNameMatching(QName objectClass) {
        if (objectClassDefinedAndNotMatching(objectClass, this.objectClassName)) {
            LOGGER.trace("Object class does not match the one defined in {}", this);
            return false;
        }

        if (objectClassDefinedAndNotMatching(objectClass, resourceObjectDefinition.getTypeName())) {
            LOGGER.trace("Object class does not match the one defined in type definition in {}", this);
            return false;
        }
        return true;
    }

    private boolean objectClassDefinedAndNotMatching(@Nullable QName objectClass, @Nullable QName policyObjectClass) {
        return objectClass != null &&
                policyObjectClass != null &&
                !QNameUtil.match(objectClass, policyObjectClass);
    }


    public ResourceObjectTypeDefinition getResourceTypeDefinitionRequired() {
        if (resourceObjectDefinition instanceof ResourceObjectTypeDefinition) {
            return (ResourceObjectTypeDefinition) resourceObjectDefinition;
        } else {
            throw new IllegalStateException("No resource object type definition present: " + resourceObjectDefinition);
        }
    }

    /** Returns the focus class this synchronization policy points to. */
    public @NotNull Class<? extends FocusType> getFocusClass() {
        return PrismContext.get().getSchemaRegistry()
                .determineClassForTypeRequired(focusTypeName);
    }

    public @Nullable String getArchetypeOid() {
        return archetypeOid;
    }

    @Override
    public String toString() { // TODO
        return getClass().getSimpleName() + "{" +
                "kind=" + kind +
                ", resourceObjectDefinition=" + resourceObjectDefinition +
                '}';
    }

    /**
     * Returned definition contains legacy correlation definition, if there's any.
     */
    public @NotNull CorrelationDefinitionType getCorrelationDefinition() {
        return correlationDefinitionBean;
    }

    /** Combines legacy and new-style information. */
    public @NotNull ResourceObjectTypeDelineation getDelineation() {
        return delineation;
    }

    public @NotNull QName getObjectClassName() {
        return objectClassName;
    }

    public @NotNull List<SynchronizationReactionDefinition> getReactions() {
        return reactions;
    }

    public boolean hasLegacyConfiguration() {
        return hasLegacyConfiguration;
    }
}
