/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisProgressBarDto;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract class representing a progress bar component used for visualizing progress in the user interface.
 * The specific behavior (inline or basic) is delegated to concrete subclasses.
 */
public abstract class AbstractRoleAnalysisProgressBar<O extends RoleAnalysisProgressBarDto> extends BasePanel<O> {
    private static final String ID_CONTAINER = "progressBarContainer";
    private static final String ID_BAR = "progressBar";
    private static final String ID_BAR_PERCENTAGE = "progressBarPercentage";
    private static final String ID_BAR_TITLE = "progressBarTitle";
    private static final String ID_TITLE_CONTAINER = "title-container";
    private static final String ID_PROGRESS_CONTAINER = "progress-container";

    public AbstractRoleAnalysisProgressBar(String id, IModel<O> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer progressContainer = new WebMarkupContainer(ID_PROGRESS_CONTAINER);
        progressContainer.setOutputMarkupId(true);
        progressContainer.add(AttributeModifier.replace("class", getProgressBarContainerCssClass()));
        add(progressContainer);

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(AttributeModifier.replace("style", getProgressBarContainerCssStyle()));
        progressContainer.add(container);

        WebMarkupContainer titleContainer = buildTitleContainer(ID_TITLE_CONTAINER);

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_BAR);
        container.add(progressBar);

        setProgressBarParameters(progressBar);

        initProgressValueLabel(ID_BAR_PERCENTAGE, titleContainer);
    }

    protected @NotNull WebMarkupContainer buildTitleContainer(String idTitleContainer) {
        WebMarkupContainer titleContainer = new WebMarkupContainer(idTitleContainer);
        titleContainer.setOutputMarkupId(true);
        titleContainer.add(new VisibleBehaviour(() -> isTitleContainerVisible() && !isInline()));
        add(titleContainer);

        addProgressBarTitleLabel(titleContainer);
        return titleContainer;
    }

    private void setProgressBarParameters(@NotNull WebMarkupContainer progressBar) {
        progressBar.add(AttributeModifier.replace("aria-valuemin",
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_MIN_VALUE)));
        progressBar.add(AttributeModifier.replace("aria-valuemax",
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_MAX_VALUE)));
        progressBar.add(AttributeModifier.replace("aria-valuenow",
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_ACTUAL_VALUE)));
        progressBar.add(AttributeModifier.replace("style", "width: " +
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_ACTUAL_VALUE).getObject() + "%"));
        // Set color
        progressBar.add(AttributeModifier.append("style", "; background-color: " +
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_PROGRESS_COLOR).getObject()));
    }

    private void addProgressBarTitleLabel(@NotNull WebMarkupContainer container) {
        IModel<String> barTitle = new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_BAR_TITLE);

        Label progressBarTitle = new Label(ID_BAR_TITLE, barTitle);
        progressBarTitle.setOutputMarkupId(true);
        progressBarTitle.add(AttributeModifier.replace("style", getProgressBarTitleCssStyle() + ";" +
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_PROGRESS_COLOR)));
        container.add(progressBarTitle);
    }

    public abstract boolean isInline();

    protected abstract boolean isTitleContainerVisible();


    protected abstract String getProgressBarContainerCssClass();

    protected abstract boolean isWider();

    protected void initProgressValueLabel(String componentId, @NotNull WebMarkupContainer container) {
        Label progressBarText = new Label(componentId,
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_ACTUAL_VALUE).getObject() + "%");
        progressBarText.setOutputMarkupId(true);
        progressBarText.add(AttributeModifier.replace("style", getProgressBarPercentageCssStyle()));
        container.add(progressBarText);
    }

    protected String getProgressBarTitleCssStyle() {
        if (isWider()) {
            return "text-transform: capitalize; font-size:16px;";
        } else {
            return "text-transform: capitalize; font-size:14px;";
        }
    }

    protected String getProgressBarPercentageCssStyle() {
        if (isWider()) {
            return "text-transform: capitalize; font-size:16px;";
        } else {
            return "text-transform: capitalize; font-size:14px;";
        }
    }

    protected String getProgressBarContainerCssStyle() {
        if (isWider()) {
            return "border-radius: 3px; height:13px;";
        } else {
            return "border-radius: 3px; height:10px;";
        }
    }
}
