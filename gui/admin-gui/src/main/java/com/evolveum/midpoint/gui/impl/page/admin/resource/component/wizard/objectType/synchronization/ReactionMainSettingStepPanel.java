/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "rw-synchronization-reaction-main",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.synchronization.reaction.mainSettings", icon = "fa fa-wrench"),
        expanded = true)
public class ReactionMainSettingStepPanel extends AbstractValueFormResourceWizardStepPanel<SynchronizationReactionType> {

    public static final String PANEL_TYPE = "rw-synchronization-reaction-main";

    public ReactionMainSettingStepPanel(ResourceDetailsModel model,
                                   IModel<PrismContainerValueWrapper<SynchronizationReactionType>> newValueModel) {
        super(model, newValueModel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.mainSettings");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.mainSettings.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.mainSettings.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }
}
