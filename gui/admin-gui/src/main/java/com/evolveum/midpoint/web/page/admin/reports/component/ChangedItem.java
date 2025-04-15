/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

public record ChangedItem(@NotNull ItemPath path,
                          @NotNull List<PrismValue> oldValues,
                          @NotNull List<ChangedItemValue> newValues) implements Serializable {

}
