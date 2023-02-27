/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "rw-connectorConfiguration-partial")
@PanelInstance(identifier = "rw-connectorConfiguration-partial",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(
                label = "PageResource.wizard.step.configuration",
                icon = "fa fa-cog"),
        containerPath = "connectorConfiguration/configurationProperties",
        expanded = true)
public class PartialConfigurationStepPanel extends AbstractFormWizardStepPanel {

    private static final String DOT_CLASS = PartialConfigurationStepPanel.class.getName() + ".";
    private static final String OPERATION_PARTIAL_CONFIGURATION_TEST = DOT_CLASS + "partialConfigurationTest";

    private static final String PANEL_TYPE = "rw-connectorConfiguration-partial";

    public PartialConfigurationStepPanel(ResourceDetailsModel model) {
        super(model);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
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
        return createStringResource("PageResource.wizard.step.partialConfiguration.subText");
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        ContainerPanelConfigurationType config = getContainerConfiguration();
        if (config != null
                && (config.getContainer().size() != 1 || config.getContainer().iterator().next().getPath() == null)) {
            return w -> ItemVisibility.AUTO;
        }
        return w -> {
            if (w.isMandatory()) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        Task task = pageBase.createSimpleTask(OPERATION_PARTIAL_CONFIGURATION_TEST);
        OperationResult result = task.getResult();

        try {
            WebComponentUtil.partialConfigurationTest(getDetailsModel().getObjectWrapper().getObjectApplyDelta(), getPageBase(), task, result);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't apply delta for resource", e);
        }

        if (result.isSuccess()) {
            return super.onNextPerformed(target);
        }
        pageBase.showResult(result);
        target.add(getFeedback());

        return false;
    }
}
