/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
