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
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.verticalForm.VerticalFormPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public class DiscoveryStepPanel extends BasicWizardPanel {

    private static final Trace LOGGER = TraceManager.getTrace(DiscoveryStepPanel.class);

    private final ResourceDetailsModel resourceModel;

    public DiscoveryStepPanel(ResourceDetailsModel model) {
        super();
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-8";
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
    protected Component createContentPanel(String id) {
        VerticalFormPanel form = new VerticalFormPanel(id, () -> getConfigurationValue()) {
            @Override
            protected String getIcon() {
                return "fa fa-list-check";
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getTitle();
            }

            @Override
            protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
                if(itemWrapper.isMandatory()) {
                    return ItemVisibility.HIDDEN;
                }
                return ItemVisibility.AUTO;
            }
        };
        form.add(AttributeAppender.append("class", "col-8"));
        return form;
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
}
