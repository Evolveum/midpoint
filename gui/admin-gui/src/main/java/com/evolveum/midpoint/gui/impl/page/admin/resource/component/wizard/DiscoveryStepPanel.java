/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.provisioning.api.DiscoveredConfiguration;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "discoverConnectorConfigurationWizard")
@PanelInstance(identifier = "discoverConnectorConfigurationWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.discovery", icon = "fa fa-list-check"),
        containerPath = "connectorConfiguration/configurationProperties",
        expanded = true)
public class DiscoveryStepPanel extends AbstractResourceWizardStepPanel {

    private static final Trace LOGGER = TraceManager.getTrace(DiscoveryStepPanel.class);

    private static final String DOT_CLASS = DiscoveryStepPanel.class.getName() + ".";
    private static final String OPERATION_DISCOVER_CONFIGURATION = DOT_CLASS + "discoverConfiguration";
    private static final String OPERATION_RESOURCE_TEST = DOT_CLASS + "partialConfigurationTest";

    private static final String PANEL_TYPE = "discoverConnectorConfigurationWizard";

    public DiscoveryStepPanel(ResourceDetailsModel model) {
        super(model);
    }

    @Override
    protected void onBeforeRender() {
        PageBase pageBase = getPageBase();
        OperationResult result = new OperationResult(OPERATION_DISCOVER_CONFIGURATION);

        try {
            DiscoveredConfiguration discoverProperties = pageBase.getModelService().discoverResourceConnectorConfiguration(
                            getResourceModel().getObjectWrapper().getObjectApplyDelta(), result);

            getResourceModel().reloadPrismObjectModel(getResourceModel().getObjectWrapper().getObjectApplyDelta());
            getResourceModel().setConnectorConfigurationSuggestions(discoverProperties);
            getResourceModel().getObjectWrapperModel().reset();

        } catch (SchemaException e) {
            result.recordFatalError("Couldn't get discovered configuration.", e);
        }

        super.onBeforeRender();
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-list-check";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.discovery");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.discovery.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.discovery.subText");
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return w -> {
            if (w.isMandatory()) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask(OPERATION_RESOURCE_TEST);
        OperationResult result = task.getResult();

        try {
            pageBase.getModelService().testResource(getResourceModel().getObjectWrapper().getObjectApplyDelta(), task, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to test resource connection", e);
            result.recordFatalError(getString("TestConnectionMessagesPanel.message.testConnection.fatalError"), e);
        }
        result.computeStatus();

        if (result.isSuccess()) {
            return super.onNextPerformed(target);
        }
        pageBase.showResult(result);
        target.add(getFeedback());

        return false;
    }
}
