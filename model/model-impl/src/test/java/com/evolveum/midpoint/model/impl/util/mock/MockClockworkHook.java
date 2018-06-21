/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.util.mock;

import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class MockClockworkHook implements ChangeHook, DebugDumpable {

	private List<LensContext<?>> contexts = new ArrayList<>();
	private LensContext<?> lastAsyncContext = null;
	private boolean record = false;
	private boolean asynchronous = false;

	public boolean isRecord() {
		return record;
	}

	public void setRecord(boolean record) {
		this.record = record;
	}

	public boolean isAsynchronous() {
		return asynchronous;
	}

	public void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}

	public List<LensContext<?>> getContexts() {
		return contexts;
	}

	public void reset() {
		System.out.println("RESETING");
		record = false;
		asynchronous = false;
		clear();
	}

	public void clear() {
		contexts.clear();
		lastAsyncContext = null;
	}

	public LensContext<?> getLastAsyncContext() {
		return lastAsyncContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.hooks.ChangeHook#invoke(com.evolveum.midpoint.model.api.context.ModelContext, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public HookOperationMode invoke(@NotNull ModelContext context, @NotNull Task task, @NotNull OperationResult result) {
		assertTrue("Unexpected INITIAL state of the context in the hook", context.getState() != ModelState.INITIAL);
		// OK to rely on implementation here. This is an implementation test.
		if (!(context instanceof LensContext)) {
			throw new IllegalArgumentException("WHOOPS! The context is of type "+context.getClass()+" which we haven't expected");
		}
		LensContext lensContext = (LensContext)context;
		if (record) {
			contexts.add(lensContext.clone());
		}
		if (asynchronous) {
			lastAsyncContext = lensContext;
			return HookOperationMode.BACKGROUND;
		}
		return HookOperationMode.FOREGROUND;
	}

    @Override
    public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result) {
        // do nothing
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.hooks.ChangeHook#postChange(java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
//	@Override
//	public void postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task,
//			OperationResult result) {
//		// TODO Auto-generated method stub
//
//	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
	 */
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("MockClockworkHook: "+contexts.size()+" contexts\n");
		sb.append(DebugUtil.debugDump(contexts, indent + 1));
		return sb.toString();
	}

}
