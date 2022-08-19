/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "basicSettingAttributeWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.basicSetting", icon = "fa fa-circle"))
public class BasicSettingStepPanel extends AbstractValueFormResourceWizardStepPanel<ResourceAttributeDefinitionType> {

    private static final String PANEL_TYPE = "basicAttributeWizard";

    public BasicSettingStepPanel(ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.attributes.basicSetting");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.basicSetting.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.basicSetting.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }
}
