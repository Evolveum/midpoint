/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-attributes-outbound-optional",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.attributes.outbound.optional", icon = "fa fa-screwdriver-wrench"),
        expanded = true)
public class OutboundMappingOptionalConfigurationStepPanel<AHDM extends AssignmentHolderDetailsModel>
        extends AbstractValueFormResourceWizardStepPanel<MappingType, AHDM> {

    public static final String PANEL_TYPE = "rw-attributes-outbound-optional";

    private static final List<ItemName> VISIBLE_ITEMS = List.of(
            MappingType.F_DESCRIPTION,
            MappingType.F_EXCLUSIVE,
            MappingType.F_AUTHORITATIVE,
            MappingType.F_CHANNEL,
            MappingType.F_EXCEPT_CHANNEL
    );

    public OutboundMappingOptionalConfigurationStepPanel(AHDM model,
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
        return createStringResource("PageResource.wizard.step.attributes.outbound.optional");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.outbound.optional.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.attributes.outbound.optional.subText");
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        onExitPreProcessing(target);
        onExitPerformed(target);
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("OnePanelPopupPanel.button.done");
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
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
