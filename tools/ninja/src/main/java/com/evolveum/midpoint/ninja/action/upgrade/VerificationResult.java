package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.schema.validator.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class VerificationResult {

    private ValidationResult result;

    private List<VerificationResultItem> items;

    public VerificationResult(ValidationResult result) {
        this.result = result;
    }

    public List<VerificationResultItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<VerificationResultItem> items) {
        this.items = items;
    }
}
