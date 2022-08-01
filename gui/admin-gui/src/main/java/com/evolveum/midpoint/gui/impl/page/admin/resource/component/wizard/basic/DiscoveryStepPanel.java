/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.DiscoveredConfiguration;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

import java.util.Collection;

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
public class DiscoveryStepPanel extends AbstractConfigurationStepPanel {

    private static final String OPERATION_DISCOVER_CONFIGURATION = DiscoveryStepPanel.class.getName() + ".discoverConfiguration";
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

            for (PrismProperty<?> suggestion : discoverProperties.getDiscoveredProperties()) {
                PrismPropertyDefinition<?> suggestionDef = suggestion.getDefinition();

                PrismPropertyWrapper<Object> item = getResourceModel().getObjectWrapper().findProperty(
                        ItemPath.create(
                                "connectorConfiguration",
                                "configurationProperties",
                                suggestionDef.getItemName()));

                if (item != null) {
                    if (suggestionDef.getAllowedValues() != null && !suggestionDef.getAllowedValues().isEmpty()) {
                        item.toMutable().setAllowedValues(
                                (Collection<? extends DisplayableValue<Object>>) suggestionDef.getAllowedValues());
                    }
                    if (suggestionDef.getSuggestedValues() != null && !suggestionDef.getSuggestedValues().isEmpty()) {
                        item.toMutable().setSuggestedValues(
                                (Collection<? extends DisplayableValue<Object>>) suggestionDef.getSuggestedValues());
                    }
                }
            }
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
}
