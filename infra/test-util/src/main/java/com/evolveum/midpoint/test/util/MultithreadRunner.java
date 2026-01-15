/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.util;

/**
 * Used mostly in tests to simplify error handling.
 *
 * @author semancik
 */
@FunctionalInterface
public interface MultithreadRunner {

    void run(int threadIndex) throws Exception;

}
