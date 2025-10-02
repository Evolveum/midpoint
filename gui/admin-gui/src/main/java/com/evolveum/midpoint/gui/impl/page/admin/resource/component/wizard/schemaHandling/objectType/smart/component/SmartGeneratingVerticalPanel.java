/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.StatusRowRecord;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Panel for monitoring and controlling a "smart generating" task.
 * <p>
 * Shows progress (elapsed time, status rows) and provides actions
 * to run in background, suspend/resume, or discard the task. Uses
 * an {@link AbstractAjaxTimerBehavior} to poll until the task
 * finishes, fails, or is suspended.
 * <p>
 * Subclasses may override hooks like
 * {@link #onFinishActionPerform(AjaxRequestTarget)},
 * {@link #onDiscardPerform(AjaxRequestTarget)},
 * {@link #onRunInBackgroundPerform(AjaxRequestTarget)} or
 * {@link #createButtons(org.apache.wicket.markup.repeater.RepeatingView)}
 * to customize behavior and appearance.
 */
public class SmartGeneratingVerticalPanel extends SmartGeneratingPanel {

    public SmartGeneratingVerticalPanel(String id, IModel<SmartGeneratingDto> model, boolean isWizardPanel) {
        super(id, model, isWizardPanel);
    }

    /** Null-safe accessor for rows. */
    protected List<StatusRowRecord> getSafeRows() {
        SmartGeneratingDto dto = getModelObject();
        if (dto != null) {
            List<StatusRowRecord> statusRows = dto.getStatusRows(getPageBase());
            if (statusRows != null && !statusRows.isEmpty()) {
                return Collections.singletonList(statusRows.get(statusRows.size() - 1));
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected @Nullable String getTitleCssClass() {
        return null;
    }
}
