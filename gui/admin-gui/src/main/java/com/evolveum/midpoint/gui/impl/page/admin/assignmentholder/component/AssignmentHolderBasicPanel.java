/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelInstances;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "basic", defaultContainerPath = "empty")
@PanelInstances(instances = {
        @PanelInstance(identifier = "basic",
                applicableForType = FocusType.class,
                excludeTypes = {TaskType.class, ResourceType.class},
                defaultPanel = true,
                display = @PanelDisplay(label = "pageAdminFocus.basic", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 10)),
        @PanelInstance(identifier = "basic",
                applicableForType = CaseType.class,
                defaultPanel = true,
                display = @PanelDisplay(label = "pageAdminFocus.basic", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 30))
}
)
public class AssignmentHolderBasicPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    private static final String ID_MAIN_PANEL = "properties";
    private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderBasicPanel.class);
    private static final String ID_VIRTUAL_PANELS = "virtualPanels";

    public AssignmentHolderBasicPanel(String id, ObjectDetailsModels<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
//        try {

//            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder().visibilityHandler(w -> ItemVisibility.AUTO);
//            builder.headerVisibility(false);
//
//            Panel main = getPageBase().initItemPanel(ID_MAIN_PANEL, getModelObject().getTypeName(),
//                    PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.EMPTY_PATH), builder.build());
//            add(main);
//
//            RepeatingView view = new RepeatingView(ID_VIRTUAL_PANELS);
//            if (getPanelConfiguration() != null) {
//                List<VirtualContainersSpecificationType> virtualContainers = getPanelConfiguration().getContainer();
//                for (VirtualContainersSpecificationType virtualContainer : virtualContainers) {
//                    PrismContainerWrapperModel virtualContainerModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), virtualContainer.getIdentifier());
//                    Panel virtualPanel = new PrismContainerPanel<>(view.newChildId(), virtualContainerModel, builder.build());
//                    view.add(virtualPanel);
//                }
//
//            }
//            add(view);
            SingleContainerPanel panel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration());
            add(panel);
//        } catch (SchemaException e) {
//            LOGGER.error("Could not create focus details panel. Reason: {}", e.getMessage(), e);
//        }
    }


}
