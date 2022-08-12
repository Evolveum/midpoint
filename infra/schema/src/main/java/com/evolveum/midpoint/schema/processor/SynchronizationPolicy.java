/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Information needed to carry out synchronization-related activities (classification, correlation,
 * and execution of synchronization reactions). This class exists to unify the "legacy" way of specifying
 * this information (in `synchronization` section of resource definition) and "modern" one - right in `schemaHandling` part.
 *
 * Created using {@link SynchronizationPolicyFactory}.
 */
public class SynchronizationPolicy {

    /**
     * Definition of the type corresponding to this synchronization policy.
     */
    @NotNull private final ResourceObjectTypeDefinition objectTypeDefinition;

    /**
     * Name of the type of focus objects referenced by this policy. (I.e. we correlate and synchronize to an object
     * of this type.)
     */
    @NotNull private final QName focusTypeName;

    /**
     * Archetype bound to this policy. It is used when correlating and when creating new objects.
     */
    @Nullable private final String archetypeOid;

    /**
     * Correlation definition.
     *
     * The legacy way of specifying correlation filter + confirmation expression is already reflected here.
     */
    @NotNull private final CorrelationDefinitionType correlationDefinitionBean;

    /**
     * If `false`, no correlation and no synchronization reaction(s) execution is done.
     *
     * TODO what about classification?
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
            @NotNull QName focusTypeName,
            @Nullable String archetypeOid,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            boolean synchronizationEnabled,
            boolean opportunistic,
            @Nullable String name,
            @NotNull ResourceObjectTypeDelineation delineation,
            @NotNull Collection<SynchronizationReactionDefinition> reactions,
            @NotNull ResourceObjectTypeDefinition objectTypeDefinition,
            boolean hasLegacyConfiguration) {
        this.focusTypeName = focusTypeName;
        this.archetypeOid = archetypeOid;
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.synchronizationEnabled = synchronizationEnabled;
        this.opportunistic = opportunistic;
        this.name = name;
        this.delineation = delineation;
        this.reactions = new ArrayList<>(reactions);
        this.reactions.sort(Comparator.naturalOrder());
        this.objectTypeDefinition = objectTypeDefinition;
        this.hasLegacyConfiguration = hasLegacyConfiguration;
    }

    public @NotNull ShadowKindType getKind() {
        return objectTypeDefinition.getKind();
    }

    public @NotNull String getIntent() {
        return objectTypeDefinition.getIntent();
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

    public @NotNull ResourceObjectTypeDefinition getObjectTypeDefinition() {
        return objectTypeDefinition;
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
                "typeDefinition=" + objectTypeDefinition +
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
        return objectTypeDefinition.getObjectClassName();
    }

    public @NotNull List<SynchronizationReactionDefinition> getReactions() {
        return reactions;
    }

    public boolean hasLegacyConfiguration() {
        return hasLegacyConfiguration;
    }

    public Integer getClassificationOrder() {
        return delineation.getClassificationOrder();
    }

    public boolean isDefaultForClassification() {
        return objectTypeDefinition.isDefaultForObjectClass();
    }
}
