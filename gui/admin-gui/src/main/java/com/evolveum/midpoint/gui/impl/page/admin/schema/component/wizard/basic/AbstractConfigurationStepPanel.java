/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.basic;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

/**
 * @author lskublik
 */
public abstract class AbstractConfigurationStepPanel extends AbstractFormWizardStepPanel<ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractConfigurationStepPanel.class);

    private static final String OPERATION_RESOURCE_TEST = AbstractConfigurationStepPanel.class.getName() + ".resourceTest";
    public AbstractConfigurationStepPanel(ResourceDetailsModel model) {
        super(model);
    }

    @Override
    protected String getIcon() {
        return "fa fa-cog";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.configuration");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.configuration.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.configuration.subText");
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {

        CapabilityCollectionType capabilities
                = ProvisioningObjectsUtil.getNativeCapabilities(getDetailsModel().getObjectType(), getPageBase());

        if (capabilities.getSchema() != null || capabilities.getTestConnection() != null) {
            PageBase pageBase = getPageBase();
            Task task = pageBase.createSimpleTask(OPERATION_RESOURCE_TEST);
            OperationResult result = task.getResult();

            try {
                pageBase.getModelService().testResource(getDetailsModel().getObjectWrapper().getObjectApplyDelta(), task, result);
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
        } else {
            super.onNextPerformed(target);
        }

        return false;
    }
}
