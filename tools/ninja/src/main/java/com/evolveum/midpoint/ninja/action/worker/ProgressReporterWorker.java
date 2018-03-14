/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;

import java.util.concurrent.BlockingQueue;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProgressReporterWorker implements Runnable {

    private BlockingQueue<PrismObject> queue;
    private OperationStatus operation;
    private Log log;

    public ProgressReporterWorker(BlockingQueue<PrismObject> queue, OperationStatus operation, Log log) {
        this.queue = queue;
        this.operation = operation;
        this.log = log;
    }

    @Override
    public void run() {
        while (!stop()) {
            if (operation.isStarted() || operation.isProducerFinished()) {
                log.info("Processed: {}, skipped: {}, avg: {}ms/req, avg: {}req/s",
                        operation.getCount(), operation.getSkipped(),
                        NinjaUtils.DECIMAL_FORMAT.format(operation.getAvg()),NinjaUtils.DECIMAL_FORMAT.format(operation.getAvgReqSpeed()));

                operation.lastPrintoutNow();
            }

            try {
                Thread.sleep(NinjaUtils.COUNT_STATUS_LOG_INTERVAL);
            } catch (InterruptedException ex) {
            }
        }
    }

    private boolean stop() {
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
}