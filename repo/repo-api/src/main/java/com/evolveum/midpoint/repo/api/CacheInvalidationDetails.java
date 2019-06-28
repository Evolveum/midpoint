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

package com.evolveum.midpoint.repo.api;

/**
 * Provides more specific information e.g. about the nature of the change that triggered the invalidation event.
 * For example, invalidations caused by repo add/modify/delete operation can contain information about the object
 * added/modified/deleted to help with the evaluation of the impact of the operation on cached query results.
 *
 * Usually it can be safely ignored by individual caches.
 *
 * This is very experimental, to say it mildly. It will probably change in the future.
 * (E.g. there is currently no mechanism how to transfer this data throughout the cluster.)
 */
public interface CacheInvalidationDetails {
}
