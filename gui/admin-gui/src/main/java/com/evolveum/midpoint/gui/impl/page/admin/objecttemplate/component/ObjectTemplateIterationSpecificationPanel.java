/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

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
        SingleContainerPanel panel = new SingleContainerPanel<>(ID_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ObjectTemplateType.F_ITERATION_SPECIFICATION),
                IterationSpecificationType.COMPLEX_TYPE) {

            @Override
            protected ItemMandatoryHandler getMandatoryHandler() {
                return wrapper -> getMandatoryOverrideFor(wrapper);
            }
        };

        add(panel);
    }

    private boolean getMandatoryOverrideFor(ItemWrapper<?, ?> itemWrapper) {
        ItemPath conflictResolutionPath = ItemPath.create(ObjectTemplateType.F_ITERATION_SPECIFICATION, IterationSpecificationType.F_MAX_ITERATIONS);
        if (conflictResolutionPath.equivalent(itemWrapper.getPath().namedSegmentsOnly())) {
            return false;
        }

        return itemWrapper.isMandatory();
    }
}
