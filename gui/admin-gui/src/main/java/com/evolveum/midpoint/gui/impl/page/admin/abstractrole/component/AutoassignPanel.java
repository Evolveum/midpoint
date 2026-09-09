/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Panel telling whether the role assigns itself and under which condition.
 *
 * @author jjarabinec
 */
@PanelType(name = "autoassign", defaultContainerPath = "autoassign")
@PanelInstance(identifier = "autoassign",
        applicableForType = AbstractRoleType.class,
        display = @PanelDisplay(label = "AutoassignPanel.label", order = 120))
public class AutoassignPanel<AR extends AbstractRoleType> extends AbstractObjectMainPanel<AR, FocusDetailsModels<AR>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_AUTOASSIGN = "autoassign";

    private static final ItemPath FOCUS_PATH = ItemPath.create(
            AbstractRoleType.F_AUTOASSIGN, AutoassignSpecificationType.F_FOCUS);

    private static final ItemPath MAPPING_PATH = FOCUS_PATH.append(FocalAutoassignSpecificationType.F_MAPPING);
    private static final ItemPath SELECTOR_PATH = FOCUS_PATH.append(FocalAutoassignSpecificationType.F_SELECTOR);
    private static final ItemPath TARGET_PATH = MAPPING_PATH.append(MappingType.F_TARGET);
    private static final ItemPath TARGET_SET_PATH = TARGET_PATH.append(VariableBindingDefinitionType.F_SET);

    private static final List<ItemName> VISIBLE_MAPPING_ITEMS = List.of(
            MappingType.F_NAME,
            MappingType.F_DESCRIPTION,
            MappingType.F_LIFECYCLE_STATE,
            MappingType.F_STRENGTH,
            MappingType.F_SOURCE,
            MappingType.F_CONDITION,
            MappingType.F_TARGET);

    private static final List<ItemName> VISIBLE_SELECTOR_ITEMS = List.of(
            ObjectSelectorType.F_NAME,
            ObjectSelectorType.F_DESCRIPTION,
            ObjectSelectorType.F_TYPE,
            ObjectSelectorType.F_FILTER,
            ObjectSelectorType.F_ARCHETYPE_REF,
            ObjectSelectorType.F_ORG_REF);

    private static final List<ItemName> VISIBLE_TARGET_ITEMS = List.of(
            VariableBindingDefinitionType.F_SET);

    private static final List<ItemName> VISIBLE_TARGET_SET_ITEMS = List.of(
            ValueSetDefinitionType.F_PREDEFINED);

    public AutoassignPanel(String id, FocusDetailsModels<AR> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel(ID_AUTOASSIGN, getObjectWrapperModel(), getPanelConfiguration()) {

                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        return AutoassignPanel.this.getVisibility(itemWrapper);
                    }
                };
        add(panel);
    }

    private ItemVisibility getVisibility(ItemWrapper<?, ?> itemWrapper) {
        if (isHiddenByConfiguration(itemWrapper)) {
            return ItemVisibility.HIDDEN;
        }

        ItemPath path = itemWrapper.getPath().namedSegmentsOnly();

        if (isDirectChildOf(path, MAPPING_PATH)) {
            return visibility(VISIBLE_MAPPING_ITEMS, itemWrapper);
        }
        if (isDirectChildOf(path, SELECTOR_PATH)) {
            return visibility(VISIBLE_SELECTOR_ITEMS, itemWrapper);
        }
        if (isDirectChildOf(path, TARGET_PATH)) {
            return visibility(VISIBLE_TARGET_ITEMS, itemWrapper);
        }
        if (isDirectChildOf(path, TARGET_SET_PATH)) {
            return visibility(VISIBLE_TARGET_SET_ITEMS, itemWrapper);
        }
        return ItemVisibility.AUTO;
    }

    private boolean isDirectChildOf(ItemPath path, ItemPath parent) {
        return path.startsWith(parent) && path.size() == parent.size() + 1;
    }

    private ItemVisibility visibility(List<ItemName> visibleItems, ItemWrapper<?, ?> itemWrapper) {
        boolean visible = visibleItems.stream()
                .anyMatch(name -> QNameUtil.match(name, itemWrapper.getItemName()));
        return visible ? ItemVisibility.AUTO : ItemVisibility.HIDDEN;
    }

    private boolean isHiddenByConfiguration(ItemWrapper<?, ?> itemWrapper) {
        ContainerPanelConfigurationType config = getPanelConfiguration();
        if (config == null) {
            return false;
        }

        for (VirtualContainersSpecificationType container : config.getContainer()) {
            if (container.getPath() != null
                    && itemWrapper.getPath().equivalent(container.getPath().getItemPath())
                    && !WebComponentUtil.getElementVisibility(container.getVisibility())) {
                return true;
            }
        }
        return false;
    }
}
