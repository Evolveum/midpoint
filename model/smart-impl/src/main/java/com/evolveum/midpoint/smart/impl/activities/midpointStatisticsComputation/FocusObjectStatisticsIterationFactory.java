/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.midpointStatisticsComputation;

import javax.xml.namespace.QName;

import org.checkerframework.framework.qual.QualifierArgument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.RepositoryService;

/**
 * Factory for creating {@link FocusObjectsByAssociatedShadowsIteration} instances.
 */
@Component
public class FocusObjectStatisticsIterationFactory {

    private final RepositoryService repositoryService;

    public FocusObjectStatisticsIterationFactory(
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Creates a new iteration instance for the given focus type.
     *
     * @param focusTypeClass The focus type class to search for
     * @return A new iteration instance
     */
    public FocusObjectsByAssociatedShadowsIteration iterationForType(QName focusTypeClass) {
        return new FocusObjectsByAssociatedShadowsIteration(repositoryService, focusTypeClass);
    }
}