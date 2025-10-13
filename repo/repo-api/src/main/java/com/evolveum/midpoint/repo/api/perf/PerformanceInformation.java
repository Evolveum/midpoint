/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api.perf;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryPerformanceInformationType;

import java.util.Map;

/**
 *
 */
public interface PerformanceInformation extends DebugDumpable, Cloneable {

    void clear();

    Map<String, OperationPerformanceInformation> getAllData();

    RepositoryPerformanceInformationType toRepositoryPerformanceInformationType();

    int getInvocationCount(String operation);

    PerformanceInformation clone();
}
