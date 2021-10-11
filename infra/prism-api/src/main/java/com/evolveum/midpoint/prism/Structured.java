/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Interface for properties that have inner structur, such as PolyString.
 * This was created due to a limitation that we cannot make every structured
 * data into a container (yet).
 *
 * This is a temporary solution in 3.x and 4.x. It should be gone in 5.x.
 * Do not realy on this with any new development.
 *
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface Structured {

    Object resolve(ItemPath subpath);

}
