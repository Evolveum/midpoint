package com.evolveum.midpoint.schema.validator;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class UpgradeValidationResult {

    private ValidationResult result;

    private final List<UpgradeValidationItem> items = new ArrayList<>();

    public UpgradeValidationResult(@NotNull ValidationResult result) {
        this.result = result;
    }

    public @NotNull ValidationResult getResult() {
        return result;
    }

    public @NotNull List<UpgradeValidationItem> getItems() {
        return items;
    }
}
