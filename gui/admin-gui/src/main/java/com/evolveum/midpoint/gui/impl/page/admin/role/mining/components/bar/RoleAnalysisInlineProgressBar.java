/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisProgressBarDto;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

public class RoleAnalysisInlineProgressBar extends AbstractRoleAnalysisProgressBar<RoleAnalysisProgressBarDto> {

    public RoleAnalysisInlineProgressBar(String id, IModel<RoleAnalysisProgressBarDto> model) {
        super(id, model);
    }

    @Override
    public boolean isInline() {
        return true;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected boolean isTitleContainerVisible() {
        return true;
    }

    @Override
    protected String getProgressBarContainerCssClass() {
        return null;
    }

    @Override
    protected void initProgressValueLabel(String componentId, @NotNull WebMarkupContainer container) {
        Label progressBarText = new Label(componentId,
                new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_ACTUAL_VALUE).getObject() + "%");
        progressBarText.setOutputMarkupId(true);
        progressBarText.add(AttributeModifier.replace("style", getProgressBarPercentageCssStyle()));
        add(progressBarText);
    }

    protected boolean isWider() {
        return false;
    }
}
