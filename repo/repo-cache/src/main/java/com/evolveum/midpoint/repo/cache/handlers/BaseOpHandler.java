/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.invalidation.Invalidator;

/**
 * Base for all operation handlers.
 */
@Component
abstract public class BaseOpHandler {

    @Autowired PrismContext prismContext;
    @Autowired RepositoryService repositoryService;
    @Autowired GlobalQueryCache globalQueryCache;
    @Autowired GlobalObjectCache globalObjectCache;
    @Autowired GlobalVersionCache globalVersionCache;
    @Autowired Invalidator invalidator;
    @Autowired CacheSetAccessInfoFactory cacheSetAccessInfoFactory;
    @Autowired CacheUpdater cacheUpdater;
}
