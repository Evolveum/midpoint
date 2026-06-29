/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.DiscoveredConfiguration;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

/**
 * Utility methods for discovering and applying connector configuration suggestions
 * to resource configuration properties.
 */
public final class ResourceConfigurationDiscoveryUtil {

    private static final ItemPath CONFIGURATION_PROPERTIES_PATH =
            ItemPath.create("connectorConfiguration", "configurationProperties");

    private static final String MANAGED_ASSOCIATION_PAIRS = "managedAssociationPairs";

    private ResourceConfigurationDiscoveryUtil() {
    }

    /**
     * Discovers connector configuration suggestions and applies them to the resource wrapper.
     */
    public static void discoverAndApply(
            @NotNull PageBase pageBase,
            @NotNull PrismObjectWrapper<ResourceType> resourceWrapper,
            @NotNull OperationResult result) throws CommonException {

        Set<QName> schemaObjectClassNames = getSchemaObjectClassNames(resourceWrapper, result);

        DiscoveredConfiguration discoveredConfiguration =
                pageBase.getModelService().discoverResourceConnectorConfiguration(
                        resourceWrapper.getObjectApplyDelta(),
                        result);

        applyDiscoveredConfiguration(resourceWrapper, discoveredConfiguration, schemaObjectClassNames);
    }

    /**
     * Applies discovered configuration suggestions to matching configuration properties.
     */
    public static void applyDiscoveredConfiguration(
            @NotNull PrismObjectWrapper<ResourceType> resourceWrapper,
            @Nullable DiscoveredConfiguration discoveredConfiguration,
            @NotNull Set<QName> schemaObjectClassNames) {

        if (discoveredConfiguration == null) {
            return;
        }

        for (PrismProperty<?> suggestion : discoveredConfiguration.getDiscoveredProperties()) {
            PrismPropertyDefinition<?> suggestionDef = suggestion.getDefinition();

            PrismPropertyWrapper<Object> item = findConfigurationProperty(resourceWrapper, suggestionDef);
            if (item == null) {
                continue;
            }

            applyAllowedValues(item, suggestionDef);

            if (MANAGED_ASSOCIATION_PAIRS.equals(suggestionDef.getItemName().getLocalPart())) {
                applySuggestedValuesForManagedAssociationPairs(item, suggestionDef, schemaObjectClassNames);
            } else {
                applySuggestedValues(item, suggestionDef);
            }

            item.mutator().setDisplayOrder(100);
            item.mutator().setEmphasized(true);
        }
    }

