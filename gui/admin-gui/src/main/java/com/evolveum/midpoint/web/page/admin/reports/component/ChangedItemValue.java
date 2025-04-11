/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import java.io.Serializable;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;

public record ChangedItemValue(
        ModificationType modificationType,
        PrismValue value) implements Serializable {

    @Override
    public String toString() {
        return modificationType + ": " + value;
    }
}
