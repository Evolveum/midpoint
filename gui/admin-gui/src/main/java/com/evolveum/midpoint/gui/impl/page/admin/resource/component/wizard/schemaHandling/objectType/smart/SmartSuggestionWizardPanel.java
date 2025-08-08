/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page.ResourceGeneratingSuggestionObjectClassWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page.ResourceObjectClassTableWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class SmartSuggestionWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable> extends AbstractWizardPanel<P, ResourceDetailsModel> {

    private static final String CLASS_DOT = SmartSuggestionWizardPanel.class.getName() + ".";
    private static final String OP_DEFINE_TYPES = CLASS_DOT + "defineTypes";

    private static final Trace LOGGER = TraceManager.getTrace(SmartSuggestionWizardPanel.class);

    public SmartSuggestionWizardPanel(String id, WizardPanelHelper<P, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(getIdOfChoicePanel())));
    }

    protected ResourceObjectClassTableWizardPanel<ResourceObjectTypeDefinitionType, P> createTablePanel(String idOfChoicePanel) {
        return new ResourceObjectClassTableWizardPanel<>(idOfChoicePanel, getHelper()) {

            @Override
            protected void onContinueWithSelected(IModel<SelectableBean<ObjectClassWrapper>> model, AjaxRequestTarget target) {

                getPageBase().taskAwareExecutor(target, OP_DEFINE_TYPES)
                        .runVoid((task, result) -> {
                            ObjectClassWrapper value = model.getObject().getValue();
                            if (value == null) {
                                return;
                            }
                            var resourceOid = getAssignmentHolderModel().getObjectType().getOid();
                            var oid = getPageBase().getSmartIntegrationService().submitSuggestObjectTypesOperation(
                                    resourceOid, value.getObjectClassName(), task, result);
                            result.setBackgroundTaskOid(oid);
                        });

                showChoiceFragment(target, buildGeneratingWizardPanel(getIdOfChoicePanel()));
            }
        };
    }

    @Contract("_ -> new")
    private @NotNull ResourceGeneratingSuggestionObjectClassWizardPanel<ResourceObjectTypeDefinitionType, P> buildGeneratingWizardPanel(
            @NotNull String idOfChoicePanel) {
        return new ResourceGeneratingSuggestionObjectClassWizardPanel<>(idOfChoicePanel, getHelper()) {

            @Override
            protected boolean isBackButtonVisible() {
                return true;
            }

            @Override
            protected IModel<String> getBackLabel() {
                return createStringResource("SmartSuggestionWizardPanel.back.to.object.class.selection");
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showChoiceFragment(target, createTablePanel(idOfChoicePanel));
            }

            @Override
            protected void onContinueWithSelected(IModel<SelectableBean<ObjectClassWrapper>> model, AjaxRequestTarget target) {

            }
        };
    }

}
