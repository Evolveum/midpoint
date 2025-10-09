/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import com.evolveum.midpoint.ninja.action.worker.ExportConsumerWorker;
import com.evolveum.midpoint.ninja.action.worker.ExportPerObjectWorker;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Ninja action realizing "export" command.
 */
public class ExportRepositoryAction extends AbstractRepositorySearchAction<ExportOptions, Void> {

    @Override
    public String getOperationName() {
        return "export";
    }

    @Override
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        if (options.isSplitFiles()) {
            return () -> {
                new ExportPerObjectWorker(context, options, queue, operation).run();
                return null;
            };
        } else {
            return () -> {
                new ExportConsumerWorker(context, options, queue, operation).run();
                return null;
            };
        }
    }
}
