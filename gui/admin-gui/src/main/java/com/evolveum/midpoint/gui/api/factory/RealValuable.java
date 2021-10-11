/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.factory;

/**
 * @author skublik
 */
public interface RealValuable<T> {

    T getRealValue();

    void setRealValue(T object);
}
