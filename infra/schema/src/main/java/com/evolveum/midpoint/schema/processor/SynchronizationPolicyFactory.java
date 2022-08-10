/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.fillDefault;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.DISPUTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.util.CloneUtil;
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
                first(
                        providedSynchronizationBean,
                        ObjectSynchronizationType::new);

        QName focusTypeName =
                first(
                        typeDef.getFocusTypeName(),
                        synchronizationBean::getFocusType,
                        () -> UserType.COMPLEX_TYPE);

        CorrelationDefinitionType correlationDefinitionBean =
                mergeCorrelationDefinition(typeDef, synchronizationBean, resource);

        boolean synchronizationEnabled = isSynchronizationEnabled(providedSynchronizationBean, typeDef);

        boolean opportunistic =
                first(
                        typeDef.isSynchronizationOpportunistic(),
                        () -> !Boolean.FALSE.equals(synchronizationBean.isOpportunistic()));

        ResourceObjectTypeDelineation delineation = getDelineation(synchronizationBean, typeDef, resource);

        Collection<SynchronizationReactionDefinition> reactions =
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
            return first(
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

    @SafeVarargs
    private static <T> @NotNull T first(T value, Supplier<T>... suppliers) {
        if (value != null) {
            return value;
        }
        for (Supplier<T> supplier : suppliers) {
            T supplied = supplier.get();
            if (supplied != null) {
                return supplied;
            }
        }
        throw new IllegalStateException("No value");
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
        for (LegacySynchronizationReactionType synchronizationReactionBean : synchronizationBean.getReaction()) {
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

    /**
     * "Compiles" the correlation definition from all available information:
     *
     * . attribute-level "correlation" configuration snippets
     * . legacy correlation/confirmation expressions/filters
     */
    private static CorrelationDefinitionType mergeCorrelationDefinition(
            @NotNull ResourceObjectTypeDefinition typeDef,
            @NotNull ObjectSynchronizationType synchronizationBean,
            @NotNull ResourceType resource) throws ConfigurationException {

        return addCorrelationDefinitionsFromAttributes(
                typeDef,
                first(
                        typeDef.getCorrelationDefinitionBean(),
                        () -> getCorrelationDefinitionBean(synchronizationBean)),
                resource);
    }

    private static CorrelationDefinitionType addCorrelationDefinitionsFromAttributes(
            @NotNull ResourceObjectTypeDefinition typeDef,
            @NotNull CorrelationDefinitionType explicitDefinition,
            @NotNull ResourceType resource) throws ConfigurationException {
        CorrelationDefinitionType cloned = null;
        for (ResourceAttributeDefinition<?> attributeDefinition : typeDef.getAttributeDefinitions()) {
            ItemCorrelationDefinitionType correlationDefBean = attributeDefinition.getCorrelationDefinitionBean();
            if (correlationDefBean != null) {
                if (cloned == null) {
                    cloned = explicitDefinition.clone();
                }
                addCorrelationDefinitionFromAttribute(cloned, attributeDefinition, correlationDefBean, typeDef, resource);
            }
        }
        return cloned != null ? cloned : explicitDefinition;
    }

    private static void addCorrelationDefinitionFromAttribute(
            @NotNull CorrelationDefinitionType overallCorrelationDefBean,
            @NotNull ResourceAttributeDefinition<?> attributeDefinition,
            @NotNull ItemCorrelationDefinitionType attributeCorrelationDefBean,
            @NotNull ResourceObjectTypeDefinition typeDef,
            @NotNull ResourceType resource) throws ConfigurationException {
        List<InboundMappingType> inboundMappingBeans = attributeDefinition.getInboundMappingBeans();
        configCheck(!inboundMappingBeans.isEmpty(),
                "Attribute-level correlation requires an inbound mapping; for %s in %s (%s)",
                attributeDefinition, typeDef, resource);
        ItemPathType itemPathBean = determineItemPathBean(attributeDefinition, attributeCorrelationDefBean);
        configCheck(itemPathBean != null,
                "Item corresponding to correlation attribute %s couldn't be determined in %s (%s). You must specify"
                        + " it either explicitly, or provide exactly one inbound mapping with a proper target",
                attributeDefinition, typeDef, resource);
        if (!attributeCorrelationDefBean.getRules().isEmpty()) {
            throw new UnsupportedOperationException(
                    String.format("Explicit specification of rules is not supported yet: in %s in %s (%s)",
                            attributeDefinition, typeDef, resource));
        }
        CompositeCorrelatorType correlators = overallCorrelationDefBean.getCorrelators();
        if (correlators == null) {
            correlators = new CompositeCorrelatorType();
            overallCorrelationDefBean.setCorrelators(correlators);
        }
        correlators.getItems().add(
                new ItemsCorrelatorType()
                        .confidence(CloneUtil.clone(attributeCorrelationDefBean.getConfidence()))
                        .item(new ItemCorrelationType()
                                .path(itemPathBean.clone())));
    }

    private static ItemPathType determineItemPathBean(
            ResourceAttributeDefinition<?> attributeDefinition, ItemCorrelationDefinitionType attributeCorrelationDefBean) {
        ItemPathType explicitItemPath = attributeCorrelationDefBean.getItem();
        if (explicitItemPath != null) {
            return explicitItemPath;
        } else {
            return guessCorrelationItemPath(attributeDefinition);
        }
    }

    /**
     * Tries to determine correlation item path from the inbound mapping target.
     */
    private static ItemPathType guessCorrelationItemPath(ResourceAttributeDefinition<?> attributeDefinition) {
        List<InboundMappingType> inboundMappingBeans = attributeDefinition.getInboundMappingBeans();
        if (inboundMappingBeans.size() != 1) {
            return null;
        }
        VariableBindingDefinitionType target = inboundMappingBeans.get(0).getTarget();
        ItemPathType itemPathType = target != null ? target.getPath() : null;
        if (itemPathType == null) {
            return null;
        }
        ItemPath itemPath = itemPathType.getItemPath();
        QName variableName = itemPath.firstToVariableNameOrNull();
        if (variableName == null) {
            return itemPathType;
        }
        String localPart = variableName.getLocalPart();
        if (ExpressionConstants.VAR_FOCUS.equals(localPart)
                || ExpressionConstants.VAR_USER.equals(localPart)) {
            return new ItemPathType(itemPath.rest());
        } else {
            LOGGER.warn("Mapping target variable name '{}' is not supported for determination of correlation item path in {}",
                    variableName, attributeDefinition);
            return null;
        }
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
}
