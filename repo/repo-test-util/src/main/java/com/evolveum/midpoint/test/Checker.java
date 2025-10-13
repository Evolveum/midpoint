/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.util.exception.CommonException;

/**
 * @author Radovan Semancik
 *
 */
public interface Checker {

    /**
     * true = done
     * false = continue waiting
     */
    boolean check() throws CommonException;

    default void timeout() {
    }

}
