/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

@PanelType(name = "activation")
@PanelInstance(
        identifier = "activation",
        applicableForType = FocusType.class,
        display = @PanelDisplay(
                label = "ActivationType.title",
                icon = GuiStyleConstants.CLASS_LIFECYCLE_ICON,
                order = 40
        ),
        containerPath = "activation",
        type = "ActivationType",
        expanded = true
)
public class FocusActivationPanel<F extends FocusType, FDM extends FocusDetailsModels<F>> extends AbstractObjectMainPanel<F, FDM> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(FocusActivationPanel.class);

    private static final String ID_PANEL = "panel";

    private static final String DOT_CLASS = FocusActivationPanel.class.getName() + ".";

    public FocusActivationPanel(String id, FDM model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        // path is defined via PanelInstance -> an therefore via panelConfiguration (virtual container)
        SingleContainerPanel panel =
                new SingleContainerPanel<>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.EMPTY_PATH),
                        getPanelConfiguration());
        add(panel);
    }
}
