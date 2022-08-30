/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ResourceAttributePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

public class AssignmentConstructionPanel extends BasePanel<PrismContainerWrapper<ConstructionType>> {

    private static final String ID_BASIC = "basic";
    private static final String ID_ATTRIBUTES = "attributes";

    private ContainerPanelConfigurationType config;

    public AssignmentConstructionPanel(String id, IModel<PrismContainerWrapper<ConstructionType>> model, ContainerPanelConfigurationType config) {
        super(id, model);
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        SingleContainerPanel<ConstructionType> baseConstructionPanel = new SingleContainerPanel<>(ID_BASIC, getModel(), ConstructionType.COMPLEX_TYPE) {

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                ItemPath itemPath = itemWrapper.getPath();
                if (isAssociation(itemPath)) {
                    return ItemVisibility.HIDDEN;
                }

                if (isAttributePath(itemPath)) {
                    return ItemVisibility.HIDDEN;
                }

                if (ConstructionType.F_KIND.equivalent(itemPath.lastName())) {
                    return ItemVisibility.HIDDEN;
                }

                if (ConstructionType.F_INTENT.equivalent(itemPath.lastName())) {
                    return ItemVisibility.HIDDEN;
                }

                if (ConstructionType.F_RESOURCE_REF.equivalent(itemPath.lastName())) {
                    return ItemVisibility.HIDDEN;
                }
                return ItemVisibility.AUTO;
            }
        };
        add(baseConstructionPanel);
    }

    public ContainerPanelConfigurationType getConfig() {
        return config;
    }

    private boolean isAttributePath(ItemPath itemPath) {
        ItemPath assignmentAttribute = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE);
        ItemPath inducementAttribute = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE);
        ItemPath pathWithoutIds = itemPath.namedSegmentsOnly();
        return pathWithoutIds.equivalent(assignmentAttribute) || pathWithoutIds.equivalent(inducementAttribute);
    }

    private boolean isAssociation(ItemPath itemPath) {
        ItemPath assignmentAssociation = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION);
        ItemPath inducementAssociation = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION);
        ItemPath pathWithoutIds = itemPath.namedSegmentsOnly();
        return pathWithoutIds.equivalent(assignmentAssociation) || pathWithoutIds.equivalent(inducementAssociation);
    }
}
