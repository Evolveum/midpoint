/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "activation")
@PanelInstance(identifier = "activation",
        applicableForType = FocusType.class,
        display = @PanelDisplay(label = "ActivationType.title", order = 40))
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
        SingleContainerPanel panel =
                new SingleContainerPanel<>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), FocusType.F_ACTIVATION),
                        PasswordType.COMPLEX_TYPE);
        add(panel);
    }
}
