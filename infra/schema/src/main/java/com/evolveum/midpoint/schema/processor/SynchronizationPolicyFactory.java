/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil.mergeCorrelationDefinition;
import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.fillDefault;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.DISPUTED;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ObjectSynchronizationReactionDefinition;
import com.evolveum.midpoint.util.MiscUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates {@link SynchronizationPolicy} objects.
 */
public class SynchronizationPolicyFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationPolicyFactory.class);

    /**
     * Creates {@link SynchronizationPolicy} by looking for type definition and synchronization definition
     * for given kind and intent in resource schema.
     *
     * NOTE: Since 4.6, we no longer support `synchronization` entry without corresponding `schemaHandling` entry.
     * This would make the code unnecessarily complex just to support some corner cases regarding
     * (now) legacy `synchronization` configuration data structure.
     */
    public static @Nullable SynchronizationPolicy forKindAndIntent(
            @NotNull ShadowKindType kind, @NotNull String intent, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {

        Preconditions.checkArgument(ShadowUtil.isKnown(kind), "kind is not known: %s", kind);
        Preconditions.checkArgument(ShadowUtil.isKnown(intent), "intent is not known: %s", intent);

        ResourceObjectTypeDefinition typeDefinition =
                ResourceSchemaFactory
                        .getCompleteSchemaRequired(resource)
                        .getObjectTypeDefinition(kind, intent);

        if (typeDefinition != null) {
            return forTypeDefinition(typeDefinition, resource);
        } else {
            LOGGER.trace("No type definition for {}/{} found", kind, intent);
            return null;
        }
    }

    /**
     * Creates {@link SynchronizationPolicy} based on known {@link ResourceObjectTypeDefinition}.
     */
    public static @NotNull SynchronizationPolicy forTypeDefinition(
            @NotNull ResourceObjectTypeDefinition typeDefinition, @NotNull ResourceType resource)
            throws ConfigurationException {
        @Nullable ObjectSynchronizationType legacySynchronizationBean = findLegacySynchronizationBean(typeDefinition, resource);
        return create(legacySynchronizationBean, typeDefinition, resource);
    }

    private static @Nullable ObjectSynchronizationType findLegacySynchronizationBean(
            ResourceObjectTypeDefinition typeDefinition, ResourceType resource) {
        SynchronizationType synchronization = resource.getSynchronization();
        if (synchronization == null) {
            return null;
        } else {
            return synchronization.getObjectSynchronization().stream()
                    .filter(objectSynchronization -> isRelatedTo(objectSynchronization, typeDefinition))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static boolean isRelatedTo(ObjectSynchronizationType objectSynchronization, ResourceObjectTypeDefinition typeDef) {
        return fillDefault(objectSynchronization.getKind()) == typeDef.getKind()
                && fillDefault(objectSynchronization.getIntent()).equals(typeDef.getIntent());
    }

    /**
     * Combines legacy {@link ObjectSynchronizationType} (potentially empty if not present)
     * and {@link ResourceObjectTypeDefinition} into a {@link SynchronizationPolicy}.
     */
    private static @NotNull SynchronizationPolicy create(
            @Nullable ObjectSynchronizationType providedSynchronizationBean,
            @NotNull ResourceObjectTypeDefinition typeDef,
            @NotNull ResourceType resource)
            throws ConfigurationException {

        ObjectSynchronizationType synchronizationBean =
                MiscUtil.first(
                        providedSynchronizationBean,
                        ObjectSynchronizationType::new);

        QName focusTypeName =
                MiscUtil.first(
                        typeDef.getFocusTypeName(),
                        synchronizationBean::getFocusType,
                        () -> UserType.COMPLEX_TYPE);

        var correlationDefinitionBean = mergeCorrelationDefinition(typeDef, synchronizationBean, resource);

        boolean synchronizationEnabled = isSynchronizationEnabled(providedSynchronizationBean, typeDef);

        boolean opportunistic =
                MiscUtil.first(
                        typeDef.isSynchronizationOpportunistic(),
                        () -> !Boolean.FALSE.equals(synchronizationBean.isOpportunistic()));

        ResourceObjectTypeDelineation delineation = getDelineation(synchronizationBean, typeDef, resource);

        var reactions =
                typeDef.hasSynchronizationReactionsDefinition() ?
                        typeDef.getSynchronizationReactions() :
                        getSynchronizationReactions(synchronizationBean);

        return new SynchronizationPolicy(
                focusTypeName,
                typeDef.getArchetypeOid(), // no representation in legacy bean
                correlationDefinitionBean,
                synchronizationEnabled,
                opportunistic,
                synchronizationBean.getName(),
                delineation,
                reactions,
                typeDef,
                providedSynchronizationBean != null);
    }

    private static boolean isSynchronizationEnabled(
            @Nullable ObjectSynchronizationType providedSynchronizationBean,
            @NotNull ResourceObjectTypeDefinition typeDef) {
        if (providedSynchronizationBean != null) {
            return MiscUtil.first(
                    typeDef.isSynchronizationEnabled(),
                    () -> !Boolean.FALSE.equals(providedSynchronizationBean.isEnabled()));
        } else {
            // If the synchronization bean is missing, we require the synchronization to be explicitly enabled
            return Boolean.TRUE.equals(typeDef.isSynchronizationEnabled());
        }
    }

    private static @NotNull ResourceObjectTypeDelineation getDelineation(
            @NotNull ObjectSynchronizationType synchronizationBean,
            @NotNull ResourceObjectTypeDefinition typeDef,
            @NotNull ResourceType resource) throws ConfigurationException {
        ResourceObjectTypeDelineation delineation = typeDef.getDelineation();
        if (synchronizationBean.getCondition() != null) {
            if (delineation.getClassificationCondition() != null) {
                throw new ConfigurationException("Both legacy and new classification conditions cannot be set in " + resource);
            }
            return delineation.classificationCondition(
                    synchronizationBean.getCondition());
        } else {
            return delineation;
        }
    }

    /**
     * Converts legacy synchronization definition bean ({@link ObjectSynchronizationType}) into a list of parsed
     * {@link SynchronizationReactionDefinition} objects.
     *
     * Especially treats the existence of `correlationDefinition/cases` item. If such an item is present,
     * "create correlation cases" action is added to "disputed" reaction (or such reaction is created, if there's none).
     */
    private static List<ObjectSynchronizationReactionDefinition> getSynchronizationReactions(
            @NotNull ObjectSynchronizationType synchronizationBean) throws ConfigurationException {
        ClockworkSettings defaultSettings = ClockworkSettings.of(synchronizationBean);
        boolean legacyCorrelationCasesEnabled = isLegacyCorrelationCasesSettingOn(synchronizationBean);

        List<ObjectSynchronizationReactionDefinition> list = new ArrayList<>();

        boolean createCasesActionAdded = false;
        for (LegacySynchronizationReactionType synchronizationReactionBean : synchronizationBean.getReaction()) {
            boolean addCreateCasesActionHere =
                    legacyCorrelationCasesEnabled && synchronizationReactionBean.getSituation() == DISPUTED;
            list.add(
                    SynchronizationReactionDefinition.legacy(
                            synchronizationReactionBean, addCreateCasesActionHere, defaultSettings));
            if (addCreateCasesActionHere) {
                createCasesActionAdded = true;
            }
        }

        if (legacyCorrelationCasesEnabled && !createCasesActionAdded) {
            list.add(SynchronizationReactionDefinition.legacy(
                    new LegacySynchronizationReactionType().situation(DISPUTED),
                    true,
                    ClockworkSettings.empty()));
        }

        return list;
    }

    private static boolean isLegacyCorrelationCasesSettingOn(@NotNull ObjectSynchronizationType synchronizationBean) {
        LegacyCorrelationDefinitionType correlationDefinition = synchronizationBean.getCorrelationDefinition();
        return correlationDefinition != null && isEnabled(correlationDefinition.getCases());
    }

    private static boolean isEnabled(CorrelationCasesDefinitionType cases) {
        return cases != null && !Boolean.FALSE.equals(cases.isEnabled());
    }
}
