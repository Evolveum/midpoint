/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.application.component.wizard.basic;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApplicationType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author lskublik
 */
@PanelType(name = "appw-basic")
@PanelInstance(identifier = "appw-basic",
        applicableForType = ApplicationType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageApplication.wizard.step.basicInformation", icon = "fa fa-wrench"),
        containerPath = "empty")
public class BasicInformationApplicationStepPanel extends AbstractFormWizardStepPanel<AbstractRoleDetailsModel<ApplicationType>> {

    private static final String PANEL_TYPE = "appw-basic";

    public BasicInformationApplicationStepPanel(AbstractRoleDetailsModel<ApplicationType> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        getDetailsModel().getObjectWrapper().setShowEmpty(false, false);
        getDetailsModel().getObjectWrapper().getValues().forEach(valueWrapper -> valueWrapper.setShowEmpty(false));
        super.onInitialize();
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageApplication.wizard.step.basicInformation");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageApplication.wizard.step.basicInformation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageApplication.wizard.step.basicInformation.subText");
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ApplicationType.F_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
