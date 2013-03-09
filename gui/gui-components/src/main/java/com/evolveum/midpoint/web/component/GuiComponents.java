/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component;

import org.apache.commons.lang.Validate;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author lazyman
 */
public class GuiComponents {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    public static void destroy() {
        EXECUTOR.shutdownNow();
    }

    public static Future<?> submitRunnable(Runnable runnable) {
        Validate.notNull(runnable, "Runnable must not be null.");

        return EXECUTOR.submit(runnable);
    }

    public static <T> Future<T> submitCallable(Callable<T> callable) {
        Validate.notNull(callable, "Callable must not be null.");

        return EXECUTOR.submit(callable);
    }
}
