/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.marking;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "rw-marking-pattern")
@PanelInstance(identifier = "rw-marking-pattern",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.marking.pattern", icon = "fa fa-circle"))
public class PatternStepPanel extends AbstractFormWizardStepPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "rw-marking-pattern";

    private final IModel<PrismContainerValueWrapper<ShadowMarkingConfigurationType>> valueModel;

    public PatternStepPanel(ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ShadowMarkingConfigurationType>> valueModel) {
        super(model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        getDetailsModel().getObjectWrapper().setShowEmpty(false, false);
        getDetailsModel().getObjectWrapper().getValues().forEach(valueWrapper -> valueWrapper.setShowEmpty(false));
        super.onInitialize();
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        IModel<? extends PrismContainerWrapper> containerModel
                = PrismContainerWrapperModel.fromContainerValueWrapper(valueModel, ShadowMarkingConfigurationType.F_PATTERN);
        containerModel.getObject().setShowEmpty(true, true);
        containerModel.getObject().setExpanded(true);
        return containerModel;
    }

    @Override
    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return null;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-circle";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.marking.pattern");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.marking.pattern.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.marking.pattern.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("OnePanelPopupPanel.button.done");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    @Override
    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        return false;
    }

    @Override
    protected boolean isExpandedButtonVisible() {
        //there is a condition in the VerticalFormContainerHeaderPanel.initLayout which avoids adding
        // a header button click action in case of multivalue containers.
        //not sure about a purpose of that condition, therefore not changing it
        //but as pattern container is multivalue, it's not clickable so no sense to show it
        return false;
    }
}