    private static @Nullable PrismPropertyWrapper<Object> findConfigurationProperty(
            @NotNull PrismObjectWrapper<ResourceType> resourceWrapper,
            @NotNull PrismPropertyDefinition<?> suggestionDef) {

        try {
            return resourceWrapper.findProperty(
                    CONFIGURATION_PROPERTIES_PATH.append(suggestionDef.getItemName()));
        } catch (SchemaException e) {
            throw new IllegalStateException(
                    "Error while applying discovered configuration: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyAllowedValues(
            @NotNull PrismPropertyWrapper<Object> item,
            @NotNull PrismPropertyDefinition<?> suggestionDef) {

        Collection<?> rawAllowedValues = suggestionDef.getAllowedValues();

        if (rawAllowedValues == null || rawAllowedValues.isEmpty()) {
            return;
        }

        Collection<? extends DisplayableValue<Object>> allowedValues =
                (Collection<? extends DisplayableValue<Object>>) rawAllowedValues;

        item.mutator().setAllowedValues(allowedValues);
        applySingleValueIfEmpty(item, allowedValues);
    }

    @SuppressWarnings("unchecked")
    private static void applySuggestedValues(
            @NotNull PrismPropertyWrapper<Object> item,
            @NotNull PrismPropertyDefinition<?> suggestionDef) {

        Collection<?> rawSuggestedValues = suggestionDef.getSuggestedValues();

        if (rawSuggestedValues == null || rawSuggestedValues.isEmpty()) {
            return;
        }

        Collection<? extends DisplayableValue<Object>> suggestedValues =
                (Collection<? extends DisplayableValue<Object>>) rawSuggestedValues;

        item.mutator().setSuggestedValues(suggestedValues);
        applySingleValueIfEmpty(item, suggestedValues);
    }

    /**
     * Applies managedAssociationPairs suggestions filtered to existing schema object classes.
     */
    @SuppressWarnings("unchecked")
    private static void applySuggestedValuesForManagedAssociationPairs(
            @NotNull PrismPropertyWrapper<Object> item,
            @NotNull PrismPropertyDefinition<?> suggestionDef,
            @NotNull Set<QName> schemaObjectClassNames) {

        Collection<?> rawSuggestedValues = suggestionDef.getSuggestedValues();

        if (rawSuggestedValues == null || rawSuggestedValues.isEmpty()) {
            return;
        }

        Collection<? extends DisplayableValue<Object>> suggestedValues =
                (Collection<? extends DisplayableValue<Object>>) rawSuggestedValues;

        Collection<? extends DisplayableValue<Object>> filteredSuggestedValues =
                suggestedValues.stream()
                        .filter(value -> referencesExistingObjectClass(
                                value, schemaObjectClassNames))
                        .toList();

        if (filteredSuggestedValues.isEmpty()) {
            return;
        }

        item.mutator().setSuggestedValues(filteredSuggestedValues);
        applySingleValueIfEmpty(item, filteredSuggestedValues);
    }

    private static void applySingleValueIfEmpty(
            @NotNull PrismPropertyWrapper<Object> item,
            @NotNull Collection<? extends DisplayableValue<Object>> values) {

        if (values.size() == 1 && item.isEmpty()) {
            item.getValues().iterator().next().setRealValue(
                    values.iterator().next().getValue());
        }
    }

    /**
     * Returns object class names available in the resource schema.
     */
    public static @NotNull Set<QName> getSchemaObjectClassNames(
            @NotNull PrismObjectWrapper<ResourceType> resourceWrapper,
            @NotNull OperationResult result) {

        NativeResourceSchema schema;

        try {
            schema = Resource.of(resourceWrapper.getObject()).getNativeResourceSchemaRequired();
        } catch (Exception e) {
            result.recordPartialError(
                    "Couldn't get native resource schema for resource " + resourceWrapper.getObject(), e);
            return Set.of();
        }

        return schema.getObjectClassDefinitions().stream()
                .map(def -> new QName(NS_RI, def.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Checks whether both object classes referenced by a managedAssociationPairs value
     * exist in the resource schema.
     */
    private static boolean referencesExistingObjectClass(
            @NotNull DisplayableValue<Object> value,
            @NotNull Set<QName> schemaObjectClassNames) {

        if (schemaObjectClassNames.isEmpty()) {
            return true;
        }

        Object realValue = value.getValue();
        if (realValue == null) {
            return false;
        }

        String valueText = String.valueOf(realValue);

        String[] sides = valueText.split("-#");
        if (sides.length != 2) {
            return false;
        }

        String leftObjectClass = extractObjectClassName(sides[0]);
        String rightObjectClass = extractObjectClassName(sides[1]);

        return existsInSchema(leftObjectClass, schemaObjectClassNames)
                && existsInSchema(rightObjectClass, schemaObjectClassNames);
    }

    private static @Nullable String extractObjectClassName(String side) {
        if (side == null) {
            return null;
        }

        String normalized = side.trim();

        int plusIndex = normalized.indexOf('+');
        if (plusIndex < 0) {
            return null;
        }

        String objectClassName = normalized.substring(0, plusIndex).trim();

        if (objectClassName.startsWith("\"") && objectClassName.endsWith("\"") && objectClassName.length() > 1) {
            objectClassName = objectClassName.substring(1, objectClassName.length() - 1);
        }

        return objectClassName;
    }

    private static boolean existsInSchema(
            @Nullable String objectClassName,
            @NotNull Set<QName> schemaObjectClassNames) {

        if (objectClassName == null || objectClassName.isBlank()) {
            return false;
        }

        return schemaObjectClassNames.stream()
                .map(QName::getLocalPart)
                .anyMatch(objectClassName::equals);
    }

}
