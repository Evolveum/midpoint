/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

public interface Matchable<T extends Matchable<T>> {

    boolean match(T other);

    boolean matches(String regex);
}
