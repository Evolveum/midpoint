/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
