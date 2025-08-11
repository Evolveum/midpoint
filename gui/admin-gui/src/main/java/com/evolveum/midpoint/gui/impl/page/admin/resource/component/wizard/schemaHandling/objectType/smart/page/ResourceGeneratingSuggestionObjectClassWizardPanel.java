/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.formatElapsedTime;

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
            StatusInfo<ObjectTypesSuggestionType> latest = loadObjectClassSuggestions();
            if (latest == null) {
                return new SmartGeneratingDto(() -> "N/A", Collections::emptyList, () -> OperationResultStatus.NOT_APPLICABLE);
            }

            OperationResultStatus operationResultStatus = latest.status();
            List<SmartGeneratingDto.StatusRow> rows = buildStatusRows(latest);
            String elapsed = formatElapsedTime(latest);
            return new SmartGeneratingDto(() -> elapsed, () -> rows, () -> operationResultStatus);
        }) {
            @Override
            protected Duration getRefreshInterval() {
                return Duration.ofSeconds(1);
            }
        };
    }

    //TODO (dummy data already present)  change this to use real data from the resource status after implementation

    /** Builds display rows depending on the suggestion status. */
    private @NotNull List<SmartGeneratingDto.StatusRow> buildStatusRows(StatusInfo<ObjectTypesSuggestionType> suggestion) {
        List<SmartGeneratingDto.StatusRow> rows = new ArrayList<>();
        if (suggestion == null) {
            rows.add(new SmartGeneratingDto.StatusRow("No suggestions available", false));
            return rows;
        }

        switch (suggestion.status()) {
            case IN_PROGRESS -> rows.add(new SmartGeneratingDto.StatusRow("Generating suggestions", false));
            case SUCCESS -> {
                rows.add(new SmartGeneratingDto.StatusRow("Generating suggestions", true));
                rows.add(new SmartGeneratingDto.StatusRow("Patterns identified", true));
            }
            case FATAL_ERROR -> {
                rows.add(new SmartGeneratingDto.StatusRow("Error: "
                        + LocalizationUtil.translateMessage(suggestion.message()), true));
            }
            default -> rows.add(new SmartGeneratingDto.StatusRow("Unknown status: " + suggestion.status(), false));
        }
        return rows;
    }

    /** Loads the current resource status. */
    private StatusInfo<ObjectTypesSuggestionType> loadObjectClassSuggestions() {
        PageBase pageBase = getPageBase();
        var task = pageBase.createSimpleTask(OP_DETERMINE_STATUS);
        var result = task.getResult();
        var resourceOid = getAssignmentHolderDetailsModel().getObjectType().getOid();

        return SmartIntegrationUtils.loadObjectClassSuggestions(pageBase, resourceOid, objectClassName, task, result);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onContinueWithSelected(target);
    }

    @Override
    protected String getSaveLabelKey() {
        return "ResourceObjectClassTableWizardPanel.saveButton";
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
    protected IModel<String> getTitleIconModel() {
        return Model.of(GuiStyleConstants.CLASS_TASK_EXECUTION_ICON + " fa fa-gears fa-2xl text-primary pt-5 pb-4");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("SmartGeneratingSuggestionStep.wizard.step.generating.suggestion.action.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("SmartGeneratingSuggestionStep.wizard.step.generating.suggestion.action.subText");
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
