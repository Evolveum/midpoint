/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static java.util.Objects.requireNonNullElse;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.DISPUTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates {@link SynchronizationPolicy} objects.
 */
public class SynchronizationPolicyFactory {

    /**
     * Creates {@link SynchronizationPolicy} for a synchronization policy present in legacy "synchronization"
     * section of the resource definition. We try to find appropriate object/class definition in the resource schema.
     *
     * Returns null if no such definition can be found.
     */
    private static @Nullable SynchronizationPolicy forStandalone(
            @NotNull ObjectSynchronizationType synchronizationBean, @NotNull ResourceSchema schema)
            throws ConfigurationException {

        ShadowKindType kind = requireNonNullElse(synchronizationBean.getKind(), ACCOUNT);
        String intent = synchronizationBean.getIntent();

        ResourceObjectDefinition objectDefinition;
        if (StringUtils.isEmpty(intent)) { // Note: intent shouldn't be the empty string!
            // We look for a default definition for this kind. That is consistent with the XSD documentation.
            // After all, this data structure is legacy and shouldn't be used anymore. So we don't need to be super-exact here.
            objectDefinition = schema.findObjectDefinitionForKindAndObjectClass(kind, synchronizationBean.getObjectClass());
        } else {
            objectDefinition = schema.findObjectDefinition(kind, intent);
        }

        if (objectDefinition == null) {
            return null;
        }

        ResourceObjectTypeDefinition typeDef =
                objectDefinition instanceof ResourceObjectTypeDefinition ?
                        (ResourceObjectTypeDefinition) objectDefinition : null;

        QName focusTypeName =
                typeDef != null && typeDef.getFocusTypeName() != null ?
                        typeDef.getFocusTypeName() :
                        synchronizationBean.getFocusType();

        QName objectClassName = synchronizationBean.getObjectClass() != null ?
                synchronizationBean.getObjectClass() :
                objectDefinition.getObjectClassName();

        CorrelationDefinitionType correlationDefinitionBean =
                typeDef != null && typeDef.getCorrelationDefinitionBean() != null ?
                        typeDef.getCorrelationDefinitionBean() :
                        getCorrelationDefinitionBean(synchronizationBean);

        Boolean enabledInType = typeDef != null ? typeDef.isSynchronizationEnabled() : null;
        boolean synchronizationEnabled = enabledInType != null ?
                enabledInType : !Boolean.FALSE.equals(synchronizationBean.isEnabled());

        Boolean opportunisticInType = typeDef != null ? typeDef.isSynchronizationOpportunistic() : null;
        boolean opportunistic = opportunisticInType != null ?
                opportunisticInType : !Boolean.FALSE.equals(synchronizationBean.isOpportunistic());

        ResourceObjectTypeDelineation delineation =
                typeDef != null ? typeDef.getDelineation() : ResourceObjectTypeDelineation.none();
        if (synchronizationBean.getCondition() != null) {
            if (delineation.getClassificationCondition() != null) {
                throw new ConfigurationException("Both legacy and new classification conditions cannot be set in " + schema);
            }
            delineation = delineation.classificationCondition(
                    synchronizationBean.getCondition());
        }

        Collection<SynchronizationReactionDefinition> reactions =
                typeDef != null && typeDef.hasSynchronizationReactionsDefinition() ?
                        typeDef.getSynchronizationReactions() :
                        getSynchronizationReactions(synchronizationBean);

        return new SynchronizationPolicy(
                kind,
                focusTypeName,
                null,
                objectClassName,
                correlationDefinitionBean,
                synchronizationEnabled,
                opportunistic,
                synchronizationBean.getName(),
                delineation,
                reactions,
                objectDefinition,
                true);
    }

