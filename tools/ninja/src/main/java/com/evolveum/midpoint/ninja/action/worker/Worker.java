/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.worker;

import java.util.concurrent.Callable;

// todo implement
public abstract class Worker<R> implements Callable<R> {

    @Override
    public R call() throws Exception {
        try {
            init();

            execute();
        } finally {
            destroy();
        }

        return null;
    }

    private void init() {

    }

    private void execute() {

    }

    private void destroy() {

    }
}
