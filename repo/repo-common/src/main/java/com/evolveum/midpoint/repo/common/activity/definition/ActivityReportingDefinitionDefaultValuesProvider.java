/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityEventLoggingOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityItemCountingOptionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityOverallItemCountingOptionType;

import org.jetbrains.annotations.NotNull;

/** Provides default values for some of the {@link ActivityReportingDefinition} items. */
public interface ActivityReportingDefinitionDefaultValuesProvider {

    @NotNull ActivityItemCountingOptionType getDetermineBucketSizeDefault();

    @NotNull ActivityOverallItemCountingOptionType getDetermineOverallSizeDefault();

    @NotNull ActivityEventLoggingOptionType getBucketCompletionLoggingDefault();

    @NotNull ActivityEventLoggingOptionType getItemCompletionLoggingDefault();
}
