/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.panel.VariableBingingDefSearchItemPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import static com.evolveum.midpoint.prism.PrismConstants.VARIABLE_BINDING_DEF_MATCHING_RULE_NAME;

public class VariableBindingDefSearchItemWrapper extends PropertySearchItemWrapper<VariableBindingDefinitionType> {

    public VariableBindingDefSearchItemWrapper(ItemPath path) {
        super(path);
    }

    @Override
    public Class<VariableBingingDefSearchItemPanel> getSearchItemPanelClass() {
        return VariableBingingDefSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<VariableBindingDefinitionType> getDefaultValue() {
        return new SearchValue<>();
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        String itemPathValue = getValue().getLabel();
        if (itemPathValue == null) {
            return null;
        }

        ItemDefinition<?> def = PrismContext.get().getSchemaRegistry().findItemDefinitionByElementName(
                MappingType.F_TARGET);

        return PrismContext.get().queryFor(type)
                .item(getPath(), def)
                .contains(itemPathValue)
                .matching(VARIABLE_BINDING_DEF_MATCHING_RULE_NAME)
                .buildFilter();
    }
}
