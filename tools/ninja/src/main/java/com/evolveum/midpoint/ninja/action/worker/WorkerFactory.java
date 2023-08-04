/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.worker;

import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.impl.NinjaContext;

@FunctionalInterface
public interface WorkerFactory<O, W> {

    W create(NinjaContext context, Operation operation, BlockingQueue<O> queue) throws Exception;
}
