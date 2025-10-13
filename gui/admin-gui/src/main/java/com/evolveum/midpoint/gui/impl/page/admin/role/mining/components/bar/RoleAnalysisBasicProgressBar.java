/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisProgressBarDto;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public class RoleAnalysisBasicProgressBar extends AbstractRoleAnalysisProgressBar<RoleAnalysisProgressBarDto> {

    public RoleAnalysisBasicProgressBar(String id, IModel<RoleAnalysisProgressBarDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public boolean isInline() {
        return false;
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
        super.initProgressValueLabel(componentId, container);
    }

    protected boolean isWider() {
        return false;
    }
}
