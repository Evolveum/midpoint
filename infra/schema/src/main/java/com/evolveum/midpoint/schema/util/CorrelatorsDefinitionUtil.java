/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectInboundProcessingDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectInboundProcessingDefinition.ItemInboundProcessingDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Utilities for manipulating correlators definitions.
 */
public class CorrelatorsDefinitionUtil {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelatorsDefinitionUtil.class);

    /**
     * Returns the name under which we will reference this item definition (using "ref" elements).
     */
    public static @NotNull String getName(@NotNull CorrelationItemType definitionBean) {
        if (definitionBean.getName() != null) {
            return definitionBean.getName();
        }
        String nameFromPath = getNameFromPath(definitionBean.getRef());
        if (nameFromPath != null) {
            return nameFromPath;
        }
        throw new IllegalArgumentException("Item definition with no name " + definitionBean);
    }

    private static @Nullable String getNameFromPath(ItemPathType path) {
        if (path == null) {
            return null;
        }
        ItemName lastName = path.getItemPath().lastName();
        if (lastName != null) {
            return lastName.getLocalPart();
        }
        return null;
    }

    /**
     * Tries to shortly identify given correlator configuration. Just to able to debug e.g. configuration resolution.
     */
    public static String identify(@Nullable AbstractCorrelatorType configBean) {
        if (configBean == null) {
            return "(none)";
        } else {
            StringBuilder sb = new StringBuilder(configBean.getClass().getSimpleName());
            sb.append(": ");
            if (configBean.getName() != null) {
                sb.append("named '")
                        .append(configBean.getName())
                        .append("', ");
            } else {
                sb.append("unnamed, ");
            }
            if (configBean.getDisplayName() != null) {
                sb.append("displayName: '")
                        .append(configBean.getDisplayName())
                        .append("', ");
            }
            if (configBean.getSuper() != null) {
                sb.append("extending super '")
                        .append(configBean.getSuper().getRef())
                        .append("', ");
            }
            CorrelatorCompositionDefinitionType composition = getComposition(configBean);
            if (composition != null) {
                if (composition.getTier() != null) {
                    sb.append("tier ")
                            .append(composition.getTier())
                            .append(", ");
                }
                if (composition.getOrder() != null) {
                    sb.append("order ")
                            .append(composition.getOrder())
                            .append(", ");
                }
            }
            if (Boolean.FALSE.equals(configBean.isEnabled())) {
                sb.append("disabled, ");
            }
            if (configBean instanceof ItemsCorrelatorType itemsCorrelator) {
                sb.append("items: ");
                sb.append(
                        itemsCorrelator.getItem().stream()
                                .map(itemDef -> String.valueOf(itemDef.getRef()))
                                .collect(Collectors.joining(", ")));
            } else {
                sb.append("configured with: ")
                        .append(configBean.asPrismContainerValue().getItemNames());
            }
            return sb.toString();
        }
    }

    public static @Nullable CorrelatorCompositionDefinitionType getComposition(AbstractCorrelatorType bean) {
        if (bean instanceof ItemsSubCorrelatorType itemsSubCorrelator) {
            return itemsSubCorrelator.getComposition();
        } else if (bean instanceof FilterSubCorrelatorType filterSubCorrelator) {
            return filterSubCorrelator.getComposition();
        } else if (bean instanceof ExpressionSubCorrelatorType expressionSubCorrelator) {
            return expressionSubCorrelator.getComposition();
        } else if (bean instanceof IdMatchSubCorrelatorType idMatchSubCorrelator) {
            return idMatchSubCorrelator.getComposition();
        } else if (bean instanceof CompositeSubCorrelatorType compositeSubCorrelator) {
            return compositeSubCorrelator.getComposition();
        } else {
            return null;
        }
    }

    private static void addSingleItemCorrelator(
            @NotNull CorrelationDefinitionType overallCorrelationDefBean,
            @NotNull ItemPath focusItemPath,
            @NotNull ItemCorrelatorDefinitionType attributeCorrelatorDefBean) {
        CompositeCorrelatorType correlators = overallCorrelationDefBean.getCorrelators();
        if (correlators == null) {
            correlators = new CompositeCorrelatorType();
            overallCorrelationDefBean.setCorrelators(correlators);
        }
        correlators.getItems().add(
                new ItemsSubCorrelatorType()
                        .item(new CorrelationItemType()
                                .ref(
                                        new ItemPathType(focusItemPath))
                                .search(
                                        CloneUtil.clone(attributeCorrelatorDefBean.getSearch()))));
    }

    /**
     * "Compiles" the correlation definition from all available information:
     *
     * . attribute-level "correlation" configuration snippets
     * . legacy correlation/confirmation expressions/filters
     */
    public static CorrelationDefinitionType mergeCorrelationDefinition(
            @NotNull ResourceObjectInboundProcessingDefinition objectInboundProcDef,
            @Nullable ObjectSynchronizationType synchronizationBean,
            @NotNull ResourceType resource) throws ConfigurationException {

        return addCorrelationDefinitionsFromItems(
                objectInboundProcDef,
                MiscUtil.first(
                        objectInboundProcDef.getCorrelation(),
                        () -> getCorrelationDefinitionBean(synchronizationBean)),
                resource);
    }

    private static CorrelationDefinitionType addCorrelationDefinitionsFromItems(
            @NotNull ResourceObjectInboundProcessingDefinition objectInboundProcDef,
            @NotNull CorrelationDefinitionType explicitDefinition,
            @NotNull ResourceType resource) throws ConfigurationException {
        CorrelationDefinitionType cloned = null;
        for (var itemInboundDef : objectInboundProcDef.getItemInboundDefinitions()) {
            var itemProcDef = itemInboundDef.inboundProcessingDefinition(); // currently always an attribute
            var correlatorDefBean = itemProcDef.getCorrelatorDefinition();
            if (correlatorDefBean != null) {
                if (cloned == null) {
                    cloned = explicitDefinition.clone();
                }
                addCorrelatorFromAttribute(cloned, itemProcDef, correlatorDefBean, objectInboundProcDef, resource);
            }
        }
        return cloned != null ? cloned : explicitDefinition;
    }

    private static void addCorrelatorFromAttribute(
            @NotNull CorrelationDefinitionType overallCorrelationDefBean,
            @NotNull ItemInboundProcessingDefinition itemProcDef,
            @NotNull ItemCorrelatorDefinitionType attributeCorrelatorDefBean,
            @NotNull ResourceObjectInboundProcessingDefinition objectProcDef,
            @NotNull ResourceType resource) throws ConfigurationException {
        List<InboundMappingType> inboundMappingBeans = itemProcDef.getInboundMappingBeans();
        configCheck(!inboundMappingBeans.isEmpty(),
                "Attribute-level correlation requires an inbound mapping; for %s in %s (%s)",
                itemProcDef, objectProcDef, resource);
        ItemPath focusItemPath = determineFocusItemPath(itemProcDef, attributeCorrelatorDefBean);
        configCheck(focusItemPath != null,
                "Item corresponding to correlation attribute %s couldn't be determined in %s (%s). You must specify"
                        + " it either explicitly, or provide exactly one inbound mapping with a proper target",
                itemProcDef, objectProcDef, resource);
        addSingleItemCorrelator(overallCorrelationDefBean, focusItemPath, attributeCorrelatorDefBean);
    }

    private static ItemPath determineFocusItemPath(
            @NotNull ItemInboundProcessingDefinition itemProcDef,
            @NotNull ItemCorrelatorDefinitionType attributeCorrelatorDefBean) {
        ItemPathType explicitItemPath = attributeCorrelatorDefBean.getFocusItem();
        if (explicitItemPath != null) {
            return explicitItemPath.getItemPath();
        } else {
            return guessFocusItemPath(itemProcDef);
        }
    }

    /** Tries to determine correlation (focus) item path from the inbound mapping target. */
    private static ItemPath guessFocusItemPath(ItemInboundProcessingDefinition itemProcDef) {
        List<InboundMappingType> inboundMappingBeans = itemProcDef.getInboundMappingBeans();
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
            return itemPath;
        }
        String localPart = variableName.getLocalPart();
        if (ExpressionConstants.VAR_FOCUS.equals(localPart)
                || ExpressionConstants.VAR_USER.equals(localPart)) {
            return itemPath.rest();
        } else {
            LOGGER.warn("Mapping target variable name '{}' is not supported for determination of correlation item path in {}",
                    variableName, itemProcDef);
            return null;
        }
    }

    private static @NotNull CorrelationDefinitionType getCorrelationDefinitionBean(
            @Nullable ObjectSynchronizationType synchronizationBean) {
        if (synchronizationBean == null) {
            return new CorrelationDefinitionType();
        }
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

    private static @NotNull FilterSubCorrelatorType createFilterCorrelator(
            List<ConditionalSearchFilterType> correlationFilters, ExpressionType confirmation) {
        FilterSubCorrelatorType filterCorrelator =
                new FilterSubCorrelatorType()
                        .confirmation(
                                CloneUtil.clone(confirmation));
        filterCorrelator.getOwnerFilter().addAll(
                CloneUtil.cloneCollectionMembers(correlationFilters));
        return filterCorrelator;
    }
}
