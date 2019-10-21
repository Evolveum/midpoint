/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.cache;

/**
 *
 */
public enum CacheType {

    LOCAL_REPO_OBJECT_CACHE, LOCAL_REPO_VERSION_CACHE, LOCAL_REPO_QUERY_CACHE,
    GLOBAL_REPO_OBJECT_CACHE, GLOBAL_REPO_VERSION_CACHE, GLOBAL_REPO_QUERY_CACHE,
    LOCAL_FOCUS_CONSTRAINT_CHECKER_CACHE, LOCAL_SHADOW_CONSTRAINT_CHECKER_CACHE,
    LOCAL_ASSOCIATION_TARGET_SEARCH_EVALUATOR_CACHE,
    LOCAL_DEFAULT_SEARCH_EVALUATOR_CACHE

}
