/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.ProgressBarDto;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;

/**
 * Represents a progress bar component used for visualizing progress in the user interface.
 * The progress bar displays a title, actual progress value, and a text representation of the progress percentage.
 * It also provides the ability to click on the title to view details about the analyzed members.
 * <p>
 * The progress bar can be customized by setting the minimum and maximum values, as well as the actual progress value.
 */
public class ProgressBar extends BasePanel<ProgressBarDto> {

    private static final String ID_CONTAINER = "progressBarContainer";
    private static final String ID_TITLE_CONTAINER = "title-container";
    private static final String ID_BAR = "progressBar";
    private static final String ID_BAR_PERCENTAGE = "progressBarPercentage";
    private static final String ID_BAR_PERCENTAGE_INLINE = "progressBarPercentageInline";
    private static final String ID_BAR_TITLE = "progressBarTitle";
    private static final String ID_BAR_TITTLE_DATA = "progressBarDetails";
    private static final String ID_CONTAINER_TITLE_DATA = "details-container";

    public ProgressBar(String id, IModel<ProgressBarDto> titleModel) {
        super(id, titleModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        this.add(AttributeModifier.replace(TITLE_CSS,
                () -> new PropertyModel<>(getModel(), ProgressBarDto.F_BAR_TOOLTIP).getObject()));

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.add(AttributeModifier.replace("style", getProgressBarContainerStyle()));
        container.setOutputMarkupId(true);
        add(container);

        WebMarkupContainer titleContainer = new WebMarkupContainer(ID_TITLE_CONTAINER);
        titleContainer.setOutputMarkupId(true);
        titleContainer.add(new VisibleBehaviour(() -> !getModelObject().isInline()));
        add(titleContainer);

        Component component = getModelObject().buildTitleComponent(ID_BAR_TITLE, this.getPageBase());
        titleContainer.add(component);

        resolveTitleDataLabel(titleContainer);

        initProgressBarText(titleContainer);

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_BAR);
        container.add(progressBar);

        setProgressBarParameters(progressBar);
    }

    private void initProgressBarText(WebMarkupContainer titleContainer) {
        if (getModelObject().isInline()) {
            Label progressBarText = new Label(ID_BAR_PERCENTAGE_INLINE,
                    () -> new PropertyModel<>(getModel(), ProgressBarDto.F_ACTUAL_VALUE).getObject() + "%");
            progressBarText.setOutputMarkupId(true);
            add(progressBarText);

            WebMarkupContainer progressBarInline = new WebMarkupContainer(ID_BAR_PERCENTAGE);
            progressBarInline.setOutputMarkupId(true);
            titleContainer.add(progressBarInline);
        } else {
            Label progressBarText = new Label(ID_BAR_PERCENTAGE,
                    () -> new PropertyModel<>(getModel(), ProgressBarDto.F_ACTUAL_VALUE).getObject() + "%");
            progressBarText.setOutputMarkupId(true);
            titleContainer.add(progressBarText);

            WebMarkupContainer progressBarInline = new WebMarkupContainer(ID_BAR_PERCENTAGE_INLINE);
            progressBarInline.setOutputMarkupId(true);
            progressBarInline.add(new VisibleBehaviour(() -> false));
            add(progressBarInline);
        }
    }

    public ProgressBarDto getModelObject() {
        return getModel().getObject();
    }

    private void resolveTitleDataLabel(@NotNull WebMarkupContainer titleContainer) {
        PropertyModel<Object> helpTooltip = new PropertyModel<>(getModel(), ProgressBarDto.F_HELP_TOOLTIP);
        String helpTextValue = helpTooltip.getObject().toString();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER_TITLE_DATA);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpTextValue)));
        titleContainer.add(container);

        Label help = new Label(ID_BAR_TITTLE_DATA);
        help.add(AttributeModifier.replace("data-original-title",
                Model.of(helpTextValue)));
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpTextValue)));
        help.setOutputMarkupId(true);
        if (getModelObject().isUnusual()) {
            help.add(AttributeModifier.append("class", "fa-exclamation-triangle text-warning"));
        } else {
            help.add(AttributeModifier.append("class", " fa-info-circle text-info"));
        }
        container.add(help);
    }

    private void setProgressBarParameters(@NotNull WebMarkupContainer progressBar) {
        progressBar.add(AttributeModifier.replace("aria-valuemin",
                new PropertyModel<>(getModel(), ProgressBarDto.F_MIN_VALUE)));
        progressBar.add(AttributeModifier.replace("aria-valuemax",
                new PropertyModel<>(getModel(), ProgressBarDto.F_MAX_VALUE)));
        progressBar.add(AttributeModifier.replace("aria-valuenow",
                new PropertyModel<>(getModel(), ProgressBarDto.F_ACTUAL_VALUE)));
        progressBar.add(AttributeModifier.replace("style", "width: " +
                new PropertyModel<>(getModel(), ProgressBarDto.F_ACTUAL_VALUE).getObject() + "%"));

        progressBar.add(AttributeModifier.append("style", "; background-color: " +
                new PropertyModel<>(getModel(), ProgressBarDto.F_PROGRESS_COLOR).getObject()));
    }

    protected String getProgressBarContainerStyle() {
        return null;
    }
}
