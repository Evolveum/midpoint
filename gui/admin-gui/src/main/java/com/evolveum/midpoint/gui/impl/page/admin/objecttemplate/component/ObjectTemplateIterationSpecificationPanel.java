/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.component;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "iterationSpecification")
@PanelInstance(identifier = "iterationSpecification",
        applicableForType = ObjectTemplateType.class,
        display = @PanelDisplay(label = "pageObjectTemplate.iterationSpecification.title", order = 20))
public class ObjectTemplateIterationSpecificationPanel extends AbstractObjectMainPanel<ObjectTemplateType, ObjectDetailsModels<ObjectTemplateType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateIterationSpecificationPanel.class);
    private static final String ID_PANEL = "panel";

    private static final String DOT_CLASS = ObjectTemplateIterationSpecificationPanel.class.getName() + ".";

    public ObjectTemplateIterationSpecificationPanel(String id, AssignmentHolderDetailsModel<ObjectTemplateType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel<>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ObjectTemplateType.F_ITERATION_SPECIFICATION),
                        IterationSpecificationType.COMPLEX_TYPE);
        add(panel);
    }
}
