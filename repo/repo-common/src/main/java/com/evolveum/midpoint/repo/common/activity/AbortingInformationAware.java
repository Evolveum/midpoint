/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAbortingInformationType;

import org.jetbrains.annotations.NotNull;

/** An exception that carries details about activity abortion (to be stored into {@link ActivityRunResult}). */
public interface AbortingInformationAware {

    @NotNull ActivityAbortingInformationType getAbortingInformation();
}
