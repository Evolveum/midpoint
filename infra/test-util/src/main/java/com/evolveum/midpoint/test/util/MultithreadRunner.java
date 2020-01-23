/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

/**
 * Used mostly in tests to simplify error handling.
 *
 * @author semancik
 */
@FunctionalInterface
public interface MultithreadRunner {

    void run(int i) throws Exception;

}
