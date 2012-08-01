/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.test.util.mock;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

/**
 * @author semancik
 *
 */
public class MockClockworkHook implements ChangeHook, Dumpable, DebugDumpable {
	
	private List<LensContext<?,?>> contexts = new ArrayList<LensContext<?,?>>();
	private LensContext<?,?> lastAsyncContext = null;
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

	public List<LensContext<?, ?>> getContexts() {
		return contexts;
	}
	
	public void reset() {
		record = false;
		asynchronous = false;
		clear();
	}

	public void clear() {
		contexts.clear();
		lastAsyncContext = null;
	}

	public LensContext<?, ?> getLastAsyncContext() {
		return lastAsyncContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.hooks.ChangeHook#invoke(com.evolveum.midpoint.model.api.context.ModelContext, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public HookOperationMode invoke(ModelContext context, Task task, OperationResult result) {
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.hooks.ChangeHook#postChange(java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task,
			OperationResult result) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.Dumpable#dump()
	 */
	@Override
	public String dump() {
		return debugDump();
	}

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
