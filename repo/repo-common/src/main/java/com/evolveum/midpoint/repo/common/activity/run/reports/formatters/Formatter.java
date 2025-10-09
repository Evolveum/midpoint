/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
