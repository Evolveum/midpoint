/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.cache;

/**
 *
 */
public enum CacheType {

	LOCAL_REPO_OBJECT_CACHE, LOCAL_REPO_VERSION_CACHE, LOCAL_REPO_QUERY_CACHE,
	GLOBAL_REPO_OBJECT_CACHE, GLOBAL_REPO_QUERY_CACHE,
	LOCAL_FOCUS_CONSTRAINT_CHECKER_CACHE, LOCAL_SHADOW_CONSTRAINT_CHECKER_CACHE,
	LOCAL_ASSOCIATION_TARGET_SEARCH_EVALUATOR_CACHE,
	LOCAL_DEFAULT_SEARCH_EVALUATOR_CACHE

}
