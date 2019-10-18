/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.result;

/**
 * @author semancik
 *
 */
public class OperationResultRunner {

    public static void run(OperationResult result, Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        }
    }

}
