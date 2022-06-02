/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.self.PageRequestAccess;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ConfigurationStepPanel extends BasicWizardPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStepPanel.class);

    private final IModel<PrismObjectWrapper<ResourceType>> resourceModel;

    public ConfigurationStepPanel(IModel<PrismObjectWrapper<ResourceType>> model) {
        super();
        this.resourceModel = model;
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

    @Override
    protected Component createContentPanel(String id) {
        return new QuickFormPanel(id, () -> getConfigurationValue()) {
            @Override
            protected String getIcon() {
                return "fa fa-cog";
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getPageBase().createStringResource("PageResource.wizard.step.configuration");
            }

            @Override
            protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
                if(itemWrapper.isMandatory()) {
                    return ItemVisibility.AUTO;
                }
                return ItemVisibility.HIDDEN;
            }
        };
    }

    private PrismContainerValueWrapper<Containerable> getConfigurationValue() {
        try {
            return resourceModel.getObject().findContainerValue(
                    ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES));
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find value of resource configuration container", e);
            return null;
        }
    }
}
