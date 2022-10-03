/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismValuePanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */

@Experimental
@PanelInstance(identifier = "associationsWizard",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.associations", icon = "fa fa-shield"),
        expanded = true)
public class AssociationStepPanel extends AbstractResourceWizardStepPanel {

    protected static final String ID_PANEL = "panel";

    public static final String PANEL_TYPE = "AssociationsWizardPanel";

    private final IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> containerModel;
    private final ResourceDetailsModel resourceModel;

    public AssociationStepPanel(ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> containerModel) {
        super(model);
        this.containerModel = containerModel;
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder();
        builder.panelConfiguration(getContainerConfiguration());

        Panel panel = getPageBase().initContainerValuePanel(ID_PANEL, getContainerFormModel(), builder.build());

        panel.setOutputMarkupId(true);
        panel.add(AttributeAppender.append("class", "card col-12"));
        add(panel);
    }

    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return WebComponentUtil.getContainerConfiguration(resourceModel.getObjectDetailsPageConfiguration().getObject(), getPanelType());
    }

    protected IModel<PrismContainerValueWrapper<ResourceObjectAssociationType>> getContainerFormModel() {
        return containerModel;
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

//    private String getIcon() {
//        return "fa fa-shield";
//    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.associations");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.associations.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.associations.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }
}
