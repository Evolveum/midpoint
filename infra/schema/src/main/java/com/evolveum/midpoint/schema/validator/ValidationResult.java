/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.validator;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 */
public class ValidationResult implements DebugDumpable {

    private List<ValidationItem> items = new ArrayList<>();

    public List<ValidationItem> getItems() {
        return items;
    }

    public void addItem(ValidationItem item) {
        items.add(item);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ValidationResult.class, indent);
        DebugUtil.debugDump(sb, items, indent + 1, false);
        return sb.toString();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

}
