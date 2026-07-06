/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper.step;

import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsContentPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsDto;
import com.evolveum.midpoint.web.component.dialog.steper.BasicPopupStepPanel;
import com.evolveum.midpoint.web.component.util.Describable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

/**
 * Popup step that displays a confirmation message together with a set of
 * selectable confirmationOptions.
 *
 * <p>The step is backed by a {@link ConfirmationWithOptionsDto} and provides
 * access to the selected confirmationOptions through
 * {@link #getSelectedOptionsModel()}.</p>
 *
 * <p>When the user proceeds to the next step, the selected confirmationOptions are passed
 * to {@link #confirmationPerformed(AjaxRequestTarget, IModel)} allowing
 * subclasses to perform initialization, validation, or other actions before
 * the popup flow continues.</p>
 *
 * @param <T> type represented by the confirmation confirmationOptions
 */
public class ConfirmationWithOptionsStepPanel<T extends Describable> extends BasicPopupStepPanel<ConfirmationWithOptionsDto<T>> {

    @Serial private static final long serialVersionUID = 1L;

    public ConfirmationWithOptionsStepPanel(IModel<ConfirmationWithOptionsDto<T>> model) {
        super(model);
    }

    public @NotNull IModel<List<ConfirmationOption<T>>> getSelectedOptionsModel() {
        return () -> getModelObject().getConfirmationOptions().stream()
                .filter(ConfirmationOption::isSelected)
                .toList();
    }

    @Override
    protected WebMarkupContainer createContentPanel(String id) {
        return new ConfirmationWithOptionsContentPanel<>(id, getModel());
    }

    @Override
    public IModel<String> getTitle() {
        return getModelObject().getConfirmationTitle();
    }

    @Override
    public IModel<String> getSubTitle() {
        return getModelObject().getConfirmationSubtitle();
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        confirmationPerformed(target, getSelectedOptionsModel());
        return true;
    }

    protected void confirmationPerformed(
            AjaxRequestTarget target,
            IModel<List<ConfirmationOption<T>>> confirmedOptions) {
    }

    @Override
    public IModel<String> getNextLabel() {
        return createStringResource("SmartSuggestConfirmationPanel.allowAndContinue");
    }
}
