/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.DefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Renders an autoassign mapping container. Little hack - hook on {@code target}, because range
 * isn't a property of the mapping itself, only a field nested inside {@code target} - so
 * {@code target} is the only row here we can actually hook on to reach it.
 *
 * And since that row is still labeled and behaves like "target", we relabel it and hide its remove
 * button.
 *
 * @author jjarabinec
 */
public class AutoassignMappingContainerPanel
        extends DefaultContainerablePanel<MappingType, PrismContainerValueWrapper<MappingType>> {

    private static final String KEY_TARGET_LABEL = "AutoassignPanel.mapping.target.label";

    public AutoassignMappingContainerPanel(
            String id,
            IModel<PrismContainerValueWrapper<MappingType>> model,
            ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void populateNonContainer(ListItem<? extends ItemWrapper<?, ?>> item) {
        ItemWrapper<?, ?> itemWrapper = item.getModelObject();
        if (!isTargetItem(itemWrapper)) {
            super.populateNonContainer(item);
            return;
        }

        item.setOutputMarkupId(true);
        relabelTargetItem(itemWrapper);
        item.add(new VisibleBehaviour(() -> itemWrapper.isVisible(getModelObject(), getVisibilityHandler())));
        item.add(createTargetPanel(item, itemWrapper));
    }

    private boolean isTargetItem(ItemWrapper<?, ?> itemWrapper) {
        return MappingType.F_TARGET.equivalent(itemWrapper.getItemName());
    }

    private void relabelTargetItem(ItemWrapper<?, ?> itemWrapper) {
        ((ItemWrapperImpl<?, ?>) itemWrapper).setDisplayName(getString(KEY_TARGET_LABEL));
    }

    private Panel createTargetPanel(ListItem<? extends ItemWrapper<?, ?>> item, ItemWrapper<?, ?> itemWrapper) {
        try {
            Panel panel = getParentPage().initItemPanel(
                    ID_PROPERTY, itemWrapper.getTypeName(), item.getModel(), settingsWithHiddenRemoveButton());
            panel.setOutputMarkupId(true);
            panel.add(AttributeAppender.replace("style", () -> getModelObject().isExpanded() ? "" : "display:none"));
            return panel;
        } catch (SchemaException e) {
            throw new SystemException("Cannot instantiate " + itemWrapper.getTypeName());
        }
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
