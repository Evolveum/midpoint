/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.util;

import com.evolveum.midpoint.model.impl.trigger.SingleTriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Radovan Semancik
 *
 */
public class MockTriggerHandler implements SingleTriggerHandler {

    public static final String HANDLER_URI = SchemaConstants.NS_MIDPOINT_TEST + "/mock-trigger-handler";

    protected static final Trace LOGGER = TraceManager.getTrace(MockTriggerHandler.class);

    private PrismObject<?> lastObject;
    private final AtomicInteger invocationCount = new AtomicInteger(0);
    private long delay;
    private boolean failOnNextInvocation;
    private boolean idempotent;

    public PrismObject<?> getLastObject() {
        return lastObject;
    }

    public int getInvocationCount() {
        return invocationCount.get();
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public boolean isFailOnNextInvocation() {
        return failOnNextInvocation;
    }

    public void setFailOnNextInvocation(boolean failOnNextInvocation) {
        this.failOnNextInvocation = failOnNextInvocation;
    }

    @Override
    public <O extends ObjectType> void handle(PrismObject<O> object, TriggerType trigger, RunningTask task, OperationResult result) {
        IntegrationTestTools.display("Mock trigger handler called with " + object);
        lastObject = object.clone();
        invocationCount.incrementAndGet();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < delay && task.canRun()) {
            try {
                //noinspection BusyWait
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // just ignore
            }
        }
        if (failOnNextInvocation) {
            failOnNextInvocation = false;
            throw new IllegalStateException("Failing as instructed");
        }
    }
    public void reset() {
        lastObject = null;
        invocationCount.set(0);
        delay = 0;
        failOnNextInvocation = false;
    }

    @Override
    public boolean isIdempotent() {
        return idempotent;
    }

    public void setIdempotent(boolean idempotent) {
        this.idempotent = idempotent;
    }
}
