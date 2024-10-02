/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

//TODO remove duplication (TBD)
public class ObjectSimpleAttributeSelectionProvider extends ChoiceProvider<ItemPathType> {
    @Serial private static final long serialVersionUID = 1L;

    QName complexType;

    public ObjectSimpleAttributeSelectionProvider(QName complexType) {
        this.complexType = complexType;
    }

    @Override
    public String getDisplayValue(ItemPathType value) {
        return getIdValue(value);
    }

    @Override
    public String getIdValue(ItemPathType value) {
        return value.toString();
    }

    @Override
    public void query(String text, int page, Response<ItemPathType> response) {

        List<String> choices = collectAvailableDefinitions(text);

        response.addAll(toChoices(choices));
    }

    @Override
    public Collection<ItemPathType> toChoices(Collection<String> values) {
        return values.stream()
                .map(value -> PrismContext.get().itemPathParser().asItemPathType(value))
                .collect(Collectors.toList());
    }

    public List<String> collectAvailableDefinitions(String input) {

        PrismContainerDefinition<?> userDef = PrismContext.get().getSchemaRegistry().findContainerDefinitionByType(complexType);

        List<ItemPath> paths = new ArrayList<>();
        for (ItemDefinition<?> def : userDef.getDefinitions()) {
            ItemPath itemPath = createPossibleAttribute(def);
            if (itemPath != null) {
                paths.add(itemPath);
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

    private static ItemPath createPossibleAttribute(ItemDefinition<?> def) {
        //TODO we want extension references, but maybe we can somehow filter relevant defs from static schema?
        if (def instanceof PrismReferenceDefinition refDef && !refDef.isOperational()) {
            return refDef.getItemName();
        }
        if (def instanceof PrismPropertyDefinition<?> propertyDef) {
            if (RoleAnalysisAttributeDefUtils.isSupportedPropertyType(propertyDef.getTypeClass())
                    && !propertyDef.isOperational()) { // TODO differentiate searchable items && def.isSearchable()) {
                return propertyDef.getItemName();
            }
        }
        if (def instanceof PrismContainerDefinition<?> containerDef) {
            ItemPath itemName = containerDef.getItemName();
            for (ItemDefinition<?> itemDef : containerDef.getDefinitions()) {
                ItemPath additionalName = createPossibleAttribute(itemDef);
                if (additionalName != null) {
                    return ItemPath.create(itemName, additionalName);
                }
            }
        }
        return null;
    }
}
