/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-attributes-inbound-main",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.inbound.main", icon = "fa fa-screwdriver-wrench"),
        expanded = true)
public class InboundMappingMainConfigurationStepPanel
        extends AbstractValueFormResourceWizardStepPanel<MappingType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-attributes-inbound-main";

    private static final List<ItemName> VISIBLE_ITEMS = List.of(
            MappingType.F_NAME,
            ResourceAttributeDefinitionType.F_REF,
            MappingType.F_SOURCE,
            MappingType.F_TARGET,
            MappingType.F_STRENGTH,
            MappingType.F_EXPRESSION,
            MappingType.F_CONDITION,
            InboundMappingType.F_USE
    );

    public InboundMappingMainConfigurationStepPanel(ResourceDetailsModel model,
                                                    IModel<PrismContainerValueWrapper<MappingType>> newValueModel) {
        super(model, newValueModel);
    }

    @Override
    protected String getIcon() {
        return "fa fa-screwdriver-wrench";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.attributes.inbound.main");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.inbound.main.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.inbound.main.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (VISIBLE_ITEMS.stream().anyMatch(item -> item.equivalent(wrapper.getItemName()))) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };
    }
}
