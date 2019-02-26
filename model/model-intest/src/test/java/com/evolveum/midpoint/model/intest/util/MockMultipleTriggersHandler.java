/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest.util;

import com.evolveum.midpoint.model.impl.trigger.MultipleTriggersHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class MockMultipleTriggersHandler implements MultipleTriggersHandler {

	public static final String HANDLER_URI = SchemaConstants.NS_MIDPOINT_TEST + "/mock-multiple-trigger-handler";

	protected static final Trace LOGGER = TraceManager.getTrace(MockMultipleTriggersHandler.class);

	private PrismObject<?> lastObject;
	private Collection<TriggerType> lastTriggers;
	private AtomicInteger invocationCount = new AtomicInteger(0);
	private long delay;
	private boolean failOnNextInvocation;

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

	public Collection<TriggerType> getLastTriggers() {
		return lastTriggers;
	}

	@Override
	public <O extends ObjectType> Collection<TriggerType> handle(PrismObject<O> object, Collection<TriggerType> triggers, RunningTask task,
			OperationResult result) {
		IntegrationTestTools.display("Mock multiple triggers handler called with " + object);
		lastTriggers = CloneUtil.cloneCollectionMembers(triggers);
		lastObject = object.clone();
		invocationCount.incrementAndGet();
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < delay && task.canRun()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// just ignore
			}
		}
		if (failOnNextInvocation) {
			failOnNextInvocation = false;
			throw new IllegalStateException("Failing as instructed");
		}
		return triggers;
	}

	public void reset() {
		lastObject = null;
		lastTriggers = null;
		invocationCount.set(0);
		delay = 0;
		failOnNextInvocation = false;
	}
}
