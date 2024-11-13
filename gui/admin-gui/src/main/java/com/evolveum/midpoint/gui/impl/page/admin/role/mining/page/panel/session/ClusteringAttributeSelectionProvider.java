/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

public class ClusteringAttributeSelectionProvider extends ChoiceProvider<ClusteringAttributeRuleType> {
    @Serial private static final long serialVersionUID = 1L;

    QName complexType;

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

        List<String> choices = collectAvailableDefinitions(text);
        response.addAll(toChoices(choices));
    }

    @Override
    public Collection<ClusteringAttributeRuleType> toChoices(Collection<String> values) {
        return values.stream()
                .map(value -> PrismContext.get().itemPathParser().asItemPathType(value))
                .map(this::createNewValue)
                .collect(Collectors.toList());
    }

    private ClusteringAttributeRuleType createNewValue(ItemPathType path) {
        ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType();
        rule.path(path)
                .similarity(100.0)
                .weight(1.0);

        return rule;
    }

    public List<String> collectAvailableDefinitions(String input) {

        PrismContainerDefinition<UserType> userDef = PrismContext.get().getSchemaRegistry().findContainerDefinitionByType(complexType);

        List<ItemPath> paths = new ArrayList<>();
        for (ItemDefinition<?> def : userDef.getDefinitions()) {
            Set<ItemPath> itemPathSet = createPossibleAttribute(def);
            if(itemPathSet != null) {
                for (ItemPath path : itemPathSet) {
                    if (path != null) {
                        paths.add(path);
                    }
                }
            }
        }

        List<String> pathsAsString = paths.stream()
                .map(ItemPath::toString)
                .sorted()
                .toList();

        if (StringUtils.isBlank(input)) {
            return pathsAsString;
        }

        return pathsAsString.stream()
                .filter(path -> path.contains(input))
                .sorted()
                .toList();

    }

    private static @Nullable Set<ItemPath> createPossibleAttribute(ItemDefinition<?> def) {
        Set<ItemPath> paths = new HashSet<>();
        //TODO we want extension references, but maybe we can somehow filter relevant defs from static schema?
        // Think about !refDef.isOperational() and searchable items.
        if (def instanceof PrismReferenceDefinition refDef) {
            return Collections.singleton(refDef.getItemName());
        }

        if (def instanceof PrismPropertyDefinition<?> propertyDef
                && RoleAnalysisAttributeDefUtils.isSupportedPropertyType(propertyDef.getTypeClass())
                    && !propertyDef.isOperational()
                    && propertyDef.isSingleValue()) { // TODO differentiate searchable items && def.isSearchable()) {
                return Collections.singleton(propertyDef.getItemName());
            }

        if (def instanceof PrismContainerDefinition<?> containerDef) {
            Set<ItemPath> possibleAttributeFromContainerDef = createPossibleAttributeFromContainerDef(containerDef);
            if(possibleAttributeFromContainerDef != null && !possibleAttributeFromContainerDef.isEmpty()) {
                paths.addAll(possibleAttributeFromContainerDef);
                return paths;
            }
        }

        return null;
    }

    private static @Nullable Set<ItemPath> createPossibleAttributeFromContainerDef(
            @NotNull PrismContainerDefinition<?> containerDef){
        Set<ItemPath> paths = new HashSet<>();
        if (containerDef.isMultiValue()) {
            return null;
        }

        for (ItemDefinition<?> def : containerDef.getDefinitions()) {
            if (def instanceof PrismReferenceDefinition refDef) {
                paths.add(refDef.getItemName());
            }

            if (def instanceof PrismPropertyDefinition<?> propertyDef
                    && RoleAnalysisAttributeDefUtils.isSupportedPropertyType(propertyDef.getTypeClass())
                        && !propertyDef.isOperational()
                        && propertyDef.isSingleValue()) {
                    paths.add(propertyDef.getItemName());
                }

        }
        return paths;
    }

}
