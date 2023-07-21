package com.evolveum.midpoint.schema.validator;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public class UpgradeValidationResult implements DebugDumpable {

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

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(UpgradeValidationResult.class, indent);
        DebugUtil.debugDumpWithLabel(sb, "items", items, indent + 1);
        return sb.toString();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean hasChanges() {
        return items.stream().anyMatch(i -> i.isChanged());
    }

    public boolean hasCritical() {
        return items.stream().anyMatch(i -> i.getPriority() != null && i.getPriority() == UpgradePriority.CRITICAL);
    }
}
