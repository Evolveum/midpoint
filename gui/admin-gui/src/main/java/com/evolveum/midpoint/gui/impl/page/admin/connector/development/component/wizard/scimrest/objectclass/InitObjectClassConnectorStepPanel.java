/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.*;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllScriptConnectorStepPanel;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllEndpointsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllObjectsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.WaitingSearchAllConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-init-object-class")
@PanelInstance(identifier = "cdw-init-object-class",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.initObjectClass", icon = "fa fa-wrench"),
        containerPath = "empty")
public class InitObjectClassConnectorStepPanel extends AbstractObjectClassConnectorStepPanel implements WizardParentStep {

    public static final String PANEL_TYPE = "cdw-init-object-class";

    public InitObjectClassConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public List<WizardStep> createChildrenSteps() {
        return List.of(
                new ObjectClassSelectConnectorStepPanel(getHelper(), getObjectClassModel()),
                new WaitingObjectClassDetailsConnectorStepPanel(getHelper(), getObjectClassModel()),
                new WaitingNativeSchemaConnectorStepPanel(getHelper(), getObjectClassModel()),
                new WaitingConnIdSchemaConnectorStepPanel(getHelper(), getObjectClassModel()),
                new SchemaScriptConnectorStepPanel(getHelper(), getObjectClassModel()),
                new ShowSchemaConnectorStepPanel(getHelper(), getObjectClassModel()),
                new SearchAllEndpointsConnectorStepPanel(getHelper(), getObjectClassModel()),
                new WaitingSearchAllConnectorStepPanel(getHelper(), getObjectClassModel()),
                new SearchAllScriptConnectorStepPanel(getHelper(), getObjectClassModel()),
                new SearchAllObjectsConnectorStepPanel(getHelper(), getObjectClassModel()));
    }

    @Override
    protected String getTitleKey() {
        return "PageConnectorDevelopment.wizard.step.initObjectClass";
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.initObjectClass.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.initObjectClass.subText");
    }
}
