/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.worker.VerifyConsumerWorker;
import com.evolveum.midpoint.ninja.opts.VerifyOptions;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;

/**
 * Created by Viliam Repan (lazyman).
 */
public class VerifyRepositoryAction extends AbstractRepositorySearchAction<VerifyOptions> {

    @Override
    protected String getOperationShortName() {
        return "verify";
    }

    @Override
    protected Runnable createConsumer(BlockingQueue<PrismObject> queue, OperationStatus operation) {
        return new VerifyConsumerWorker(context, options, queue, operation);
    }

}
