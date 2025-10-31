/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAbortingInformationType;

import org.jetbrains.annotations.NotNull;

/** An exception that carries details about activity abortion (to be stored into {@link ActivityRunResult}). */
public interface AbortingInformationAware {

    @NotNull ActivityAbortingInformationType getAbortingInformation();
}
