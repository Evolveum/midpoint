/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.factory;

/**
 * @author skublik
 */
public interface RealValuable<T> {

    T getRealValue();

    void setRealValue(T object);
}
