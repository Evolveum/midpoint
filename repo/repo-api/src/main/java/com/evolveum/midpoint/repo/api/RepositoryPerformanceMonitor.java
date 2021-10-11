/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.schema.util.DiagnosticContext;

/**
 * @author semancik
 */
public interface RepositoryPerformanceMonitor extends DiagnosticContext {

    void recordRepoOperation(long durationMillis);

}
