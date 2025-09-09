/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

@PanelType(name = "rw-generating-suggestion-object-class")
@PanelInstance(identifier = "rw-generating-suggestion-object-class",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "SmartGeneratingSuggestionStep.wizard.step.generating.suggestion.action.title", icon = "fa fa-arrows-rotate"))
public abstract class ResourceGeneratingSuggestionObjectClassWizardPanel<C extends ResourceObjectTypeDefinitionType, P extends Containerable>
        extends AbstractResourceWizardBasicPanel<P> {

    private static final String OP_DETERMINE_STATUS = ResourceGeneratingSuggestionObjectClassWizardPanel.class.getName() + ".determineStatus";

    private static final String ID_PANEL_COMPONENT = "panelComponent";

    private final QName objectClassName;

    public ResourceGeneratingSuggestionObjectClassWizardPanel(
            String id,
            WizardPanelHelper<P, ResourceDetailsModel> superHelper,
            QName objectClassName
    ) {
        super(id, superHelper);
        this.objectClassName = objectClassName;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Component panelComponent = createPanelComponent(ID_PANEL_COMPONENT);
        panelComponent.setOutputMarkupId(true);
        add(panelComponent);
    }

    /** Creates the generating panel with pre-filled data. */
    protected Component createPanelComponent(String id) {
        return new SmartGeneratingPanel(id, () -> {
            Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
            OperationResult result = task.getResult();

            LoadableModel<StatusInfo<?>> statusInfoModel = new LoadableModel<>() {
                @Override
                protected StatusInfo<?> load() {
                    return loadObjectClassSuggestions();
                }
            };

            StatusInfo<?> latest = statusInfoModel.getObject();
            if (latest == null) {
                return new SmartGeneratingDto();
            }

            String token = latest.getToken();
            PrismObject<TaskType> taskTypePrismObject = WebModelServiceUtils.loadObject(TaskType.class, token, getPageBase(), task, result);
            return new SmartGeneratingDto(statusInfoModel, () -> taskTypePrismObject);
        }, true) {
            @Override
            protected void onFinishActionPerform(AjaxRequestTarget target) {
                onSubmitPerformed(target);
            }

            @Override
            protected void onRunInBackgroundPerform(AjaxRequestTarget target) {
                onExitPerformed(target);
            }

            @Override
            protected void onDiscardPerform(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
    }

    /** Loads the current resource status. */
    private StatusInfo<ObjectTypesSuggestionType> loadObjectClassSuggestions() {
        PageBase pageBase = getPageBase();
        var task = pageBase.createSimpleTask(OP_DETERMINE_STATUS);
        var result = task.getResult();
        var resourceOid = getAssignmentHolderDetailsModel().getObjectType().getOid();

        return SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions(pageBase, resourceOid, objectClassName, task, result);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onContinueWithSelected(target);
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    protected abstract void onContinueWithSelected(AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("ResourceObjectClassTableWizardPanel.breadcrumbLabel");
    }

    @Override
    protected @Nullable IModel<String> getBreadcrumbIcon() {
        return Model.of(GuiStyleConstants.CLASS_ICON_WIZARD);
    }

    @Override
    protected boolean isFeedbackContainerVisible() {
        return false;
    }

    @Override
    protected IModel<String> getTextModel() {
        return null;
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return null;
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-10";
    }

    @Override
    protected WebMarkupContainer getButtonsContainer() {
        return super.getButtonsContainer();
    }
}
