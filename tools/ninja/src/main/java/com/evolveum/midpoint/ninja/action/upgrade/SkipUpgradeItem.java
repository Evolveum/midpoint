/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.Objects;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public class SkipUpgradeItem implements DebugDumpable {

    private final String path;

    private final String identifier;

    public SkipUpgradeItem(String path, String identifier) {
        this.path = path;
        this.identifier = identifier;
    }

    public String getPath() {
        return path;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "path", path, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "path", identifier, indent);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        SkipUpgradeItem that = (SkipUpgradeItem) o;
        return Objects.equals(path, that.path) && Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, identifier);
    }
}
