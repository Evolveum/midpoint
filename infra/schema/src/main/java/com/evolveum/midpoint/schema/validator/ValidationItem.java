/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.validator;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * @author semancik
 */
public record ValidationItem<T>(
        ValidationItemType type,
        ValidationItemStatus status,
        LocalizableMessage message,
        ItemPath path,
        T data)
        implements DebugDumpable, ShortDumpable {

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ValidationItem.class, indent);

        DebugUtil.debugDumpWithLabelToStringLn(sb, "type", type, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "status", status, indent + 1);
        DebugUtil.debugDumpWithLabelShortDumpLn(sb, "message", message, indent + 1);
        DebugUtil.debugDumpWithLabelShortDumpLn(sb, "itemPath", path, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "data", data, indent + 1);

        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (status != null) {
            sb.append(status);
            sb.append(" ");
        }
        if (path != null) {
            sb.append(path);
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
