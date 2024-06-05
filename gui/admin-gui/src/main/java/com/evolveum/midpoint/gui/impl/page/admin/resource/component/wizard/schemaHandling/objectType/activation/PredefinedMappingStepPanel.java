/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPredefinedActivationMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-activation-predefined",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.activation.predefined", icon = "fa fa-circle"),
        expanded = true)
public class PredefinedMappingStepPanel
        extends AbstractValueFormResourceWizardStepPanel<AbstractPredefinedActivationMappingType, ResourceDetailsModel> {

    public static final String PANEL_TYPE = "rw-activation-predefined";

    public PredefinedMappingStepPanel(ResourceDetailsModel model,
                                      IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> newValueModel) {
        super(model, newValueModel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.activation.predefined");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.activation.predefined.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.activation.predefined.subText");
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
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleBehaviour.ALWAYS_INVISIBLE;
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
            if (wrapper.getItemName().equals(MappingType.F_LIFECYCLE_STATE)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }
}
