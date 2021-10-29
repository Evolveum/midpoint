/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import com.evolveum.midpoint.prism.ItemDefinition;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Experimental
public interface Formatter {

    @NotNull List<String> formatHeader(@NotNull ItemDefinition<?> def);

    @NotNull List<String> formatValue(Object object);

    @NotNull List<String> formatMultiValue(Collection<?> values);
}
