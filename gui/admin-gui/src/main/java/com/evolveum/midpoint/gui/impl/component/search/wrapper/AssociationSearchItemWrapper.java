/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AssociationSearchItemWrapper extends ChoicesSearchItemWrapper<ItemName> {

    private final ResourceObjectDefinition objectDefinition;

    public AssociationSearchItemWrapper(ResourceObjectDefinition objectDefinition) {
        super(ItemPath.EMPTY_PATH, initAvailableValues(objectDefinition));
        this.objectDefinition = objectDefinition;
    }

    private static List<DisplayableValue<ItemName>> initAvailableValues(ResourceObjectDefinition objectDefinition) {
        if (objectDefinition == null) {
            return List.of();
        }
        List<? extends ShadowAssociationDefinition> associations = objectDefinition.getAssociationDefinitions();
        List<DisplayableValue<ItemName>> values = new ArrayList<>();
        associations.stream()
                .filter(association -> !association.isComplex())
                .forEach(association -> values.add(
                new SearchValue<>(
                        association.getItemName(),
                        GuiDisplayNameUtil.getDisplayName(association.getModernAssociationTypeDefinitionBean()))));
        return values;
    }

    @Override
    public DisplayableValue<ItemName> getDefaultValue() {
        if (getAvailableValues().size() >= 1) {
            return getAvailableValues().get(0);
        }
        return new SearchValue();
    }

    @Override
    public @NotNull IModel<String> getName() {
        return PageBase.createStringResourceStatic("ConstructionType.association");
    }

    @Override
    public boolean isEnabled() {
        return getAvailableValues().size() > 1;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        return ProvisioningObjectsUtil.getShadowTypeFilterForAssociation(objectDefinition, getValue().getValue());
    }

    public boolean allowNull() {
        return false;
    }

    @Override
    public boolean canRemoveSearchItem() {
        return false;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
