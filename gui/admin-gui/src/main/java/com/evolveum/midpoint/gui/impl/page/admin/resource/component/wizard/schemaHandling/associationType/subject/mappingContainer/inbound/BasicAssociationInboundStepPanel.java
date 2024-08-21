/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-association-inbound-basic",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.associationType.inbound.basicSettings", icon = "fa fa-circle"))
public class BasicAssociationInboundStepPanel
        extends AbstractValueFormResourceWizardStepPanel<MappingType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-association-inbound-basic";

    public BasicAssociationInboundStepPanel(
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<MappingType>> newValueModel) {
        super(model, newValueModel, null);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.associationType.inbound.basicSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.inbound.basicSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.inbound.basicSettings.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(MappingType.F_SOURCE)
                    || wrapper.getItemName().equals(MappingType.F_TARGET)
                    || wrapper.getItemName().equals(MappingType.F_EXPRESSION)
                    || wrapper.getItemName().equals(MappingType.F_CONDITION)
                    || wrapper.getItemName().equals(MappingType.F_ENABLED)
                    || wrapper.getItemName().equals(MappingType.F_TIME_FROM)
                    || wrapper.getItemName().equals(MappingType.F_TIME_TO)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }
}
