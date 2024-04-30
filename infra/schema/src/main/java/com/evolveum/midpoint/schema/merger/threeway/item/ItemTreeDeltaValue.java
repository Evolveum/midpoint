/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public class ItemTreeDeltaValue<V extends PrismValue> implements DebugDumpable {

    // todo improve
    private List<Object> naturalKey;

    private V value;

    private ModificationType modificationType;

    public ItemTreeDeltaValue(V value, ModificationType modificationType) {
        this.value = value;
        this.modificationType = modificationType;
    }

    @NotNull
    public List<Object> getNaturalKey() {
        if (naturalKey == null) {
            naturalKey = new ArrayList<>();
        }
        return naturalKey;
    }

    public void setNaturalKey(@NotNull List<Object> naturalKey) {
        this.naturalKey = naturalKey;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public ModificationType getModificationType() {
        return modificationType;
    }

    public void setModificationType(ModificationType modificationType) {
        this.modificationType = modificationType;
    }

    @Override
    public String toString() {
        return debugDump();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        debugDumpTitle(sb, indent);
        debugDumpContent(sb, indent);

        return sb.toString();
    }

    protected String debugDumpShortName() {
        return getClass().getSimpleName();
    }

    protected void debugDumpTitle(StringBuilder sb, int indent) {
        sb.append(DebugUtil.debugDump(debugDumpShortName(), indent));
        DebugUtil.debugDumpWithLabelLn(sb, "modification", modificationType, indent);
    }

    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "naturalKey", naturalKey, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "value", value, indent + 1);
    }
}
