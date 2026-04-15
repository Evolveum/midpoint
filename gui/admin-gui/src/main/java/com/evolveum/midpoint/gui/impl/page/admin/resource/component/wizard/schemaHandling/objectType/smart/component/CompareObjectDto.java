/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;

/**
 * DTO that represents a comparison context between a "primary" (compared) container value
 * and a collection of candidate values, based on a specific identifier path.
 *
 * @param <C> Type of the container object being compared.
 */
public class CompareObjectDto<C extends Containerable> implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Represents one compared or candidate item in the comparison list.
     */
    public record ComparedItem<C extends Containerable>(
            @NotNull String identifier,
            @NotNull PrismContainerValueWrapper<C> value,
            boolean isPrimary) implements Serializable {

        @Serial private static final long serialVersionUID = 1L;
    }

    private final List<ItemPath> comparedPaths;
    private final List<ComparedItem<C>> comparedItems;
    private final ComparedItem<C> primaryItem;
    private final ItemPath identifierPath;
    private final Set<String> appliedIdentifiers = new HashSet<>();

    public CompareObjectDto(
            @NotNull IModel<PrismContainerValueWrapper<C>> comparedValue,
            @NotNull IModel<List<PrismContainerValueWrapper<C>>> candidateValues,
            @NotNull ItemPath identifierPath,
            List<ItemPath> comparedPaths) {

        this.comparedPaths = comparedPaths;
        this.identifierPath = identifierPath;
        this.primaryItem = buildPrimaryItem(comparedValue);
        this.comparedItems = buildComparedItemsList(candidateValues);
    }

    /**
     * Builds the primary compared item from the given model.
     */
    private @NotNull ComparedItem<C> buildPrimaryItem(@NotNull IModel<PrismContainerValueWrapper<C>> comparedValue) {
        PrismContainerValueWrapper<C> primary = comparedValue.getObject();
        String primaryId = extractIdentifier(primary, translate("CompareObjectDto.primaryObjectLabel"));
        return new ComparedItem<>(primaryId, primary, true);
    }

    /**
     * Builds a unified list of compared + candidate items.
     * The first item (isPrimary=true) is the comparedValue.
     */
    private @NotNull List<ComparedItem<C>> buildComparedItemsList(
            @NotNull IModel<List<PrismContainerValueWrapper<C>>> candidateValues) {
        List<ComparedItem<C>> list = new ArrayList<>();

        List<PrismContainerValueWrapper<C>> candidates = candidateValues.getObject();
        if (candidates != null && !candidates.isEmpty()) {
            int counter = 0;
            for (PrismContainerValueWrapper<C> candidate : candidates) {
                String identifier = extractIdentifier(
                        candidate, translate("CompareObjectDto.existingObjectLabel", ++counter));
                list.add(new ComparedItem<>(identifier, candidate, false));
            }
        }

        return list;
    }

    /**
     * Extracts the identifier from the given container value wrapper based on the identifier path.
     * If the identifier is blank, it uses the provided blankIdentifierName.
     * It also ensures uniqueness by appending a counter for duplicates.
     */
    protected String extractIdentifier(
            @NotNull PrismContainerValueWrapper<C> containerValueWrapper,
            @NotNull String blankIdentifierName) {
        try {
            PrismPropertyWrapper<?> property = containerValueWrapper.findProperty(identifierPath);
            if (property == null || property.getValue() == null) {
                return null;
            }
            Object realValue = property.getValue().getRealValue();

            String identifier = realValue != null ? realValue.toString() : null;
            if (StringUtils.isBlank(identifier)) {
                identifier = blankIdentifierName;
            }

            int duplicateCounter = 0;
            for (String id : appliedIdentifiers) {
                String normalizedId = id.replaceAll("\\s?\\(\\d+\\)$", "");
                if (normalizedId.equals(identifier)) {
                    duplicateCounter++;
                }
            }
            if (duplicateCounter > 0) {
                identifier = identifier + " (" + duplicateCounter + ")";
            }

            appliedIdentifiers.add(identifier);

            return identifier;
        } catch (SchemaException e) {
            throw new IllegalStateException(
                    "Couldn't extract identifier from container value wrapper: " + containerValueWrapper, e);
        }
    }

    public List<ComparedItem<C>> getComparedItems() {
        return comparedItems;
    }

    public List<ItemPath> getComparedPaths() {
        return comparedPaths;
    }

    public ComparedItem<C> getPrimaryItem() {
        return primaryItem;
    }
}
