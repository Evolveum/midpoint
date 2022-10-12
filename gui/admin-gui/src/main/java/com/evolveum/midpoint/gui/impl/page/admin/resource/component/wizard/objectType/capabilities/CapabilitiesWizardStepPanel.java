/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.capabilities;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.CapabilitiesPanel;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationItemRefsTable;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

/**
 * @author lskublik
 */
@Experimental
public abstract class CapabilitiesWizardStepPanel extends AbstractWizardBasicPanel {

    private static final String ID_PANEL = "panel";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public CapabilitiesWizardStepPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CapabilitiesPanel panel = new CapabilitiesPanel(ID_PANEL, getResourceModel(), valueModel);
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected String getSubmitIcon() {
        return "fa fa-floppy-disk";
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("CapabilitiesWizardStepPanel.saveButton");
    }

    @Override
    protected void addCustomButtons(RepeatingView buttons) {
        AjaxIconButton saveButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of(getSubmitIcon()),
                getSubmitLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onSaveResourcePerformed(target);
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(AttributeAppender.append("class", "btn btn-success"));
        buttons.add(saveButton);
    }

    protected abstract void onSaveResourcePerformed(AjaxRequestTarget target);

    @Override
    protected IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CapabilitiesWizardStepPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CapabilitiesWizardStepPanel.subText");
    }
}
