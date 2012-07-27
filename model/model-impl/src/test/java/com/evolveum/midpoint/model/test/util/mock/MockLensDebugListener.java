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

import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensDebugListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

/**
 * @author semancik
 *
 */
public class MockLensDebugListener implements LensDebugListener {
	
	private static final Trace LOGGER = TraceManager.getTrace(MockLensDebugListener.class);

	private static final String SEPARATOR = "############################################################################";

	private LensContext lastSyncContext;
	
	public <F extends ObjectType, P extends ObjectType>  LensContext<F, P> getLastSyncContext() {
		return lastSyncContext;
	}

	public <F extends ObjectType, P extends ObjectType> void setLastSyncContext(LensContext<F, P> lastSyncContext) {
		this.lastSyncContext = lastSyncContext;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.lens.LensDebugListener#beforeSync(com.evolveum.midpoint.model.lens.LensContext)
	 */
	@Override
	public <F extends ObjectType, P extends ObjectType> void beforeSync(LensContext<F, P> context) {
		LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT BEFORE SYNC\n{}\n"+SEPARATOR, context.dump());
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.lens.LensDebugListener#afterSync(com.evolveum.midpoint.model.lens.LensContext)
	 */
	@Override
	public <F extends ObjectType, P extends ObjectType> void afterSync(LensContext<F, P> context) {
		LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT AFTER SYNC\n{}\n"+SEPARATOR, context.dump());
		lastSyncContext = context;
	}

}
