/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class BaseWorker<O extends Object, T extends Object> implements Runnable {

    public static final int CONSUMER_POLL_TIMEOUT = 2;

    private List<? extends BaseWorker> workers;

    protected BlockingQueue<T> queue;
    protected NinjaContext context;
    protected O options;

    protected OperationStatus operation;

    private boolean done;

    public BaseWorker(NinjaContext context, O options, BlockingQueue<T> queue, OperationStatus operation) {
        this(context, options, queue, operation, null);
    }

    public BaseWorker(NinjaContext context, O options, BlockingQueue<T> queue, OperationStatus operation,
                      List<? extends BaseWorker> workers) {
        this.queue = queue;
        this.context = context;
        this.options = options;
        this.operation = operation;
        this.workers = workers;
    }

    protected boolean shouldConsumerStop() {
        if (operation.isFinished()) {
            return true;
        }

        if (operation.isStarted()) {
            return false;
        }

        if (operation.isProducerFinished() && !queue.isEmpty()) {
            return false;
        }

        return true;
    }

    public boolean isDone() {
        return done;
    }

    public void markDone() {
        this.done = true;
    }

    protected boolean isWorkersDone() {
        if (workers == null) {
            return true;
        }

        for (BaseWorker worker : workers) {
            if (!worker.isDone()) {
                return false;
            }
        }

        return true;
    }
}
