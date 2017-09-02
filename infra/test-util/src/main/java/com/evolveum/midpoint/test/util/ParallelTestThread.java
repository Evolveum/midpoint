/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class ParallelTestThread extends Thread {

	private static final Trace LOGGER = TraceManager.getTrace(ParallelTestThread.class);
	
	private int i;
	private MultithreadRunner target;
	private Throwable exception;

	public ParallelTestThread(int i, MultithreadRunner target) {
		super();
		this.i = i;
		this.target = target;
	}

	@Override
	public void run() {
		try {
			target.run(i);
		} catch (RuntimeException | Error e) {
			recordException(e);
			throw e;
		} catch (Throwable e) {
			recordException(e);
			throw new SystemException(e.getMessage(), e);
		}
	}

	public Throwable getException() {
		return exception;
	}

	public void recordException(Throwable e) {
		LOGGER.error("Test thread failed: {}", e.getMessage(), e);
		this.exception = e;
	}
	
}
