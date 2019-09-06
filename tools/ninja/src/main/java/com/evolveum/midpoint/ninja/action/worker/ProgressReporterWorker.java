/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;

import java.util.concurrent.BlockingQueue;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProgressReporterWorker extends BaseWorker<Object, PrismObject> {

    public ProgressReporterWorker(NinjaContext context, Object options, BlockingQueue<PrismObject> queue,
                                  OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    public void run() {
        while (!shouldConsumerStop()) {
            if (operation.isStarted() || operation.isProducerFinished()) {
                operation.print(context.getLog());
            }

            try {
                Thread.sleep(NinjaUtils.COUNT_STATUS_LOG_INTERVAL);
            } catch (InterruptedException ex) {
            }
        }
    }
}
