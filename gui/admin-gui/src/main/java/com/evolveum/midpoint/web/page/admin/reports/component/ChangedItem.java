/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

public record ChangedItem(String oldValue, String newValue) {

    @Override
    public String toString() {
        if (oldValue == null) {
            return newValue;
        }

        return oldValue + " -> " + newValue;
    }
}
