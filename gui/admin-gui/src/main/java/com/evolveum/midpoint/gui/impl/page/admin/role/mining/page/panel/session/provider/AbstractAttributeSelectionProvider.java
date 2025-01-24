/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.provider;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.component.search.SearchableItemsDefinitions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.search.Property;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractAttributeSelectionProvider<T> extends ChoiceProvider<T> {

    @Serial private static final long serialVersionUID = 1L;

    private final Map<String, T> availableAttributes = new HashMap<>();

    protected AbstractAttributeSelectionProvider(Class<?> complexType, ModelServiceLocator modelServiceLocator) {
        loadAvailableAttributes(complexType, modelServiceLocator);
    }

    @Override
    public String getDisplayValue(T value) {
        return getIdValue(value);
    }

    @Override
    public String getIdValue(T value) {
        return availableAttributes.entrySet().stream()
                .filter(entry -> isMatchingValue(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("N/A");
    }

    @Override
    public void query(String text, int page, @NotNull Response<T> response) {
        List<String> matchingDefinitions = collectAvailableDefinitions(text);
        response.addAll(toChoices(matchingDefinitions));
    }

    @Override
    public Collection<T> toChoices(@NotNull Collection<String> values) {
        return values.stream()
                .map(availableAttributes::get)
                .collect(Collectors.toList());
    }

    public List<String> collectAvailableDefinitions(String input) {
        if (StringUtils.isBlank(input)) {
            return availableAttributes.keySet().stream()
                    .sorted()
                    .toList();
        }

        return availableAttributes.keySet().stream()
                .filter(name -> name.toLowerCase().contains(input.toLowerCase()))
                .sorted()
                .toList();
    }

    private void loadAvailableAttributes(Class<?> complexType, ModelServiceLocator modelServiceLocator) {
        Map<ItemPath, ItemDefinition<?>> availableDefs = new SearchableItemsDefinitions(complexType, modelServiceLocator)
                .createAvailableSearchItems();

        availableDefs.forEach((path, def) -> {
            boolean isConditionMet = isDefinitionConditionMet(def);

            if (!isConditionMet) {
                return;
            }

            Property property = new Property(def, path);
            String name = property.getName();
            T attribute = createAttribute(path, def);
            availableAttributes.put(name, attribute);
        });
    }

    protected boolean isDefinitionConditionMet(ItemDefinition<?> def) {
        return includeMultivaluedDef() || !def.isMultiValue();
    }

    protected abstract boolean includeMultivaluedDef();

    protected abstract T createAttribute(ItemPath path, ItemDefinition<?> definition);

    protected abstract boolean isMatchingValue(T attribute, T value);

    protected @NotNull String simpleValue(@NotNull ItemPathType value) {
        return value.toString().toLowerCase();
    }
}
