/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.schema.util.DiagnosticContext;

/**
 * @author semancik
 */
public interface RepositoryPerformanceMonitor extends DiagnosticContext {

    void recordRepoOperation(long durationMillis);

}
