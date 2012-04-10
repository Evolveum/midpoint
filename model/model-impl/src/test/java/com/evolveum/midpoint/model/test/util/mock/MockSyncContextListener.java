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

import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.synchronizer.SyncContextListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class MockSyncContextListener implements SyncContextListener {
	
	private static final Trace LOGGER = TraceManager.getTrace(MockSyncContextListener.class);

	private static final String SEPARATOR = "############################################################################";

	private SyncContext lastSyncContext;
	
	public SyncContext getLastSyncContext() {
		return lastSyncContext;
	}

	public void setLastSyncContext(SyncContext lastSyncContext) {
		this.lastSyncContext = lastSyncContext;
	}
	
	@Override
	public void beforeSync(SyncContext context) {
		LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT BEFORE SYNC\n{}\n"+SEPARATOR, context.dump());
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.synchronizer.SyncContextListener#afterFinish(com.evolveum.midpoint.model.SyncContext)
	 */
	@Override
	public void afterSync(SyncContext context) {
		LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT AFTER SYNC\n{}\n"+SEPARATOR, context.dump());
		lastSyncContext = context;
	}

}
