/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
