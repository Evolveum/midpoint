package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class UpgradeObjectsConsumerWorker extends BaseWorker<UpgradeObjectsOptions, ObjectType> {

    public UpgradeObjectsConsumerWorker(
            NinjaContext context, UpgradeObjectsOptions options,
            BlockingQueue<ObjectType> queue, OperationStatus operation) {

        super(context, options, queue, operation);
    }

    @Override
    public void run() {
        // todo implement
    }
}
