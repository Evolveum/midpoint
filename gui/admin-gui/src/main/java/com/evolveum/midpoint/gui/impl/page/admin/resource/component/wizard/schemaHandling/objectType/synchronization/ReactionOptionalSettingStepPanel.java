/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-synchronization-reaction-optional",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.synchronization.reaction.optionalSettings", icon = "fa fa-wrench"),
        expanded = true)
@PanelInstance(identifier = "rw-association-synchronization-reaction-optional",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.synchronization.reaction.optionalSettings", icon = "fa fa-wrench"),
        expanded = true)
public class ReactionOptionalSettingStepPanel<C extends Containerable>
        extends AbstractValueFormResourceWizardStepPanel<C, ResourceDetailsModel> {

    public static final String OBJECT_TYPE_PANEL_TYPE = "rw-synchronization-reaction-optional";
    public static final String ASSOCIATION_TYPE_PANEL_TYPE = "rw-association-synchronization-reaction-optional";

    public ReactionOptionalSettingStepPanel(ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<C>> newValueModel) {
        super(model, newValueModel);
    }

    protected String getPanelType() {
        if (getValueModel().getObject().getParentContainerValue(ResourceObjectTypeDefinitionType.class) != null) {
            return OBJECT_TYPE_PANEL_TYPE;
        }
        return ASSOCIATION_TYPE_PANEL_TYPE;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.optionalSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.optionalSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.optionalSettings.subText");
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
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
}
