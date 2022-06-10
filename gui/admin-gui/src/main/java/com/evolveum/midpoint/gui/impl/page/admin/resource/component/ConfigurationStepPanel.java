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
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
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

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

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
        display = @PanelDisplay(label = "PageResource.tab.connector.configuration", icon = "fa fa-plug"))
public class ConfigurationStepPanel extends BasicWizardPanel {

    private static final String ID_FORM = "form";

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStepPanel.class);

    private final ResourceDetailsModel resourceModel;

    public ConfigurationStepPanel(ResourceDetailsModel model) {
        super();
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        resourceModel.reset();
        super.onInitialize();
        iniLayout();
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-8";
    }

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

    private void iniLayout() {
        VerticalFormPanel form = new VerticalFormPanel(ID_FORM, () -> getConfigurationValue()) {
            @Override
            protected String getIcon() {
                return "fa fa-cog";
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getPageBase().createStringResource("PageResource.wizard.step.configuration");
            }

//            @Override
//            protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
//                if(itemWrapper.isMandatory()) {
//                    return ItemVisibility.AUTO;
//                }
//                return ItemVisibility.HIDDEN;
//            }
        };
        form.add(AttributeAppender.append("class", "col-8"));
        add(form);
    }

    private PrismContainerValueWrapper<Containerable> getConfigurationValue() {
        try {
            return resourceModel.getConfigurationModelObject().findContainerValue(
                    ItemPath.create(new QName(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME)));
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find value of resource configuration container", e);
            return null;
        }
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        getVerticalForm().visitChildren(VerticalFormPrismPropertyValuePanel.class, (component, objectIVisit) -> {
            ((VerticalFormPrismPropertyValuePanel)component).updateFeedbackPanel(target);
        });
    }

    private MarkupContainer getVerticalForm() {
        return (MarkupContainer) get(ID_FORM);
    }
}
