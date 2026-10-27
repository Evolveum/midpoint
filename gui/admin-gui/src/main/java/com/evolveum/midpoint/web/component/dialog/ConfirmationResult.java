/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.util.Describable;

/** Selected values and AI service decision supplied by a confirmation result model. */
public record ConfirmationResult<T extends Describable>(List<T> selectedOptions, boolean aiServiceRequested) implements Serializable {

    public ConfirmationResult {
        selectedOptions = List.copyOf(selectedOptions);
    }

    public static <T extends Describable> ConfirmationResult<T> empty() {
        return new ConfirmationResult<>(List.of(), false);
    }
}
