/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.runSuggestionAction;

public class SmartSuggestionWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable> extends AbstractWizardPanel<P, ResourceDetailsModel> {

    private static final String CLASS_DOT = SmartSuggestionWizardPanel.class.getName() + ".";
    private static final String OP_DEFINE_TYPES = CLASS_DOT + "defineTypes";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + "determineStatus";

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
                QName objectClassName = model.getObject().getValue().getObjectClassName();
                var resourceOid = getAssignmentHolderModel().getObjectType().getOid();

                Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
                boolean executed = runSuggestionAction(getPageBase(), resourceOid, objectClassName, target, OP_DEFINE_TYPES, task);

                showChoiceFragment(target, buildGeneratingWizardPanel(getIdOfChoicePanel(), objectClassName));
            }

        };
    }

    @Contract("_, _ -> new")
    private @NotNull ResourceGeneratingSuggestionObjectClassWizardPanel<ResourceObjectTypeDefinitionType, P> buildGeneratingWizardPanel(
            @NotNull String idOfChoicePanel, QName objectClassName) {
        return new ResourceGeneratingSuggestionObjectClassWizardPanel<>(idOfChoicePanel, getHelper(), objectClassName) {
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
            protected void onContinueWithSelected(AjaxRequestTarget target) {
                showChoiceFragment(target, buildSelectSuggestedObjectTypeWizardPanel(idOfChoicePanel, objectClassName));
            }
        };
    }

    @Contract("_, _ -> new")
    private @NotNull ResourceSuggestedObjectTypeTableWizardPanel<ResourceObjectTypeDefinitionType, P> buildSelectSuggestedObjectTypeWizardPanel(
            @NotNull String idOfChoicePanel, QName objectClassName) {
        return new ResourceSuggestedObjectTypeTableWizardPanel<>(idOfChoicePanel, getHelper(), objectClassName) {

            @Override
            protected void onContinueWithSelected(
                    @NotNull IModel<ObjectTypeSuggestionType> model,
                    @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
                    @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel,
                    @NotNull AjaxRequestTarget target) {

                getAssignmentHolderModel().getPageResource()
                        .showObjectTypeWizard(newValue, target, containerModel.getObject().getPath());

            }

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
        };
    }
}
