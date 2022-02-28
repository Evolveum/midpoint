/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.data;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public class ServerResponse implements DebugDumpable {

    private final int responseCode;
    private final String message;
    private final String entity;

    public ServerResponse(int responseCode, String message, String entity) {
        this.responseCode = responseCode;
        this.message = message;
        this.entity = entity;
    }

    public String getMessage() {
        return message;
    }

    public String getEntity() {
        return entity;
    }

    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "responseCode", responseCode, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "message", message, indent + 1);
        if (entity != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "entity",
                    DebugUtil.fixIndentInMultiline(indent + 1, DebugDumpable.INDENT_STRING, entity), indent + 1);
        }
        return sb.toString();
    }

    public String getExceptionMessage() {
        return String.format("ID Match service returned %d: %s", responseCode, message);
    }
}
