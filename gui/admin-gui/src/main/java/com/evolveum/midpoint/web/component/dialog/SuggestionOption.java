/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record SuggestionOption(
        List<ConfirmationOption<DataAccessPermission>> confirmationOptions,
        boolean requiresAiService) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static SuggestionOption of(
            List<ConfirmationOption<DataAccessPermission>> options) {
        return new SuggestionOption(options, false);
    }

    public static SuggestionOption aiOnly(
            List<ConfirmationOption<DataAccessPermission>> options) {
        return new SuggestionOption(options, true);
    }

    public static SuggestionOption empty() {
        return new SuggestionOption(List.of(), false);
    }


}
