/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.DefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Renders an autoassign mapping container. Hides the remove button on {@code target}, since it's
 * the role's own assignment and can't be removed.
 *
 * @author jjarabinec
 */
public class AutoassignMappingContainerPanel
        extends DefaultContainerablePanel<MappingType, PrismContainerValueWrapper<MappingType>> {

    public AutoassignMappingContainerPanel(
            String id,
            IModel<PrismContainerValueWrapper<MappingType>> model,
            ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected ItemPanelSettings createItemSettings(ItemWrapper<?, ?> itemWrapper) {
        if (!isTargetItem(itemWrapper)) {
            return super.createItemSettings(itemWrapper);
        }
        return settingsWithHiddenRemoveButton();
    }

    private boolean isTargetItem(ItemWrapper<?, ?> itemWrapper) {
        return MappingType.F_TARGET.equivalent(itemWrapper.getItemName());
    }

    private ItemPanelSettings settingsWithHiddenRemoveButton() {
        ItemPanelSettings settings = getSettings();
        if (settings == null) {
            return null;
        }

        return new ItemPanelSettingsBuilder()
                .editabilityHandler(settings.getEditabilityHandler())
                .visibilityHandler(settings.getVisibilityHandler())
                .mandatoryHandler(settings.getMandatoryHandler())
                .displayedInColumn(settings.isDisplayedInColumn())
                .isRemoveButtonVisible(false)
                .panelConfiguration(settings.getConfig())
                .build();
    }
}
