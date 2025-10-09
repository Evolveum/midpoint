/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.Containerable;

/**
 * Handles iterative processes that concern containerables.
 */
@FunctionalInterface
public interface ContainerableResultHandler<C extends Containerable> extends ObjectHandler<C> {

}
