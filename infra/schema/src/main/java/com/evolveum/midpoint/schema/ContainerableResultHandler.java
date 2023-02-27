/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.Containerable;

/**
 * Handles iterative processes that concern containerables.
 */
@FunctionalInterface
public interface ContainerableResultHandler<C extends Containerable> extends ObjectHandler<C> {

}
