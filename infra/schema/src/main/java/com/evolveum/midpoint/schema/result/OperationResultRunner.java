/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
