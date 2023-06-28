package com.evolveum.midpoint.ninja.action.upgrade;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.util.LocalizableMessage;

public class VerificationResultItem {

    private final ValidationItem validationItem;

    private final UpgradeObjectResult upgradeResult;

    public VerificationResultItem(@NotNull ValidationItem validationItem, @Nullable UpgradeObjectResult result) {
        this.validationItem = validationItem;
        this.upgradeResult = result;
    }

    public ValidationItem getValidationItem() {
        return validationItem;
    }

    public UpgradePhase getPhase() {
        return upgradeResult != null ? upgradeResult.getPhase() : null;
    }

    public UpgradePriority getPriority() {
        return upgradeResult != null ? upgradeResult.getPriority() : null;
    }

    public UpgradeType getType() {
        return upgradeResult != null ? upgradeResult.getType() : null;
    }

    public String getIdentifier() {
        return upgradeResult != null ? upgradeResult.getIdentifier() : null;
    }

    public ObjectDelta getDelta() {
        return upgradeResult != null ? upgradeResult.getDelta() : null;
    }

    public OperationResultStatus getStatus() {
        return validationItem.getStatus();
    }

    public ItemPath getItemPath() {
        return validationItem.getItemPath();
    }

    public LocalizableMessage getMessage() {
        return validationItem.getMessage();
    }

    public boolean isChanged() {
        return upgradeResult != null ? upgradeResult.isChanged() : null;
    }
}
