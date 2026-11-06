/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.validator;

import java.io.Serializable;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;

import org.jspecify.annotations.NonNull;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;

/** Reports conflicting iteration limits without invalidating the input or preventing saving. */
public class IterationLimitValidator implements INullAcceptingValidator<Integer> {

    private final IModel<PrismPropertyWrapper<Integer>> itemModel;
    private final FormComponent<Integer> input;
    private boolean warning;

    public IterationLimitValidator(IModel<PrismPropertyWrapper<Integer>> itemModel, FormComponent<Integer> input) {
        this.itemModel = itemModel;
        this.input = input;
    }

    @Override
    public void validate(IValidatable<Integer> validatable) {
        refreshWarning(validatable.getValue());
    }

    public boolean hasWarning() {
        return warning;
    }

    /** Updates the conflict warning while preserving unrelated feedback. */
    public void refreshWarning(Integer value) {
        warning = false;
        // Preserve conversion errors and feedback from other validators.
        input.getFeedbackMessages().clear(message -> message.getMessage() instanceof LimitWarning);
        var item = itemModel.getObject();
        if (value == null || item == null || item.getParent() == null
                || item.getParent().getStatus() == ValueStatus.DELETED
                || item.getValues().stream().noneMatch(v -> v.getStatus() != ValueStatus.DELETED)) {
            return;
        }
        var siblingName = IterationSpecificationType.F_END.equivalent(item.getItemName())
                ? IterationSpecificationType.F_MAX_ITERATIONS : IterationSpecificationType.F_END;
        try {
            PrismPropertyWrapper<Integer> sibling = item.getParent().findProperty(siblingName);
            if (sibling != null && sibling.getValues().stream()
                    .anyMatch(v -> v.getStatus() != ValueStatus.DELETED && v.getRealValue() != null)) {
                warning = true;
                input.warn(new LimitWarning(translate("IterationLimitValidator.conflictingLimits")));
            }
        } catch (SchemaException e) {
            throw new SystemException("Couldn't read iteration limits", e);
        }
    }

    /** Identifies iteration warnings so they can be cleared separately from other feedback. */
    private record LimitWarning(String text) implements Serializable {
        @Override
        public @NonNull String toString() {
            return text;
        }
    }
}
