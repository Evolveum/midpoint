/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.verticalForm.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.verticalForm.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
@PanelType(name = "connectorConfigurationWizard")
@PanelInstance(identifier = "connectorConfigurationWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        defaultPanel = true,
        display = @PanelDisplay(
                label = "PageResource.wizard.step.configuration",
                icon = "fa fa-cog"),
        containerPath = "connectorConfiguration/configurationProperties",
        expanded = true)
public class ConfigurationStepPanel extends AbstractResourceWizardStepPanel {

    private static final String PANEL_TYPE = "connectorConfigurationWizard";

    private static final String ID_FORM = "form";

    public ConfigurationStepPanel(ResourceDetailsModel model) {
        super(model);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-cog";
    }

//    @Override
//    protected void onInitialize() {
//        getResourceModel().reset();
//        super.onInitialize();
//    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.configuration");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.configuration.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.configuration.subText");
    }

//    private ContainerPanelConfigurationType getContainerConfiguration() {
//        ContainerPanelConfigurationType config
//                = WebComponentUtil.getContainerConfiguration(resourceModel.getObjectDetailsPageConfiguration().getObject(), PANEL_TYPE);
//        if (config != null && config.getPath() == null) {
//            config.path(new ItemPathType(
//                    ItemPath.create(
//                            ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(),
//                            SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME)));
//        }
//        return config;
//    }

//    protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
//        if (itemWrapper.isMandatory()) {
//            return ItemVisibility.AUTO;
//        }
//        return ItemVisibility.HIDDEN;
//    }
}
