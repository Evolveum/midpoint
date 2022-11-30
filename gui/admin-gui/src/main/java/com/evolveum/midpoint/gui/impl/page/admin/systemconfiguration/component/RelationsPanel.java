/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "relationsPanel")
@PanelInstance(
        identifier = "relationsPanel",
        applicableForType = RoleManagementConfigurationType.class,
        display = @PanelDisplay(
                label = "RelationsContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        )
)
@Counter(provider = RelationsCounter.class)
public class RelationsPanel extends BasePanel<AssignmentHolderDetailsModel> {

    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN = "main";
    private static final String ID_RELATIONS = "relations";

    private final ContainerPanelConfigurationType configuration;

    public RelationsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configuration) {
        super(id, new Model<>(model));

        this.configuration = configuration != null ? configuration.clone() : null;

        if (configuration != null) {
            configuration.setPath(new ItemPathType(ItemPath.create(
                    SystemConfigurationType.F_ROLE_MANAGEMENT,
                    RoleManagementConfigurationType.F_RELATIONS)));
        }

        initLayout();
    }

    private void initLayout() {
        PrismContainerWrapperModel model = PrismContainerWrapperModel.fromContainerWrapper(getModelObject().getObjectWrapperModel(),
                ItemPath.create(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_RELATIONS));

        SingleContainerPanel main = new SingleContainerPanel(ID_MAIN, model, RelationsDefinitionType.COMPLEX_TYPE) {

            @Override
            protected void onInitialize() {
                PrismContainerWrapper wrapper = model.getObject();
                wrapper.setExpanded(true);
                wrapper.setShowEmpty(true, true);
                wrapper.getValues().forEach(v -> ((PrismContainerValueWrapper) v).setExpanded(true));

                super.onInitialize();
            }

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                ItemPath path = itemWrapper.getPath();
                if (path != null && path.equals(ItemPath.create(
                        SystemConfigurationType.F_ROLE_MANAGEMENT,
                        RoleManagementConfigurationType.F_RELATIONS,
                        RelationsDefinitionType.F_RELATION))) {
                    return ItemVisibility.HIDDEN;
                }

                return ItemVisibility.AUTO;
            }
        };
        add(main);

        RelationsContentPanel relations = new RelationsContentPanel(ID_RELATIONS, getModelObject(), configuration);
        add(relations);
    }
}
