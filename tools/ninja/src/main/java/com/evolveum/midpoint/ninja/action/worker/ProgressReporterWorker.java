/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProgressReporterWorker<T> extends BaseWorker<Object, T> {

    public ProgressReporterWorker(NinjaContext context,
            Object options, BlockingQueue<T> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    public void run() {
        final Log log = context.getLog();
        log.info("Progress reporter starting");

        while (!shouldConsumerStop()) {
            if (operation.isStarted() || operation.isProducerFinished()) {
                operation.print(context.getLog());
            }

            try {
                //noinspection BusyWait
                Thread.sleep(NinjaUtils.COUNT_STATUS_LOG_INTERVAL);
            } catch (InterruptedException ex) {
                // ignored
            }
        }
    }
}
