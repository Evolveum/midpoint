/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update;

import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-object-class-update")
@PanelInstance(identifier = "cdw-object-class-update",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.objectClassUpdate", icon = "fa fa-wrench"),
        containerPath = "empty")
public class UpdateObjectClassConnectorStepPanel extends AbstractObjectClassConnectorStepPanel implements WizardParentStep {

    public static final String PANEL_TYPE = "cdw-object-class-update";

    public UpdateObjectClassConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public List<WizardStep> createChildrenSteps() {
        return List.of(
                new UpdateEndpointsConnectorStepPanel(getHelper(), getObjectClassModel()),
                new WaitingUpdateConnectorStepPanel(getHelper(), getObjectClassModel()),
                new UpdateScriptConnectorStepPanel(getHelper(), getObjectClassModel()));
    }

    @Override
    protected String getTitleKey() {
        return "PageConnectorDevelopment.wizard.step.objectClassUpdate";
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClassUpdate.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClassUpdate.subText");
    }
}
