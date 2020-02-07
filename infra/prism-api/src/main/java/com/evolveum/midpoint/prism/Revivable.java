/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface Revivable {

    void revive(PrismContext prismContext) throws SchemaException;

}
