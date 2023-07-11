/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.validator;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * @author semancik
 */
public class ValidationItem implements DebugDumpable, ShortDumpable {

    private OperationResultStatus status;
    private LocalizableMessage message;
    private ItemPath itemPath;
    // TODO? line number?

    public OperationResultStatus getStatus() {
        return status;
    }

    public void setStatus(OperationResultStatus status) {
        this.status = status;
    }

    public LocalizableMessage getMessage() {
        return message;
    }

    public void setMessage(LocalizableMessage message) {
        this.message = message;
    }

    public ItemPath getItemPath() {
        return itemPath;
    }

    public void setItemPath(ItemPath itemPath) {
        this.itemPath = itemPath;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ValidationItem.class, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "status", status, indent + 1);
        DebugUtil.debugDumpWithLabelShortDumpLn(sb, "message", message, indent + 1);
        DebugUtil.debugDumpWithLabelShortDump(sb, "itemPath", itemPath, indent + 1);
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (status != null) {
            sb.append(status);
            sb.append(" ");
        }
        if (itemPath != null) {
            sb.append(itemPath);
            sb.append(" ");
        }
        if (message != null) {
            sb.append(message.getFallbackMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ValidationItem(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }
}
