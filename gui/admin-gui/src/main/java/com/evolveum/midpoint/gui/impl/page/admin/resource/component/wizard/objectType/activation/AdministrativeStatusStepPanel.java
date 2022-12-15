/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */

@Experimental
@PanelInstance(identifier = "rw-activation-administrativeStatus",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.activation.administrativeStatus", icon = "fa fa-toggle-off"),
        expanded = true)
public abstract class AdministrativeStatusStepPanel extends ActivationMappingStepPanel {

    public static final String PANEL_TYPE = "rw-activation-administrativeStatus";

    public AdministrativeStatusStepPanel(ResourceDetailsModel model,
                                         IModel<PrismContainerWrapper<ResourceBidirectionalMappingType>> containerModel) {
        super(model, containerModel);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

//    private String getIcon() {
//        return "fa fa-toggle-off";
//    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.activation.administrativeStatus");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.activation.administrativeStatus.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.activation.administrativeStatus.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }
}
