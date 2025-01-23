/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeRuleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

public class ClusteringAttributeSelectionProvider extends ChoiceProvider<ClusteringAttributeRuleType> {
    @Serial private static final long serialVersionUID = 1L;

    QName complexType;
    private final Map<String, ItemPathDto> choices = new HashMap<>();

    public ClusteringAttributeSelectionProvider(QName complexType) {
        this.complexType = complexType;
    }

    @Override
    public String getDisplayValue(ClusteringAttributeRuleType value) {
        return getIdValue(value);
    }

    @Override
    public String getIdValue(ClusteringAttributeRuleType value) {
        return value.getPath().toString();
    }

    @Override
    public void query(String text, int page, Response<ClusteringAttributeRuleType> response) {
        collectAvailableDefinitions(text);
        response.addAll(toChoices(choices.keySet()));
    }

    @Override
    public Collection<ClusteringAttributeRuleType> toChoices(@NotNull Collection<String> values) {
        return values.stream()
                .map(value -> {
                    ItemPathDto itemPathDto = choices.get(value);
                    if (itemPathDto != null) {
                        ItemPathType itemPathType = PrismContext.get().itemPathParser().asItemPathType(value);
                        boolean isMultiValue = itemPathDto.getItemDef().isMultiValue();
                        return createNewValue(itemPathType, isMultiValue);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private @NotNull ClusteringAttributeRuleType createNewValue(ItemPathType path, boolean isMultiValue) {
        ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType();
        rule.path(path)
                .similarity(100.0)
                .isMultiValue(isMultiValue)
                .weight(1.0);

        return rule;
    }

    public void collectAvailableDefinitions(String input) {
        choices.clear();
        PrismContainerDefinition<?> userDef = PrismContext.get().getSchemaRegistry().findContainerDefinitionByType(complexType);

        for (ItemDefinition<?> def : userDef.getDefinitions()) {
            @Nullable Set<ItemPathDto> itemPathSet = createPossibleAttribute(def);
            if (itemPathSet != null) {
                itemPathSet.forEach(pathDto -> loadPathDtoData(input, pathDto));
            }
        }
    }

    private void loadPathDtoData(String input, @NotNull ItemPathDto itemPathDto) {
        ItemPath itemPath = itemPathDto.toItemPath();
        if (!StringUtils.isBlank(input)) {
            if (itemPathDto.toItemPath().toString().contains(input)) {
                choices.put(itemPath.toString(), itemPathDto);
            }
        } else {
            choices.put(itemPath.toString(), itemPathDto);
        }
    }

    private static @Nullable Set<ItemPathDto> createPossibleAttribute(ItemDefinition<?> def) {
        Set<ItemPathDto> paths = new HashSet<>();
        //TODO we want extension references, but maybe we can somehow filter relevant defs from static schema?
        // Think about !refDef.isOperational() and searchable items.
        if (def instanceof PrismReferenceDefinition refDef) {
            return Collections.singleton(new ItemPathDto(refDef.getItemName(), refDef, refDef.getTypeName()));
        }

        if (def instanceof PrismPropertyDefinition<?> propertyDef
                && RoleAnalysisAttributeDefUtils.isSupportedPropertyType(propertyDef.getTypeClass())
                && !propertyDef.isOperational()
                && propertyDef.isSingleValue()) { // TODO differentiate searchable items && def.isSearchable()) {
            return Collections.singleton(new ItemPathDto(propertyDef.getItemName(), propertyDef, def.getTypeName()));
        }

        if (def instanceof PrismContainerDefinition<?> containerDef) {
            @Nullable Set<ItemPathDto> possibleAttributeFromContainerDef = createPossibleAttributeFromContainerDef(containerDef);
            if (possibleAttributeFromContainerDef != null && !possibleAttributeFromContainerDef.isEmpty()) {
                paths.addAll(possibleAttributeFromContainerDef);
                return paths;
            }
        }

        return null;
    }

    private static @Nullable Set<ItemPathDto> createPossibleAttributeFromContainerDef(
            @NotNull PrismContainerDefinition<?> containerDef) {
        Set<ItemPathDto> paths = new HashSet<>();
        if (containerDef.isMultiValue()) {
            return null;
        }

        for (ItemDefinition<?> def : containerDef.getDefinitions()) {

            if (def instanceof PrismReferenceDefinition refDef) {
                paths.add(new ItemPathDto(ItemPath.create(containerDef.getItemName(), refDef.getItemName()),
                        refDef, containerDef.getTypeName()));
            }

            if (def instanceof PrismPropertyDefinition<?> propertyDef
                    && RoleAnalysisAttributeDefUtils.isSupportedPropertyType(propertyDef.getTypeClass())
                    && !propertyDef.isOperational()
                    && propertyDef.isSingleValue()) {
                paths.add(new ItemPathDto(ItemPath.create(containerDef.getItemName(), propertyDef.getItemName()),
                        propertyDef, containerDef.getTypeName()));
            }

        }
        return paths;
    }

}
