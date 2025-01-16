/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class RoleAnalysisSessionMaintenanceWizardPanel
        extends AbstractWizardStepPanel<TaskDetailsModel> {
    private static final String ID_TITLE_RETENTION = "title-retention";
    private static final String ID_LABEL_RETENTION = "label-retention";
    private static final String ID_INPUT_RETENTION = "input-retention";

    private static final String ID_PROCESSING_CONTAINER = "processing-container";
    private static final String ID_TITLE_PROCESSING = "title-processing";
    private static final String ID_DESCRIPTION_PROCESSING = "description-processing";
    private static final String ID_REBUILD_PROCESSING = "rebuild-processing";
    private static final String ID_DELETE_PROCESSING = "delete-processing";

    private static final String DEFAULT_BUTTON_CSS = "text-left btn btn-default ";
    private static final String COLORED_BUTTON_CSS = "colored-form-primary ";

    boolean isRebuild = true;
    Model<Boolean> isActiveModel = Model.of(false);

    public RoleAnalysisSessionMaintenanceWizardPanel(TaskDetailsModel model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        inputRetentionPart();

        inputProcessingPart();
    }

    public Component getRebuildButton() {
        return get(getPageBase().createComponentPath(ID_REBUILD_PROCESSING));
    }

    public Component getDeleteButton() {
        return get(getPageBase().createComponentPath(ID_DELETE_PROCESSING));
    }

    private void inputRetentionPart() {
        ToggleCheckBoxPanel toggleCheckBoxPanel = new ToggleCheckBoxPanel(ID_TITLE_RETENTION,
                isActiveModel,
                createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.data.retain.label"),
                createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.data.retain.help")) {

            @Contract(pure = true)
            @Override
            public @NotNull String getDescriptionCssClass() {
                return "text-gray";
            }

            @Override
            public @NotNull Component getTitleComponent(String id) {
                IconWithLabel iconWithLabel = new IconWithLabel(id,
                        createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.data.retain.label")) {
                    @Override
                    protected String getIconCssClass() {
                        return GuiStyleConstants.CLASS_ICON_RESOURCE_MAINTENANCE;
                    }
                };
                iconWithLabel.setOutputMarkupId(true);
                iconWithLabel.add(AttributeModifier.replace(CLASS_CSS, "d-flex align-items-center gap-2 h5"));
                return iconWithLabel;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getComponentCssClass() {
                return "d-flex align-items-center flex-row gap-3";
            }
        };
        toggleCheckBoxPanel.setOutputMarkupId(true);
        add(toggleCheckBoxPanel);

        LabelWithHelpPanel labelWithHelpPanel = new LabelWithHelpPanel(ID_LABEL_RETENTION,
                createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.keep.data.label")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.keep.data.help");
            }
        };
        add(labelWithHelpPanel);

        TextField<Double> textField = new TextField<>(ID_INPUT_RETENTION, Model.of(72.0));
        textField.setOutputMarkupId(true);
        add(textField);
    }

    @SuppressWarnings("unchecked")
    public TextField<Double> getRetentionField() {
        return (TextField<Double>) get(getPageBase().createComponentPath(ID_INPUT_RETENTION));
    }

    private void inputProcessingPart() {

        WebMarkupContainer processingContainer = new WebMarkupContainer(ID_PROCESSING_CONTAINER);
        processingContainer.setOutputMarkupId(true);
        processingContainer.add(new VisibleBehaviour(() -> false));
        add(processingContainer);

        IconWithLabel iconWithLabel = new IconWithLabel(ID_TITLE_PROCESSING,
                createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.data.processing.label")) {
            @Override
            protected String getIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_RESOURCE_MAINTENANCE;
            }
        };
        iconWithLabel.setOutputMarkupId(true);
        iconWithLabel.add(AttributeModifier.append(CLASS_CSS, "d-flex align-items-center gap-2 h5"));
        processingContainer.add(iconWithLabel);

        Label description = new Label(ID_DESCRIPTION_PROCESSING,
                createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.data.processing.help"));
        description.setOutputMarkupId(true);
        description.add(AttributeModifier.append(CLASS_CSS, "text-gray"));
        processingContainer.add(description);

        initDeleteButton(processingContainer);
        initRebuildButton(processingContainer);
    }

    private void initRebuildButton(WebMarkupContainer processingContainer) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_REFRESH,
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton rebuildButton = new AjaxCompositedIconSubmitButton(ID_REBUILD_PROCESSING,
                iconBuilder.build(),
                createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.rebuild.label")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(@NotNull AjaxRequestTarget target) {
                setRebuild(true);
                this.add(AttributeModifier.replace(CLASS_CSS, DEFAULT_BUTTON_CSS + COLORED_BUTTON_CSS));
                getDeleteButton().add(AttributeModifier.replace(CLASS_CSS, DEFAULT_BUTTON_CSS));
                target.add(getDeleteButton());
                target.add(this);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        rebuildButton.titleAsLabel(true);
        rebuildButton.setOutputMarkupId(true);

        rebuildButton.add(AttributeModifier.replace(CLASS_CSS, DEFAULT_BUTTON_CSS));
        if (isRebuild()) {
            rebuildButton.add(AttributeModifier.append(CLASS_CSS, COLORED_BUTTON_CSS));
        }

        processingContainer.add(rebuildButton);
    }

    private void initDeleteButton(WebMarkupContainer processingContainer) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_TRASH,
                IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton deleteButton = new AjaxCompositedIconSubmitButton(ID_DELETE_PROCESSING,
                iconBuilder.build(),
                createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.delete.label")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(@NotNull AjaxRequestTarget target) {
                setRebuild(false);
                this.add(AttributeModifier.replace(CLASS_CSS, DEFAULT_BUTTON_CSS + COLORED_BUTTON_CSS));
                getRebuildButton().add(AttributeModifier.replace(CLASS_CSS, DEFAULT_BUTTON_CSS));
                target.add(getRebuildButton());
                target.add(this);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        deleteButton.titleAsLabel(true);
        deleteButton.setOutputMarkupId(true);

        deleteButton.add(AttributeModifier.replace(CLASS_CSS, DEFAULT_BUTTON_CSS));
        if (!isRebuild()) {
            deleteButton.add(AttributeModifier.append(CLASS_CSS, COLORED_BUTTON_CSS));
        }

        processingContainer.add(deleteButton);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onSubmitMaintenancePerform();
    }

    //TODO nasty hack remove later
    private void onSubmitMaintenancePerform() {
        Boolean isActive = isActiveModel.getObject();
        if (isActive.equals(Boolean.TRUE)) {
            Double modelObject = getRetentionField().getModelObject();
            if (modelObject != null) {
                int seconds = (int) (modelObject * 60 * 60);

                ScheduleType schedule = new ScheduleType();
                schedule.setInterval(seconds);
                getDetailsModel().getObjectType().setSchedule(schedule);
            }

        }
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("RoleAnalysisSessionMaintenanceWizardPanel.subText");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    public boolean isRebuild() {
        return isRebuild;
    }

    public void setRebuild(boolean rebuild) {
        isRebuild = rebuild;
    }

}
