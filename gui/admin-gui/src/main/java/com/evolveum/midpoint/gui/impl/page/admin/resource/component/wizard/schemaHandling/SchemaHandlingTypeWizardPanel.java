/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanelWithChoicePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic.ResourceObjectTypeBasicWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 */
public abstract class SchemaHandlingTypeWizardPanel<C extends Containerable> extends AbstractWizardPanelWithChoicePanel<C, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlingTypeWizardPanel.class);

    public SchemaHandlingTypeWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createNewTypeWizard()));
    }

    private AbstractWizardPanel<C, ResourceDetailsModel> createNewTypeWizard() {
        WizardPanelHelper<C, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                SchemaHandlingTypeWizardPanel.this.onExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<C>> getValueModel() {
                return SchemaHandlingTypeWizardPanel.this.getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = SchemaHandlingTypeWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
        return createNewTypeWizard(getIdOfChoicePanel(), helper);
    }

    protected abstract AbstractWizardPanel<C, ResourceDetailsModel> createNewTypeWizard(
            String id, WizardPanelHelper<C, ResourceDetailsModel> helper);

    protected void showTypePreviewFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createTypePreview());
    }
}