    /**
     * Converts legacy synchronization definition bean ({@link ObjectSynchronizationType}) into a list of parsed
     * {@link SynchronizationReactionDefinition} objects.
     *
     * Especially treats the existence of `correlationDefinition/cases` item. If such an item is present,
     * "create correlation cases" action is added to "disputed" reaction (or such reaction is created, if there's none).
     */
    private static List<SynchronizationReactionDefinition> getSynchronizationReactions(
            @NotNull ObjectSynchronizationType synchronizationBean) throws ConfigurationException {
        ClockworkSettings defaultSettings = ClockworkSettings.of(synchronizationBean);
        boolean legacyCorrelationCasesEnabled = isLegacyCorrelationCasesSettingOn(synchronizationBean);

        List<SynchronizationReactionDefinition> list = new ArrayList<>();

        boolean createCasesActionAdded = false;
        for (SynchronizationReactionType synchronizationReactionBean : synchronizationBean.getReaction()) {
            boolean addCreateCasesActionHere =
                    legacyCorrelationCasesEnabled && synchronizationReactionBean.getSituation() == DISPUTED;
            list.add(
                    SynchronizationReactionDefinition.of(
                            synchronizationReactionBean, addCreateCasesActionHere, defaultSettings));
            if (addCreateCasesActionHere) {
                createCasesActionAdded = true;
            }
        }

        if (legacyCorrelationCasesEnabled && !createCasesActionAdded) {
            list.add(SynchronizationReactionDefinition.of(
                    new SynchronizationReactionType().situation(DISPUTED),
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

    private static @NotNull CorrelationDefinitionType getCorrelationDefinitionBean(
            @NotNull ObjectSynchronizationType synchronizationBean) {
        if (synchronizationBean.getCorrelationDefinition() != null) {
            return synchronizationBean.getCorrelationDefinition();
        }
        List<ConditionalSearchFilterType> correlationFilters = synchronizationBean.getCorrelation();
        if (correlationFilters.isEmpty()) {
            return new CorrelationDefinitionType();
        } else {
            return new CorrelationDefinitionType()
                    .correlators(new CompositeCorrelatorType()
                            .filter(
                                    createFilterCorrelator(correlationFilters, synchronizationBean.getConfirmation())));
        }
    }

    private static @NotNull FilterCorrelatorType createFilterCorrelator(
            List<ConditionalSearchFilterType> correlationFilters, ExpressionType confirmation) {
        FilterCorrelatorType filterCorrelator =
                new FilterCorrelatorType()
                        .confirmation(
                                CloneUtil.clone(confirmation));
        filterCorrelator.getOwnerFilter().addAll(
                CloneUtil.cloneCollectionMembers(correlationFilters));
        return filterCorrelator;
    }

    /**
     * Creates {@link SynchronizationPolicy} for a policy embedded in a resource object type definition
     * (i.e. in schema handling section).
     *
     * Assuming there is *no* explicit standalone synchronization definition!
     */
    private static @NotNull SynchronizationPolicy forEmbedded(@NotNull ResourceObjectTypeDefinition typeDef) {
        return new SynchronizationPolicy(
                typeDef.getKind(),
                typeDef.getFocusTypeName(),
                typeDef.getArchetypeRef(),
                typeDef.getObjectClassName(),
                java.util.Objects.requireNonNullElseGet(
                        typeDef.getCorrelationDefinitionBean(),
                        CorrelationDefinitionType::new),
                Boolean.TRUE.equals(typeDef.isSynchronizationEnabled()),
                !Boolean.FALSE.equals(typeDef.isSynchronizationOpportunistic()),
                null,
                typeDef.getDelineation(),
                typeDef.getSynchronizationReactions(),
                typeDef,
                false);
    }

    /**
     * Creates {@link SynchronizationPolicy} by looking for type definition and synchronization
     * for given kind and intent in resource schema.
     */
    public static @Nullable SynchronizationPolicy forKindAndIntent(
            @NotNull ShadowKindType kind, @NotNull String intent, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {

        Preconditions.checkArgument(ShadowUtil.isKnown(kind), "kind is not known: %s", kind);
        Preconditions.checkArgument(ShadowUtil.isKnown(intent), "intent is not known: %s", intent);

        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        SynchronizationPolicy standalonePolicy = getStandalonePolicyIfPresent(kind, intent, resource, schema);
        if (standalonePolicy != null) {
            return standalonePolicy;
        } else {
            return getEmbeddedPolicyIfPresent(kind, intent, schema);
        }
    }

    /**
     * Use this method if you are absolutely sure that given kind/intent definition must exist in the resource.
     *
     * @throws IllegalStateException if there's no type definition for given kind/intent
     */
    public static @NotNull SynchronizationPolicy forKindAndIntentStrictlyRequired(
            @NotNull ShadowKindType kind, @NotNull String intent, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {
        return MiscUtil.requireNonNull(
                forKindAndIntent(kind, intent, resource),
                () -> new IllegalStateException("No " + kind + "/" + intent + " definition in " + resource));
    }

    private static @Nullable SynchronizationPolicy getEmbeddedPolicyIfPresent(
            @NotNull ShadowKindType kind, @NotNull String intent, @NotNull ResourceSchema schema) {
        ResourceObjectDefinition definition = schema.findObjectDefinition(kind, intent);
        if (definition instanceof ResourceObjectTypeDefinition) {
            return forEmbedded((ResourceObjectTypeDefinition) definition);
        } else {
            return null;
        }
    }

    private static @Nullable SynchronizationPolicy getStandalonePolicyIfPresent(
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @NotNull ResourceType resource,
            @NotNull ResourceSchema schema) throws ConfigurationException {
        if (resource.getSynchronization() == null) {
            return null;
        }
        // We don't directly compare bean.intent, because the binding of sync <-> schema handling may be implicit
        // using object class name (todo - really? or just I think so?)
        for (ObjectSynchronizationType synchronizationBean : resource.getSynchronization().getObjectSynchronization()) {
            SynchronizationPolicy standalone = forStandalone(synchronizationBean, schema);
            if (standalone != null
                    && kind == standalone.getKind()
                    && intent.equals(standalone.getIntent())) {
                return standalone;
            }
        }
        return null;
    }

    /**
     * Looks up the policy corresponding to the object type definition *bean* retrieved from schema handling.
     */
    public static @Nullable SynchronizationPolicy forDefinitionBean(
            @NotNull ResourceObjectTypeDefinitionType typeDefBean, @NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (schema == null) {
            return null;
        }
        ResourceObjectDefinition objectDefinition =
                schema.findObjectDefinition(
                        Objects.requireNonNullElse(typeDefBean.getKind(), ACCOUNT),
                        Objects.requireNonNullElse(typeDefBean.getIntent(), SchemaConstants.INTENT_DEFAULT),
                        typeDefBean.getObjectClass());
        if (objectDefinition instanceof ResourceObjectTypeDefinition) {
            ResourceObjectTypeDefinition typeDef = (ResourceObjectTypeDefinition) objectDefinition;
            return forKindAndIntent(typeDef.getKind(), typeDef.getIntent(), resource);
        } else {
            // shouldn't occur
            return null;
        }
    }
}
