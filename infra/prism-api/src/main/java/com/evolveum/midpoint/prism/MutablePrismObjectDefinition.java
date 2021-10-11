/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 *
 */
public interface MutablePrismObjectDefinition<O extends Objectable> extends PrismObjectDefinition<O>, MutablePrismContainerDefinition<O> {

}
