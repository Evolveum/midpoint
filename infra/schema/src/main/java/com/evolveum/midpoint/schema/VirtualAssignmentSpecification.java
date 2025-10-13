/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VirtualAssignmentSpecification<R extends AbstractRoleType>(
        @NotNull Class<R> type,
        @Nullable ObjectFilter filter) {
}
