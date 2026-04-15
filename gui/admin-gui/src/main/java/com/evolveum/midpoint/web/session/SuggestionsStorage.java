/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.web.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Stores UI "suggestions" settings in session.
 */
public class SuggestionsStorage implements Serializable, DebugDumpable {

    @Serial private static final long serialVersionUID = 1L;

    private static final boolean DEFAULT_ENABLED = false;

    public enum SuggestionType {
        INBOUND_MAPPING,
        OUTBOUND_MAPPING,
        OBJECT_TYPE,
        CORRELATION,
        ASSOCIATION
    }

    private final Map<SuggestionType, Boolean> enabled = new EnumMap<>(SuggestionType.class);

    public boolean isEnabled(SuggestionType type) {
        return enabled.getOrDefault(type, DEFAULT_ENABLED);
    }

    public void setEnabled(SuggestionType type, boolean value) {
        enabled.put(type, value);
    }

    public Map<SuggestionType, Boolean> getEnabledMap() {
        return enabled;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("SuggestionsStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "enabled", enabled, indent + 1);
        return sb.toString();
    }
}
