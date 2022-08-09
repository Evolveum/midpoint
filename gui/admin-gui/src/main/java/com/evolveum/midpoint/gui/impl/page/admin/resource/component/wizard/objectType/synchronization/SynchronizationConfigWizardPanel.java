/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.BasicSettingResourceObjectTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.PreviewResourceObjectTypeDataWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPreviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.PasswordInboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.PasswordOutboundStepPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class SynchronizationConfigWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public SynchronizationConfigWizardPanel(String id, ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createSynchronizationConfigSteps(getValueModel())))));
    }

    public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    private List<WizardStep> createSynchronizationConfigSteps(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        DefaultSettingStepPanel settingPanel = new DefaultSettingStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SynchronizationConfigWizardPanel.this.onExitPerformed(target);
            }
        };
        settingPanel.setOutputMarkupId(true);
        steps.add(settingPanel);

        steps.add(new ReactionStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SynchronizationConfigWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onFinishPerformed(AjaxRequestTarget target) {
                OperationResult result = SynchronizationConfigWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    new Toast()
                            .success()
                            .title(getString("SynchronizationConfigWizardPanel.save"))
                            .icon("fas fa-circle-check")
                            .autohide(true)
                            .delay(5_000)
                            .body(getString("SynchronizationConfigWizardPanel.save.text")).show(target);
                    onExitPerformed(target);
                }
            }
        });



        return steps;
    }
}
