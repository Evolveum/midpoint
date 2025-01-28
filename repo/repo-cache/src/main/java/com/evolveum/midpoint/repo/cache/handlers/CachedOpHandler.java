/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

/**
 * Superclass for handlers for caching operations - `getObject`, `getVersion`, `search`.
 * Currently there's not nothing here so we might consider removing this class.
 *
 * @see CachedOpExecution
 */
abstract class CachedOpHandler extends BaseOpHandler {

}
