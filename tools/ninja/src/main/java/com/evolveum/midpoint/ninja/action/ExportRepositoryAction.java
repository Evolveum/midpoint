/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.worker.ExportConsumerWorker;
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
    protected Runnable createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return new ExportConsumerWorker(context, options, queue, operation);
    }
}
